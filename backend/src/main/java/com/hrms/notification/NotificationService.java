package com.hrms.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists notifications and pushes real-time updates over WebSocket. */
@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
  private final NotificationRepository repository;
  private final NotificationWebSocketHandler webSocket;
  private final ObjectMapper mapper;

  public NotificationService(NotificationRepository repository, NotificationWebSocketHandler webSocket,
                             ObjectMapper mapper) {
    this.repository = repository;
    this.webSocket = webSocket;
    this.mapper = mapper;
  }

  @Transactional
  public Notification createInApp(UUID userId, String title, String message, String link,
                                  String referenceType, UUID referenceId) {
    Notification n = new Notification();
    n.setUserId(userId);
    n.setType(Notification.Type.IN_APP);
    n.setTitle(title);
    n.setMessage(message);
    n.setLink(link);
    n.setReferenceType(referenceType);
    n.setReferenceId(referenceId);
    n = repository.save(n);
    push(n);
    return n;
  }

  @Transactional
  public Notification createEmailRecord(UUID userId, String emailTo, String title) {
    Notification n = new Notification();
    n.setUserId(userId);
    n.setType(Notification.Type.EMAIL);
    n.setTitle(title);
    n.setEmailTo(emailTo);
    n.setStatus(Notification.Status.PENDING);
    return repository.save(n);
  }

  @Transactional
  public void markSent(UUID notificationId, boolean success) {
    repository.findById(notificationId).ifPresent(n -> {
      n.setStatus(success ? Notification.Status.SENT : Notification.Status.FAILED);
      n.setSentAt(success ? java.time.Instant.now() : null);
      repository.save(n);
    });
  }

  private void push(Notification n) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("id", n.getId().toString());
      payload.put("title", n.getTitle());
      payload.put("message", n.getMessage());
      payload.put("link", n.getLink());
      payload.put("type", n.getType().name());
      payload.put("status", n.getStatus().name());
      payload.put("createdAt", n.getCreatedAt().toString());
      webSocket.push(n.getUserId(), mapper.writeValueAsString(payload));
    } catch (Exception e) {
      log.warn("Failed to push real-time notification: {}", e.getMessage());
    }
  }

  public java.util.List<Notification> list(UUID userId) {
    return repository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
  }

  public long unreadCount(UUID userId) {
    return repository.countByUserIdAndTypeAndStatusNot(userId, Notification.Type.IN_APP, Notification.Status.READ);
  }

  @Transactional
  public void markRead(UUID userId, UUID notificationId) {
    repository.findById(notificationId).ifPresent(n -> {
      if (n.getUserId().equals(userId) && n.getStatus() != Notification.Status.READ) {
        n.setStatus(Notification.Status.READ);
        n.setReadAt(java.time.Instant.now());
        repository.save(n);
      }
    });
  }

  @Transactional
  public int markAllRead(UUID userId) {
    var unread = repository.findByUserIdAndTypeAndStatus(userId, Notification.Type.IN_APP, Notification.Status.PENDING);
    int count = 0;
    for (Notification n : unread) {
      n.setStatus(Notification.Status.READ);
      n.setReadAt(java.time.Instant.now());
      repository.save(n);
      count++;
    }
    return count;
  }
}
