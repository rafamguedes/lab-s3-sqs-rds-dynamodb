package com.demo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalStackInitializer implements ApplicationRunner {

    private final S3Client s3Client;
    private final SqsClient sqsClient;
    private final DynamoDbClient dynamoDbClient;
    private final SesClient sesClient;

    private final AwsProperties props;

    @Override
    public void run(ApplicationArguments args) {
        createBucket();
        createQueue();
        createDynamoTable();
        verifySesEmail();
    }

    private void createBucket() {
        String bucket = props.getS3().getBucketName();
        try {
            s3Client.headBucket(r -> r.bucket(bucket));
            log.info("S3 bucket '{}' already exists", bucket);
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(r -> r.bucket(bucket));
            log.info("S3 bucket '{}' created", bucket);
        }
    }

    private void createQueue() {
        String queue = props.getSqs().getQueueName();
        try {
            sqsClient.createQueue(r -> r.queueName(queue));
            log.info("SQS queue '{}' created", queue);
        } catch (QueueNameExistsException e) {
            log.info("SQS queue '{}' already exists", queue);
        }
    }

    private void createDynamoTable() {
        String table = props.getDynamodb().getTableName();
        try {
            dynamoDbClient.describeTable(r -> r.tableName(table));
            log.info("DynamoDB table '{}' already exists", table);
        } catch (ResourceNotFoundException e) {
            dynamoDbClient.createTable(r -> r
                    .tableName(table)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("fileId")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("fileId")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
            );
            log.info("DynamoDB table '{}' created", table);
        }
    }

    private void verifySesEmail() {
        String email = props.getSes().getFromEmail();
        sesClient.verifyEmailIdentity(r -> r.emailAddress(email));
        log.info("SES email '{}' checking", email);
    }
}
