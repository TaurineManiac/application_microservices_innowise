package org.example.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.payment_service.enums.PaymentStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID orderPublicId;

    @Column(nullable = false)
    private UUID userPublicId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal amount=BigDecimal.ZERO;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;


}
