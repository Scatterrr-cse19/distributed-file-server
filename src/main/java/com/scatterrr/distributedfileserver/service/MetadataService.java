package com.scatterrr.distributedfileserver.service;

import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.scatterrr.distributedfileserver.dto.*;
import com.scatterrr.distributedfileserver.model.Metadata;
import com.scatterrr.distributedfileserver.repository.FileServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataService {

    private final FileServerRepository fileServerRepository;
    private final FileManager fileManager;
    private final MerkleTree merkleTree;
    private final PeerAwareInstanceRegistry registry;

    private final WebClient.Builder webClientBuilder;

    public void saveMetadata(MultipartFile file) throws Exception{
        ArrayList<byte[]> chunks = fileManager.chunkFile(file);
        String merkleRootHash = merkleTree.createMerkleTree(chunks);
        String firstChunkUrl = distributeChunks(chunks, file.getOriginalFilename(), merkleRootHash);
        Metadata metadata = Metadata.builder()
                .fileName(file.getOriginalFilename())
                .numberOfChunks(chunks.size())
                .locationOfFirstChunk(firstChunkUrl)
                .merkleRootHash(merkleRootHash)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .build();
        fileServerRepository.save(metadata);
    }

    public Metadata getMetadata(String fileName) {
        return fileServerRepository.findById(fileName).orElse(null);
    }

    public ChunksResponse retrieveFile(String fileName, boolean allowTampered) {
        Metadata metadata = getMetadata(fileName);
        try {
            ArrayList<byte[]> chunks = getChunks(metadata.getLocationOfFirstChunk(), fileName);
            String merkleRootHash = merkleTree.createMerkleTree(chunks);
            // check if merkleRootHash is equal to metadata.getMerkleRootHash(), hence the file is authentic
            boolean isAuthentic =  merkleRootHash.equals(metadata.getMerkleRootHash());
            if (!isAuthentic && !allowTampered){
                throw new TamperedMetadataException("Chunks are tampered");
            }
            return ChunksResponse.builder()
                    .chunks(fileManager.mergeChunks(chunks))
                    .isAuthenticated(isAuthentic)
                    .build();
        } catch (TamperedMetadataException e) {
            // Metadata in the nodes are tampered, hence the file is not authentic
            boolean isAuthentic = false;
            log.error("File is authentic: {}", isAuthentic);
            return ChunksResponse.builder()
                    .chunks(null)
                    .isAuthenticated(isAuthentic)
                    .build();
        } catch (Exception e) {
            // Error in merging chunks
            return null;
        }
    }

    private String distributeChunks(ArrayList<byte[]> chunks, String fileName, String merkleRootHash){
        log.info("Distributing chunks of file {} with merkleHash {}", fileName, merkleRootHash);
        ArrayList<Node> nodes = registry.getApplications().getRegisteredApplications().stream().
                map(application ->
                        application.getInstances().stream().map(instance ->
                                new Node(
                                        application.getName(),
                                        instance.getHomePageUrl()
                                )
                        ).collect(Collectors.toCollection(ArrayList::new))
                ).collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);

        // Randomize nodes list
        Collections.shuffle(nodes);

        // Distribute chunks to nodes
        int i = 0;
        int j = 0;

        // prevHash for the first node record is "0"
        String prevHash = "0";

        MultiValueMap<String, Object> body;
        while (i < chunks.size()) {
            String nextNode;
            if (j == nodes.size() - 1) {
                nextNode = nodes.get(0).getHomeUrl();
            } else {
                nextNode = nodes.get(j+1).getHomeUrl();
            }
            if (i == chunks.size() - 1) {
                nextNode = "null";
            }

            body = new LinkedMultiValueMap<>();
            body.add("chunk", chunks.get(i));
            body.add("chunkId", String.valueOf(i));
            body.add("fileName", fileName);
            body.add("merkleRootHash", merkleRootHash);

            body.add("nextNode", nextNode);
            body.add("prevHash", prevHash);

            log.info("Sending chunk {} to node {}", i, nodes.get(j).getName());
            WebClient.ResponseSpec responseSpec = webClientBuilder.build().post()
                    .uri(nodes.get(j).getHomeUrl() + "/api/node/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve();

            UploadResponse uploadResponse = Objects.requireNonNull(
                    responseSpec.bodyToMono(UploadResponse.class).block());
            log.info("Received upload response {}", responseSpec);
            if (uploadResponse.getStatusCode() == HttpStatus.OK.value()) {
                prevHash = uploadResponse.getMessage();
                i += 1;
                log.info("Chunk {} uploaded to {} with the hash {}", i, nodes.get(j).getName(), prevHash);
            }
            if (j == nodes.size() - 1) {
                j = 0;
            } else {
                j += 1;
            }
        }

        // Return URL of first chunk
        return nodes.get(0).getHomeUrl();
    }

    private ArrayList<byte[]> getChunks(String firstChunkUrl, String fileName) throws TamperedMetadataException, NoSuchAlgorithmException {
        log.info("Retrieving chunks of file {}", fileName);
        ArrayList<byte[]> chunks = new ArrayList<>();
        // Get chunks from nodes
        int chunkId = 0;
        String prevHash = "0";
        String nodeUrl = firstChunkUrl;

        while (!nodeUrl.equals("null")) {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("fileName", fileName);
            body.add("chunkId", chunkId);

            log.info("Retrieving chunk {} from node {}", chunkId, nodeUrl);
            WebClient.ResponseSpec responseSpec = webClientBuilder.build().post()
                    .uri(nodeUrl + "/api/node/retrieve")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve();

            log.info("Received response {}", responseSpec.toString());

            RetrieveResponse retrieveResponse = Objects.requireNonNull(
                    responseSpec.bodyToMono(RetrieveResponse.class).block());

            if (retrieveResponse.getStatusCode() == HttpStatus.OK.value()) {
                chunks.add(retrieveResponse.getChunk());
                String retrievedPrevHash = retrieveResponse.getPrevHash();
                // compare metadata of the node (w.r.t. previous node) and throw exception if metadata is tampered
                // throw new TamperedMetadataException("Metadata tampered on node"); if metadata of file on the node is tampered
                String metadataRecord = retrieveResponse.getMetadataRecord();
                String metadataRecordHash = sha256(metadataRecord);
                if (prevHash.equals(retrievedPrevHash))
                    prevHash = metadataRecordHash;
                else
                    throw new TamperedMetadataException("Metadata tampered on node");

                nodeUrl = retrieveResponse.getNextNode();
                chunkId += 1;
                log.info("Chunk {} retrieved, next node is {}. node hash is {}", chunkId, nodeUrl, retrievedPrevHash);
            }
        }
        return chunks;
    }

    // Hash the metadata record
    private String sha256(String original) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(original.getBytes());
        byte[] digest = md.digest();
        return Hex.encodeHexString(digest);
    }

    public ArrayList<MetadataResponse> getFiles() {
        return fileServerRepository.findAll().stream()
                .map(metadata ->
                        MetadataResponse.builder()
                                .fileName(metadata.getFileName())
                                .fileSize(metadata.getFileSize())
                                .fileType(metadata.getFileType())
                                .build()
                )
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
