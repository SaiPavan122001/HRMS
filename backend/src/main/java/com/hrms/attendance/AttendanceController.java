package com.hrms.attendance;

import com.hrms.common.exception.ApiException;
import com.hrms.role.RbacService;
import com.hrms.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance")
@Tag(name = "Attendance", description = "Clock in/out, breaks, corrections, holidays, policies")
public class AttendanceController {

  public record AttendanceDto(UUID id, UUID profileId, String date, String checkInTime, String checkOutTime,
      String breakStartTime, String breakEndTime, BigDecimal totalHours, String status, String notes,
      boolean isCorrected, String correctionReason) {
    static AttendanceDto from(Attendance a) {
      return new AttendanceDto(a.getId(), a.getProfileId(), a.getDate().toString(),
          iso(a.getCheckInTime()), iso(a.getCheckOutTime()), iso(a.getBreakStartTime()), iso(a.getBreakEndTime()),
          a.getTotalHours(), a.getStatus(), a.getNotes(), a.isCorrected(), a.getCorrectionReason());
    }
    private static String iso(Instant t) { return t == null ? null : t.toString(); }
  }

  public record PolicyDto(UUID id, String name, String description, String workingHoursStart,
      String workingHoursEnd, int breakDurationMinutes, int gracePeriodMinutes,
      BigDecimal overtimeThresholdHours, boolean isActive) {
    static PolicyDto from(AttendancePolicy p) {
      return new PolicyDto(p.getId(), p.getName(), p.getDescription(), p.getWorkingHoursStart().toString(),
          p.getWorkingHoursEnd().toString(), p.getBreakDurationMinutes(), p.getGracePeriodMinutes(),
          p.getOvertimeThresholdHours(), p.isActive());
    }
  }

  public record HolidayDto(UUID id, String name, String date, boolean isRecurring, Integer year) {
    static HolidayDto from(Holiday h) {
      return new HolidayDto(h.getId(), h.getName(), h.getDate().toString(), h.isRecurring(), h.getYear());
    }
  }

  public record CheckInRequest(String notes) {}
  public record CorrectionRequest(@NotNull UUID attendanceId, @NotBlank String reason) {}
  public record CorrectionApproval(String status, String notes) {}
  public record HolidayRequest(@NotBlank String name, @NotNull String date, boolean isRecurring, Integer year) {}

  private final AttendanceService service;
  private final RbacService rbac;

  public AttendanceController(AttendanceService service, RbacService rbac) {
    this.service = service;
    this.rbac = rbac;
  }

  private UUID myProfileId() { return rbac.currentProfileId(SecurityUtils.currentUserId()); }

  @GetMapping("/today")
  @Operation(summary = "My attendance for today")
  public AttendanceDto today() {
    Attendance a = service.today(myProfileId());
    return a == null ? null : AttendanceDto.from(a);
  }

  @GetMapping("/me")
  public List<AttendanceDto> myHistory(@RequestParam String start, @RequestParam String end) {
    return service.history(myProfileId(), LocalDate.parse(start), LocalDate.parse(end))
        .stream().map(AttendanceDto::from).toList();
  }

  @GetMapping("/policies")
  public List<PolicyDto> policies() {
    return service.activePolicies().stream().map(PolicyDto::from).toList();
  }

