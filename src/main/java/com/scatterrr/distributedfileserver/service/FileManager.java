package com.scatterrr.distributedfileserver.service;

import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import static com.scatterrr.distributedfileserver.config.Config.CHUNK_SIZE;
import static com.scatterrr.distributedfileserver.config.Config.ROOT;

public class FileManager {

    public ArrayList<String> chunkFile(String fileName) throws Exception {
        File inputFile = new File(ROOT + fileName);
        FileInputStream inputStream = new FileInputStream(inputFile);

        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead;
        int count = 0;

        ArrayList<String> chunkFileNames = new ArrayList<>();

        File directory = new File(ROOT + "chunks/" + StringUtils.stripFilenameExtension(fileName));
        if (!directory.exists()) {
            directory.mkdirs();
        }

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            String chunkfileName = StringUtils.stripFilenameExtension(fileName) + "-"+ (++count) + ".txt";
            FileOutputStream outputStream = new FileOutputStream(directory.getPath() + "/" + chunkfileName);
            chunkFileNames.add(chunkfileName);
            outputStream.write(buffer, 0, bytesRead);
            outputStream.close();
        }

        inputStream.close();
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
