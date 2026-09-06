package com.hrms.auth.repo;

import com.hrms.auth.entity.OneTimeToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, UUID> {
  Optional<OneTimeToken> findByTokenHashAndPurpose(String tokenHash, OneTimeToken.Purpose purpose);
  long deleteByExpiresAtBefore(Instant cutoff);
}
