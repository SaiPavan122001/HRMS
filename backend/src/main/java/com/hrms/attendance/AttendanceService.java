package com.hrms.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.common.exception.ApiException;
import com.hrms.events.OutboxService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

  private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

  private final AttendanceRepository repository;
  private final AttendancePolicyRepository policyRepository;
  private final HolidayRepository holidayRepository;
  private final AttendanceAuditLogRepository auditRepository;
  private final ObjectMapper mapper;
  private final OutboxService outbox;
  private final com.hrms.employee.ProfileRepository profileRepository;

  public AttendanceService(AttendanceRepository repository, AttendancePolicyRepository policyRepository,
                           HolidayRepository holidayRepository, AttendanceAuditLogRepository auditRepository,
                           ObjectMapper mapper, OutboxService outbox,
                           com.hrms.employee.ProfileRepository profileRepository) {
    this.repository = repository;
    this.policyRepository = policyRepository;
    this.holidayRepository = holidayRepository;
    this.auditRepository = auditRepository;
    this.mapper = mapper;
    this.outbox = outbox;
    this.profileRepository = profileRepository;
  }

  public Attendance today(UUID profileId) {
    return repository.findByProfileIdAndDate(profileId, LocalDate.now()).orElse(null);
  }

  public List<Attendance> history(UUID profileId, LocalDate start, LocalDate end) {
    return repository.findByProfileIdAndDateBetweenOrderByDateDesc(profileId, start, end);
  }

  public List<AttendancePolicy> activePolicies() {
    return policyRepository.findByActiveTrue();
  }

  @Transactional
  public Attendance checkIn(UUID profileId, String notes) {
    LocalDate today = LocalDate.now();
    if (repository.findByProfileIdAndDate(profileId, today).isPresent()) {
      throw ApiException.businessRule("You have already checked in today");
    }
    Attendance a = new Attendance();
    a.setProfileId(profileId);
    a.setDate(today);
    a.setCheckInTime(Instant.now());
    a.setNotes(notes);
    a.setStatus(isLate() ? "late" : "present");
    Attendance saved = repository.save(a);
    writeAudit(saved, "created", null, saved, null);
    return saved;
  }

  private boolean isLate() {
    return policyRepository.findByActiveTrue().stream().findFirst()
        .map(p -> LocalTime.now().isAfter(p.getWorkingHoursStart().plusMinutes(p.getGracePeriodMinutes())))
        .orElse(false);
  }

  @Transactional
  public Attendance checkOut(UUID profileId, String notes) {
    Attendance a = today(profileId);
    if (a == null) throw ApiException.badRequest("You have not checked in today");
    if (a.getCheckOutTime() != null) throw ApiException.businessRule("You have already checked out today");
    a.setCheckOutTime(Instant.now());
    if (notes != null && !notes.isBlank()) a.setNotes(notes);
    a.setTotalHours(computeTotalHours(a));
    Attendance saved = repository.save(a);
    writeAudit(saved, "updated", null, saved, null);
    return saved;
  }

  @Transactional
  public Attendance startBreak(UUID profileId) {
    Attendance a = today(profileId);
    if (a == null) throw ApiException.badRequest("You have not checked in today");
    if (a.getBreakStartTime() != null && a.getBreakEndTime() == null) {
      throw ApiException.businessRule("A break is already in progress");
    }
    a.setBreakStartTime(Instant.now());
    a.setBreakEndTime(null);
    return repository.save(a);
  }

  @Transactional
  public Attendance endBreak(UUID profileId) {
    Attendance a = today(profileId);
    if (a == null || a.getBreakStartTime() == null || a.getBreakEndTime() != null) {
      throw ApiException.businessRule("No break in progress");
    }
    a.setBreakEndTime(Instant.now());
    return repository.save(a);
  }

  private BigDecimal computeTotalHours(Attendance a) {
    if (a.getCheckInTime() == null || a.getCheckOutTime() == null) return null;
    long millis = Duration.between(a.getCheckInTime(), a.getCheckOutTime()).toMillis();
    if (a.getBreakStartTime() != null && a.getBreakEndTime() != null) {
      millis -= Duration.between(a.getBreakStartTime(), a.getBreakEndTime()).toMillis();
    }
    return BigDecimal.valueOf(millis / 3_600_000.0).setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
  }

  @Transactional
  public Attendance requestCorrection(UUID requesterProfileId, UUID attendanceId, String reason) {
    Attendance a = repository.findById(attendanceId)
        .orElseThrow(() -> ApiException.notFound("Attendance record not found"));
    if (!a.getProfileId().equals(requesterProfileId)) {
      throw ApiException.forbidden("You can only correct your own attendance");
    }
    a.setCorrected(false);
    a.setCorrectionReason(reason);
    return repository.save(a);
  }

  @Transactional
  public Attendance approveCorrection(UUID approverUserId, UUID attendanceId, String newStatus, String notes) {
    Attendance a = repository.findById(attendanceId)
        .orElseThrow(() -> ApiException.notFound("Attendance record not found"));
    Map<String, Object> oldSnapshot = snapshot(a);
    a.setCorrected(true);
    a.setCorrectedBy(approverUserId);
    if (newStatus != null) a.setStatus(newStatus);
    if (notes != null) a.setNotes(notes);
    Attendance saved = repository.save(a);
    writeAudit(saved, "corrected", oldSnapshot, saved, saved.getCorrectionReason());
    outbox.append("ATTENDANCE_CORRECTED", "attendance", saved.getId(), Map.of(
        "attendanceId", saved.getId().toString(),
        "profileId", saved.getProfileId().toString(),
        "status", saved.getStatus()));
    return saved;
  }

  public List<Holiday> holidays(int year) {
    return holidayRepository.findByYearOrRecurringTrue(year, org.springframework.data.domain.Sort.by("date"));
  }

  @Transactional
  public Holiday createHoliday(String name, LocalDate date, boolean recurring, Integer year) {
    Holiday h = new Holiday();
    h.setName(name);
    h.setDate(date);
    h.setRecurring(recurring);
    h.setYear(year != null ? year : date.getYear());
    return holidayRepository.save(h);
  }

  @Transactional
  public void deleteHoliday(UUID id) {
    if (!holidayRepository.existsById(id)) throw ApiException.notFound("Holiday not found");
    holidayRepository.deleteById(id);
  }

  /** Monthly attendance report: per-day status map. */
  public Map<String, Object> monthlyReport(UUID profileId, int year, int month) {
    LocalDate start = LocalDate.of(year, month, 1);
    LocalDate end = start.plusMonths(1).minusDays(1);
    List<Attendance> records = repository.findByProfileIdAndDateBetweenOrderByDateDesc(profileId, start, end);
    var byDate = new java.util.TreeMap<LocalDate, Attendance>();
    records.forEach(r -> byDate.put(r.getDate(), r));
    BigDecimal totalHours = records.stream()
        .map(Attendance::getTotalHours).filter(java.util.Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    long present = records.stream().filter(r -> "present".equals(r.getStatus()) || "late".equals(r.getStatus())).count();
    long late = records.stream().filter(r -> "late".equals(r.getStatus())).count();
    long absent = records.stream().filter(r -> "absent".equals(r.getStatus())).count();
    var days = byDate.entrySet().stream().map(e -> Map.of(
        "date", e.getKey().toString(), "status", e.getValue().getStatus(),
        "checkIn", e.getValue().getCheckInTime() == null ? "" : e.getValue().getCheckInTime().toString(),
        "checkOut", e.getValue().getCheckOutTime() == null ? "" : e.getValue().getCheckOutTime().toString(),
        "totalHours", e.getValue().getTotalHours() == null ? "0" : e.getValue().getTotalHours().toString())).toList();
    return Map.of(
        "year", year, "month", month,
        "totalHours", totalHours, "present", present, "late", late, "absent", absent,
        "days", days);
  }

  private Map<String, Object> snapshot(Attendance a) {
    Map<String, Object> m = new HashMap<>();
    m.put("status", a.getStatus());
    m.put("totalHours", a.getTotalHours());
    m.put("notes", a.getNotes());
    m.put("isCorrected", a.isCorrected());
    return m;
  }

  private void writeAudit(Attendance a, String action, Object oldV, Object newV, String reason) {
    try {
      AttendanceAuditLog entry = new AttendanceAuditLog();
      entry.setAttendanceId(a.getId());
      entry.setChangedBy(com.hrms.security.SecurityUtils.currentUserId());
      entry.setAction(action);
      entry.setOldValues(oldV == null ? null : mapper.writeValueAsString(oldV));
      entry.setNewValues(mapper.writeValueAsString(newV));
      entry.setReason(reason);
      auditRepository.save(entry);
    } catch (Exception e) {
      log.warn("Attendance audit write failed: {}", e.getMessage());
    }
  }

  // ---- Extended operations for the team/corrections UI ----

  public java.util.List<java.util.Map<String, Object>> pendingCorrections() {
    java.util.List<Attendance> records = repository.findByCorrectionReasonIsNotNullAndCorrectedFalseOrderByDateDesc();
    java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
    for (Attendance a : records) {
      java.util.Map<String, Object> m = new HashMap<>();
      m.put("id", a.getId());
      m.put("date", a.getDate().toString());
      m.put("check_in_time", a.getCheckInTime() == null ? null : a.getCheckInTime().toString());
      m.put("check_out_time", a.getCheckOutTime() == null ? null : a.getCheckOutTime().toString());
      m.put("correction_reason", a.getCorrectionReason());
      m.put("is_corrected", a.isCorrected());
      profileRepository.findById(a.getProfileId()).ifPresent(p -> m.put("profiles",
          java.util.Map.of("first_name", p.getFirstName(), "last_name", p.getLastName(),
              "employee_id", p.getEmployeeId() == null ? "" : p.getEmployeeId())));
      out.add(m);
    }
    return out;
  }

  @Transactional
  public Attendance approveCorrectionExtended(UUID approverUserId, UUID attendanceId, String newStatus,
                                              String notes, Instant checkIn, Instant checkOut) {
    Attendance a = repository.findById(attendanceId)
        .orElseThrow(() -> ApiException.notFound("Attendance record not found"));
    Map<String, Object> oldSnapshot = snapshot(a);
    if (checkIn != null) a.setCheckInTime(checkIn);
    if (checkOut != null) a.setCheckOutTime(checkOut);
    if (checkIn != null && checkOut != null) {
      a.setTotalHours(computeTotalHours(a));
    }
    a.setCorrected(true);
    a.setCorrectedBy(approverUserId);
    if (newStatus != null) a.setStatus(newStatus);
    if (notes != null) a.setNotes(notes);
    Attendance saved = repository.save(a);
    writeAudit(saved, "corrected", oldSnapshot, saved, saved.getCorrectionReason());
    outbox.append("ATTENDANCE_CORRECTED", "attendance", saved.getId(), Map.of(
        "attendanceId", saved.getId().toString(), "profileId", saved.getProfileId().toString()));
    return saved;
  }

  @Transactional
  public void rejectCorrection(UUID attendanceId) {
    Attendance a = repository.findById(attendanceId)
        .orElseThrow(() -> ApiException.notFound("Attendance record not found"));
    a.setCorrectionReason(null);
    a.setCorrected(false);
    repository.save(a);
  }

  public java.util.List<Attendance> teamAttendance(LocalDate start, LocalDate end) {
    return repository.findByDateBetweenOrderByDateDesc(start, end);
  }

  // ---- Attendance policy admin ----
  @Transactional
  public AttendancePolicy createPolicy(String name, String description, LocalTime start, LocalTime end,
                                       int breakMinutes, int graceMinutes, java.math.BigDecimal overtime, boolean active) {
    AttendancePolicy p = new AttendancePolicy();
    p.setName(name);
    p.setDescription(description);
    p.setWorkingHoursStart(start);
    p.setWorkingHoursEnd(end);
    p.setBreakDurationMinutes(breakMinutes);
    p.setGracePeriodMinutes(graceMinutes);
    p.setOvertimeThresholdHours(overtime);
    p.setActive(active);
    return policyRepository.save(p);
  }

  @Transactional
  public AttendancePolicy updatePolicy(UUID id, String name, String description, LocalTime start, LocalTime end,
                                       Integer breakMinutes, Integer graceMinutes, java.math.BigDecimal overtime,
                                       Boolean active) {
    AttendancePolicy p = policyRepository.findById(id)
        .orElseThrow(() -> ApiException.notFound("Policy not found"));
    if (name != null) p.setName(name);
    if (description != null) p.setDescription(description);
    if (start != null) p.setWorkingHoursStart(start);
    if (end != null) p.setWorkingHoursEnd(end);
    if (breakMinutes != null) p.setBreakDurationMinutes(breakMinutes);
    if (graceMinutes != null) p.setGracePeriodMinutes(graceMinutes);
    if (overtime != null) p.setOvertimeThresholdHours(overtime);
    if (active != null) p.setActive(active);
    return policyRepository.save(p);
  }

  @Transactional
  public void deletePolicy(UUID id) {
    if (!policyRepository.existsById(id)) throw ApiException.notFound("Policy not found");
    policyRepository.deleteById(id);
  }
}

