package com.scatterrr.distributedfileserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DistributedFileServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistributedFileServerApplication.class, args);
	}

}
