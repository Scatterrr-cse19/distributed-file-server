package com.scatterrr.distributedfileserver.service;

import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.scatterrr.distributedfileserver.dto.Node;
import com.scatterrr.distributedfileserver.model.Metadata;
import com.scatterrr.distributedfileserver.repository.FileServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final FileServerRepository fileServerRepository;
    private final FileManager fileManager;
    private final MerkleTree merkleTree;
    private final PeerAwareInstanceRegistry registry;

    public void saveMetadata(MultipartFile file) throws Exception{
        ArrayList<byte[]> chunks = fileManager.chunkFile(file);
        String merkleRootHash = merkleTree.createMerkleTree(chunks);
//        String firstChunkUrl = distributeChunks(chunks);
        String firstChunkUrl = "<First Chunk URL>"; // Temporary
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

    // TODO: Implement distribute chunks method
    private String distributeChunks(ArrayList<byte[]> chunkFileNames) {
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

        // Return URL of first chunk
        return "<First Chunk URL>";
    }

    // TODO: Implement get chunks method
    private ArrayList<byte[]> getChunks(String firstChunkUrl, String fileName) throws TamperedMetadataException {
        ArrayList<byte[]> chunks = new ArrayList<>();
        // Get chunks from nodes
        // compare metadata of the node (w.r.t. previous node) and throw exception if metadata is tampered
        // throw new TamperedMetadataException("Metadata tampered on node"); if metadata of file on the node is tampered
        return chunks;
    }

}