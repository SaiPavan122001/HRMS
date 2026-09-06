package com.hrms.employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

  Optional<Profile> findByUserId(UUID userId);
  Optional<Profile> findByEmailIgnoreCase(String email);
  boolean existsByEmployeeId(String employeeId);
  List<Profile> findByManagerId(UUID managerId);
  List<Profile> findByIsActiveTrue();

  @Query("SELECT p FROM Profile p WHERE p.active = true "
       + "AND (:search IS NULL OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) "
       + "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) "
       + "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) "
       + "OR LOWER(p.employeeId) LIKE LOWER(CONCAT('%', :search, '%'))) "
       + "AND (:department IS NULL OR LOWER(p.department) = LOWER(:department))")
  Page<Profile> search(@Param("search") String search, @Param("department") String department, Pageable pageable);
}
