package org.example.user_service.repository;

import org.example.user_service.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, BigDecimal> {

}
