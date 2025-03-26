package com.example.filedrive.repository;

import com.example.filedrive.model.FileMetadata;
import com.example.filedrive.model.FileShare;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FileShareRepository extends CrudRepository<FileShare, Integer> {
    List<FileShare> findByUserId(String userId);
}
