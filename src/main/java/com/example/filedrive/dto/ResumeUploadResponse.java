package com.example.filedrive.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ResumeUploadResponse {
    private String fileId;
    private String uploadId;
    private String fileName;
    private Long fileSize;
    private Integer totalChunks;
    private List<UploadedChunkInfo> completedChunks;
    private String uploadUrl;
    private Integer chunkNumber;

    @Getter
    @Setter
    public static class UploadedChunkInfo {
        private Integer chunkNumber;
        private String eTag;
    }
}