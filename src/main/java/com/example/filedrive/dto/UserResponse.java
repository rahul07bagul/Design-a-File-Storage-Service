package com.example.filedrive.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private List<FileMetadataResponse> files;
}
