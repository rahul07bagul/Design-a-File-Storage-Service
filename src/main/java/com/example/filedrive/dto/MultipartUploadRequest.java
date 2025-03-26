package com.example.filedrive.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MultipartUploadRequest {
    private String fileId;
    private int chunkNumber;
}
