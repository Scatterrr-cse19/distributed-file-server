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

    public ArrayList<String> chunkFile(MultipartFile inputFile) throws Exception {
        String fileName = inputFile.getOriginalFilename();

        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead;
        int count = 0;

        ArrayList<String> chunkFileNames = new ArrayList<>();

        File directory = new File(ROOT + "chunks/" + StringUtils.stripFilenameExtension(fileName));
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (InputStream inputStream = inputFile.getInputStream()) {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String chunkFileName = StringUtils.stripFilenameExtension(fileName) + "-" + (++count) + ".txt";
                try (FileOutputStream outputStream = new FileOutputStream(directory.getPath() + "/" + chunkFileName)) {
                    chunkFileNames.add(chunkFileName);
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }
        return chunkFileNames;
    }

    public void mergeChunks(ArrayList<String> chunkFileNames) throws Exception {
        String filename = chunkFileNames.get(0).replaceAll("-\\d+\\.txt", "");
        String outputFilePath = ROOT + "output/" + filename + ".txt";
        FileOutputStream outputStream = new FileOutputStream(outputFilePath);
        for (String chunkFileName : chunkFileNames) {
            FileInputStream inputStream = new FileInputStream(ROOT + filename + "/chunks/"
                    + chunkFileName);
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
        }

        outputStream.close();
    }
}
