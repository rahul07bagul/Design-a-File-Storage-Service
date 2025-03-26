package com.example.filedrive.controller;

import com.example.filedrive.service.FileService;
import com.example.filedrive.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/drive/s3")
public class S3NotificationController {

    private final FileService fileService;

    @Autowired
    public S3NotificationController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> handleS3Notification(@RequestBody Map<String, String> payload) {
        boolean result = fileService.updateFileUploadStatus(payload.get("fileUrl"));

        Map<String, String> response = new HashMap<>();

        if (result) {
            response.put("message", "File status updated successfully");
        }else{
            response.put("message", "File status update failed");
        }
        return ResponseEntity.ok(response);
    }
}

