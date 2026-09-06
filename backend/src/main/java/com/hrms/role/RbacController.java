package com.hrms.role;

import com.hrms.audit.AuditRecorder;
import com.hrms.common.exception.ApiException;
import com.hrms.employee.Profile;
import com.hrms.employee.ProfileRepository;
import com.hrms.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "RBAC", description = "Roles, permissions and assignments (admin)")
public class RbacController {

  public record RoleDto(UUID id, String name, String description, boolean isSystemRole) {
    static RoleDto from(RoleEntity r) {
      return new RoleDto(r.getId(), r.getName(), r.getDescription(), r.isSystemRole());
    }
  }

  public record PermissionDto(UUID id, String module, String action, String description) {
    static PermissionDto from(PermissionEntity p) {
      return new PermissionDto(p.getId(), p.getModule(), p.getAction(), p.getDescription());
    }
  }

  public record AssignmentDto(UUID id, UUID userId, UUID roleId, String roleName, String assignedAt) {
    static AssignmentDto from(UserRoleAssignment a) {
      return new AssignmentDto(a.getId(), a.getUserId(), a.getRole().getId(), a.getRole().getName(),
          a.getAssignedAt().toString());
    }
  }

  public record RoleRequest(String name, String description) {}
  public record AssignPermissionRequest(UUID permissionId) {}
  public record AssignRoleRequest(UUID roleId) {}

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final UserRoleAssignmentRepository assignmentRepository;
  private final ProfileRepository profileRepository;
  private final RbacService rbac;
  private final AuditRecorder audit;

  public RbacController(RoleRepository roleRepository, PermissionRepository permissionRepository,
                        RolePermissionRepository rolePermissionRepository,
                        UserRoleAssignmentRepository assignmentRepository,
                        ProfileRepository profileRepository, RbacService rbac, AuditRecorder audit) {
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.rolePermissionRepository = rolePermissionRepository;
    this.assignmentRepository = assignmentRepository;
    this.profileRepository = profileRepository;
    this.rbac = rbac;
    this.audit = audit;
  }

  private UUID me() { return SecurityUtils.currentUserId(); }

  @GetMapping("/roles")
  @Operation(summary = "List roles")
  public List<RoleDto> roles() {
    return roleRepository.findAll().stream().map(RoleDto::from)
        .sorted(Comparator.comparing(RoleDto::name)).toList();
  }

  @PostMapping("/roles")
  @Operation(summary = "Create a role (super admin)")
  public RoleDto createRole(@RequestBody RoleRequest body) {
    rbac.requirePermission(me(), "role_management", "create");
    if (roleRepository.findByNameIgnoreCase(body.name()).isPresent()) {
      throw ApiException.duplicate("Role already exists");
    }
    RoleEntity role = new RoleEntity();
    role.setName(body.name());
    role.setDescription(body.description());
    role.setSystemRole(false);
    RoleEntity saved = roleRepository.save(role);
    audit.record("ROLE_CREATED", "role", saved.getId(), null, Map.of("name", saved.getName()));
    return RoleDto.from(saved);
  }

  @PutMapping("/roles/{id}")
  public RoleDto updateRole(@PathVariable UUID id, @RequestBody RoleRequest body) {
    rbac.requirePermission(me(), "role_management", "edit");
    RoleEntity role = roleRepository.findById(id).orElseThrow(() -> ApiException.notFound("Role not found"));
    if (role.isSystemRole() && body.name() != null && !body.name().equalsIgnoreCase(role.getName())) {
      throw ApiException.businessRule("System roles cannot be renamed");
    }
    if (body.name() != null) role.setName(body.name());
    if (body.description() != null) role.setDescription(body.description());
    RoleEntity saved = roleRepository.save(role);
    audit.record("ROLE_UPDATED", "role", id, null, Map.of("name", saved.getName()));
    return RoleDto.from(saved);
  }

  @DeleteMapping("/roles/{id}")
  public void deleteRole(@PathVariable UUID id) {
    rbac.requirePermission(me(), "role_management", "delete");
    RoleEntity role = roleRepository.findById(id).orElseThrow(() -> ApiException.notFound("Role not found"));
    if (role.isSystemRole()) throw ApiException.businessRule("System roles cannot be deleted");
    roleRepository.delete(role);
    audit.record("ROLE_DELETED", "role", id, Map.of("name", role.getName()), null);
  }

  @GetMapping("/permissions")
  @Operation(summary = "List all permissions")
  public List<PermissionDto> permissions() {
    return permissionRepository.findAll().stream().map(PermissionDto::from)
        .sorted(Comparator.comparing(PermissionDto::module).thenComparing(PermissionDto::action)).toList();
  }

  @GetMapping("/role-permissions/{roleId}")
  @Operation(summary = "Permissions granted to a role")
  public List<PermissionDto> rolePermissions(@PathVariable UUID roleId) {
    RoleEntity role = roleRepository.findById(roleId).orElseThrow(() -> ApiException.notFound("Role not found"));
    return rolePermissionRepository.findByRole(role).stream().map(RolePermission::getPermission)
        .map(PermissionDto::from).toList();
  }

