package com.gharse.auth;

import com.gharse.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    void deleteByUser(User user);

    void deleteByExpiresAtBefore(Instant time);

    List<PasswordResetToken> findAllByUser_Id(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.tokenHash = :tokenHash and t.expiresAt > :now")
    Optional<PasswordResetToken> findValidToken(@Param("tokenHash") String tokenHash, @Param("now") Instant now);
}
