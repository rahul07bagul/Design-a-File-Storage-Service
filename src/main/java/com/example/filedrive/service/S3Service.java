package com.example.filedrive.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    @Getter
    private final String bucketName;

    public S3Service(
            @Value("${cloud.aws.credentials.access-key}") String accessKey,
            @Value("${cloud.aws.credentials.secret-key}") String secretKey,
            @Value("${cloud.aws.region.static}") String region,
            @Value("${application.bucket.name}") String bucketName) {

        this.bucketName = bucketName;

        try {
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsCredentials);

            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();

            this.presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();
        } catch (Exception e) {
            System.err.println("Failed to initialize S3 client: " + e.getMessage());
            throw new RuntimeException("Could not initialize S3 service: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                throw new IllegalArgumentException("File path cannot be null or empty");
            }

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (S3Exception e) {
            System.err.println("S3 service error while deleting file: " + e.getMessage());
            throw new RuntimeException("Failed to delete file from S3: " + filePath, e);
        } catch (Exception e) {
            System.err.println("Unexpected error while deleting file: " + e.getMessage());
            throw new RuntimeException("Failed to delete file from S3: " + filePath, e);
        }
    }

    public String generatePresignedDownloadUrl(String objectKey, Duration expiration) {
        try {
            if (objectKey == null || objectKey.isEmpty()) {
                throw new IllegalArgumentException("Object key cannot be null or empty");
            }

            if (expiration == null) {
                throw new IllegalArgumentException("Expiration duration cannot be null");
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

            return presignedRequest.url().toString();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (S3Exception e) {
            System.err.println("S3 service error generating download URL: " + e.getMessage());
            throw new RuntimeException("Failed to generate presigned download URL: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error generating download URL: " + e.getMessage());
            throw new RuntimeException("Failed to generate presigned download URL", e);
        }
    }

    public String generatePresignedUploadUrl(String objectKey, Duration expiration) {
        try {
            if (objectKey == null || objectKey.isEmpty()) {
                throw new IllegalArgumentException("Object key cannot be null or empty");
            }

            if (expiration == null) {
                throw new IllegalArgumentException("Expiration duration cannot be null");
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

            return presignedRequest.url().toString();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (S3Exception e) {
            System.err.println("S3 service error generating upload URL: " + e.getMessage());
            throw new RuntimeException("Failed to generate presigned upload URL: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error generating upload URL: " + e.getMessage());
            throw new RuntimeException("Failed to generate presigned upload URL", e);
        }
    }

    public String initiateMultipartUpload(String objectKey) {
        try {
            if (objectKey == null || objectKey.isEmpty()) {
                throw new IllegalArgumentException("Object key cannot be null or empty");
            }

            CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createMultipartUploadRequest);
            return response.uploadId();
        } catch (S3Exception e) {
            System.err.println("S3 service error initiating multipart upload: " + e.getMessage());
            throw new RuntimeException("Failed to initiate multipart upload: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error initiating multipart upload: " + e.getMessage());
            throw new RuntimeException("Failed to initiate multipart upload", e);
        }
    }

    public String generatePresignedUploadUrlForChunk(String objectKey, String uploadId, int partNumber, Duration expiration) {
        try {
            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(expiration)
                    .uploadPartRequest(UploadPartRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .uploadId(uploadId)
                            .partNumber(partNumber)
                            .build())
                    .build();

            PresignedUploadPartRequest presignedRequest = presigner.presignUploadPart(presignRequest);
            return presignedRequest.url().toString();
        } catch (S3Exception e) {
            System.err.println("S3 service error generating upload URL for part: " + e.getMessage());
            throw new RuntimeException("Failed to generate presigned URL for part: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error generating upload URL for part: " + e.getMessage());
            throw new RuntimeException("Failed to generate presigned URL for part", e);
        }
    }

    public void completeMultipartUpload(String objectKey, String uploadId, List<CompletedPart> completedParts) {
        try {
            CompletedMultipartUpload multipartUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();

            CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .uploadId(uploadId)
                    .multipartUpload(multipartUpload)
                    .build();

            s3Client.completeMultipartUpload(completeMultipartUploadRequest);
        } catch (S3Exception e) {
            System.err.println("S3 service error completing multipart upload: " + e.getMessage());
            throw new RuntimeException("Failed to complete multipart upload: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error completing multipart upload: " + e.getMessage());
            throw new RuntimeException("Failed to complete multipart upload", e);
        }
    }

    public void abortMultipartUpload(String objectKey, String uploadId) {
        try {
            if (objectKey == null || objectKey.isEmpty()) {
                throw new IllegalArgumentException("Object key cannot be null or empty");
            }

            if (uploadId == null || uploadId.isEmpty()) {
                throw new IllegalArgumentException("Upload ID cannot be null or empty");
            }

            AbortMultipartUploadRequest abortMultipartUploadRequest = AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .uploadId(uploadId)
                    .build();

            s3Client.abortMultipartUpload(abortMultipartUploadRequest);
        } catch (S3Exception e) {
            System.err.println("S3 service error aborting multipart upload: " + e.getMessage());
            throw new RuntimeException("Failed to abort multipart upload: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error aborting multipart upload: " + e.getMessage());
            throw new RuntimeException("Failed to abort multipart upload", e);
        }
    }
}