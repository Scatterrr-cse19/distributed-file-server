package com.scatterrr.distributedfileserver.service;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;

import static com.scatterrr.distributedfileserver.config.Config.CHUNK_SIZE;
import static com.scatterrr.distributedfileserver.config.Config.ROOT;

@Service
@NoArgsConstructor
public class FileManager {

    public ArrayList<byte[]> chunkFile(MultipartFile inputFile) throws Exception {
        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead;

        ArrayList<byte[]> fileChunks = new ArrayList<>();

        InputStream inputStream = inputFile.getInputStream();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                fileChunks.add(outputStream.toByteArray());
                outputStream.reset();
            }
        }
        return fileChunks;
    }

    public byte[] mergeChunks(ArrayList<byte[]> fileChunks) throws IOException {
        ByteArrayOutputStream mergedOutput = new ByteArrayOutputStream();
        for (byte[] chunk : fileChunks) {
            mergedOutput.write(chunk);
        }
        return mergedOutput.toByteArray();
    }
}
