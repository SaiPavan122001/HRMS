package com.hrms.events;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxDispatcher {

  private final OutboxService outboxService;

  public OutboxDispatcher(OutboxService outboxService) {
    this.outboxService = outboxService;
  }

  @Scheduled(fixedDelayString = "2000", initialDelay = 5000)
  public void dispatch() {
    outboxService.dispatchPending();
  }

  @Scheduled(cron = "0 0 3 * * *")
  public void purge() {
    outboxService.purgeProcessed();
  }
}
