package com.demo.controller;

import com.demo.aws.dynamodb.DynamoDbService;
import com.demo.entity.FileRecord;
import com.demo.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final DynamoDbService dynamoDbService;

    @GetMapping("/{id}/status")
    public ResponseEntity<?> getStatus(@PathVariable String id) {
        var status = dynamoDbService.findByFileId(id);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/status/all")
    public ResponseEntity<?> getAllStatus() {
        return ResponseEntity.ok(dynamoDbService.findAll());
    }

    @PostMapping("/upload")
    public ResponseEntity<FileRecord> upload(@RequestParam("file") MultipartFile file) throws IOException {
        var record = fileService.upload(file);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileRecord> findById(@PathVariable String id) {
        return ResponseEntity.ok(fileService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<FileRecord>> findAll() {
        return ResponseEntity.ok(fileService.findAll());
    }
}
