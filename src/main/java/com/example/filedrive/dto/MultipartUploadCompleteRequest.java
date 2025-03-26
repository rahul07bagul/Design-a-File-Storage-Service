package com.example.filedrive.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class MultipartUploadCompleteRequest {
    private String fileId;
    private String uploadId;
    private List<ChunkDetail> parts;

    @Getter
    @Setter
    public static class ChunkDetail {
        private int chunkNumber;
        private String eTag;
    }
}