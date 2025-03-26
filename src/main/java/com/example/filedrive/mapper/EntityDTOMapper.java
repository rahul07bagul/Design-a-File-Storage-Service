package com.example.filedrive.mapper;

import com.example.filedrive.dto.FileMetadataResponse;
import com.example.filedrive.dto.UserResponse;
import com.example.filedrive.model.FileMetadata;
import com.example.filedrive.model.FileShare;
import com.example.filedrive.model.User;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityDTOMapper {

    public FileMetadataResponse toFileMetadataDTO(FileMetadata entity) {
        if (entity == null) {
            return null;
        }

        FileMetadataResponse dto = new FileMetadataResponse();
        dto.setUserId(entity.getUser().getId());
        dto.setFileName(entity.getFileName());
        dto.setFileId(entity.getFileId());
        dto.setFileType(entity.getFileType());
        dto.setFileSize(entity.getFileSize());
        dto.setS3Url(entity.getS3Url());
        dto.setStatus(entity.getStatus());
        dto.setFilePath(entity.getFilePath());
        dto.setLastModifiedData(entity.getLastModifiedData());

        return dto;
    }

    public List<FileMetadataResponse> toFileMetadataDTOList(List<FileMetadata> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toFileMetadataDTO)
                .collect(Collectors.toList());
    }

    public FileMetadataResponse toFileShareDTO(FileShare entity) {
        if (entity == null || entity.getFileMetadata() == null) {
            return null;
        }

        FileMetadataResponse dto = new FileMetadataResponse();
        dto.setFileId(entity.getFileMetadata().getFileId());
        dto.setUserId(entity.getUserId());
        dto.setFileName(entity.getFileMetadata().getFileName());
        dto.setFileType(entity.getFileMetadata().getFileType());
        dto.setFileSize(entity.getFileMetadata().getFileSize());
        dto.setS3Url(entity.getFileMetadata().getS3Url());
        dto.setStatus(entity.getFileMetadata().getStatus());
        dto.setFilePath(entity.getFileMetadata().getFilePath());
        dto.setLastModifiedData(entity.getFileMetadata().getLastModifiedData());

        return dto;
    }

    public List<FileMetadataResponse> toFileShareDTOList(List<FileShare> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toFileShareDTO)
                .collect(Collectors.toList());
    }

    public UserResponse toUserDTO(User entity) {
        if (entity == null) {
            return null;
        }

        UserResponse dto = new UserResponse();
        dto.setId(entity.getId());
        dto.setEmail(entity.getEmail());
        dto.setName(entity.getName());

        // Convert the files to summary DTOs if they exist
        if (entity.getFiles() != null) {
            dto.setFiles(entity.getFiles().stream()
                    .map(this::toFileMetadataDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
