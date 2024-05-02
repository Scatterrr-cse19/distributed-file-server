package com.scatterrr.distributedfileserver.service;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static com.scatterrr.distributedfileserver.config.Config.CHUNK_SIZE;
import static com.scatterrr.distributedfileserver.config.Config.ROOT;
import static com.scatterrr.distributedfileserver.config.Config.MERGED_FILE_PATH;

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

    public byte[] mergeChunks(ArrayList<byte[]> fileChunks, String fileName) throws IOException {
        System.out.println("mw call wenne nedda bn");
//        File mergedFile = new File(MERGED_FILE_PATH+fileName);

        // Create FileOutputStream to write the merged content into the new file
        try (FileOutputStream fos = new FileOutputStream(MERGED_FILE_PATH+fileName)) {
            // Iterate through each byte[] item in the ArrayList
            for (byte[] chunk : fileChunks) {
                // Write the content of each byte[] item to the FileOutputStream
                fos.write(chunk);
            }
        } catch (IOException e) {
            // Handle any IO exceptions
            e.printStackTrace();
            throw e;
        }
        byte[] outputFile =  Files.readAllBytes(Paths.get(MERGED_FILE_PATH+fileName));
//        mergedFile.delete();
        return outputFile;
    }
}
