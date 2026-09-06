package com.hrms.timesheet;

import com.hrms.common.exception.ApiException;
import com.hrms.events.OutboxService;
import com.hrms.role.RbacService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimesheetService {

  private final TimesheetRepository timesheetRepository;
  private final TimesheetEntryRepository entryRepository;
  private final RbacService rbac;
  private final OutboxService outbox;

  public TimesheetService(TimesheetRepository timesheetRepository, TimesheetEntryRepository entryRepository,
                          RbacService rbac, OutboxService outbox) {
    this.timesheetRepository = timesheetRepository;
    this.entryRepository = entryRepository;
    this.rbac = rbac;
    this.outbox = outbox;
  }

  @Transactional
  public Timesheet createOrGet(UUID userId, LocalDate weekStart) {
    UUID profileId = rbac.currentProfileId(userId);
    LocalDate monday = weekStart.with(DayOfWeek.MONDAY);
    Timesheet ts = timesheetRepository.findByProfileIdAndWeekStartDate(profileId, monday).orElse(null);
    if (ts != null) return ts;
    Timesheet t = new Timesheet();
    t.setProfileId(profileId);
    t.setWeekStartDate(monday);
    t.setWeekEndDate(monday.plusDays(6));
    t.setTotalHours(BigDecimal.ZERO);
    t.setStatus("draft");
    return timesheetRepository.save(t);
  }

  public List<Timesheet> visibleSheets(UUID userId, String role) {
    UUID profileId = rbac.currentProfileId(userId);
    return switch (role) {
      case "super_admin", "hr" -> timesheetRepository.findAll().stream().limit(200).toList();
      case "manager" -> timesheetRepository.findTeam(profileId);
      default -> timesheetRepository.findTop100ByProfileIdOrderByWeekStartDateDesc(profileId);
    };
  }

  public List<TimesheetEntry> entries(UUID timesheetId) {
    return entryRepository.findByTimesheetIdOrderByDateAsc(timesheetId);
  }

  @Transactional
  public TimesheetEntry saveEntry(UUID userId, UUID timesheetId, SaveEntry body) {
    Timesheet ts = timesheetRepository.findById(timesheetId)
        .orElseThrow(() -> ApiException.notFound("Timesheet not found"));
    UUID profileId = rbac.currentProfileId(userId);
    if (!ts.getProfileId().equals(profileId)) throw ApiException.forbidden("Not your timesheet");
    if (!"draft".equals(ts.getStatus()) && !"rejected".equals(ts.getStatus())) {
      throw ApiException.businessRule("Entries can only be changed on draft or rejected timesheets");
    }
    TimesheetEntry entry = body.id() != null
        ? entryRepository.findById(body.id()).orElseThrow(() -> ApiException.notFound("Entry not found"))
        : new TimesheetEntry();
    entry.setTimesheetId(timesheetId);
    entry.setDate(LocalDate.parse(body.date()));
    entry.setHours(body.hours());
    entry.setProjectName(body.projectName());
    entry.setTaskDescription(body.taskDescription());
    entry.setBillable(body.isBillable());
    TimesheetEntry saved = entryRepository.save(entry);
    recalculate(ts);
    return saved;
  }

  public record SaveEntry(UUID id, String date, BigDecimal hours, String projectName,
                          String taskDescription, boolean isBillable) {}

  @Transactional
  public void deleteEntry(UUID userId, UUID timesheetId, UUID entryId) {
    Timesheet ts = timesheetRepository.findById(timesheetId)
        .orElseThrow(() -> ApiException.notFound("Timesheet not found"));
    UUID profileId = rbac.currentProfileId(userId);
    if (!ts.getProfileId().equals(profileId)) throw ApiException.forbidden("Not your timesheet");
    if (!"draft".equals(ts.getStatus()) && !"rejected".equals(ts.getStatus())) {
      throw ApiException.businessRule("Entries can only be changed on draft or rejected timesheets");
    }
    entryRepository.findById(entryId).ifPresent(e -> {
      if (!e.getTimesheetId().equals(timesheetId)) throw ApiException.badRequest("Entry does not belong to this timesheet");
      entryRepository.delete(e);
    });
    recalculate(ts);
  }

  private void recalculate(Timesheet ts) {
    BigDecimal total = entryRepository.findByTimesheetIdOrderByDateAsc(ts.getId()).stream()
        .map(TimesheetEntry::getHours).reduce(BigDecimal.ZERO, BigDecimal::add);
    ts.setTotalHours(total);
    timesheetRepository.save(ts);
  }

  @Transactional
  public Timesheet submit(UUID userId, UUID timesheetId) {
    Timesheet ts = timesheetRepository.findById(timesheetId)
        .orElseThrow(() -> ApiException.notFound("Timesheet not found"));
    UUID profileId = rbac.currentProfileId(userId);
    if (!ts.getProfileId().equals(profileId)) throw ApiException.forbidden("Not your timesheet");
    if (!"draft".equals(ts.getStatus()) && !"rejected".equals(ts.getStatus())) {
      throw ApiException.businessRule("Timesheet already submitted");
    }
    recalculate(ts);
    ts.setStatus("submitted");
    ts.setSubmittedAt(java.time.Instant.now());
    Timesheet saved = timesheetRepository.save(ts);
    outbox.append("TIMESHEET_SUBMITTED", "timesheet", saved.getId(), Map.of(
        "timesheetId", saved.getId().toString(),
        "profileId", saved.getProfileId().toString(),
        "weekStart", saved.getWeekStartDate().toString(),
        "weekEnd", saved.getWeekEndDate().toString()));
    return saved;
  }

  @Transactional
  public Timesheet decide(UUID approverUserId, UUID timesheetId, boolean approved, String reason) {
    rbac.requirePermission(approverUserId, "timesheets", "approve");
    Timesheet ts = timesheetRepository.findById(timesheetId)
        .orElseThrow(() -> ApiException.notFound("Timesheet not found"));
    if (!"submitted".equals(ts.getStatus())) {
      throw ApiException.businessRule("Only submitted timesheets can be approved or rejected");
    }
    if (approved) {
      ts.setStatus("approved");
      ts.setApprovedBy(approverUserId);
      ts.setApprovedAt(java.time.Instant.now());
    } else {
      if (reason == null || reason.isBlank()) throw ApiException.badRequest("Rejection reason is required");
      ts.setStatus("rejected");
      ts.setRejectionReason(reason);
    }
    Timesheet saved = timesheetRepository.save(ts);
    outbox.append(approved ? "TIMESHEET_APPROVED" : "TIMESHEET_REJECTED", "timesheet", saved.getId(), Map.of(
        "timesheetId", saved.getId().toString(),
        "profileId", saved.getProfileId().toString(),
        "approvedBy", String.valueOf(approverUserId),
        "weekStart", saved.getWeekStartDate().toString(),
        "weekEnd", saved.getWeekEndDate().toString(),
        "reason", reason == null ? "" : reason));
    return saved;
  }

  @Transactional
  public int bulkApprove(UUID approverUserId, List<UUID> ids) {
    int count = 0;
    for (UUID id : ids) {
      try {
        Timesheet ts = timesheetRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Timesheet not found"));
        if ("submitted".equals(ts.getStatus())) {
          decide(approverUserId, id, true, null);
          count++;
        }
      } catch (Exception ignored) { }
    }
    return count;
  }

  public BigDecimal hoursFor(UUID profileId, LocalDate start, LocalDate end) {
    return entryRepository.sumHours(profileId, start, end);
  }

  public List<Object[]> projectBreakdown(UUID profileId, LocalDate start, LocalDate end) {
    return entryRepository.projectBreakdown(profileId, start, end);
  }
}
