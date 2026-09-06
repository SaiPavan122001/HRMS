package com.hrms.role;

import com.hrms.common.exception.ApiException;
import com.hrms.employee.Profile;
import com.hrms.employee.ProfileRepository;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Central authorization service. Replaces the Supabase RLS policies, the legacy
 * has_role() SQL function and the get_user_permissions() RPC.
 */
@Service
public class RbacService {

  private static final Logger log = LoggerFactory.getLogger(RbacService.class);
  private static final String PERM_CACHE_PREFIX = "hrms:perm:";

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final UserRoleAssignmentRepository assignmentRepository;
  private final UserRoleLegacyRepository legacyRepository;
  private final ProfileRepository profileRepository;
  private final StringRedisTemplate redis;

  public RbacService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                     RolePermissionRepository rolePermissionRepository,
                     UserRoleAssignmentRepository assignmentRepository,
                     UserRoleLegacyRepository legacyRepository, ProfileRepository profileRepository,
                     StringRedisTemplate redis) {
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.rolePermissionRepository = rolePermissionRepository;
    this.assignmentRepository = assignmentRepository;
    this.legacyRepository = legacyRepository;
    this.profileRepository = profileRepository;
    this.redis = redis;
  }

  /** Effective role (lowercase), combining legacy + RBAC systems exactly like the original has_role(). */
  public String effectiveRole(UUID userId) {
    List<UserRoleLegacy.AppRole> legacy = legacyRepository.findByUserId(userId).stream()
        .map(UserRoleLegacy::getRole).toList();
    if (!legacy.isEmpty()) {
      if (legacy.contains(UserRoleLegacy.AppRole.SUPER_ADMIN)) return "super_admin";
      if (legacy.contains(UserRoleLegacy.AppRole.HR)) return "hr";
      if (legacy.contains(UserRoleLegacy.AppRole.MANAGER)) return "manager";
      return "employee";
    }
    List<String> names = assignmentRepository.findByUserId(userId).stream()
        .map(a -> a.getRole().getName().toLowerCase()).toList();
    if (names.contains("super admin") || names.contains("admin")) return "super_admin";
    if (names.contains("hr admin") || names.contains("hr")) return "hr";
    if (names.contains("manager")) return "manager";
    return "employee";
  }

  public List<PermissionEntity> permissionsOf(UUID userId) {
    try {
      String cached = redis.opsForValue().get(PERM_CACHE_PREFIX + userId);
      if (cached != null) {
        if (cached.isBlank()) return List.of();
        return decode(cached);
      }
    } catch (Exception e) {
      log.warn("Permission cache unavailable, falling back to DB: {}", e.getMessage());
    }
    List<PermissionEntity> perms = loadPermissions(userId);
    cache(userId, perms);
    return perms;
  }

  private List<PermissionEntity> decode(String cached) {
    List<PermissionEntity> out = new ArrayList<>();
    for (String line : cached.split("\\n")) {
      if (line.isBlank()) continue;
      String[] parts = line.split("\\|", -1);
      PermissionEntity p = new PermissionEntity();
      p.setModule(parts.length > 0 ? parts[0] : "");
      p.setAction(parts.length > 1 ? parts[1] : "");
      p.setDescription(parts.length > 2 ? parts[2] : null);
      out.add(p);
    }
    return out;
  }

  private void cache(UUID userId, List<PermissionEntity> perms) {
    try {
      String joined = perms.stream()
          .map(p -> p.getModule() + "|" + p.getAction() + "|" + Optional.ofNullable(p.getDescription()).orElse(""))
          .collect(Collectors.joining("\n"));
      redis.opsForValue().set(PERM_CACHE_PREFIX + userId, joined, java.time.Duration.ofMinutes(5));
    } catch (Exception ignored) { }
  }

  private List<PermissionEntity> loadPermissions(UUID userId) {
    List<UUID> roleIds = assignmentRepository.findByUserId(userId).stream().map(a -> a.getRole().getId()).toList();
    if (roleIds.isEmpty()) return List.of();
    Set<PermissionEntity> out = new LinkedHashSet<>();
    for (UUID roleId : roleIds) {
      rolePermissionRepository.findByRole(roleRepository.getReferenceById(roleId))
          .forEach(rp -> out.add(rp.getPermission()));
    }
    return List.copyOf(out);
  }

  public boolean hasPermission(UUID userId, String module, String action) {
    return permissionsOf(userId).stream()
        .anyMatch(p -> p.getModule().equalsIgnoreCase(module) && p.getAction().equalsIgnoreCase(action));
  }

  public void requirePermission(UUID userId, String module, String action) {
    if (!hasPermission(userId, module, action)) {
      throw new AccessDeniedException("Missing permission " + module + ":" + action);
    }
  }

  public void invalidateUserCache(UUID userId) {
    try { redis.delete(PERM_CACHE_PREFIX + userId); } catch (Exception ignored) { }
  }

  /** Replaces the Supabase handle_new_user() trigger: default employee role on signup. */
  public void assignDefaultRole(UUID userId, UUID profileId) {
    UserRoleLegacy legacy = new UserRoleLegacy();
    legacy.setUserId(userId);
    legacy.setProfileId(profileId);
    legacy.setRole(UserRoleLegacy.AppRole.EMPLOYEE);
    legacyRepository.save(legacy);
    RoleEntity employee = roleRepository.findByNameIgnoreCase("Employee")
        .orElseThrow(() -> new IllegalStateException("Employee role not seeded"));
    UserRoleAssignment assignment = new UserRoleAssignment();
    assignment.setUserId(userId);
    assignment.setRole(employee);
    assignmentRepository.save(assignment);
    invalidateUserCache(userId);
  }

  public UUID currentProfileId(UUID userId) {
    Profile profile = profileRepository.findByUserId(userId)
        .orElseThrow(() -> ApiException.notFound("Profile not found for current user"));
    return profile.getId();
  }
}

