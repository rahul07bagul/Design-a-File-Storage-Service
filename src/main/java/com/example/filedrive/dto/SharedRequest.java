package com.example.filedrive.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SharedRequest {
    List<String> recipients;
    String fileId;
}