  @PostMapping("/check-in")
  @ResponseStatus(HttpStatus.CREATED)
  public AttendanceDto checkIn(@RequestBody(required = false) CheckInRequest request) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "attendance", "create");
    return AttendanceDto.from(service.checkIn(myProfileId(), request == null ? null : request.notes()));
  }

  @PostMapping("/check-out")
  public AttendanceDto checkOut(@RequestBody(required = false) CheckInRequest request) {
    return AttendanceDto.from(service.checkOut(myProfileId(), request == null ? null : request.notes()));
  }

  @PostMapping("/break/start")
  public AttendanceDto startBreak() {
    return AttendanceDto.from(service.startBreak(myProfileId()));
  }

  @PostMapping("/break/end")
  public AttendanceDto endBreak() {
    return AttendanceDto.from(service.endBreak(myProfileId()));
  }

  @PostMapping("/corrections")
  @Operation(summary = "Request an attendance correction")
  public AttendanceDto requestCorrection(@Valid @RequestBody CorrectionRequest request) {
    return AttendanceDto.from(service.requestCorrection(myProfileId(), request.attendanceId(), request.reason()));
  }

  @PostMapping("/corrections/{id}/approve")
  @Operation(summary = "Approve an attendance correction (manager/HR)")
  public AttendanceDto approveCorrection(@PathVariable UUID id, @RequestBody CorrectionApproval body) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "attendance", "approve");
    return AttendanceDto.from(service.approveCorrection(SecurityUtils.currentUserId(), id, body.status(), body.notes()));
  }

  @GetMapping("/report/{profileId}")
  @Operation(summary = "Monthly attendance report")
  public Map<String, Object> monthlyReport(@PathVariable UUID profileId, @RequestParam int year, @RequestParam int month) {
    return service.monthlyReport(profileId, year, month);
  }

  // ---- Holidays (view: everyone; manage: HR / super admin via permission check) ----
  @GetMapping("/holidays")
  public List<HolidayDto> holidays(@RequestParam(defaultValue = "0") int year) {
    int y = year > 0 ? year : LocalDate.now().getYear();
    return service.holidays(y).stream().map(HolidayDto::from).toList();
  }

  @PostMapping("/holidays")
  @ResponseStatus(HttpStatus.CREATED)
  public HolidayDto createHoliday(@Valid @RequestBody HolidayRequest request) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "attendance", "edit");
    return HolidayDto.from(service.createHoliday(request.name(), LocalDate.parse(request.date()),
        request.isRecurring(), request.year()));
  }

  @DeleteMapping("/holidays/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteHoliday(@PathVariable UUID id) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "attendance", "edit");
    service.deleteHoliday(id);
  }

  // ---- Corrections (manager/HR) ----

  @GetMapping("/corrections")
  @Operation(summary = "Pending attendance correction requests")
  public java.util.List<java.util.Map<String, Object>> corrections() {
    rbac.requirePermission(SecurityUtils.currentUserId(), "attendance", "approve");
    return service.pendingCorrections();
  }

  public record CorrectionApprovalExt(String status, String notes, String checkInTime, String checkOutTime) {}

  @PostMapping("/corrections/{id}/approve")
  @Operation(summary = "Approve an attendance correction")
  public AttendanceDto approveCorrectionExt(@PathVariable UUID id, @RequestBody CorrectionApprovalExt body) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "attendance", "approve");
    return AttendanceDto.from(service.approveCorrectionExtended(SecurityUtils.currentUserId(), id,
        body.status(), body.notes(),
        body.checkInTime() == null ? null : java.time.Instant.parse(body.checkInTime()),
        body.checkOutTime() == null ? null : java.time.Instant.parse(body.checkOutTime())));
  }

  @PostMapping("/corrections/{id}/reject")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Reject an attendance correction request")
  public void rejectCorrection(@PathVariable UUID id) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "attendance", "approve");
    service.rejectCorrection(id);
  }

  @GetMapping("/team")
  @Operation(summary = "Team attendance for a date range (manager/HR)")
  public java.util.List<AttendanceDto> team(@RequestParam String start, @RequestParam String end) {
    String role = rbac.effectiveRole(SecurityUtils.currentUserId());
    if ("employee".equals(role)) throw ApiException.forbidden("Not permitted");
    return service.teamAttendance(java.time.LocalDate.parse(start), java.time.LocalDate.parse(end))
        .stream().map(AttendanceDto::from).toList();
  }

  // ---- Attendance policy admin ----
  public record PolicyRequest(String name, String description, String workingHoursStart, String workingHoursEnd,
      int breakDurationMinutes, int gracePeriodMinutes, double overtimeThresholdHours, boolean isActive) {}

  @PostMapping("/policies")
  @ResponseStatus(HttpStatus.CREATED)
  public PolicyDto createPolicy(@RequestBody PolicyRequest r) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "attendance", "edit");
    return PolicyDto.from(service.createPolicy(r.name(), r.description(),
        java.time.LocalTime.parse(r.workingHoursStart()), java.time.LocalTime.parse(r.workingHoursEnd()),
        r.breakDurationMinutes(), r.gracePeriodMinutes(), java.math.BigDecimal.valueOf(r.overtimeThresholdHours()),
        r.isActive()));
  }

  @PutMapping("/policies/{id}")
  public PolicyDto updatePolicy(@PathVariable UUID id, @RequestBody PolicyRequest r) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "attendance", "edit");
    return PolicyDto.from(service.updatePolicy(id, r.name(), r.description(),
        java.time.LocalTime.parse(r.workingHoursStart()), java.time.LocalTime.parse(r.workingHoursEnd()),
        r.breakDurationMinutes(), r.gracePeriodMinutes(), java.math.BigDecimal.valueOf(r.overtimeThresholdHours()),
        r.isActive()));
  }

  @DeleteMapping("/policies/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePolicy(@PathVariable UUID id) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "attendance", "edit");
    service.deletePolicy(id);
  }
}

