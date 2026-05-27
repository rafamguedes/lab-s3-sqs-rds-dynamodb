package com.demo.worker;

import com.demo.aws.dynamodb.DynamoDbService;
import com.demo.aws.dynamodb.ProcessingStatus;
import com.demo.aws.ses.SesService;
import com.demo.aws.sqs.FileProcessingMessage;
import com.demo.aws.sqs.SqsService;
import com.demo.entity.FileRecord;
import com.demo.entity.FileStatus;
import com.demo.repository.FileRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessingWorker {

    private final SqsService sqsService;
    private final DynamoDbService dynamoDbService;
    private final SesService sesService;

    private final FileRecordRepository repository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void processQueue() {
        List<Message> messages = sqsService.receiveMessages(5);

        if (messages.isEmpty()) {
            log.debug("Nenhuma mensagem na fila");
            return;
        }

        log.info("Recebidas {} mensagens da fila", messages.size());

        for (Message message : messages) {
            processMessage(message);
        }
    }

    private void processMessage(Message message) {
        FileProcessingMessage payload = null;
        try {
            payload = objectMapper.readValue(message.body(), FileProcessingMessage.class);
            log.info("Processando fileId={}", payload.getFileId());

            FileRecord record = repository.findById(payload.getFileId())
                    .orElseThrow(() -> new RuntimeException("FileRecord não encontrado"));

            // PROCESSING
            updateStatus(record, FileStatus.PROCESSING, payload.getFileName(), null);

            processFile(record, payload);

            // DONE
            updateStatus(record, FileStatus.DONE, payload.getFileName(), null);

            sqsService.deleteMessage(message.receiptHandle());
            log.info("FileId={} processado com sucesso", payload.getFileId());

        } catch (Exception e) {
            log.error("Erro ao processar — fileId={}: {}",
                    payload != null ? payload.getFileId() : "desconhecido", e.getMessage(), e);

            if (payload != null) {
                final String fileId = payload.getFileId();
                final String fileName = payload.getFileName();
                final String errMsg = e.getMessage();

                repository.findById(fileId).ifPresent(record -> {
                    updateStatus(record, FileStatus.ERROR, fileName, errMsg);
                });
            }
        }
    }

    private void updateStatus(FileRecord record, FileStatus status, String fileName, String errorMessage) {
        // Atualiza Postgres
        record.setStatus(status);
        record.setErrorMessage(errorMessage);
        repository.save(record);

        // Registra evento no DynamoDB
        dynamoDbService.saveStatus(ProcessingStatus.builder()
                .fileId(record.getId())
                .status(status.name())
                .fileName(fileName)
                .errorMessage(errorMessage)
                .build());

        if (status == FileStatus.DONE) {
            sesService.sendSuccessEmail(fileName, record.getId());
        } else if (status == FileStatus.ERROR) {
            sesService.sendErrorEmail(fileName, record.getId(), errorMessage);
        }
    }

    private void processFile(FileRecord record, FileProcessingMessage payload) {
        // Por enquanto só simula o processamento
        // Na Etapa 4 vamos integrar com DynamoDB para rastrear o status aqui
        log.info("Processando arquivo '{}' do tipo '{}'", payload.getFileName(), payload.getContentType());

        // Simula tempo de processamento
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
