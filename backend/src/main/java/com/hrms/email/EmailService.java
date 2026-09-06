package com.hrms.email;

import com.hrms.config.AppProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Real SMTP email delivery (SendGrid relay by default).
 * Invoked asynchronously from Kafka consumers; failures are retried with backoff and
 * counted via Micrometer. Email failures never fail business transactions.
 */
@Service
public class EmailService {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailService.class);
  private static final int MAX_ATTEMPTS = 3;

  private final JavaMailSender mailSender;
  private final AppProperties props;
  private final String fromAddress;
  private final Counter sentCounter;
  private final Counter failedCounter;

  public EmailService(JavaMailSender mailSender, AppProperties props, MeterRegistry registry,
                      @Value("${spring.mail.from}") String fromAddress) {
    this.mailSender = mailSender;
    this.props = props;
    this.fromAddress = fromAddress;
    this.sentCounter = Counter.builder("hrms.email.sent").description("Emails delivered").register(registry);
    this.failedCounter = Counter.builder("hrms.email.failed").description("Email delivery failures").register(registry);
  }

  public void send(String to, String subject, String htmlBody, String textBody) {
    if (!props.mail().enabled()) {
      log.info("Email sending disabled (MAIL_ENABLED=false). Would send to '{}' subject '{}'", to, subject);
      return;
    }
    Exception lastError = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(textBody, htmlBody);
        mailSender.send(message);
        sentCounter.increment();
        log.info("Email sent to '{}' subject '{}'", to, subject);
        return;
      } catch (Exception e) {
        lastError = e;
        failedCounter.increment();
        log.warn("Email attempt {}/{} failed for {}: {}", attempt, MAX_ATTEMPTS, to, e.getMessage());
        try {
          Thread.sleep(1000L * attempt);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
    log.error("Email delivery failed permanently to {} subject '{}': {}", to, subject,
        lastError != null ? lastError.getMessage() : "unknown");
  }
}
