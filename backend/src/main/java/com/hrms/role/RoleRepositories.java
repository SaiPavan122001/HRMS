package com.hrms.role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
  Optional<RoleEntity> findByNameIgnoreCase(String name);
}

interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {
  List<PermissionEntity> findByModuleOrderByName(String module);
}

interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
  List<RolePermission> findByRole(RoleEntity role);
  boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
  void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
}

interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {
  List<UserRoleAssignment> findByUserId(UUID userId);
  boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
}

interface UserRoleLegacyRepository extends JpaRepository<UserRoleLegacy, UUID> {
  List<UserRoleLegacy> findByUserId(UUID userId);
}
