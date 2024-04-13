package com.scatterrr.distributedfileserver.service;

import com.scatterrr.distributedfileserver.model.Metadata;
import com.scatterrr.distributedfileserver.repository.FileServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final FileServerRepository fileServerRepository;
    private final FileManager fileManager;
    private final MerkleTree merkleTree;

    public void saveMetadata(MultipartFile file) throws Exception{
        ArrayList<String> chunkFileNames = fileManager.chunkFile(file);
        String merkleRootHash = merkleTree.createMerkleTree(chunkFileNames);
        String firstChunkUrl = distributeChunks(chunkFileNames);
        Metadata metadata = Metadata.builder()
                .fileName(file.getOriginalFilename())
                .numberOfChunks(chunkFileNames.size())
                .locationOfFirstChunk(firstChunkUrl)
                .merkleRootHash(merkleRootHash)
                .build();
        fileServerRepository.save(metadata);
    }

    // TODO: Implement distribute chunks method
    private String distributeChunks(ArrayList<String> chunkFileNames) {
        return "<First Chunk URL>";
    }
}