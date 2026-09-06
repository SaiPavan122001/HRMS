package com.hrms.notification;

import com.hrms.security.JwtTokenProvider;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

/** Raw WebSocket handler for real-time notifications. Authenticated via ?token=<access JWT>. */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
  private final Map<UUID, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
  private final JwtTokenProvider tokenProvider;

  public NotificationWebSocketHandler(JwtTokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    UUID userId = extractUserId(session);
    if (userId == null) {
      close(session, CloseStatus.NOT_ACCEPTABLE);
      return;
    }
    sessionsByUser.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
    log.debug("WebSocket connected for user {}", userId);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    UUID userId = extractUserId(session);
    if (userId != null) {
      Set<WebSocketSession> sessions = sessionsByUser.get(userId);
      if (sessions != null) {
        sessions.remove(session);
        if (sessions.isEmpty()) sessionsByUser.remove(userId, sessions);
      }
    }
  }

  public boolean push(UUID userId, String json) {
    Set<WebSocketSession> sessions = sessionsByUser.get(userId);
    if (sessions == null || sessions.isEmpty()) return false;
    boolean delivered = false;
    for (WebSocketSession session : sessions) {
      try {
        if (session.isOpen()) {
          synchronized (session) {
            session.sendMessage(new TextMessage(json));
          }
          delivered = true;
        }
      } catch (IOException e) {
        log.warn("WebSocket push failed for user {}: {}", userId, e.getMessage());
      }
    }
    return delivered;
  }

  private UUID extractUserId(WebSocketSession session) {
    URI uri = session.getUri();
    if (uri == null) return null;
    String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
    if (token == null || !tokenProvider.isValid(token)) return null;
    return tokenProvider.getUserId(token);
  }

  private void close(WebSocketSession session, CloseStatus status) {
    try { session.close(status); } catch (IOException ignored) { }
  }
}
