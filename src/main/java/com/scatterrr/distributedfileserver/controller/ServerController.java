package com.scatterrr.distributedfileserver.controller;

import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.scatterrr.distributedfileserver.model.Node;
import com.scatterrr.distributedfileserver.service.FileManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/server")
public class ServerController {

    private final PeerAwareInstanceRegistry registry;
    private final FileManager fileManager;

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
        ArrayList<String> chunkFileNames = fileManager.chunkFile(file);
        return chunkFileNames.toString();
    }

}
