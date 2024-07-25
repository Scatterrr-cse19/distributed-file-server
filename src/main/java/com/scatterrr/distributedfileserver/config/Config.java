package com.scatterrr.distributedfileserver.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    public static final String ROOT = "src/main/resources/";
    public static final int CHUNK_SIZE = 64 * 1024 * 1024;

}
