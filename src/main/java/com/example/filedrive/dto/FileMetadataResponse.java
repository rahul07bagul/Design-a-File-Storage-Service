package com.example.filedrive.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class FileMetadataResponse {
    private String userId;
    private String fileName;
    private String fileId;
    private String fileType;
    private Long fileSize;
    private String s3Url;
    private String status;
    private String filePath;
    private Date lastModifiedData;
}
