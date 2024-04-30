package com.scatterrr.distributedfileserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class RetrieveResponse {
    private int statusCode;
    private String nextNode;
    private String prevHash;
    private MultipartFile chunk;
    private String metadataRecord;
}
