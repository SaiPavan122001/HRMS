package com.hrms.employee;

import com.hrms.role.RbacService;
import com.hrms.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees", description = "Employee directory and management")
public class EmployeeController {

  public record EmployeeDto(UUID id, UUID userId, String email, String firstName, String lastName,
      String employeeId, String department, String jobTitle, String phone, UUID managerId,
      String hireDate, boolean isActive, List<Map<String, Object>> roles) {
    static EmployeeDto from(Profile p, List<Map<String, Object>> roles) {
      return new EmployeeDto(p.getId(), p.getUserId(), p.getEmail(), p.getFirstName(), p.getLastName(),
          p.getEmployeeId(), p.getDepartment(), p.getJobTitle(), p.getPhone(),
          p.getManager() == null ? null : p.getManager().getId(),
          p.getHireDate() == null ? null : p.getHireDate().toString(), p.isActive(), roles);
    }
  }

  public record CreateEmployee(String email, String firstName, String lastName, String department,
      String jobTitle, String phone, UUID managerId, String hireDate, String employeeId) {}

  public record UpdateEmployee(String firstName, String lastName, String department, String jobTitle,
      String phone, UUID managerId, String hireDate, Boolean isActive) {}

  private final EmployeeService service;
  private final RbacService rbac;

  public EmployeeController(EmployeeService service, RbacService rbac) {
    this.service = service;
    this.rbac = rbac;
  }

  @GetMapping
  @Operation(summary = "Search employees (HR/manager: everyone; employee: active directory)")
  public Map<String, Object> search(@RequestParam(required = false) String search,
      @RequestParam(required = false) String department,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    Page<Profile> result = service.search(search, department, page, size);
    return Map.of(
        "items", result.getContent().stream().map(p -> EmployeeDto.from(p, rolesOf(p))).toList(),
        "page", result.getNumber(), "totalPages", result.getTotalPages(),
        "total", result.getTotalElements());
  }

  @GetMapping("/active")
  public List<EmployeeDto> active() {
    return service.activeEmployees().stream().map(p -> EmployeeDto.from(p, rolesOf(p))).toList();
  }

  @GetMapping("/me")
  public EmployeeDto me() {
    UUID userId = SecurityUtils.currentUserId();
    return EmployeeDto.from(service.byUserId(userId), rolesOf(service.byUserId(userId)));
  }

  @GetMapping("/{id}")
  public EmployeeDto byId(@PathVariable UUID id) {
    return EmployeeDto.from(service.byId(id), rolesOf(service.byId(id)));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create an employee (HR/admin)")
  public EmployeeDto create(@RequestBody CreateEmployee body) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "role_management", "assign");
    Profile p = service.create(body.email(), body.firstName(), body.lastName(), body.department(),
        body.jobTitle(), body.phone(), body.managerId(), body.hireDate(), body.employeeId());
    return EmployeeDto.from(p, rolesOf(p));
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Update an employee (HR/admin); isActive=false offboards")
  public EmployeeDto update(@PathVariable UUID id, @RequestBody UpdateEmployee body) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "role_management", "edit");
    Profile p = service.update(id, body.firstName(), body.lastName(), body.department(), body.jobTitle(),
        body.phone(), body.managerId(), body.hireDate(), body.isActive());
    return EmployeeDto.from(p, rolesOf(p));
  }

  private List<Map<String, Object>> rolesOf(Profile p) {
    if (p.getUserId() == null) return List.of();
    return rbac.permissionsOf(p.getUserId()).stream()
        .map(x -> Map.<String, Object>of("module", x.getModule(), "action", x.getAction())).toList();
  }
}
