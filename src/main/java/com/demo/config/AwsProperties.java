package com.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

    private String region;
    private String endpoint;
    private Credentials credentials = new Credentials();
    private S3 s3 = new S3();
    private Sqs sqs = new Sqs();
    private DynamoDb dynamodb = new DynamoDb();
    private Ses ses = new Ses();

    @Data
    public static class Credentials {
        private String accessKey;
        private String secretKey;
    }

    @Data
    public static class S3 {
        private String bucketName;
    }

    @Data
    public static class Sqs {
        private String queueName;
    }

    @Data
    public static class DynamoDb {
        private String tableName;
    }

    @Data
    public static class Ses {
        private String fromEmail;
        private String toEmail;
    }
}
