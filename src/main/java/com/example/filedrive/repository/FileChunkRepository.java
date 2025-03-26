package com.example.filedrive.repository;

import com.example.filedrive.model.FileChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileChunkRepository extends JpaRepository<FileChunk, String> {
    List<FileChunk> findByUserId(String userId);
    List<FileChunk> findByUserIdAndLastUpdatedAtBefore(String userId, LocalDateTime dateTime);
}
