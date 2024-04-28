package com.scatterrr.distributedfileserver.service;

import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.scatterrr.distributedfileserver.dto.Node;
import com.scatterrr.distributedfileserver.model.Metadata;
import com.scatterrr.distributedfileserver.repository.FileServerRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
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
                .build();
        fileServerRepository.save(metadata);
    }

    public Metadata getMetadata(String fileName) {
        return fileServerRepository.findById(fileName).orElse(null);
    }

    public byte[] retrieveFile(String fileName) {
        Metadata metadata = getMetadata(fileName);
        try {
            ArrayList<byte[]> chunks = getChunks(metadata.getLocationOfFirstChunk(), fileName);
            String merkleRootHash = merkleTree.createMerkleTree(chunks);
            // check if merkleRootHash is equal to metadata.getMerkleRootHash(), hence the file is authentic
            boolean isAuthentic =  merkleRootHash.equals(metadata.getMerkleRootHash());
            return fileManager.mergeChunks(chunks);
        } catch (TamperedMetadataException e) {
            // Metadata in the nodes are tampered, hence the file is not authentic
            boolean isAuthentic = false;
            return null;
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
                nextNode = nodes.get(0).getName();
            } else {
                nextNode = nodes.get(j).getName();
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

            log.info("Received response {}", responseSpec.toString());
            if (responseSpec.bodyToMono(UploadResponse.class).block().getStatusCode() == HttpStatus.OK.value()) {
                prevHash = responseSpec.bodyToMono(UploadResponse.class).block().getMessage();
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

    // TODO: Implement get chunks method
    private ArrayList<byte[]> getChunks(String firstChunkUrl, String fileName) throws TamperedMetadataException {
        ArrayList<byte[]> chunks = new ArrayList<>();
        // Get chunks from nodes
        String chunkId = "0";
        String prevHash = "0";
        String nodeUrl = firstChunkUrl;

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("fileName", fileName);
        body.add("chunkId", chunkId);

        WebClient.ResponseSpec responseSpec = webClientBuilder.build().post()
                .uri(nodeUrl + "/api/node/retrieve")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve();

        log.info("Received response {}", responseSpec.toString());

        // compare metadata of the node (w.r.t. previous node) and throw exception if metadata is tampered
        // throw new TamperedMetadataException("Metadata tampered on node"); if metadata of file on the node is tampered
        return chunks;
    }

}

// Class to hold upload response data
@Data
@AllArgsConstructor
class UploadResponse {
    private final int statusCode;
    private final String message;
}

// Class to hold retrieve response data
@Data
@AllArgsConstructor
class RetrieveResponse {
    private final int statusCode;
    private final String nextNode;
    private final String prevHash;
    private final byte[] chunk;
}
