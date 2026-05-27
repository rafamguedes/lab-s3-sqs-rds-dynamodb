package com.demo.aws.s3;

import com.demo.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final AwsProperties props;

    public void uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        String bucket = props.getS3().getBucketName();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

        log.info("Arquivo '{}' enviado para S3 bucket '{}'", key, bucket);
    }

    public InputStream downloadFile(String key) {
        String bucket = props.getS3().getBucketName();
        return s3Client.getObject(r -> r.bucket(bucket).key(key));
    }

    public void deleteFile(String key) {
        String bucket = props.getS3().getBucketName();
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
        log.info("Arquivo '{}' removido do S3", key);
    }
}
