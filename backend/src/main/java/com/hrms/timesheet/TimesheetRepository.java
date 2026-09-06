package com.hrms.timesheet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimesheetRepository extends JpaRepository<Timesheet, UUID> {
  List<Timesheet> findTop100ByProfileIdOrderByWeekStartDateDesc(UUID profileId);
  Optional<Timesheet> findByProfileIdAndWeekStartDate(UUID profileId, LocalDate weekStart);
  List<Timesheet> findByStatus(String status);

  @Query("SELECT t FROM Timesheet t WHERE t.profileId IN "
       + "(SELECT p.id FROM Profile p WHERE p.manager.id = :managerId) ORDER BY t.weekStartDate DESC")
  List<Timesheet> findTeam(@Param("managerId") UUID managerId);
}