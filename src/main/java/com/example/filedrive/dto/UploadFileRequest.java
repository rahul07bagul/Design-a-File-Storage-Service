package com.example.filedrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class UploadFileRequest {
    @NotNull(message = "userId is required")
    private String userId;

    @NotBlank(message = "fileName is required")
    private String fileName;

    @NotBlank(message = "fileType is required")
    private String fileType;

    @NotNull(message = "fileSize is required")
    private long fileSize;

    private Date fileLastModifiedDate;

    private Integer totalChunks;
}
