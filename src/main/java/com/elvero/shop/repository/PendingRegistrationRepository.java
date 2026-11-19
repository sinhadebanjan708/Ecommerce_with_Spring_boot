package com.elvero.shop.repository;

import com.elvero.shop.entity.PendingRegistration;
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
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Integer> {

    Optional<PendingRegistration> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PendingRegistration p where p.email = :email and p.expiresAt > :now")
    Optional<PendingRegistration> findActiveForUpdate(@Param("email") String email, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from PendingRegistration p where p.email = :email")
    int deleteByEmailAddress(@Param("email") String email);
}
