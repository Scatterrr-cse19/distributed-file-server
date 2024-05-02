package com.scatterrr.distributedfileserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.File;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class ChunksResponse {

    private File chunks;
    private boolean isAuthenticated;
}