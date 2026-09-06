package com.hrms.audit;

import com.hrms.role.RbacService;
import com.hrms.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@Tag(name = "Admin", description = "Administrative endpoints")
public class AuditController {

  private final AuditRepository repository;
  private final RbacService rbac;

  public AuditController(AuditRepository repository, RbacService rbac) {
    this.repository = repository;
    this.rbac = rbac;
  }

  @GetMapping
  @Operation(summary = "Paged audit log (admin only, append-only)")
  public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "50") int size) {
    rbac.requirePermission(SecurityUtils.currentUserId(), "role_management", "view");
    Page<AuditLog> result = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(200, size)));
    return Map.of("items", result.getContent(), "page", result.getNumber(),
        "total", result.getTotalElements());
  }
}
