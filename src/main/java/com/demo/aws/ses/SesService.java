package com.demo.aws.ses;

import com.demo.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SesService {

    private final SesClient sesClient;
    private final AwsProperties props;

    public void sendSuccessEmail(String fileName, String fileId) {
        String subject = "Arquivo processado com sucesso";
        String body = buildSuccessBody(fileName, fileId);
        send(subject, body);
    }

    public void sendErrorEmail(String fileName, String fileId, String errorMessage) {
        String subject = "Erro ao processar arquivo";
        String body = buildErrorBody(fileName, fileId, errorMessage);
        send(subject, body);
    }

    private void send(String subject, String htmlBody) {
        String from = props.getSes().getFromEmail();
        String to   = props.getSes().getToEmail();

        SendEmailRequest request = SendEmailRequest.builder()
                .source(from)
                .destination(Destination.builder().toAddresses(to).build())
                .message(software.amazon.awssdk.services.ses.model.Message.builder()
                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                        .body(Body.builder()
                                .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                .build())
                        .build())
                .build();

        try {
            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("E-mail enviado via SES — messageId={}", response.messageId());
        } catch (Exception e) {
            // Loga mas não deixa o erro de e-mail quebrar o fluxo principal
            log.error("Falha ao enviar e-mail SES: {}", e.getMessage());
        }
    }

    private String buildSuccessBody(String fileName, String fileId) {
        return """
            <html><body style="font-family: sans-serif; padding: 24px;">
              <h2 style="color: #2e7d32;">Processamento concluído</h2>
              <p>Seu arquivo foi processado com sucesso.</p>
              <table style="border-collapse: collapse; margin-top: 16px;">
                <tr>
                  <td style="padding: 8px 16px 8px 0; color: #666;">Arquivo</td>
                  <td style="padding: 8px 0;"><strong>%s</strong></td>
                </tr>
                <tr>
                  <td style="padding: 8px 16px 8px 0; color: #666;">ID</td>
                  <td style="padding: 8px 0;"><code>%s</code></td>
                </tr>
                <tr>
                  <td style="padding: 8px 16px 8px 0; color: #666;">Status</td>
                  <td style="padding: 8px 0; color: #2e7d32;"><strong>DONE</strong></td>
                </tr>
              </table>
            </body></html>
            """.formatted(fileName, fileId);
    }

    private String buildErrorBody(String fileName, String fileId, String errorMessage) {
        return """
            <html><body style="font-family: sans-serif; padding: 24px;">
              <h2 style="color: #c62828;">Erro no processamento</h2>
              <p>Ocorreu um erro ao processar seu arquivo.</p>
              <table style="border-collapse: collapse; margin-top: 16px;">
                <tr>
                  <td style="padding: 8px 16px 8px 0; color: #666;">Arquivo</td>
                  <td style="padding: 8px 0;"><strong>%s</strong></td>
                </tr>
                <tr>
                  <td style="padding: 8px 16px 8px 0; color: #666;">ID</td>
                  <td style="padding: 8px 0;"><code>%s</code></td>
                </tr>
                <tr>
                  <td style="padding: 8px 16px 8px 0; color: #666;">Erro</td>
                  <td style="padding: 8px 0; color: #c62828;">%s</td>
                </tr>
              </table>
            </body></html>
            """.formatted(fileName, fileId, errorMessage);
    }
}