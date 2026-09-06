package com.hrms.leave;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
  List<LeaveBalance> findByProfileIdAndYear(UUID profileId, int year);
  Optional<LeaveBalance> findByProfileIdAndLeaveTypeIdAndYear(UUID profileId, UUID leaveTypeId, int year);
  Optional<LeaveBalance> findByProfileIdAndLeaveTypeId(UUID profileId, UUID leaveTypeId);
}


