package com.hrms.leave;

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
@RequestMapping("/api/v1/leaves")
@Tag(name = "Leave", description = "Leave types, balances, requests and approvals")
public class LeaveController {

  public record LeaveTypeDto(UUID id, String name, String description, Integer maxDaysPerYear,
                             boolean isCarryForward, boolean isActive) {
    static LeaveTypeDto from(LeaveType t) {
      return new LeaveTypeDto(t.getId(), t.getName(), t.getDescription(), t.getMaxDaysPerYear(),
          t.isCarryForward(), t.isActive());
    }
  }

  public record LeaveBalanceDto(UUID id, UUID profileId, UUID leaveTypeId, int year,
                                BigDecimal totalDays, BigDecimal usedDays, BigDecimal remainingDays) {
    static LeaveBalanceDto from(LeaveBalance b) {
      return new LeaveBalanceDto(b.getId(), b.getProfileId(), b.getLeaveTypeId(), b.getYear(),
          b.getTotalDays(), b.getUsedDays(), b.getRemainingDays());
    }
  }

  public record LeaveRequestDto(UUID id, UUID profileId, UUID leaveTypeId, String startDate, String endDate,
      BigDecimal daysRequested, String reason, String status, String approvedBy, String approvedAt,
      String rejectionReason, String createdAt) {
    static LeaveRequestDto from(LeaveRequest r) {
      return new LeaveRequestDto(r.getId(), r.getProfileId(), r.getLeaveTypeId(), r.getStartDate().toString(),
          r.getEndDate().toString(), r.getDaysRequested(), r.getReason(), r.getStatus(),
          r.getApprovedBy() == null ? null : r.getApprovedBy().toString(),
          r.getApprovedAt() == null ? null : r.getApprovedAt().toString(),
          r.getRejectionReason(), r.getCreatedAt().toString());
    }
  }

  public record CreateLeaveRequest(@NotNull UUID leaveTypeId, @NotNull String startDate, @NotNull String endDate,
      @NotNull BigDecimal daysRequested, String reason) {}

  public record DecideRequest(boolean approved, String reason) {}

  public record LeaveTypeRequest(@NotBlank String name, String description, Integer maxDaysPerYear,
                                 boolean isCarryForward) {}

  public record LeaveTypeUpdate(String name, String description, Integer maxDaysPerYear,
                                Boolean isCarryForward, Boolean isActive) {}

  private final LeaveService service;
  private final RbacService rbac;

  public LeaveController(LeaveService service, RbacService rbac) {
    this.service = service;
    this.rbac = rbac;
  }

  @GetMapping("/types")
  public List<LeaveTypeDto> types() {
    return service.activeTypes().stream().map(LeaveTypeDto::from).toList();
  }

  @GetMapping("/balances")
  public List<LeaveBalanceDto> balances(@RequestParam(defaultValue = "0") int year) {
    int y = year > 0 ? year : LocalDate.now().getYear();
    UUID userId = SecurityUtils.currentUserId();
    return service.balances(rbac.currentProfileId(userId), y).stream().map(LeaveBalanceDto::from).toList();
  }

  @GetMapping("/requests")
  @Operation(summary = "Leave requests visible to the current user (role-aware)")
  public List<LeaveRequestDto> requests() {
    UUID userId = SecurityUtils.currentUserId();
    String role = rbac.effectiveRole(userId);
    UUID profileId = rbac.currentProfileId(userId);
    return service.visibleRequests(userId, profileId, role).stream().map(LeaveRequestDto::from).toList();
  }

  @PostMapping("/requests")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Submit a leave request (sends notifications via Kafka)")
  public LeaveRequestDto create(@Valid @RequestBody CreateLeaveRequest body) {
    UUID userId = SecurityUtils.currentUserId();
    LeaveRequest saved = service.createRequest(userId, rbac.currentProfileId(userId),
        new LeaveService.CreateRequest(body.leaveTypeId(), LocalDate.parse(body.startDate()),
            LocalDate.parse(body.endDate()), body.daysRequested(), body.reason()));
    return LeaveRequestDto.from(saved);
  }

  @PostMapping("/requests/{id}/decision")
  @Operation(summary = "Approve or reject a leave request (manager/HR)")
  public LeaveRequestDto decide(@PathVariable UUID id, @Valid @RequestBody DecideRequest body) {
    return LeaveRequestDto.from(service.decide(SecurityUtils.currentUserId(), id, body.approved(), body.reason()));
  }

  @DeleteMapping("/requests/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Cancel a pending leave request")
  public void cancel(@PathVariable UUID id) {
    service.cancel(SecurityUtils.currentUserId(), id);
  }

  // ---- Admin: leave types ----
  @PostMapping("/types")
  @ResponseStatus(HttpStatus.CREATED)
  public LeaveTypeDto createType(@Valid @RequestBody LeaveTypeRequest body) {
    return LeaveTypeDto.from(service.createType(SecurityUtils.currentUserId(), body.name(),
        body.description(), body.maxDaysPerYear(), body.isCarryForward()));
  }

  @PutMapping("/types/{id}")
  public LeaveTypeDto updateType(@PathVariable UUID id, @RequestBody LeaveTypeUpdate body) {
    return LeaveTypeDto.from(service.updateType(SecurityUtils.currentUserId(), id, body.name(),
        body.description(), body.maxDaysPerYear(), body.isCarryForward(), body.isActive()));
  }

  @DeleteMapping("/types/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteType(@PathVariable UUID id) {
    service.deleteType(SecurityUtils.currentUserId(), id);
  }

  // ---- Admin: balances (HR grants days) ----
  @PostMapping("/balances")
  @Operation(summary = "Grant/adjust a leave balance for an employee (HR)")
  public LeaveBalanceDto upsertBalance(@RequestBody Map<String, Object> body) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "leave_management", "approve");
    UUID profileId = UUID.fromString(String.valueOf(body.get("profileId")));
    UUID leaveTypeId = UUID.fromString(String.valueOf(body.get("leaveTypeId")));
    int year = Integer.parseInt(String.valueOf(body.getOrDefault("year", LocalDate.now().getYear())));
    BigDecimal total = body.get("totalDays") == null ? null : new BigDecimal(String.valueOf(body.get("totalDays")));
    return LeaveBalanceDto.from(service.ensureBalance(profileId, leaveTypeId, year, total));
  }
}
