package com.hrms.timesheet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, UUID> {
  List<TimesheetEntry> findByTimesheetIdOrderByDateAsc(UUID timesheetId);
  void deleteByTimesheetId(UUID timesheetId);

  @Query("SELECT COALESCE(SUM(e.hours), 0) FROM TimesheetEntry e "
       + "WHERE e.timesheetId IN (SELECT t.id FROM Timesheet t WHERE t.profileId = :profileId) "
       + "AND e.date BETWEEN :start AND :end")
  BigDecimal sumHours(@Param("profileId") UUID profileId,
                      @Param("start") LocalDate start, @Param("end") LocalDate end);

  @Query("SELECT e.projectName, SUM(e.hours) FROM TimesheetEntry e "
       + "WHERE e.timesheetId IN (SELECT t.id FROM Timesheet t WHERE t.profileId = :profileId) "
       + "AND e.date BETWEEN :start AND :end GROUP BY e.projectName ORDER BY SUM(e.hours) DESC")
  List<Object[]> projectBreakdown(@Param("profileId") UUID profileId,
                                  @Param("start") LocalDate start, @Param("end") LocalDate end);
}
