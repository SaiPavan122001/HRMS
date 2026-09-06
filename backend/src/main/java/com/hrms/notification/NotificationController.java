package com.hrms.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "In-app notifications")
public class NotificationController {

  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  public record NotificationDto(UUID id, String type, String title, String message, String status,
                                String link, String createdAt) {
    public static NotificationDto from(Notification n) {
      return new NotificationDto(n.getId(), n.getType().name(), n.getTitle(), n.getMessage(),
          n.getStatus().name(), n.getLink(), n.getCreatedAt().toString());
    }
  }

  @GetMapping
  @Operation(summary = "List my notifications")
  public Map<String, Object> list() {
    UUID userId = com.hrms.security.SecurityUtils.currentUserId();
    List<NotificationDto> items = service.list(userId).stream().map(NotificationDto::from).toList();
    return Map.of("items", items, "unread", service.unreadCount(userId));
  }

  @GetMapping("/unread-count")
  public Map<String, Long> unreadCount() {
    return Map.of("unread", service.unreadCount(com.hrms.security.SecurityUtils.currentUserId()));
  }

  @PostMapping("/{id}/read")
  @Operation(summary = "Mark notification as read")
  public ResponseEntity<Void> markRead(@PathVariable UUID id) {
    service.markRead(com.hrms.security.SecurityUtils.currentUserId(), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/read-all")
  @Operation(summary = "Mark all notifications as read")
  public Map<String, Integer> markAllRead() {
    return Map.of("updated", service.markAllRead(com.hrms.security.SecurityUtils.currentUserId()));
  }
}
