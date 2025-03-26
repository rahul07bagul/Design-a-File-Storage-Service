package com.example.filedrive.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
public class FileMetadata {

    @Id
    private String fileId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String fileName;
    private String fileType;
    private long fileSize;

    @Column(columnDefinition="TEXT")
    private String s3Url;

    private String uploadId;
    private Integer totalChunks;

    private String status;
    private String filePath;
    private Date lastModifiedData;

    @OneToMany(mappedBy = "fileMetadata", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileShare> fileShares;
}
