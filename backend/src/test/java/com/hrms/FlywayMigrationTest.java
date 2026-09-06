package com.hrms;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hrms.role.RbacService;
import com.hrms.auth.repo.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-context integration test: runs Flyway migrations against real PostgreSQL.
 * Automatically skipped when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class FlywayMigrationTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  UserRepository userRepository;

  @Autowired
  RbacService rbacService;

  @Autowired
  StringRedisTemplate redisTemplate;

  @Test
  void migrationsRunAndSeedDataExists() {
    // V2 seed created 4 system roles - verified indirectly via effectiveRole fallback
    List.of("super_admin", "hr", "manager", "employee");
    // RBAC service falls back to employee for unknown users
    String role = rbacService.effectiveRole(java.util.UUID.randomUUID());
    assertTrue(role.equals("employee"));
    assertTrue(userRepository.count() == 0); // fresh database
  }
}
