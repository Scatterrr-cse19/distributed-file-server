package com.scatterrr.distributedfileserver.repository;

import com.scatterrr.distributedfileserver.model.Metadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileServerRepository extends JpaRepository<Metadata, String> {
}
