package com.hrms.config;

import com.hrms.events.HrmsEvent;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Retry twice with backoff, then publish failed events to the DLT topic. */
@Configuration
public class KafkaErrorConfig {

  @Bean
  public DefaultErrorHandler errorHandler(
      org.springframework.kafka.core.KafkaTemplate<String, HrmsEvent> template,
      @Value("${spring.kafka.topic.hrms-events-dlt}") String dltTopic) {
    var recoverer = new DeadLetterPublishingRecoverer(
        (KafkaOperations<String, Object>) (KafkaOperations<?, ?>) template,
        (record, ex) -> new TopicPartition(dltTopic, record.partition()));
    var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(3000L, 2));
    return handler;
  }

  private interface Strict {}

  interface MapAlias extends Map<String, Object> {}
}

