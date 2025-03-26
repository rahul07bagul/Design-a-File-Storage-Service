package com.example.filedrive.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UploadFileResponse {
    String preSignedUrl;
    String uploadId;
    String fileId;
}
