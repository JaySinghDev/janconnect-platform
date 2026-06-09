package com.janconnect.repository;

import com.janconnect.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findByEmailAndOtpAndUsedFalse(String email, String otp);

    /** Invalidate all previous unused OTPs for the email before issuing a new one. */
    @Modifying
    @Query("UPDATE PasswordResetOtp p SET p.used = true WHERE p.email = :email AND p.used = false")
    void invalidateAllByEmail(String email);
}
