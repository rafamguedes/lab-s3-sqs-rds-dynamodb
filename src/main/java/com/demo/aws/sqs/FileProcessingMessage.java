package com.demo.aws.sqs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessingMessage {
    private String fileId;
    private String s3Key;
    private String fileName;
    private String contentType;
}
