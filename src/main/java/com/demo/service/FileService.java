package com.demo.service;

import com.demo.aws.s3.S3Service;
import com.demo.aws.sqs.FileProcessingMessage;
import com.demo.aws.sqs.SqsService;
import com.demo.entity.FileRecord;
import com.demo.entity.FileStatus;
import com.demo.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Service s3Service;
    private final SqsService sqsService;

    private final FileRecordRepository repository;

    public FileRecord upload(MultipartFile file) throws IOException {
        validateFile(file);

        var s3Key = "uploads/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        var record = new FileRecord();
        record.setFileName(file.getOriginalFilename());
        record.setContentType(file.getContentType());
        record.setStatus(FileStatus.UPLOADED);
        record.setS3Key(s3Key);

        record = repository.save(record);

        try {
            s3Service.uploadFile(s3Key, file.getInputStream(), file.getSize(), file.getContentType());

            // Publish to the SQS queue after successful upload
            sqsService.sendMessage(FileProcessingMessage.builder()
                    .fileId(record.getId())
                    .s3Key(s3Key)
                    .fileName(record.getFileName())
                    .contentType(record.getContentType())
                    .build());

            record.setStatus(FileStatus.QUEUED);
            record = repository.save(record);

            log.info("Queued file — fileId={}", record.getId());
        } catch (Exception e) {
            record.setStatus(FileStatus.ERROR);
            record.setErrorMessage("Upload failed S3: " + e.getMessage());
            repository.save(record);
            throw e;
        }

        return record;
    }

    public FileRecord findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found: " + id));
    }

    public List<FileRecord> findAll() {
        return repository.findAll();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        String ct = file.getContentType();
        if (ct == null || (!ct.equals("application/pdf") && !ct.equals("application/xml") && !ct.equals("text/xml"))) {
            throw new IllegalArgumentException("Only PDF and XML files are accepted. Received: " + ct);
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File exceeds 10MB limit.");
        }
    }
}