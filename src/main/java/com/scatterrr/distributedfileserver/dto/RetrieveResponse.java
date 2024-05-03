package com.scatterrr.distributedfileserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class RetrieveResponse {
    private int statusCode;
    private String nextNode;
    private String prevHash;
    private byte[] chunk;
    private String metadataRecord;
    private String merkleRootHash;
}
