package com.hrms.events;

import com.hrms.attendance.AttendanceService;
import com.hrms.auth.repo.UserRepository;
import com.hrms.config.AppProperties;
import com.hrms.email.EmailService;
import com.hrms.email.EmailTemplates;
import com.hrms.employee.Profile;
import com.hrms.employee.ProfileRepository;
import com.hrms.leave.LeaveType;
import com.hrms.leave.LeaveTypeRepository;
import com.hrms.notification.NotificationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumes HRMS events and produces notifications (in-app + real SMTP email).
 * Idempotent via Redis dedup keys; failures go to the DLT after retries.
 */
@Component
public class HrmsEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(HrmsEventConsumer.class);
  private static final Duration IDEMPOTENCY_TTL = Duration.ofDays(7);

  private final NotificationService notifications;
  private final EmailService emailService;
  private final ProfileRepository profileRepository;
  private final UserRepository userRepository;
  private final LeaveTypeRepository leaveTypeRepository;
  private final StringRedisTemplate redis;
  private final AppProperties props;
  private final Counter processed;
  private final Counter failed;
  private final KafkaTemplate<String, HrmsEvent> kafkaTemplate;
  private final String dltTopic;

  public HrmsEventConsumer(NotificationService notifications, EmailService emailService,
                           ProfileRepository profileRepository, UserRepository userRepository,
                           LeaveTypeRepository leaveTypeRepository, StringRedisTemplate redis,
                           AppProperties props, MeterRegistry registry,
                           KafkaTemplate<String, HrmsEvent> kafkaTemplate,
                           @org.springframework.beans.factory.annotation.Value("${spring.kafka.topic.hrms-events-dlt}") String dltTopic) {
    this.notifications = notifications;
    this.emailService = emailService;
    this.profileRepository = profileRepository;
    this.userRepository = userRepository;
    this.leaveTypeRepository = leaveTypeRepository;
    this.redis = redis;
    this.props = props;
    this.processed = Counter.builder("hrms.events.processed").register(registry);
    this.failed = Counter.builder("hrms.events.failed").register(registry);
    this.kafkaTemplate = kafkaTemplate;
    this.dltTopic = dltTopic;
  }

  
  @KafkaListener(topics = "${spring.kafka.topic.hrms-events}", groupId = "hrms-notification")
  public void onEvent(HrmsEvent event, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
    if (event == null || event.type() == null) return;
    String dedupKey = "hrms:evdone:" + event.eventId();
    try {
      Boolean first = redis.opsForValue().setIfAbsent(dedupKey, "1", IDEMPOTENCY_TTL);
      if (Boolean.FALSE.equals(first)) {
        log.info("Skipping already-processed event {}", event.eventId());
        return;
      }
      handle(event);
      processed.increment();
    } catch (Exception e) {
      failed.increment();
      redis.delete(dedupKey);
      log.error("Event {} processing failed: {}", event.type(), e.getMessage());
      throw e;
    }
  }

  @org.springframework.kafka.annotation.DltHandler
  public void dlt(HrmsEvent event, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
    log.error("Event moved to DLT: type={} key={}", event == null ? "null" : event.type(), key);
  }

  @SuppressWarnings("unchecked")
  private void handle(HrmsEvent event) {
    Map<String, Object> p = event.payload() == null ? Map.of() : event.payload();
    String link = props.frontendUrl();
    switch (event.type()) {
      case "EMAIL_VERIFICATION" -> {
        var template = EmailTemplates.verification((String) p.get("link"));
        emailService.send((String) p.get("email"), template.subject(), template.html(), template.text());
      }
      case "PASSWORD_RESET_REQUESTED" -> {
        var template = EmailTemplates.passwordReset((String) p.get("link"));
        emailService.send((String) p.get("email"), template.subject(), template.html(), template.text());
      }
      case "LEAVE_REQUESTED" -> {
        Profile employee = profile(p.get("profileId"));
        LeaveType type = leaveType(p.get("leaveTypeId"));
        String dates = p.get("startDate") + " to " + p.get("endDate");
        // Notify managers and HR in-app + email
        for (Profile manager : profileRepository.findByIsActiveTrue()) {
          if (manager.getId().equals(employee.getId())) continue;
          if (!isApprover(manager)) continue;
          notifications.createInApp(manager.getUserId(), "New leave request",
              employee.getFirstName() + " " + employee.getLastName() + " requested " + type.getName()
                  + " (" + dates + ")", "/leave", "leave_request", uuid(p.get("requestId")));
          var t = EmailTemplates.leaveRequested(manager.getFirstName(),
              employee.getFirstName() + " " + employee.getLastName(), type.getName(), dates, link + "/leave");
          emailService.send(manager.getEmail(), t.subject(), t.html(), t.text());
        }
      }
      case "LEAVE_APPROVED", "LEAVE_REJECTED" -> {
        Profile employee = profile(p.get("profileId"));
        boolean approved = "LEAVE_APPROVED".equals(event.type());
        notifications.createInApp(employee.getUserId(),
            approved ? "Leave approved" : "Leave rejected",
            approved ? "Your leave request was approved." : "Your leave request was rejected.",
            "/leave", "leave_request", uuid(p.get("requestId")));
        var t = EmailTemplates.leaveDecision(employee.getFirstName(), "leave",
            "your requested dates", approved, (String) p.get("reason"), link + "/leave");
        emailService.send(employee.getEmail(), t.subject(), t.html(), t.text());
      }
      case "TIMESHEET_SUBMITTED" -> {
        Profile employee = profile(p.get("profileId"));
        String week = p.get("weekStart") + " - " + p.get("weekEnd");
        for (Profile approver : profileRepository.findByIsActiveTrue()) {
          if (approver.getId().equals(employee.getId())) continue;
          if (!isApprover(approver)) continue;
          notifications.createInApp(approver.getUserId(), "Timesheet submitted",
              employee.getFirstName() + " " + employee.getLastName() + " submitted a timesheet (" + week + ")",
              "/timesheets", "timesheet", uuid(p.get("timesheetId")));
          var t = EmailTemplates.timesheetSubmitted(approver.getFirstName(),
              employee.getFirstName() + " " + employee.getLastName(), week, link + "/timesheets");
          emailService.send(approver.getEmail(), t.subject(), t.html(), t.text());
        }
      }
      case "TIMESHEET_APPROVED", "TIMESHEET_REJECTED" -> {
        Profile employee = profile(p.get("profileId"));
        boolean approved = "TIMESHEET_APPROVED".equals(event.type());
        notifications.createInApp(employee.getUserId(),
            approved ? "Timesheet approved" : "Timesheet rejected",
            approved ? "Your timesheet was approved." : "Your timesheet was rejected.",
            "/timesheets", "timesheet", uuid(p.get("timesheetId")));
        var t = EmailTemplates.timesheetDecision(employee.getFirstName(),
            p.get("weekStart") + " - " + p.get("weekEnd"), approved, (String) p.get("reason"), link + "/timesheets");
        emailService.send(employee.getEmail(), t.subject(), t.html(), t.text());
      }
      case "ATTENDANCE_CORRECTED" -> {
        Profile employee = profile(p.get("profileId"));
        notifications.createInApp(employee.getUserId(), "Attendance corrected",
            "An attendance correction was applied to one of your records.", "/attendance",
            "attendance", uuid(p.get("attendanceId")));
      }
      default -> log.debug("No handler for event type {}", event.type());
    }
  }

  private boolean isApprover(Profile manager) {
    return manager.getManager() == null; // top-level profiles (HR/admins) act as approvers fallback
  }

  private Profile profile(Object id) {
    return profileRepository.findById(UUID.fromString(String.valueOf(id)))
        .orElseThrow(() -> new IllegalStateException("Profile not found: " + id));
  }

  private LeaveType leaveType(Object id) {
    return leaveTypeRepository.findById(UUID.fromString(String.valueOf(id))).orElse(null);
  }

  private UUID uuid(Object id) {
    return id == null ? null : UUID.fromString(String.valueOf(id));
  }
}


