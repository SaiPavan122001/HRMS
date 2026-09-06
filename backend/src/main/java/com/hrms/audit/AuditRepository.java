package com.hrms.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditLog, UUID> {
  Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
