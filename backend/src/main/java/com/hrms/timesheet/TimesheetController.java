package com.hrms.timesheet;

import com.hrms.role.RbacService;
import com.hrms.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/timesheets")
@Tag(name = "Timesheets", description = "Weekly timesheets and entries")
public class TimesheetController {

  public record TimesheetDto(UUID id, UUID profileId, String weekStartDate, String weekEndDate,
      BigDecimal totalHours, String status, String submittedAt, String approvedBy, String approvedAt,
      String rejectionReason, List<EntryDto> entries) {
    static TimesheetDto from(Timesheet t, List<EntryDto> entries) {
      return new TimesheetDto(t.getId(), t.getProfileId(), t.getWeekStartDate().toString(),
          t.getWeekEndDate().toString(), t.getTotalHours(), t.getStatus(),
          t.getSubmittedAt() == null ? null : t.getSubmittedAt().toString(),
          t.getApprovedBy() == null ? null : t.getApprovedBy().toString(),
          t.getApprovedAt() == null ? null : t.getApprovedAt().toString(),
          t.getRejectionReason(), entries);
    }
  }

  public record EntryDto(UUID id, String date, BigDecimal hours, String projectName,
      String taskDescription, boolean isBillable, UUID timesheetId) {
    static EntryDto from(TimesheetEntry e) {
      return new EntryDto(e.getId(), e.getDate().toString(), e.getHours(), e.getProjectName(),
          e.getTaskDescription(), e.isBillable(), e.getTimesheetId());
    }
  }

  public record CreateOrGetRequest(String weekStartDate) {}
  public record SaveEntryRequest(UUID id, String date, BigDecimal hours, String projectName,
      String taskDescription, boolean isBillable) {}
  public record DecideRequest(boolean approved, String reason) {}
  public record BulkApproveRequest(List<UUID> ids) {}

  private final TimesheetService service;
  private final RbacService rbac;

  public TimesheetController(TimesheetService service, RbacService rbac) {
    this.service = service;
    this.rbac = rbac;
  }

  @GetMapping
  @Operation(summary = "Timesheets visible to the current user (role-aware)")
  public List<TimesheetDto> list() {
    UUID userId = SecurityUtils.currentUserId();
    String role = rbac.effectiveRole(userId);
    return service.visibleSheets(userId, role).stream().map(t -> TimesheetDto.from(t,
        service.entries(t.getId()).stream().map(EntryDto::from).toList())).toList();
  }

  @PostMapping("/current")
  @Operation(summary = "Get or create the timesheet for a week (Monday date)")
  public TimesheetDto current(@RequestBody(required = false) CreateOrGetRequest body) {
    UUID userId = SecurityUtils.currentUserId();
    LocalDate week = body != null && body.weekStartDate() != null
        ? LocalDate.parse(body.weekStartDate()) : LocalDate.now().with(java.time.DayOfWeek.MONDAY);
    Timesheet ts = service.createOrGet(userId, week);
    return TimesheetDto.from(ts, service.entries(ts.getId()).stream().map(EntryDto::from).toList());
  }

  @PostMapping("/{id}/entries")
  public EntryDto saveEntry(@PathVariable UUID id, @Valid @RequestBody SaveEntryRequest body) {
    return EntryDto.from(service.saveEntry(SecurityUtils.currentUserId(), id,
        new TimesheetService.SaveEntry(body.id(), body.date(), body.hours(), body.projectName(),
            body.taskDescription(), body.isBillable())));
  }

  @DeleteMapping("/{id}/entries/{entryId}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void deleteEntry(@PathVariable UUID id, @PathVariable UUID entryId) {
    service.deleteEntry(SecurityUtils.currentUserId(), id, entryId);
  }

  @PostMapping("/{id}/submit")
  public TimesheetDto submit(@PathVariable UUID id) {
    Timesheet ts = service.submit(SecurityUtils.currentUserId(), id);
    return TimesheetDto.from(ts, service.entries(ts.getId()).stream().map(EntryDto::from).toList());
  }

  @PostMapping("/{id}/decision")
  @Operation(summary = "Approve or reject a submitted timesheet (manager/HR)")
  public TimesheetDto decide(@PathVariable UUID id, @RequestBody DecideRequest body) {
    Timesheet ts = service.decide(SecurityUtils.currentUserId(), id, body.approved(), body.reason());
    return TimesheetDto.from(ts, service.entries(ts.getId()).stream().map(EntryDto::from).toList());
  }

  @PostMapping("/bulk-approve")
  public Map<String, Integer> bulkApprove(@RequestBody BulkApproveRequest body) {
    return Map.of("approved", service.bulkApprove(SecurityUtils.currentUserId(), body.ids()));
  }
}
