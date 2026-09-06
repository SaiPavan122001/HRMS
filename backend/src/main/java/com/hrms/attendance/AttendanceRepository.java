package com.hrms.attendance;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
  Optional<Attendance> findByProfileIdAndDate(UUID profileId, LocalDate date);
  List<Attendance> findByProfileIdAndDateBetweenOrderByDateDesc(UUID profileId, LocalDate start, LocalDate end);
  List<Attendance> findByDate(LocalDate date);
  List<Attendance> findByCorrectionReasonIsNotNullAndCorrectedFalseOrderByDateDesc();
  List<Attendance> findByDateBetweenOrderByDateDesc(java.time.LocalDate start, java.time.LocalDate end);

  @Query("SELECT a FROM Attendance a WHERE a.profileId IN (SELECT p.id FROM Profile p WHERE p.manager.id = :managerId) ORDER BY a.date DESC")
  Page<Attendance> findTeamAttendance(@Param("managerId") UUID managerId, Pageable pageable);
}

