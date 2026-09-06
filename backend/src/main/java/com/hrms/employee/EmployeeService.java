package com.hrms.employee;

import com.hrms.audit.AuditRecorder;
import com.hrms.common.exception.ApiException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

  private final ProfileRepository repository;
  private final AuditRecorder audit;

  public EmployeeService(ProfileRepository repository, AuditRecorder audit) {
    this.repository = repository;
    this.audit = audit;
  }

  public Page<Profile> search(String search, String department, int page, int size) {
    Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
    return repository.search(emptyToNull(search), emptyToNull(department), pageable);
  }

  public List<Profile> activeEmployees() {
    return repository.findByIsActiveTrue();
  }

  public Profile byId(UUID id) {
    return repository.findById(id).orElseThrow(() -> ApiException.notFound("Employee not found"));
  }

  public Profile byUserId(UUID userId) {
    return repository.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Profile not found"));
  }

  @Transactional
  public Profile create(String email, String firstName, String lastName, String department,
                        String jobTitle, String phone, UUID managerId, String hireDate, String employeeId) {
    if (repository.findByEmailIgnoreCase(email).isPresent()) {
      throw ApiException.duplicate("An employee with this email already exists");
    }
    if (employeeId != null && !employeeId.isBlank() && repository.existsByEmployeeId(employeeId)) {
      throw ApiException.duplicate("Employee ID already in use");
    }
    Profile p = new Profile();
    p.setEmail(email.toLowerCase());
    p.setFirstName(firstName);
    p.setLastName(lastName);
    p.setDepartment(department);
    p.setJobTitle(jobTitle);
    p.setPhone(phone);
    if (managerId != null) p.setManager(byId(managerId));
    if (hireDate != null && !hireDate.isBlank()) p.setHireDate(java.time.LocalDate.parse(hireDate));
    p.setEmployeeId(employeeId == null || employeeId.isBlank()
        ? "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() : employeeId);
    Profile saved = repository.save(p);
    audit.record("EMPLOYEE_CREATED", "profile", saved.getId(), null, Map.of("email", saved.getEmail()));
    return saved;
  }

  @Transactional
  public Profile update(UUID id, String firstName, String lastName, String department, String jobTitle,
                        String phone, UUID managerId, String hireDate, Boolean active) {
    Profile p = byId(id);
    Map<String, Object> old = new HashMap<>();
    old.put("department", p.getDepartment());
    old.put("jobTitle", p.getJobTitle());
    old.put("active", p.isActive());
    if (firstName != null) p.setFirstName(firstName);
    if (lastName != null) p.setLastName(lastName);
    if (department != null) p.setDepartment(department);
    if (jobTitle != null) p.setJobTitle(jobTitle);
    if (phone != null) p.setPhone(phone);
    if (managerId != null) p.setManager(byId(managerId));
    if (hireDate != null && !hireDate.isBlank()) p.setHireDate(java.time.LocalDate.parse(hireDate));
    if (active != null) p.setActive(active); // onboarding/offboarding
    Profile saved = repository.save(p);
    audit.record("EMPLOYEE_UPDATED", "profile", id, old, Map.of("department", saved.getDepartment(),
        "jobTitle", saved.getJobTitle(), "active", saved.isActive()));
    return saved;
  }

  private String emptyToNull(String s) { return s == null || s.isBlank() ? null : s; }
}
