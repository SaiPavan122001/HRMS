package com.hrms.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

  @Value("${spring.kafka.topic.hrms-events}")
  private String eventsTopic;

  @Value("${spring.kafka.topic.hrms-events-dlt}")
  private String dltTopic;

  @Bean
  public NewTopic hrmsEventsTopic() {
    return TopicBuilder.name(eventsTopic).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic hrmsEventsDltTopic() {
    return TopicBuilder.name(dltTopic).partitions(1).replicas(1).build();
  }
}
