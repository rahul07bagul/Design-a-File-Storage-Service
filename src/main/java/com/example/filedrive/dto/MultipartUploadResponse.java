package com.example.filedrive.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MultipartUploadResponse {
    private String uploadId;
    private String uploadUrl;
    private Integer chunkNumber;
}
