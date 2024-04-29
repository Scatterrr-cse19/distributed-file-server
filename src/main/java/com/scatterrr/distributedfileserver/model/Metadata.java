package com.scatterrr.distributedfileserver.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Metadata {

    @Id
    private String fileName;
    private String locationOfFirstChunk;
    private int numberOfChunks;
    private String merkleRootHash;
    private long fileSize;
    private String fileType;


    public String toJSONString() {
        return "{"
                + "\"fileName\":\"" + fileName + "\","
                + "\"locationOfFirstChunk\":\"" + locationOfFirstChunk + "\","
                + "\"numberOfChunks\":" + numberOfChunks + ","
                + "\"merkleRootHash\":\"" + merkleRootHash + "\""
                + "}";
    }
}
