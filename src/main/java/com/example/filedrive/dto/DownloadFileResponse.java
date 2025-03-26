package com.example.filedrive.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DownloadFileResponse {
    String downloadUrl;
    String fileId;
}
