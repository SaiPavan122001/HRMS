package com.hrms.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
  List<Notification> findByUserIdAndTypeAndStatus(UUID userId, Notification.Type type, Notification.Status status);
  long countByUserIdAndTypeAndStatusNot(UUID userId, Notification.Type type, Notification.Status status);
}
