package com.demo.aws.dynamodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingStatus {
    private String fileId;
    private String status;
    private String fileName;
    private String timestamp;
    private String errorMessage;
}
