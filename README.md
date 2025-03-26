# File Drive - Cloud Storage Application

A secure, scalable file storage backend built with Spring Boot that integrates with AWS S3 for efficient file storage, retrieval, and sharing capabilities.

<p align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-F2F4F9?style=for-the-badge&logo=spring-boot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazonaws&logoColor=white" alt="AWS S3">
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase">
</p>

## Features

- **Secure File Storage:** Utilize AWS S3 for reliable cloud storage
- **Pre-signed URLs:** Generate secure temporary links for uploads and downloads
- **File Metadata Management:** Track and manage file information
- **File Sharing:** Enable users to share files with others
- **User Authentication:** Authentication using Firebase

## Demo
![Demo](https://github.com/rahul07bagul/FileDrive/blob/main/assets/File_Drive_Video.gif)

## High Level Design
![HLD](https://github.com/rahul07bagul/FileDrive/blob/main/assets/High%20Level.png)

## Architecture

The backend follows a layered architecture:

- **Controller Layer:** REST endpoints for client communication
- **Service Layer:** Business logic and AWS S3 integration
- **Repository Layer:** Data access and persistence
- **Model Layer:** Domain entities

## Technical Implementation

### AWS S3 Integration

The application leverages AWS S3 for storing user files with the following implementation:

- **S3Service.java:** Handles all interactions with AWS S3, including:
  - Generating pre-signed URLs for secure file uploads
  - Creating pre-signed download URLs with expiration

```java
// Pre-signed URL generation for secure direct upload
public String generatePresignedUploadUrl(String objectKey, Duration expiration) {
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
}
```

### File Upload Flow

1. Client requests a pre-signed URL via `/api/v1/drive/upload/file` endpoint
2. Backend (FileService) generates a unique file ID and creates metadata entry
3. Backend generates a pre-signed URL (valid for 15 minutes) for direct S3 upload
4. File metadata is stored in the database with status "URL Generated"
5. Client uploads file directly to S3 using the pre-signed URL
6. AWS S3 triggers a Lambda function upon successful file upload
7. Lambda function sends a notification to the backend service
8. Backend receives the notification and updates the file status to "Uploaded" in the database

### File Download Flow

1. Client requests download URL via `/api/v1/drive/download/file/{fileId}` endpoint
2. Backend generates a time-limited pre-signed URL (valid for 10 minutes)
3. The pre-signed URL allows secure temporary access to the file in S3

### AWS Lambda Integration for Upload Notifications

The application utilizes AWS Lambda to handle file upload notifications:

1. **S3 Event Trigger:** Configured to trigger a Lambda function when objects are created in the S3 bucket
2. **Lambda Function:** Processes the S3 event and extracts file information
3. **Backend Notification:** Lambda sends a notification to the backend API with the file URL (via Ngrok for local development)
```bash
import json
import requests

def lambda_handler(event, context):
    for record in event['Records']:
        s3_bucket = record['s3']['bucket']['name']
        s3_key = record['s3']['object']['key']
        file_url = f"https://{s3_bucket}.s3.amazonaws.com/{s3_key}"

        # Notify Spring Boot server
        response = requests.post("https://f82f-2601-806-4200-3bd0-30c0-ad0e-8105-d911.ngrok-free.app/api/v1/drive/s3/upload", json={"fileUrl": file_url})
        return response.json()
```

### System Flow Diagram

The complete flow for file uploads with S3, Lambda, and Ngrok:

```
┌─────────┐          ┌────────────┐          ┌─────────┐          ┌──────────┐
│  Client │          │  Backend   │          │   AWS   │          │  Lambda  │
│         │          │  Service   │          │   S3    │          │ Function │
└────┬────┘          └─────┬──────┘          └────┬────┘          └─────┬────┘
     │                     │                      │                     │
     │ 1. Request Upload   │                      │                     │
     │─────────────────────>                      │                     │
     │                     │                      │                     │
     │ 2. Pre-signed URL   │                      │                     │
     │<─────────────────────                      │                     │
     │                     │                      │                     │
     │ 3. Upload File      │                      │                     │
     │────────────────────────────────────────────>                     │
     │                     │                      │                     │
     │                     │                      │ 4. Create Event     │
     │                     │                      │────────────────────>│
     │                     │                      │                     │
     │                     │ 5. Notification      │                     │
     │                     │<────────────────────────────────────────────
     │                     │                      │                     │
     │                     │ 6. Update DB Status  │                     │
     │                     │─────────┐            │                     │
     │                     │         │            │                     │
     │                     │<────────┘            │                     │
     │                     │                      │                     │
     │ 7. File Status      │                      │                     │
     │<─────────────────────                      │                     │
     │                     │                      │                     │
```

Notes:
- Ngrok creates a secure tunnel to expose the backend service to the internet
- This allows Lambda to notify the backend even when running locally
- In production, replace Ngrok with your deployed backend URL

### File Sharing Implementation

The system enables file sharing through the following components:

- **FileShare model:** Tracks shared files with permission levels
- **SharePermission enum:** Defines access levels (READ)
- **File sharing endpoint:** `/api/v1/drive/file/share` for creating shares

## Setting Up the Backend

### Prerequisites

- Java 17+
- Maven
- AWS account with S3 access
- MySQL database
- Firebase account and setup for authentication
- ngrok if you are on local machine

### Configuration

Create `application.properties` with the following configurations:

```properties
# AWS Configuration
cloud.aws.credentials.access-key=YOUR_AWS_ACCESS_KEY
cloud.aws.credentials.secret-key=YOUR_AWS_SECRET_KEY
cloud.aws.region.static=YOUR_AWS_REGION
application.bucket.name=YOUR_S3_BUCKET_NAME

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/filedrive
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
spring.jpa.hibernate.ddl-auto=update

# Server Configuration
server.port=8080
```

### Building and Running

```bash
mvn clean install
```

## API Endpoints

| Method | Endpoint                                | Description                                |
|--------|----------------------------------------|--------------------------------------------|
| POST   | `/api/v1/drive/upload/file`           | Generate pre-signed URL for file upload    |
| GET    | `/api/v1/drive/download/file/{fileId}` | Generate pre-signed URL for file download |
| GET    | `/api/v1/drive/files/{userId}`        | List all files for a specific user         |
| GET    | `/api/v1/drive/file/{uniqueFileId}`   | Get details for a specific file            |
| POST   | `/api/v1/drive/file/share`            | Share a file with other users              |
| GET    | `/api/v1/drive/files/shared/{userId}` | List files shared with a user              |
| DELETE | `/api/v1/drive/file/delete/{fileId}`  | Delete a file                              |
| POST   | `/api/v1/drive/auth/user`             | Authenticate a new/existing user using firebase token                    |
| GET    | `/api/v1/drive/users/search/{searchKeyword}` | Search for users                    |

