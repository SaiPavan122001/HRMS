package com.hrms.attendance;

import com.hrms.config.AppProperties;
import com.hrms.email.EmailService;
import com.hrms.email.EmailTemplates;
import com.hrms.employee.Profile;
import com.hrms.employee.ProfileRepository;
import com.hrms.notification.NotificationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily attendance reminders: 10:00 check-in nudge, 18:00 check-out nudge. */
@Component
public class AttendanceReminderJob {

  private final AttendanceRepository attendanceRepository;
  private final ProfileRepository profileRepository;
  private final NotificationService notifications;
  private final EmailService emailService;
  private final AppProperties props;
  private final Counter reminderCounter;

  public AttendanceReminderJob(AttendanceRepository attendanceRepository, ProfileRepository profileRepository,
                               NotificationService notifications, EmailService emailService, AppProperties props,
                               MeterRegistry registry) {
    this.attendanceRepository = attendanceRepository;
    this.profileRepository = profileRepository;
    this.notifications = notifications;
    this.emailService = emailService;
    this.props = props;
    this.reminderCounter = Counter.builder("hrms.reminders.attendance").register(registry);
  }

  @Scheduled(cron = "0 0 10 * * MON-FRI", zone = "UTC")
  public void checkInReminder() {
    remind("check_in");
  }

  @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "UTC")
  public void checkOutReminder() {
    remind("check_out");
  }

  private void remind(String kind) {
    LocalDate today = LocalDate.now();
    List<Profile> employees = profileRepository.findByIsActiveTrue();
    for (Profile profile : employees) {
      var record = attendanceRepository.findByProfileIdAndDate(profile.getId(), today);
      boolean needsReminder = record.isEmpty()
          ? "check_in".equals(kind)
          : "check_out".equals(kind) && record.get().getCheckOutTime() == null;
      if (!needsReminder) continue;
      reminderCounter.increment();
      try {
        notifications.createInApp(profile.getUserId(), "Attendance reminder",
            "check_in".equals(kind) ? "You have not checked in yet today." : "You have not checked out yet today.",
            "/attendance", "attendance", null);
      } catch (Exception ignored) { }
      var template = EmailTemplates.reminder(profile.getFirstName(), kind, props.frontendUrl() + "/attendance");
      emailService.send(profile.getEmail(), template.subject(), template.html(), template.text());
    }
  }
}
