package com.demo.aws.dynamodb;

import com.demo.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamoDbService {

    private final DynamoDbClient dynamoDbClient;
    private final AwsProperties props;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public void saveStatus(ProcessingStatus status) {
        String table = props.getDynamodb().getTableName();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("fileId",    attr(status.getFileId()));
        item.put("status",    attr(status.getStatus()));
        item.put("fileName",  attr(status.getFileName()));
        item.put("timestamp", attr(LocalDateTime.now().format(FORMATTER)));

        if (status.getErrorMessage() != null) {
            item.put("errorMessage", attr(status.getErrorMessage()));
        }

        dynamoDbClient.putItem(r -> r.tableName(table).item(item));
        log.info("Status saved on DynamoDB — fileId={}, status={}", status.getFileId(), status.getStatus());
    }

    public ProcessingStatus findByFileId(String fileId) {
        String table = props.getDynamodb().getTableName();

        Map<String, AttributeValue> key = Map.of(
                "fileId", attr(fileId)
        );

        GetItemResponse response = dynamoDbClient.getItem(r -> r
                .tableName(table)
                .key(key)
        );

        if (!response.hasItem()) {
            return null;
        }

        return toProcessingStatus(response.item());
    }

    public List<ProcessingStatus> findAll() {
        String table = props.getDynamodb().getTableName();

        return dynamoDbClient.scan(r -> r.tableName(table))
                .items()
                .stream()
                .map(this::toProcessingStatus)
                .collect(Collectors.toList());
    }

    private ProcessingStatus toProcessingStatus(Map<String, AttributeValue> item) {
        return ProcessingStatus.builder()
                .fileId(str(item, "fileId"))
                .status(str(item, "status"))
                .fileName(str(item, "fileName"))
                .timestamp(str(item, "timestamp"))
                .errorMessage(str(item, "errorMessage"))
                .build();
    }

    private AttributeValue attr(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private String str(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return v != null ? v.s() : null;
    }
}
