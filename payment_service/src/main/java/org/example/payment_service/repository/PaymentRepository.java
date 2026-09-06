package org.example.payment_service.repository;

import feign.Param;
import org.example.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment,Long>, JpaSpecificationExecutor<Payment> {
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.userPublicId = :userId AND p.createdAt BETWEEN :from AND :to")
    BigDecimal getTotalAmountForUser(
            @Param("userId") UUID publicUserId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.createdAt BETWEEN :from AND :to")
    BigDecimal getTotalAmount(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
