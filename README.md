# Design GoogleDrive/Dropbox: File Drive - Cloud Storage Application

A secure, scalable file storage backend built with Spring Boot that integrates with AWS S3 for efficient file storage, retrieval, and sharing capabilities.

<p align="center">
  <img src="https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
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
![Demo](https://github.com/rahul07bagul/Design-a-File-Storage-Service/blob/main/assets/File_Drive_Video.gif)

## High Level Design
![HLD](https://github.com/rahul07bagul/Design-a-File-Storage-Service/blob/main/assets/High%20Level.png)

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
  - Creating Multipart upload for large files, AWS S3 manages whole process.

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

### How am I supporting large files and resumable uploads?
- Deciding Upload Strategy (Client-Side)
  - If the file size exceeds 10MB, we use multipart upload.
  - Otherwise, we upload the file directly using a pre-signed URL.
- Initiating Multipart Upload (Backend)
  - The client sends a request to initiate a multipart upload.
  - The backend creates an S3 multipart upload request and receives an uploadId.
  - The uploadId is stored in FileMetadata, and each chunk is tracked in FileChunk.
- Chunking & Uploading Parts (Client-Side)
  - The client splits the file into chunks (e.g., 5MB per chunk).
  - For each chunk:
    - The client requests a pre-signed URL from the backend.
    - The chunk is uploaded directly to S3.
    - The backend updates FileChunk with the chunk's upload status (uploaded or pending).
- Completing the Multipart Upload
  - Once all chunks are uploaded, the client calls completeMultipartUpload.
  - S3 merges all chunks into a single file.
  - The backend updates metadata and stores the final S3 URL in FileMetadata.
- Handling Upload Failures (Resumable Uploads)
  - If an upload fails or stops: the status of uploaded chunks is stored in FileChunk.
  - When the client revisits:
    - The backend retrieves uploaded chunks and offers an option to resume.
    - The user reselects the file, and its metadata (size, name, last modified date) is validated.
    - Already uploaded chunks are skipped, and only remaining chunks are uploaded (from client side).

```bash
┌─────────┐          ┌────────────┐          ┌─────────┐
│  Client │          │  Backend   │          │   AWS   │
│         │          │  Service   │          │   S3    │
└────┬────┘          └─────┬──────┘          └────┬────┘
     │                     │                      │
     │ 1. Decide Upload    │                      │
     │─────────────────────>                      │
     │                     │                      │
     │ 2. Initiate Upload  │                      │
     │─────────────────────>                      │
     │                     │ 3. Create Multipart  │
     │                     │    Upload Request    │
     │                     │────────────────────> │
     │                     │                      │
     │                     │ 4. Receive UploadId  │
     │                     │<──────────────────── │
     │                     │                      │
     │ 5. Split File       │                      │
     │                     │                      │
     │ 6. Request URLs for │                      │
     │    Each Chunk       │                      │
     │─────────────────────>                      │
     │                     │ 7. Generate URLs     │
     │                     │────────────────────> │
     │                     │                      │
     │ 8. Receive URLs     │                      │
     │<─────────────────────                      │
     │                     │                      │
     │ 9. Upload Chunks    │                      │
     │───────────────────────────────────────────>│
     │                     │                      │
     │ 10. Track Chunk     │                      │
     │     Upload Status   │                      │
     │─────────────────────>                      │
     │                     │                      │
     │ 11. Complete Upload │                      │
     │─────────────────────>                      │
     │                     │ 12. Merge Chunks     │
     │                     │────────────────────> │
     │                     │                      │
     │ 13. Upload Complete │                      │
     │<─────────────────────                      │
```

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
