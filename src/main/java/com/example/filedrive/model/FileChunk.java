package com.example.filedrive.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class FileChunk {
    @Id
    private String fileId;

    @Column(nullable = false)
    private String uploadId;

    @Column(nullable = false)
    private String userId;

    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer totalChunks;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;

    @ElementCollection
    @CollectionTable(name = "file_chunks", joinColumns = @JoinColumn(name = "file_id"))
    private List<UploadedChunk> uploadedChunk = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }

    @Embeddable
    @Getter
    @Setter
    public static class UploadedChunk{
        private Integer chunkNumber;
        private String eTag;
    }
}
