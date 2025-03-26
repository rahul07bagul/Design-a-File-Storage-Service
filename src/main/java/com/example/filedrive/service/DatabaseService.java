package com.example.filedrive.service;

import com.example.filedrive.model.FileMetadata;
import com.example.filedrive.model.FileShare;
import com.example.filedrive.model.FileStatus;
import com.example.filedrive.repository.FileMetadataRepository;
import com.example.filedrive.repository.FileShareRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseService {

    private FileMetadataRepository fileMetadataRepository;
    private FileShareRepository fileShareRepository;
    private S3Service s3Service;

    @Autowired
    public DatabaseService(FileMetadataRepository fileMetadataRepository, FileShareRepository fileShareRepository, S3Service s3Service) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileShareRepository = fileShareRepository;
        this.s3Service = s3Service;
    }

    public boolean createFileMetadata(FileMetadata fileMetadata) {
        try {
            fileMetadataRepository.save(fileMetadata);
            return true;
        }catch (Exception e){
            System.out.println(e.getMessage());
            return false;
        }
    }

    public boolean updateFileMetadata(FileMetadata fileMetadata) {
        fileMetadataRepository.save(fileMetadata);
        return true;
    }

    public FileMetadata getFileMetadata(String uniqueFileId) {
        return fileMetadataRepository.findByFileId(uniqueFileId);
    }

    public List<FileMetadata> getAllFiles(String userId){
        return fileMetadataRepository.findByUser_IdAndStatus(userId, FileStatus.UPLOADED.toString());
    }

    public Boolean shareFile(List<FileShare> fileShareList) {
        fileShareRepository.saveAll(fileShareList);
        return true;
    }

    public List<FileShare> getAllFileShares(String userId) {
        return fileShareRepository.findByUserId(userId);
    }

    public boolean deleteFile(String uniqueFileId) {
        try {
            FileMetadata fileMetadata = fileMetadataRepository.findByFileId(uniqueFileId);
            if (fileMetadata != null) {
                fileMetadataRepository.deleteById(uniqueFileId);
                s3Service.deleteFile(fileMetadata.getFilePath());

                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
