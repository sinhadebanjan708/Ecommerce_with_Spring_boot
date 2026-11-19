package com.elvero.shop.repository;

import com.elvero.shop.entity.Otp;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    /** The newest unused, unexpired OTP for this email/type, row-locked like Sequelize's LOCK.UPDATE. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Otp o where o.email = :email and o.otpCode = :code and o.otpType = :type "
            + "and o.isUsed = false and o.expiresAt > :now order by o.id desc limit 1")
    Optional<Otp> findValidForUpdate(@Param("email") String email, @Param("code") String code,
                                     @Param("type") String type, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from Otp o where o.email = :email and o.otpType = :type")
    int deleteByEmailAndType(@Param("email") String email, @Param("type") String type);
}
