package com.hrms.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.security.SecurityUtils;
import com.hrms.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditRecorder {

  private static final Logger log = LoggerFactory.getLogger(AuditRecorder.class);
  private final AuditRepository repository;
  private final ObjectMapper mapper;

  public AuditRecorder(AuditRepository repository, ObjectMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  /** Append-only audit write. Never breaks the business transaction. */
  public void record(String action, String entityType, UUID entityId, Object oldValue, Object newValue) {
    try {
      AuditLog entry = new AuditLog();
      entry.setAction(action);
      entry.setEntityType(entityType);
      entry.setEntityId(entityId);
      try {
        UserPrincipal principal = SecurityUtils.currentUser();
        entry.setUserId(principal.userId());
        entry.setUserEmail(principal.email());
      } catch (Exception ignored) { }
      if (oldValue != null) entry.setOldValues(mapper.writeValueAsString(oldValue));
      if (newValue != null) entry.setNewValues(mapper.writeValueAsString(newValue));
      if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
        HttpServletRequest req = attrs.getRequest();
        String ip = req.getHeader("X-Forwarded-For");
        entry.setIpAddress(ip != null ? ip.split(",")[0].trim() : req.getRemoteAddr());
      }
      entry.setCorrelationId(MDC.get("correlationId"));
      repository.save(entry);
    } catch (Exception e) {
      log.warn("Failed to write audit log for {}: {}", action, e.getMessage());
    }
  }
}
