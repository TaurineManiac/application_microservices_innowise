package org.example.payment_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.payment_service.dto.PaymentFilterRequest;
import org.example.payment_service.dto.PaymentRequestEvent;
import org.example.payment_service.dto.PaymentFullResponseEvent;
import org.example.payment_service.entity.Payment;
import org.example.payment_service.kafka.PaymentEventProducer;
import org.example.payment_service.mapper.PaymentMapper;
import org.example.payment_service.repository.PaymentRepository;
import org.example.payment_service.specification.PaymentSpecification;
import org.example.payment_service.util.AmountValidatorUtil;
import org.example.payment_service.util.PaymentStatusGeneratorUtil;
import org.example.payment_service.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final PaymentEventProducer eventProducer;

    @Transactional
    public PaymentFullResponseEvent createPayment(PaymentRequestEvent paymentRequestEvent) {
        BigDecimal rounded = AmountValidatorUtil.isValidAmountOrThrow(paymentRequestEvent.getAmount());

        Payment payment = mapper.toEntity(paymentRequestEvent);
        payment.setAmount(rounded);
        payment.setStatus(PaymentStatusGeneratorUtil.generateRandomStatus());

        Payment savedPayment = repository.save(payment);

        eventProducer.sendPaymentStatusEvent(savedPayment);

        log.info("Payment created: id={}, status={}", savedPayment.getId(), savedPayment.getStatus());
        return mapper.toFullResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentFullResponseEvent> getPayments(PaymentFilterRequest filter, Pageable pageable) {

        UUID currentUserId = SecurityUtils.getCurrentUserPublicId();
        boolean isAdmin = SecurityUtils.isAdmin();

        if(!isAdmin) {
            if(filter.getUserPublicId() != null &&  !filter.getUserPublicId().equals(currentUserId)) {
                throw new AccessDeniedException("You can only view your own payments");
            }
            filter.setUserPublicId(currentUserId);
        }

        Specification<Payment> spec = Specification
                .where(PaymentSpecification.hasUserPublicId(filter.getUserPublicId()))
                .and(PaymentSpecification.hasOrderPublicId(filter.getOrderPublicId()))
                .and(PaymentSpecification.hasStatus(filter.getPaymentStatus()))
                .and(PaymentSpecification.createdBetween(filter.getCreatedFrom(), filter.getCreatedTo()))
                .and(PaymentSpecification.updatedBetween(filter.getUpdatedFrom(), filter.getUpdatedTo()))
                .and(PaymentSpecification.amountBetween(filter.getMinAmount(), filter.getMaxAmount()));

        return repository.findAll(spec, pageable)
                .map(mapper::toFullResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or #userPublicId == authentication.principal")
    public BigDecimal getTotalForUser(UUID userPublicId, LocalDateTime from, LocalDateTime to) {
        return repository.getTotalAmountForUser(userPublicId, from, to);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public BigDecimal getTotalForAllUsers(LocalDateTime from, LocalDateTime to) {
        return repository.getTotalAmount(from, to);
    }

}
