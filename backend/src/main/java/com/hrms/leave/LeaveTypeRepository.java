package com.hrms.leave;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {
  List<LeaveType> findByActiveTrueOrderByNameAsc();
  java.util.Optional<LeaveType> findByNameIgnoreCase(String name);
}
