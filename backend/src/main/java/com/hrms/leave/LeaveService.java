package com.hrms.leave;

import com.hrms.attendance.Attendance;
import com.hrms.common.exception.ApiException;
import com.hrms.events.OutboxService;
import com.hrms.role.RbacService;
import com.hrms.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveService {

  private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

  private final LeaveTypeRepository typeRepository;
  private final LeaveBalanceRepository balanceRepository;
  private final LeaveRequestRepository requestRepository;
  private final RbacService rbac;
  private final OutboxService outbox;

  public LeaveService(LeaveTypeRepository typeRepository, LeaveBalanceRepository balanceRepository,
                      LeaveRequestRepository requestRepository, RbacService rbac, OutboxService outbox) {
    this.typeRepository = typeRepository;
    this.balanceRepository = balanceRepository;
    this.requestRepository = requestRepository;
    this.rbac = rbac;
    this.outbox = outbox;
  }

  public List<LeaveType> activeTypes() {
    return typeRepository.findByActiveTrueOrderByNameAsc();
  }

  public List<LeaveBalance> balances(UUID profileId, int year) {
    return balanceRepository.findByProfileIdAndYear(profileId, year);
  }

  /** Visible according to role: employees see their own, managers/HR see all/team. */
  public List<LeaveRequest> visibleRequests(UUID userId, UUID profileId, String role) {
    return switch (role) {
      case "super_admin", "hr" -> requestRepository.findTop200ByStatusOrderByCreatedAtDesc(null) == null
          ? List.of() : requestRepository.findAll().stream().limit(200).toList();
      case "manager" -> {
        UUID managerProfileId = rbac.currentProfileId(userId);
        yield requestRepository.findTeamAll(managerProfileId);
      }
      default -> requestRepository.findTop200ByProfileIdOrderByCreatedAtDesc(profileId);
    };
  }

  @Transactional
  public LeaveRequest createRequest(UUID userId, UUID profileId, CreateRequest req) {
    rbac.requirePermission(userId, "leave_management", "create");
    LeaveType type = typeRepository.findById(req.leaveTypeId())
        .orElseThrow(() -> ApiException.notFound("Leave type not found"));
    if (!type.isActive()) throw ApiException.businessRule("Leave type is not active");
    if (req.endDate().isBefore(req.startDate())) {
      throw ApiException.badRequest("End date cannot be before start date");
    }
    if (requestRepository.existsOverlapping(profileId, req.startDate(), req.endDate())) {
      throw ApiException.businessRule("You already have a pending or approved leave that overlaps these dates");
    }
    LeaveBalance balance = balanceRepository
        .findByProfileIdAndLeaveTypeIdAndYear(profileId, type.getId(), req.startDate().getYear())
        .orElseThrow(() -> ApiException.businessRule("No leave balance configured for this leave type"));
    if (req.daysRequested().compareTo(balance.getRemainingDays()) > 0) {
      throw ApiException.businessRule("Insufficient remaining balance (" + balance.getRemainingDays() + " days left)");
    }
    LeaveRequest request = new LeaveRequest();
    request.setProfileId(profileId);
    request.setLeaveTypeId(type.getId());
    request.setStartDate(req.startDate());
    request.setEndDate(req.endDate());
    request.setDaysRequested(req.daysRequested());
    request.setReason(req.reason());
    request.setStatus("pending");
    LeaveRequest saved = requestRepository.save(request);

    outbox.append("LEAVE_REQUESTED", "leave_request", saved.getId(), Map.of(
        "requestId", saved.getId().toString(),
        "profileId", profileId.toString(),
        "leaveTypeId", type.getId().toString(),
        "typeName", type.getName(),
        "startDate", saved.getStartDate().toString(),
        "endDate", saved.getEndDate().toString()));
    return saved;
  }

  public record CreateRequest(UUID leaveTypeId, LocalDate startDate, LocalDate endDate,
                              BigDecimal daysRequested, String reason) {}

  @Transactional
  public LeaveRequest decide(UUID approverUserId, UUID requestId, boolean approved, String reason) {
    rbac.requirePermission(approverUserId, "leave_management", "approve");
    LeaveRequest request = requestRepository.findById(requestId)
        .orElseThrow(() -> ApiException.notFound("Leave request not found"));
    if (!"pending".equals(request.getStatus())) {
      throw ApiException.businessRule("This leave request has already been decided");
    }
    if (approved) {
      LeaveBalance balance = balanceRepository
          .findByProfileIdAndLeaveTypeIdAndYear(request.getProfileId(), request.getLeaveTypeId(), request.getStartDate().getYear())
          .orElseThrow(() -> ApiException.businessRule("Leave balance missing"));
      if (request.getDaysRequested().compareTo(balance.getRemainingDays()) > 0) {
        throw ApiException.businessRule("Insufficient remaining balance at approval time");
      }
      balance.setUsedDays(balance.getUsedDays().add(request.getDaysRequested()));
      balance.setRemainingDays(balance.getTotalDays().subtract(balance.getUsedDays()).max(BigDecimal.ZERO));
      balanceRepository.save(balance);
      request.setStatus("approved");
      request.setApprovedBy(approverUserId);
      request.setApprovedAt(Instant.now());
    } else {
      if (reason == null || reason.isBlank()) throw ApiException.badRequest("Rejection reason is required");
      request.setStatus("rejected");
      request.setRejectionReason(reason);
    }
    LeaveRequest saved = requestRepository.save(request);
    outbox.append(approved ? "LEAVE_APPROVED" : "LEAVE_REJECTED", "leave_request", saved.getId(), Map.of(
        "requestId", saved.getId().toString(),
        "profileId", saved.getProfileId().toString(),
        "approvedBy", String.valueOf(approverUserId),
        "reason", reason == null ? "" : reason));
    return saved;
  }

  @Transactional
  public void cancel(UUID userId, UUID requestId) {
    LeaveRequest request = requestRepository.findById(requestId)
        .orElseThrow(() -> ApiException.notFound("Leave request not found"));
    if (!"pending".equals(request.getStatus())) {
      throw ApiException.businessRule("Only pending leave requests can be cancelled");
    }
    requestRepository.delete(request);
  }

  // ---- Leave type admin (super admin) ----
  @Transactional
  public LeaveType createType(UUID userId, String name, String description, Integer maxDays, boolean carryForward) {
    rbac.requirePermission(userId, "role_management", "create");
    if (typeRepository.findByNameIgnoreCase(name).isPresent()) {
      throw ApiException.duplicate("A leave type with this name already exists");
    }
    LeaveType type = new LeaveType();
    type.setName(name);
    type.setDescription(description);
    type.setMaxDaysPerYear(maxDays);
    type.setCarryForward(carryForward);
    return typeRepository.save(type);
  }

  @Transactional
  public LeaveType updateType(UUID userId, UUID id, String name, String description, Integer maxDays,
                              Boolean carryForward, Boolean active) {
    rbac.requirePermission(userId, "role_management", "edit");
    LeaveType type = typeRepository.findById(id).orElseThrow(() -> ApiException.notFound("Leave type not found"));
    if (name != null) type.setName(name);
    if (description != null) type.setDescription(description);
    if (maxDays != null) type.setMaxDaysPerYear(maxDays);
    if (carryForward != null) type.setCarryForward(carryForward);
    if (active != null) type.setActive(active);
    return typeRepository.save(type);
  }

  @Transactional
  public void deleteType(UUID userId, UUID id) {
    rbac.requirePermission(userId, "role_management", "delete");
    if (!typeRepository.existsById(id)) throw ApiException.notFound("Leave type not found");
    typeRepository.deleteById(id);
  }

  // ---- Balance admin (HR / super admin) ----
  @Transactional
  public LeaveBalance ensureBalance(UUID profileId, UUID leaveTypeId, int year, BigDecimal totalDays) {
    LeaveBalance balance = balanceRepository
        .findByProfileIdAndLeaveTypeIdAndYear(profileId, leaveTypeId, year)
        .orElseGet(() -> {
          LeaveBalance b = new LeaveBalance();
          b.setProfileId(profileId);
          b.setLeaveTypeId(leaveTypeId);
          b.setYear(year);
          b.setTotalDays(totalDays);
          b.setUsedDays(BigDecimal.ZERO);
          b.setRemainingDays(totalDays);
          return b;
        });
    if (totalDays != null) {
      balance.setTotalDays(totalDays);
      balance.setRemainingDays(totalDays.subtract(balance.getUsedDays()).max(BigDecimal.ZERO));
    }
    return balanceRepository.save(balance);
  }
}
