package com.scatterrr.distributedfileserver.controller;

import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.scatterrr.distributedfileserver.dto.Node;
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
    public String uploadFile(@RequestParam("file") MultipartFile file) throws Exception{
        metadataService.saveMetadata(file);
        return "File uploaded successfully";
    }

    @GetMapping(value = "/retrieve") // returns the merged file as a byte array
    public ResponseEntity<byte []> retrieveFile(@RequestParam("fileName") String fileName) {
        // returns the merged file as a byte array
        // returns null if the file is not authentic
        byte[] fileData = metadataService.retrieveFile(fileName);
        if (fileData == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else
            return new ResponseEntity<>(fileData, HttpStatus.OK);
    }

    // Not sure if this is needed
    @GetMapping(value = "/metadata")
    public String getMetadata(@RequestParam("fileName") String fileName) {
        // returns the metadata of the file as a JSONString (to be viewed at file explorer)
        return metadataService.getMetadata(fileName).toJSONString();
    }

}
