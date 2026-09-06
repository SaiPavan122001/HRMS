package com.hrms.leave;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
  List<LeaveRequest> findTop200ByProfileIdOrderByCreatedAtDesc(UUID profileId);
  List<LeaveRequest> findTop200ByStatusOrderByCreatedAtDesc(String status);

  @Query("SELECT r FROM LeaveRequest r WHERE r.status = 'pending' "
       + "AND r.profileId IN (SELECT p.id FROM Profile p WHERE p.manager.id = :managerId) "
       + "ORDER BY r.createdAt DESC")
  List<LeaveRequest> findTeamPending(@Param("managerId") UUID managerId);

  @Query("SELECT r FROM LeaveRequest r WHERE r.profileId IN "
       + "(SELECT p.id FROM Profile p WHERE p.manager.id = :managerId) ORDER BY r.createdAt DESC")
  List<LeaveRequest> findTeamAll(@Param("managerId") UUID managerId);

  @Query("SELECT COUNT(r) > 0 FROM LeaveRequest r WHERE r.profileId = :profileId "
       + "AND r.status IN ('pending','approved') "
       + "AND r.startDate <= :end AND r.endDate >= :start")
  boolean existsOverlapping(@Param("profileId") UUID profileId,
                            @Param("start") LocalDate start, @Param("end") LocalDate end);
}



