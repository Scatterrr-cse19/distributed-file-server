package com.scatterrr.distributedfileserver.service;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.io.IOUtils;
import org.springframework.mock.web.MockMultipartFile;


import java.io.*;
import java.util.ArrayList;

import static com.scatterrr.distributedfileserver.config.Config.CHUNK_SIZE;

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
        // TODO: Merge MultipartFile chunks to get the original file
        ByteArrayOutputStream mergedOutput = new ByteArrayOutputStream();
        for (byte[] chunk : fileChunks) {
            mergedOutput.write(chunk);
        }
        return mergedOutput.toByteArray();
    }
    // Chunk a MultipartFile into multiple MultipartFile objects
    public ArrayList<MultipartFile> chunkIntoMultipartFiles(MultipartFile inputFile) throws Exception {
        byte[] buffer = new byte[CHUNK_SIZE];
        int partCounter = 1;
        String contentType = inputFile.getContentType();

        ArrayList<MultipartFile> multipartFiles = new ArrayList<>();

        String fileName = inputFile.getOriginalFilename();

        try (InputStream inputStream = inputFile.getInputStream();
             BufferedInputStream bis = new BufferedInputStream(inputStream)) {
            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                String filePartName = String.format("%s.%d.", fileName, partCounter++);
                File newFile = File.createTempFile(filePartName, null);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                    out.flush();
                }
                // Convert the temporary file to a MultipartFile
                multipartFiles.add(convertFileToMultipartFile(newFile,contentType));
                // Delete the temporary file after converting to MultipartFile
                newFile.delete();
            }
        }
        return multipartFiles;
    }
    //Convert File to MultipartFile
    public MultipartFile convertFileToMultipartFile(File file, String contentType) throws IOException {
        FileInputStream input = new FileInputStream(file);
        return new MockMultipartFile(file.getName(), file.getName(), contentType, IOUtils.toByteArray(input));
    }
}
