package com.example.filedrive.controller;

import com.example.filedrive.dto.*;
import com.example.filedrive.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/drive")
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload/file")
    public ResponseEntity<?> uploadFile(@RequestBody UploadFileRequest request) {
        logger.info("API Call: POST /upload/file");
        try {
            if (request == null || request.getUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid request or missing user ID."));
            }

            UploadFileResponse response = fileService.uploadFile(request);
            if (response == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "File upload failed."));
            }
            return ResponseEntity.ok().body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request during file upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during file upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error during file upload: " + e.getMessage()));
        }
    }

    @PostMapping("/upload/multipart/init")
    public ResponseEntity<?> uploadMultipartFile(@RequestBody UploadFileRequest request){
        logger.info("API Call: POST /upload/multipart");
        try {
            if (request == null || request.getUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid request or missing user ID."));
            }

            UploadFileResponse response = fileService.initiateMultipartUpload(request);
            if (response == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "File upload failed."));
            }
            return ResponseEntity.ok().body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request during file upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during file upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error during file upload: " + e.getMessage()));
        }
    }

    @PostMapping("/upload/multipart/chunk")
    public ResponseEntity<?> getMultipartUploadUrl(@RequestBody MultipartUploadRequest request) {
        logger.info("API Call: POST /upload/multipart/chunk");
        try {
            if (request == null || request.getFileId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid request or missing file ID."));
            }

            MultipartUploadResponse response = fileService.getMultipartUploadUrl(request);
            if (response == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to generate upload URL for part."));
            }
            return ResponseEntity.ok().body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request during multipart upload part: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting upload URL for part: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error getting upload URL for part: " + e.getMessage()));
        }
    }

    @PostMapping("/upload/multipart/part/status")
    public ResponseEntity<?> updatePartStatus(@RequestBody Map<String, Object> request) {
        logger.info("API Call: POST /upload/multipart/part/status");
        try {
            String fileId = (String) request.get("fileId");
            Integer chunkNumber = (Integer) request.get("chunkNumber");
            String eTag = (String) request.get("eTag");

            if (fileId == null || chunkNumber == null || eTag == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "File ID, part number, and ETag are required."));
            }

            boolean result = fileService.updateChunkUploadStatus(fileId, chunkNumber, eTag);
            if (result) {
                return ResponseEntity.ok().body(Map.of("message", "Part status updated successfully"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update part status."));
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request during part status update: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating part status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error updating part status: " + e.getMessage()));
        }
    }

    @PostMapping("/upload/multipart/complete")
    public ResponseEntity<?> completeMultipartUpload(@RequestBody MultipartUploadCompleteRequest request) {
        logger.info("API Call: POST /upload/multipart/complete");
        try {
            if (request == null || request.getFileId() == null || request.getUploadId() == null || request.getParts() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid complete multipart upload request."));
            }

            boolean result = fileService.completeMultipartUpload(request);
            if (result) {
                return ResponseEntity.ok().body(Map.of("message", "Multipart upload completed successfully"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to complete multipart upload."));
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request during multipart upload completion: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error completing multipart upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error completing multipart upload: " + e.getMessage()));
        }
    }

    @GetMapping("/upload/multipart/status/{userId}")
    public ResponseEntity<List<FileChunkStatusResponse>> getInProgressUploads(@PathVariable String userId) {
        logger.info("API Call: GET /upload/multipart/status/{}", userId);
        try {
            if (userId == null) {
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            List<FileChunkStatusResponse> uploadStatuses = fileService.getInProgressUploads(userId);
            return ResponseEntity.ok(uploadStatuses);
        } catch (Exception e) {
            logger.error("Error getting in-progress uploads: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @PostMapping("/upload/multipart/resume")
    public ResponseEntity<?> resumeMultipartUpload(@RequestBody ResumeUploadRequest request) {
        logger.info("API Call: POST /upload/multipart/resume");
        try {
            if (request == null || request.getFileId() == null || request.getUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid resume request."));
            }

            ResumeUploadResponse response = fileService.getUploadStateForResume(request);
            if (response != null) {
                return ResponseEntity.ok().body(response);
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Upload state not found or not accessible."));
        } catch (Exception e) {
            logger.error("Error resuming multipart upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error resuming multipart upload: " + e.getMessage()));
        }
    }

    @DeleteMapping("/upload/multipart/cancel/{fileId}")
    public ResponseEntity<?> cancelMultipartUpload(@PathVariable String fileId) {
        logger.info("API Call: DELETE /upload/multipart/cancel/{}", fileId);
        try {
            if (fileId == null || fileId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File ID cannot be empty."));
            }

            boolean result = fileService.cancelMultipartUpload(fileId);
            if (result) {
                return ResponseEntity.ok().body(Map.of("message", "Upload canceled successfully"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to cancel upload."));
        } catch (Exception e) {
            logger.error("Error canceling multipart upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error canceling multipart upload: " + e.getMessage()));
        }
    }

    @GetMapping("/download/file/{fileId}")
    public ResponseEntity<?> downloadFile(@PathVariable String fileId) {
        logger.info("API Call: GET /download/file/{}", fileId);
        try {
            if (fileId == null || fileId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File ID cannot be empty."));
            }

            DownloadFileResponse response = fileService.downloadFile(fileId);
            if (response == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "File download failed."));
            }
            return ResponseEntity.ok().body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request during file download: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during file download: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error during file download: " + e.getMessage()));
        }
    }

    @GetMapping("/files/{userId}")
    public ResponseEntity<List<FileMetadataResponse>> listFiles(@PathVariable String userId) {
        logger.info("API Call: GET /files/{}", userId);
        try {
            if (userId == null) {
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            List<FileMetadataResponse> files = fileService.getAllFiles(userId);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Error listing files: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/file/{uniqueFileId}")
    public ResponseEntity<FileMetadataResponse> getFile(@PathVariable String uniqueFileId) {
        logger.info("API Call: GET /file/{}", uniqueFileId);
        try {
            if (uniqueFileId == null || uniqueFileId.isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            FileMetadataResponse file = fileService.getFile(uniqueFileId);
            if (file != null) {
                return ResponseEntity.ok(file);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request during get file: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error getting file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/file/share")
    public ResponseEntity<?> shareFile(@RequestBody SharedRequest request) {
        logger.info("API Call: POST /file/share");
        try {
            if (request == null || request.getFileId() == null || request.getRecipients() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid share request."));
            }

            boolean result = fileService.shareFile(request);
            if (result) {
                return ResponseEntity.ok().body(Map.of("message", "File shared successfully"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "File sharing failed."));
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request during file share: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error sharing file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error during file sharing: " + e.getMessage()));
        }
    }

    @GetMapping("/files/shared/{userId}")
    public ResponseEntity<List<FileMetadataResponse>> listSharedFiles(@PathVariable String userId) {
        logger.info("API Call: GET /files/shared/{}", userId);
        try {
            if (userId == null) {
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            List<FileMetadataResponse> fileShareList = fileService.getFileShares(userId);
            return ResponseEntity.ok(fileShareList);
        } catch (Exception e) {
            logger.error("Error listing shared files: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @DeleteMapping("/file/delete/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable String fileId) {
        logger.info("API Call: DELETE /file/delete{}", fileId);
        try {
            if (fileId == null || fileId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File ID cannot be empty."));
            }

            boolean result = fileService.deleteFile(fileId);
            if (result) {
                return ResponseEntity.ok().body(Map.of("message", "File deleted successfully"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "File delete failed."));
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request during file delete: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error during file deletion: " + e.getMessage()));
        }
    }
}