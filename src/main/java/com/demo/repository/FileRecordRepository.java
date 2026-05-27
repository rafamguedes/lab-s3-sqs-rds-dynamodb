package com.demo.repository;

import com.demo.entity.FileRecord;
import com.demo.entity.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, String> {
    List<FileRecord> findByStatus(FileStatus status);
}
