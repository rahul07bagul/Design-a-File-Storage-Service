package com.example.filedrive.service;

import com.example.filedrive.dto.*;
import com.example.filedrive.mapper.EntityDTOMapper;
import com.example.filedrive.model.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.time.Duration;

@Service
@Validated
public class FileService {
    private final S3Service s3Service;
    private final DatabaseService databaseService;
    private final UserService userService;
    private final EntityDTOMapper entityDTOMapper;
    private final FileChunkService fileChunkService;

    @Autowired
    public FileService(S3Service s3Service, DatabaseService databaseService, UserService userService, EntityDTOMapper entityDTOMapper, FileChunkService fileChunkService) {
        this.s3Service = s3Service;
        this.databaseService = databaseService;
        this.userService = userService;
        this.entityDTOMapper = entityDTOMapper;
        this.fileChunkService = fileChunkService;
    }

    public UploadFileResponse uploadFile(@Valid UploadFileRequest request) {
        try {
            String userId = request.getUserId();
            User user = userService.getUserById(userId);
            if (user == null) {
                throw new IllegalArgumentException("User not found with ID: " + userId);
            }

            String uniqueFileId = UUID.randomUUID().toString();
            String filePath = "user/" + userId + "/" + uniqueFileId;

            String url;
            try {
                url = s3Service.generatePresignedUploadUrl(filePath, Duration.ofSeconds(900));
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
            }

            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setUser(user);
            fileMetadata.setFileName(request.getFileName());
            fileMetadata.setFileSize(request.getFileSize());
            fileMetadata.setFileType(request.getFileType());
            fileMetadata.setFilePath(filePath);
            fileMetadata.setFileId(uniqueFileId);
            fileMetadata.setLastModifiedData(request.getFileLastModifiedDate());
            fileMetadata.setStatus(FileStatus.URL_GENERATED.toString());

            boolean saved;
            try {
                saved = databaseService.createFileMetadata(fileMetadata);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save file metadata: " + e.getMessage(), e);
            }

            if (saved) {
                UploadFileResponse response = new UploadFileResponse();
                response.setPreSignedUrl(url);
                return response;
            } else {
                throw new RuntimeException("Failed to save file metadata");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during file upload: " + e.getMessage(), e);
        }
    }

    public boolean updateFileUploadStatus(String url) {
        try {
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("S3 URL cannot be null or empty");
            }

            System.out.println("Received S3 URL notification: " + url);
            Map<String, String> extractedIds = extractIdsFromUrl(url);
            String uniqueFileId = extractedIds.get("uniqueFileId");

            if (uniqueFileId == null || uniqueFileId.isEmpty()) {
                System.err.println("Failed to extract valid IDs from URL");
                return false;
            }

            FileMetadata fileMetadata;
            try {
                fileMetadata = databaseService.getFileMetadata(uniqueFileId);
                if (fileMetadata == null) {
                    throw new IllegalArgumentException("File metadata not found for ID: " + uniqueFileId);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve file metadata: " + e.getMessage(), e);
            }

            fileMetadata.setStatus(FileStatus.UPLOADED.toString());
            fileMetadata.setS3Url(url);

            try {
                return databaseService.updateFileMetadata(fileMetadata);
            } catch (Exception e) {
                throw new RuntimeException("Failed to update file metadata: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            System.err.println("Error updating file upload status: " + e.getMessage());
            return false;
        }
    }

    public DownloadFileResponse downloadFile(String uniqueFileId) {
        try {
            FileMetadata fileMetadata;
            try {
                fileMetadata = databaseService.getFileMetadata(uniqueFileId);
                if (fileMetadata == null) {
                    throw new IllegalArgumentException("File not found with ID: " + uniqueFileId);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve file metadata: " + e.getMessage(), e);
            }

            String filePath = fileMetadata.getFilePath();
            if (filePath == null || filePath.isEmpty()) {
                throw new IllegalArgumentException("File path is missing for ID: " + uniqueFileId);
            }

            String url;
            try {
                url = s3Service.generatePresignedDownloadUrl(filePath, Duration.ofMinutes(10));
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate download URL: " + e.getMessage(), e);
            }

            DownloadFileResponse response = new DownloadFileResponse();
            response.setDownloadUrl(url);
            response.setFileId(uniqueFileId);
            return response;
        } catch (Exception e) {
            System.err.println("Error generating download URL: " + e.getMessage());
            throw e;
        }
    }

    public List<FileMetadataResponse> getAllFiles(String userId) {
        try {
            List<FileMetadata> fileEntities;
            try {
                fileEntities = databaseService.getAllFiles(userId);
                if (fileEntities == null) {
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve files: " + e.getMessage(), e);
            }

            return entityDTOMapper.toFileMetadataDTOList(fileEntities);
        } catch (Exception e) {
            System.err.println("Error getting all files: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public FileMetadataResponse getFile(String uniqueFileId) {
        try {
            FileMetadata fileEntity;
            try {
                fileEntity = databaseService.getFileMetadata(uniqueFileId);
                if (fileEntity == null) {
                    return null;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve file: " + e.getMessage(), e);
            }

            return entityDTOMapper.toFileMetadataDTO(fileEntity);
        } catch (Exception e) {
            System.err.println("Error getting file: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteFile(String uniqueFileId) {
        try {
            try {
                return databaseService.deleteFile(uniqueFileId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            System.err.println("Error deleting file: " + e.getMessage());
            return false;
        }
    }

    public List<FileMetadataResponse> getFileShares(String userId) {
        try {
            List<FileShare> fileShareList;
            try {
                fileShareList = databaseService.getAllFileShares(userId);
                if (fileShareList == null) {
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve file shares: " + e.getMessage(), e);
            }

            return entityDTOMapper.toFileShareDTOList(fileShareList);
        } catch (Exception e) {
            System.err.println("Error getting file shares: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean shareFile(SharedRequest request) {
        try {
            String uniqueFileId = request.getFileId();
            List<String> recipients = request.getRecipients();
            FileMetadata fileMetadata;
            try {
                fileMetadata = databaseService.getFileMetadata(uniqueFileId);
                if (fileMetadata == null) {
                    throw new IllegalArgumentException("File not found with ID: " + uniqueFileId);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve file metadata: " + e.getMessage(), e);
            }

            List<FileShare> fileShareList = new ArrayList<>();
            for (String userId : recipients) {
                if (userId == null) {
                    continue;
                }

                FileShare fileShare = new FileShare();
                fileShare.setUserId(userId);
                fileShare.setFileMetadata(fileMetadata);
                fileShare.setPermission(SharePermission.READ);
                fileShare.setCreatedAt(LocalDateTime.now());
                fileShareList.add(fileShare);
            }

            if (fileShareList.isEmpty()) {
                throw new IllegalArgumentException("No valid recipients found");
            }

            try {
                return databaseService.shareFile(fileShareList);
            } catch (Exception e) {
                throw new RuntimeException("Failed to share file: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            System.err.println("Error sharing file: " + e.getMessage());
            return false;
        }
    }

    private static Map<String, String> extractIdsFromUrl(String s3Url) {
        Map<String, String> result = new HashMap<>();

        try {
            URL url = new URL(s3Url);
            String path = url.getPath();
            String[] pathParts = path.split("/");

            if (pathParts.length > 2) {
                result.put("userId", pathParts[2]);
            }

            if (pathParts.length > 3) {
                String uniqueFileId = pathParts[3];
                if (uniqueFileId.contains("_")) {
                    result.put("uniqueFileId", uniqueFileId.substring(0, uniqueFileId.indexOf("_")));
                } else {
                    result.put("uniqueFileId", uniqueFileId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing S3 URL: " + e.getMessage());
        }
        return result;
    }

    /**Multipart Methods */
    public UploadFileResponse initiateMultipartUpload(@Valid UploadFileRequest request) {
        try {
            String userId = request.getUserId();
            User user = userService.getUserById(userId);
            String uniqueFileId = UUID.randomUUID().toString();
            String filePath = "user/" + userId + "/" + uniqueFileId;

            String uploadId;
            try {
                uploadId = s3Service.initiateMultipartUpload(filePath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initiate multipart upload: " + e.getMessage(), e);
            }

            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setUser(user);
            fileMetadata.setFileName(request.getFileName());
            fileMetadata.setFileSize(request.getFileSize());
            fileMetadata.setFileType(request.getFileType());
            fileMetadata.setFilePath(filePath);
            fileMetadata.setFileId(uniqueFileId);
            fileMetadata.setLastModifiedData(request.getFileLastModifiedDate());
            fileMetadata.setStatus(FileStatus.MULTIPART_INITIATED.toString());
            fileMetadata.setUploadId(uploadId);
            fileMetadata.setTotalChunks(request.getTotalChunks());

            boolean saved;
            try {
                saved = databaseService.createFileMetadata(fileMetadata);
            } catch (Exception e) {
                s3Service.abortMultipartUpload(filePath, uploadId);
                throw new RuntimeException("Failed to save file metadata: " + e.getMessage(), e);
            }

            if (saved) {
                // Save the upload state for resume capability
                try {

                    fileChunkService.saveUploadState(
                            uniqueFileId,
                            uploadId,
                            userId,
                            request.getFileName(),
                            request.getFileType(),
                            request.getFileSize(),
                            request.getTotalChunks()
                    );
                } catch (Exception e) {
                    System.err.println("Failed to save upload state for resuming: " + e.getMessage());
                }

                UploadFileResponse response = new UploadFileResponse();
                response.setPreSignedUrl(null);
                response.setUploadId(uploadId);
                response.setFileId(uniqueFileId);
                return response;
            } else {
                // Abort the multipart upload if metadata saving fails
                s3Service.abortMultipartUpload(filePath, uploadId);
                throw new RuntimeException("Failed to save file metadata");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during multipart upload initiation: " + e.getMessage(), e);
        }
    }

    public MultipartUploadResponse getMultipartUploadUrl(MultipartUploadRequest request) {
        try {
            String fileId = request.getFileId();
            int chunkNumber = request.getChunkNumber();
            FileMetadata fileMetadata;
            try {
                fileMetadata = databaseService.getFileMetadata(fileId);
                if (fileMetadata == null) {
                    throw new IllegalArgumentException("File metadata not found for ID: " + fileId);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve file metadata: " + e.getMessage(), e);
            }

            // Verify file status
            if (!FileStatus.MULTIPART_INITIATED.toString().equals(fileMetadata.getStatus()) &&
                    !FileStatus.MULTIPART_IN_PROGRESS.toString().equals(fileMetadata.getStatus())) {
                throw new IllegalArgumentException("File is not in a valid state for multipart upload");
            }

            // Update status to in progress if needed
            if (FileStatus.MULTIPART_INITIATED.toString().equals(fileMetadata.getStatus())) {
                fileMetadata.setStatus(FileStatus.MULTIPART_IN_PROGRESS.toString());
                databaseService.updateFileMetadata(fileMetadata);
            }

            String uploadUrlForPart;
            try {
                String uploadId = fileMetadata.getUploadId();
                if (uploadId == null || uploadId.isEmpty()) {
                    throw new IllegalArgumentException("Upload ID not found for file");
                }
                uploadUrlForPart = s3Service.generatePresignedUploadUrlForChunk(
                        fileMetadata.getFilePath(),
                        uploadId,
                        chunkNumber,
                        Duration.ofMinutes(30));
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate upload URL for part: " + e.getMessage(), e);
            }

            MultipartUploadResponse response = new MultipartUploadResponse();
            response.setUploadUrl(uploadUrlForPart);
            response.setChunkNumber(chunkNumber);
            response.setUploadId(fileMetadata.getUploadId());
            return response;
        } catch (Exception e) {
            System.err.println("Error generating upload URL for part: " + e.getMessage());
            throw e;
        }
    }

    public boolean completeMultipartUpload(MultipartUploadCompleteRequest request) {
        try {
            String fileId = request.getFileId();
            String uploadId = request.getUploadId();
            List<MultipartUploadCompleteRequest.ChunkDetail> parts = request.getParts();
            if (parts == null || parts.isEmpty()) {
                throw new IllegalArgumentException("Parts list cannot be null or empty");
            }

            FileMetadata fileMetadata;
            try {
                fileMetadata = databaseService.getFileMetadata(fileId);
                if (fileMetadata == null) {
                    throw new IllegalArgumentException("File metadata not found for ID: " + fileId);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve file metadata: " + e.getMessage(), e);
            }

            // Verify file status
            if (!FileStatus.MULTIPART_IN_PROGRESS.toString().equals(fileMetadata.getStatus())) {
                throw new IllegalArgumentException("File is not in a valid state for completing multipart upload");
            }

            // Get completed chunks from fileChunkService instead of converting from request
            List<CompletedPart> completedParts = fileChunkService.getCompletedChunks(fileId);

            if (completedParts.isEmpty()) {
                throw new IllegalArgumentException("No completed chunks found for file");
            }

            try {
                s3Service.completeMultipartUpload(fileMetadata.getFilePath(), uploadId, completedParts);
            } catch (Exception e) {
                throw new RuntimeException("Failed to complete multipart upload: " + e.getMessage(), e);
            }

            // Update file status
            fileMetadata.setStatus(FileStatus.UPLOADED.toString());

            // Generate the final S3 URL for the completed file
            String s3Url = "https://" + s3Service.getBucketName() + ".s3.amazonaws.com/" + fileMetadata.getFilePath();
            fileMetadata.setS3Url(s3Url);

            try {
                databaseService.updateFileMetadata(fileMetadata);
                fileChunkService.removeUploadState(fileId);
                return true;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update file metadata: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            System.err.println("Error completing multipart upload: " + e.getMessage());
            return false;
        }
    }

    public boolean updateChunkUploadStatus(String fileId, int chunkNumber, String eTag) {
        try {
            fileChunkService.updateUploadedChunk(fileId, chunkNumber, eTag);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating chunk upload status: " + e.getMessage());
            return false;
        }
    }

    public ResumeUploadResponse getUploadStateForResume(ResumeUploadRequest request) {
        try {
            return fileChunkService.getUploadState(request);
        } catch (Exception e) {
            System.err.println("Error getting upload state: " + e.getMessage());
            return null;
        }
    }

    public List<FileChunkStatusResponse> getInProgressUploads(String userId) {
        try {
            return fileChunkService.getInProgressUploads(userId);
        } catch (Exception e) {
            System.err.println("Error getting in-progress uploads: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean cancelMultipartUpload(String fileId) {
        try {
            FileMetadata fileMetadata;
            try {
                fileMetadata = databaseService.getFileMetadata(fileId);
                if (fileMetadata == null) {
                    throw new IllegalArgumentException("File metadata not found for ID: " + fileId);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve file metadata: " + e.getMessage(), e);
            }

            String uploadId = fileMetadata.getUploadId();
            if (uploadId == null || uploadId.isEmpty()) {
                throw new IllegalArgumentException("Upload ID not found for file");
            }

            try {
                s3Service.abortMultipartUpload(fileMetadata.getFilePath(), uploadId);
            } catch (Exception e) {
                System.err.println("Error aborting multipart upload in S3: " + e.getMessage());
            }

            fileChunkService.removeUploadState(fileId);

            // Delete the file metadata
            return databaseService.deleteFile(fileId);
        } catch (Exception e) {
            System.err.println("Error canceling multipart upload: " + e.getMessage());
            return false;
        }
    }

    public void cleanupStaleUploads(String userId) {
        try {
            // Clean up uploads older than 7 days
            fileChunkService.cleanupStaleUploads(userId, 7);
        } catch (Exception e) {
            System.err.println("Error cleaning up stale uploads: " + e.getMessage());
        }
    }
}