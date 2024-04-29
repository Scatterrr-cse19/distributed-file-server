package com.scatterrr.distributedfileserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class MetadataResponse {
    private String fileName;
    private long fileSize;
    private String fileType;
}