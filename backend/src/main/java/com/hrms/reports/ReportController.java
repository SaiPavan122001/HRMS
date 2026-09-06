package com.hrms.reports;

import com.hrms.attendance.Attendance;
import com.hrms.attendance.AttendanceRepository;
import com.hrms.employee.Profile;
import com.hrms.employee.ProfileRepository;
import com.hrms.role.RbacService;
import com.hrms.security.SecurityUtils;
import com.hrms.timesheet.TimesheetEntryRepository;
import com.hrms.timesheet.TimesheetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

/** Performance/efficiency reports backing the Performance dashboard. */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Performance and efficiency reports")
public class ReportController {

  private final ProfileRepository profileRepository;
  private final AttendanceRepository attendanceRepository;
  private final TimesheetEntryRepository entryRepository;
  private final RbacService rbac;

  public ReportController(ProfileRepository profileRepository, AttendanceRepository attendanceRepository,
                          TimesheetEntryRepository entryRepository, RbacService rbac) {
    this.profileRepository = profileRepository;
    this.attendanceRepository = attendanceRepository;
    this.entryRepository = entryRepository;
    this.rbac = rbac;
  }

  @GetMapping("/performance")
  @Operation(summary = "Per-employee performance metrics (HR/manager view)")
  public List<Map<String, Object>> performance(@RequestParam(required = false) String department) {
    UUID userId = SecurityUtils.currentUserId();
    rbac.requirePermission(userId, "attendance", "view");
    LocalDate start = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
    LocalDate end = start.plusDays(6);
    return profileRepository.findByIsActiveTrue().stream()
        .filter(p -> department == null || department.isBlank()
            || department.equalsIgnoreCase(p.getDepartment()))
        .map(p -> metrics(p, start, end))
        .collect(Collectors.toList());
  }

  @GetMapping("/performance/{profileId}/detail")
  @Operation(summary = "Detailed weekly hours / attendance trend / workload for one employee")
  public Map<String, Object> detail(@PathVariable UUID profileId) {
    UUID userId = SecurityUtils.currentUserId();
    rbac.requirePermission(userId, "attendance", "view");
    Profile p = profileRepository.findById(profileId)
        .orElseThrow(() -> new com.hrms.common.exception.ApiException(
            org.springframework.http.HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Employee not found"));

    // Last 8 weekly hours
    List<Map<String, Object>> weeklyHours = new ArrayList<>();
    LocalDate thisWeek = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
    for (int i = 7; i >= 0; i--) {
      LocalDate start = thisWeek.minusWeeks(i);
      BigDecimal hours = entryRepository.sumHours(profileId, start, start.plusDays(6));
      weeklyHours.add(Map.of("week", start.toString(), "hours", hours, "target", 40));
    }

    // Last 30 days attendance trend
    LocalDate trendStart = LocalDate.now().minusDays(30);
    List<Attendance> records = attendanceRepository
        .findByProfileIdAndDateBetweenOrderByDateDesc(profileId, trendStart, LocalDate.now());
    List<Map<String, Object>> attendanceTrend = records.stream()
        .map(r -> Map.<String, Object>of("date", r.getDate().toString(), "status", r.getStatus()))
        .collect(Collectors.toList());

    // Workload breakdown by project (this week)
    Map<String, BigDecimal> projectHours = new LinkedHashMap<>();
    entryRepository.projectBreakdown(profileId, trendStart, LocalDate.now())
        .forEach(row -> projectHours.put(row[0] == null ? "No Project" : String.valueOf(row[0]),
            (BigDecimal) row[1]));
    List<Map<String, Object>> workload = projectHours.entrySet().stream()
        .limit(5).map(e -> Map.<String, Object>of("category", e.getKey(), "hours", e.getValue()))
        .collect(Collectors.toList());

    return Map.of(
        "employee", Map.of("id", p.getId(), "name", p.getFirstName() + " " + p.getLastName()),
        "weeklyHours", weeklyHours,
        "attendanceTrend", attendanceTrend,
        "workloadBreakdown", workload);
  }

  private Map<String, Object> metrics(Profile p, LocalDate start, LocalDate end) {
    BigDecimal hours = entryRepository.sumHours(p.getId(), start, end);
    var attendance = attendanceRepository.findByProfileIdAndDateBetweenOrderByDateDesc(p.getId(), start, end);
    long presentDays = attendance.stream()
        .filter(a -> "present".equals(a.getStatus()) || "late".equals(a.getStatus())).count();
    double attendanceRate = attendance.isEmpty() ? 0 : Math.round(presentDays * 100.0 / attendance.size());
    double utilization = Math.min(100.0, hours.doubleValue() / 40.0 * 100);
    double score = Math.round((attendanceRate * 0.5 + utilization * 0.5));
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("profileId", p.getId());
    m.put("employeeId", p.getEmployeeId() == null ? "" : p.getEmployeeId());
    m.put("firstName", p.getFirstName());
    m.put("lastName", p.getLastName());
    m.put("department", p.getDepartment() == null ? "N/A" : p.getDepartment());
    m.put("jobTitle", p.getJobTitle() == null ? "N/A" : p.getJobTitle());
    m.put("hoursLogged", hours);
    m.put("targetHours", 40);
    m.put("attendanceRate", attendanceRate);
    m.put("utilizationRate", utilization);
    m.put("performanceScore", score);
    return m;
  }
}

