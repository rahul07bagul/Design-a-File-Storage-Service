package com.example.filedrive.service;

import com.example.filedrive.dto.FileChunkStatusResponse;
import com.example.filedrive.dto.ResumeUploadRequest;
import com.example.filedrive.dto.ResumeUploadResponse;
import com.example.filedrive.model.FileChunk;
import com.example.filedrive.repository.FileChunkRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.model.CompletedPart;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FileChunkService {
    private final FileChunkRepository fileChunkRepository;

    @Autowired
    public FileChunkService(FileChunkRepository uploadStateRepository) {
        this.fileChunkRepository = uploadStateRepository;
    }

    @Transactional
    public void saveUploadState(String fileId, String uploadId, String userId, String fileName,
                                String fileType, Long fileSize, Integer totalChunks) {
        FileChunk state = new FileChunk();
        state.setFileId(fileId);
        state.setUploadId(uploadId);
        state.setUserId(userId);
        state.setFileName(fileName);
        state.setFileType(fileType);
        state.setFileSize(fileSize);
        state.setTotalChunks(totalChunks);
        fileChunkRepository.save(state);
    }

    @Transactional
    public void updateUploadedChunk(String fileId, Integer ChunkNumber, String eTag) {
        Optional<FileChunk> stateOpt = fileChunkRepository.findById(fileId);
        if (stateOpt.isPresent()) {
            FileChunk state = stateOpt.get();

            boolean ChunkExists = state.getUploadedChunk().stream()
                    .anyMatch(Chunk -> Chunk.getChunkNumber().equals(ChunkNumber));

            if (!ChunkExists) {
                FileChunk.UploadedChunk Chunk = new FileChunk.UploadedChunk();
                Chunk.setChunkNumber(ChunkNumber);
                Chunk.setETag(eTag);
                state.getUploadedChunk().add(Chunk);
                fileChunkRepository.save(state);
            }
        }
    }

    public ResumeUploadResponse getUploadState(ResumeUploadRequest request) {
        String fileId = request.getFileId();
        String userId = request.getUserId();
        Optional<FileChunk> stateOpt = fileChunkRepository.findById(fileId);
        if (stateOpt.isPresent()) {
            FileChunk state = stateOpt.get();

            if (!state.getUserId().equals(userId)) {
                return null;
            }

            ResumeUploadResponse response = new ResumeUploadResponse();
            response.setFileId(state.getFileId());
            response.setUploadId(state.getUploadId());
            response.setFileName(state.getFileName());
            response.setFileSize(state.getFileSize());
            response.setTotalChunks(state.getTotalChunks());

            List<ResumeUploadResponse.UploadedChunkInfo> Chunks = state.getUploadedChunk().stream()
                    .map(Chunk -> {
                        ResumeUploadResponse.UploadedChunkInfo info = new ResumeUploadResponse.UploadedChunkInfo();
                        info.setChunkNumber(Chunk.getChunkNumber());
                        info.setETag(Chunk.getETag());
                        return info;
                    })
                    .collect(Collectors.toList());

            response.setCompletedChunks(Chunks);
            return response;
        }

        return null;
    }

    public List<FileChunkStatusResponse> getInProgressUploads(String userId) {
        List<FileChunk> states = fileChunkRepository.findByUserId(userId);

        return states.stream()
                .map(state -> {
                    FileChunkStatusResponse response = new FileChunkStatusResponse();
                    response.setFileId(state.getFileId());
                    response.setFileName(state.getFileName());
                    response.setFileSize(state.getFileSize());
                    response.setTotalChunks(state.getTotalChunks());
                    response.setCompletedChunks(state.getUploadedChunk().size());
                    response.setLastUpdatedAt(state.getLastUpdatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeUploadState(String fileId) {
        fileChunkRepository.deleteById(fileId);
    }

    public List<CompletedPart> getCompletedChunks(String fileId) {
        Optional<FileChunk> stateOpt = fileChunkRepository.findById(fileId);
        if (stateOpt.isPresent()) {
            FileChunk state = stateOpt.get();

            return state.getUploadedChunk().stream()
                    .map(Chunk -> CompletedPart.builder()
                            .partNumber(Chunk.getChunkNumber())
                            .eTag(Chunk.getETag())
                            .build())
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Transactional
    public void cleanupStaleUploads(String userId, int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<FileChunk> staleUploads = fileChunkRepository.findByUserIdAndLastUpdatedAtBefore(userId, cutoffDate);

        fileChunkRepository.deleteAll(staleUploads);
    }
}
