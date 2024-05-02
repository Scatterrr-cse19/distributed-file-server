package com.scatterrr.distributedfileserver.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    public static final String ROOT = "src/main/resources/"; // TODO: TBD
    public static final int CHUNK_SIZE = 15 * 1024; // TODO: Need to change
    public static final String MERGED_FILE_PATH = "E:\\Aca\\8th Semester\\CS4262 - Distributed Systems\\Scatterrr-cse19\\distributed-file-server\\MergedFiles\\";

}
