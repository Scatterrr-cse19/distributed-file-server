package com.scatterrr.distributedfileserver.controller;

import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.scatterrr.distributedfileserver.dto.ChunksResponse;
import com.scatterrr.distributedfileserver.dto.MetadataResponse;
import com.scatterrr.distributedfileserver.dto.Node;
import com.scatterrr.distributedfileserver.model.Metadata;
import com.scatterrr.distributedfileserver.service.MetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/server")
public class ServerController {

    private final PeerAwareInstanceRegistry registry;
    private final MetadataService metadataService;

    @GetMapping(value = "/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ArrayList<Node> getInstances() {
        return registry.getApplications().getRegisteredApplications().stream().
                map(application ->
                        application.getInstances().stream().map(instance ->
                                new Node(
                                        application.getName(),
                                        instance.getHomePageUrl()
                                )
                        ).collect(Collectors.toCollection(ArrayList::new))
                ).collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
    }

    @PostMapping(value = "/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        metadataService.saveMetadata(file);
        return "File uploaded successfully";
    }

    @GetMapping(value = "/files")
    public ArrayList<MetadataResponse> getFiles() {
        return metadataService.getFiles();
    }

    @DeleteMapping("/delete")
    public void deleteChunks(@RequestParam("fileName") String fileName) {
        metadataService.deleteChunks(fileName);
    }

        @GetMapping(value = "/retrieve") // returns the merged file as a byte array
    public ResponseEntity<byte[]> retrieveFile(@RequestParam("fileName") String fileName
            , @RequestParam(
            value = "allowTampered", required = false, defaultValue = "false") Boolean allowTampered) {

        // returns the merged file as a byte array
        // returns null if the file is not authentic
        ChunksResponse chunksResponse = metadataService.retrieveFile(fileName, allowTampered);
        byte[] fileData = chunksResponse.getChunks();
        boolean isAuthenticated = chunksResponse.isAuthenticated();
        if (!isAuthenticated) {
            if (fileData == null)
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            else
                return new ResponseEntity<>(fileData, HttpStatus.OK);
        } else
            return new ResponseEntity<>(fileData, HttpStatus.OK);
    }

    // Not sure if this is needed
    @GetMapping(value = "/metadata")
    public MetadataResponse getMetadata(@RequestParam("fileName") String fileName) {
        Metadata metadata = metadataService.getMetadata(fileName);
        return MetadataResponse.builder()
                .fileName(metadata.getFileName())
                .fileSize(metadata.getFileSize())
                .fileType(metadata.getFileType())
                .build();
    }

}
