package com.example.filedrive.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class FileChunkStatusResponse {
    private String fileId;
    private String fileName;
    private Long fileSize;
    private Integer totalChunks;
    private Integer completedChunks;
    private LocalDateTime lastUpdatedAt;
}
