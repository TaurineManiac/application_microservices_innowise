package org.example.user_service.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.example.user_service.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {
    List<PaymentCard> findAllByUser_Id(Long id);

    @Query("SELECT c FROM PaymentCard c WHERE c.user.id = :userId AND c.active = true ")
    List<PaymentCard> findAllActiveCardsByUserId(@Param("userId") Long id);

    @Modifying
    @Query(value = "UPDATE payment_cards SET active = :active WHERE id = :cardId", nativeQuery = true)
    void updateCardStatus(@Param("cardId") Long cardId, @Param("active") boolean active);

    long countByUserId(Long userId);

    boolean existsByNumber(String number);

    boolean findPaymentCardByNumber(String number);
}
