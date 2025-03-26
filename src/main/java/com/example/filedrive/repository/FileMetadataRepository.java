package com.example.filedrive.repository;

import com.example.filedrive.model.FileMetadata;
import com.example.filedrive.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FileMetadataRepository extends CrudRepository<FileMetadata, String> {
    FileMetadata findByFileId(String fileId);
    List<FileMetadata> findByUser(User user);
    List<FileMetadata> findByUser_Id(String userId);
    List<FileMetadata> findByUser_IdAndStatus(String userId, String status);
    Integer user(User user);
}
