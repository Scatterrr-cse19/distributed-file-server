package com.scatterrr.distributedfileserver.service;

public class TamperedMetadataException extends Exception {
    public TamperedMetadataException(String message) {
        super(message);
    }
}
