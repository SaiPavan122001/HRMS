package com.hrms.email;

import org.springframework.stereotype.Component;

/** HTML email templates with plain-text fallbacks. */
@Component
public class EmailTemplates {

  public record Email(String subject, String html, String text) {}

  private static String wrap(String title, String bodyHtml) {
    return """
        <!DOCTYPE html>
        <html><body style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:24px;">
        <h2 style="color:#1a73e8;">%s</h2>
        %s
        <hr style="border:none;border-top:1px solid #eee;margin-top:24px;"/>
        <p style="color:#888;font-size:12px;">Sent by HRMS. If you did not request this, you can safely ignore this email.</p>
        </body></html>
        """.formatted(esc(title), bodyHtml);
  }

  public static Email verification(String link) {
    String body = "<p>Welcome to HRMS!</p><p>Please confirm your email address:</p>"
        + button(link, "Verify Email") + "<p>This link expires in 24 hours.</p>";
    String text = "Welcome to HRMS! Verify your email: " + link + " (expires in 24 hours)";
    return new Email("Verify your HRMS email", wrap("Verify your email", body), text);
  }

  public static Email passwordReset(String link) {
    String body = "<p>We received a request to reset your HRMS password.</p>"
        + button(link, "Reset Password")
        + "<p>This link expires in 1 hour. If you did not request this, you can safely ignore it.</p>";
    String text = "Reset your HRMS password: " + link + " (expires in 1 hour)";
    return new Email("Reset your HRMS password", wrap("Reset your password", body), text);
  }

  public static Email leaveRequested(String managerName, String employeeName, String typeName, String dates, String link) {
    String body = "<p>Hi " + esc(managerName) + ",</p><p><b>" + esc(employeeName) + "</b> has requested <b>"
        + esc(typeName) + "</b> leave for " + esc(dates) + ".</p>" + button(link, "Open HRMS to review");
    String text = managerName + ", " + employeeName + " requested " + typeName + " leave for " + dates + ". Review: " + link;
    return new Email("Leave request: " + employeeName, wrap("Leave request awaiting approval", body), text);
  }

  public static Email leaveDecision(String employeeName, String typeName, String dates, boolean approved, String reason, String link) {
    String verdict = approved ? "approved" : "rejected";
    String body = "<p>Hi " + esc(employeeName) + ",</p><p>Your <b>" + esc(typeName) + "</b> leave (" + esc(dates)
        + ") was <b>" + verdict + "</b>.</p>"
        + (reason != null && !reason.isBlank() ? "<p>Reason: " + esc(reason) + "</p>" : "")
        + button(link, "View in HRMS");
    String text = "Hi " + employeeName + ", your " + typeName + " leave (" + dates + ") was " + verdict
        + (reason != null && !reason.isBlank() ? ". Reason: " + reason : ".") + " Link: " + link;
    return new Email("Your leave request was " + verdict, wrap("Leave request " + verdict, body), text);
  }

  public static Email timesheetDecision(String employeeName, String week, boolean approved, String reason, String link) {
    String verdict = approved ? "approved" : "rejected";
    String body = "<p>Hi " + esc(employeeName) + ",</p><p>Your timesheet for week " + esc(week)
        + " was <b>" + verdict + "</b>.</p>"
        + (reason != null && !reason.isBlank() ? "<p>Reason: " + esc(reason) + "</p>" : "")
        + button(link, "View in HRMS");
    String text = "Hi " + employeeName + ", your timesheet for week " + week + " was " + verdict
        + (reason != null && !reason.isBlank() ? ". Reason: " + reason : ".") + " Link: " + link;
    return new Email("Timesheet " + verdict, wrap("Timesheet " + verdict, body), text);
  }

  public static Email timesheetSubmitted(String managerName, String employeeName, String week, String link) {
    String body = "<p>Hi " + esc(managerName) + ",</p><p><b>" + esc(employeeName)
        + "</b> submitted their timesheet for week " + esc(week) + ".</p>" + button(link, "Review in HRMS");
    String text = managerName + ", " + employeeName + " submitted a timesheet for week " + week + ". Link: " + link;
    return new Email("Timesheet submitted: " + employeeName, wrap("Timesheet submitted", body), text);
  }

  public static Email reminder(String employeeName, String kind, String link) {
    String action = "check_in".equals(kind) ? "check in" : "check out";
    String body = "<p>Hi " + esc(employeeName) + ",</p><p>This is a reminder to " + action + " for today.</p>"
        + button(link, "Open HRMS");
    String text = "Hi " + employeeName + ", reminder to " + action + ". Link: " + link;
    return new Email("HRMS reminder", wrap(action + " reminder", body), text);
  }

  public static Email generic(String subject, String message, String link) {
    String body = "<p>" + esc(message) + "</p>" + (link != null ? button(link, "Open HRMS") : "");
    String text = message + (link != null ? " Link: " + link : "");
    return new Email(subject, wrap(subject, body), text);
  }

  private static String button(String link, String label) {
    return "<p><a href='" + esc(link) + "' style=\"background:#1a73e8;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none;display:inline-block;\">"
        + esc(label) + "</a></p>";
  }

  private static String esc(String s) {
    return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }
}
