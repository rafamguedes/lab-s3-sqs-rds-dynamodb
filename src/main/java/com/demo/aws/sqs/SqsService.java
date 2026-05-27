package com.demo.aws.sqs;

import com.demo.config.AwsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsService {

    private final SqsClient sqsClient;
    private final AwsProperties props;
    private final ObjectMapper objectMapper;

    private String getQueueUrl() {
        return sqsClient.getQueueUrl(r -> r.queueName(props.getSqs().getQueueName()))
                .queueUrl();
    }

    public void sendMessage(Object payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            SendMessageResponse response = sqsClient.sendMessage(r -> r
                    .queueUrl(getQueueUrl())
                    .messageBody(body)
            );
            log.info("Message sent to queue — messageId={}", response.messageId());
        } catch (Exception e) {
            throw new RuntimeException("Error to sent message to queue", e);
        }
    }

    public List<Message> receiveMessages(int maxMessages) {
        return sqsClient.receiveMessage(r -> r
                .queueUrl(getQueueUrl())
                .maxNumberOfMessages(maxMessages)
                .waitTimeSeconds(5)
                .visibilityTimeout(30)
        ).messages();
    }

    public void deleteMessage(String receiptHandle) {
        sqsClient.deleteMessage(r -> r
                .queueUrl(getQueueUrl())
                .receiptHandle(receiptHandle)
        );
        log.info("Message remove from queue");
    }
}