  @PostMapping("/role-permissions/{roleId}")
  @Operation(summary = "Grant a permission to a role (super admin)")
  public void grant(@PathVariable UUID roleId, @RequestBody AssignPermissionRequest body) {
    rbac.requirePermission(me(), "role_management", "edit");
    RoleEntity role = roleRepository.findById(roleId).orElseThrow(() -> ApiException.notFound("Role not found"));
    PermissionEntity permission = permissionRepository.findById(body.permissionId())
        .orElseThrow(() -> ApiException.notFound("Permission not found"));
    if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permission.getId())) {
      RolePermission rp = new RolePermission();
      rp.setRole(role);
      rp.setPermission(permission);
      rolePermissionRepository.save(rp);
      invalidateForRole(role);
    }
    audit.record("PERMISSION_GRANTED", "role", roleId, null, Map.of("permission", permission.getModule() + ":" + permission.getAction()));
  }

  @DeleteMapping("/role-permissions/{roleId}/{permissionId}")
  public void revoke(@PathVariable UUID roleId, @PathVariable UUID permissionId) {
    rbac.requirePermission(me(), "role_management", "edit");
    rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
    roleRepository.findById(roleId).ifPresent(this::invalidateForRole);
    audit.record("PERMISSION_REVOKED", "role", roleId, Map.of("permissionId", String.valueOf(permissionId)), null);
  }

  @GetMapping("/user-roles/{userId}")
  @Operation(summary = "Role assignments for a user")
  public List<AssignmentDto> userRoles(@PathVariable UUID userId) {
    return assignmentRepository.findByUserId(userId).stream().map(AssignmentDto::from).toList();
  }

  @PostMapping("/user-roles/{userId}")
  @Operation(summary = "Assign a role to a user (super admin)")
  public AssignmentDto assignRole(@PathVariable UUID userId, @RequestBody AssignRoleRequest body) {
    rbac.requirePermission(me(), "role_management", "assign");
    if (assignmentRepository.existsByUserIdAndRoleId(userId, body.roleId())) {
      throw ApiException.duplicate("Role already assigned to this user");
    }
    UserRoleAssignment a = new UserRoleAssignment();
    a.setUserId(userId);
    a.setRole(roleRepository.findById(body.roleId()).orElseThrow(() -> ApiException.notFound("Role not found")));
    a.setAssignedBy(me());
    UserRoleAssignment saved = assignmentRepository.save(a);
    rbac.invalidateUserCache(userId);
    audit.record("ROLE_ASSIGNED", "user", userId, null, Map.of("role", a.getRole().getName()));
    return AssignmentDto.from(saved);
  }

  @DeleteMapping("/user-roles/{userId}/{roleId}")
  public void removeRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
    rbac.requirePermission(me(), "role_management", "assign");
    assignmentRepository.findByUserId(userId).stream()
        .filter(a -> a.getRole().getId().equals(roleId))
        .findFirst()
        .ifPresent(a -> {
          assignmentRepository.delete(a);
          rbac.invalidateUserCache(userId);
          audit.record("ROLE_REMOVED", "user", userId, Map.of("role", a.getRole().getName()), null);
        });
  }

  @GetMapping("/users-with-roles")
  @Operation(summary = "All users with their role assignments (admin)")
  public List<Map<String, Object>> usersWithRoles() {
    rbac.requirePermission(me(), "role_management", "view");
    Map<UUID, List<UserRoleAssignment>> byUser = assignmentRepository.findAll().stream()
        .collect(Collectors.groupingBy(UserRoleAssignment::getUserId));
    return profileRepository.findAll().stream().map(p -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", p.getId());
      m.put("userId", p.getUserId());
      m.put("email", p.getEmail());
      m.put("firstName", p.getFirstName());
      m.put("lastName", p.getLastName());
      m.put("employeeId", p.getEmployeeId());
      m.put("department", p.getDepartment());
      m.put("jobTitle", p.getJobTitle());
      m.put("managerId", p.getManager() == null ? null : p.getManager().getId());
      m.put("roles", byUser.getOrDefault(p.getUserId(), List.of()).stream()
          .map(a -> Map.of("id", a.getRole().getId(), "name", a.getRole().getName())).toList());
      return m;
    }).toList();
  }

  @GetMapping("/me/permissions")
  @Operation(summary = "My effective permissions (replaces get_user_permissions RPC)")
  public List<Map<String, Object>> myPermissions() {
    UUID userId = me();
    return rbac.permissionsOf(userId).stream()
        .map(p -> Map.<String, Object>of("module", p.getModule(), "action", p.getAction(),
            "description", p.getDescription() == null ? "" : p.getDescription()))
        .collect(Collectors.toList());
  }

  private void invalidateForRole(RoleEntity role) {
    assignmentRepository.findAll().stream()
        .filter(a -> a.getRole().getId().equals(role.getId()))
        .forEach(a -> rbac.invalidateUserCache(a.getUserId()));
  }
}

