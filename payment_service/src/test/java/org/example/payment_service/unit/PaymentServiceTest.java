package org.example.payment_service.unit;

import org.example.payment_service.dto.PaymentFilterRequest;
import org.example.payment_service.dto.PaymentFullResponseEvent;
import org.example.payment_service.dto.PaymentRequestEvent;
import org.example.payment_service.entity.Payment;
import org.example.payment_service.enums.PaymentStatus;
import org.example.payment_service.exception.AccessDeniedException;
import org.example.payment_service.kafka.PaymentEventProducer;
import org.example.payment_service.mapper.PaymentMapper;
import org.example.payment_service.repository.PaymentRepository;
import org.example.payment_service.service.PaymentService;
import org.example.payment_service.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @Mock
    private PaymentEventProducer eventProducer;

    @Mock
    private PaymentMapper mapper;

    @InjectMocks
    private PaymentService paymentService;

    private UUID userPublicId;
    private UUID orderPublicId;
    private PaymentRequestEvent request;
    private Payment payment;
    private PaymentFullResponseEvent response;

    @BeforeEach
    void setUp() {
        userPublicId = UUID.randomUUID();
        orderPublicId = UUID.randomUUID();

        request = PaymentRequestEvent.builder()
                .orderPublicId(orderPublicId)
                .userPublicId(userPublicId)
                .amount(new BigDecimal("100.00"))
                .build();

        payment = Payment.builder()
                .id(1L)
                .orderPublicId(orderPublicId)
                .userPublicId(userPublicId)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        response = PaymentFullResponseEvent.builder()
                .id(1L)
                .orderPublicId(orderPublicId)
                .userPublicId(userPublicId)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldCreatePaymentAndSendKafkaEvent() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            when(repository.save(any(Payment.class))).thenReturn(payment);
            when(mapper.toEntity(any(PaymentRequestEvent.class))).thenReturn(payment);
            when(mapper.toFullResponse(any(Payment.class))).thenReturn(response);
            doNothing().when(eventProducer).sendPaymentStatusEvent(any(Payment.class));

            PaymentFullResponseEvent result = paymentService.createPayment(request);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isIn(PaymentStatus.COMPLETED, PaymentStatus.FAILED);
            verify(repository, times(1)).save(any(Payment.class));
            verify(eventProducer, times(1)).sendPaymentStatusEvent(any(Payment.class));
        }
    }

    @Test
    void shouldThrowWhenAmountHasTooManyDecimalPlaces() {
        request.setAmount(new BigDecimal("100.12345"));
        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must have at most 2 decimal places");
    }

    @Test
    void shouldReturnOnlyCurrentUserPaymentsForNonAdmin() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserPublicId).thenReturn(userPublicId);
            securityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            Pageable pageable = PageRequest.of(0, 10);
            PaymentFilterRequest filter = new PaymentFilterRequest();

            Page<Payment> page = new PageImpl<>(List.of(payment));
            when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(mapper.toFullResponse(any(Payment.class))).thenReturn(response);

            Page<PaymentFullResponseEvent> result = paymentService.getPayments(filter, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(repository, times(1)).findAll(any(Specification.class), eq(pageable));
        }
    }

    @Test
    void shouldReturnAllPaymentsForAdmin() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserPublicId).thenReturn(userPublicId);
            securityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            Pageable pageable = PageRequest.of(0, 10);
            PaymentFilterRequest filter = new PaymentFilterRequest();
            filter.setUserPublicId(UUID.randomUUID());

            Page<Payment> page = new PageImpl<>(List.of(payment));
            when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(mapper.toFullResponse(any(Payment.class))).thenReturn(response);

            Page<PaymentFullResponseEvent> result = paymentService.getPayments(filter, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(repository, times(1)).findAll(any(Specification.class), eq(pageable));
        }
    }

    @Test
    void shouldThrowAccessDeniedWhenNonAdminTriesToViewOtherUserPayments() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID currentUser = UUID.randomUUID();
            UUID otherUser = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserPublicId).thenReturn(currentUser);
            securityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            PaymentFilterRequest filter = new PaymentFilterRequest();
            filter.setUserPublicId(otherUser);

            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> paymentService.getPayments(filter, pageable))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("You can only view your own payments");
        }
    }

    @Test
    void shouldFilterPaymentsByStatus() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserPublicId).thenReturn(userPublicId);
            securityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            Pageable pageable = PageRequest.of(0, 10);
            PaymentFilterRequest filter = new PaymentFilterRequest();
            filter.setPaymentStatus(PaymentStatus.COMPLETED);

            Page<Payment> page = new PageImpl<>(List.of(payment));
            when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(mapper.toFullResponse(any(Payment.class))).thenReturn(response);

            Page<PaymentFullResponseEvent> result = paymentService.getPayments(filter, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(repository, times(1)).findAll(any(Specification.class), eq(pageable));
        }
    }

    @Test
    void shouldReturnEmptyPageWhenNoPayments() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserPublicId).thenReturn(userPublicId);
            securityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            Pageable pageable = PageRequest.of(0, 10);
            PaymentFilterRequest filter = new PaymentFilterRequest();

            Page<Payment> emptyPage = new PageImpl<>(List.of());
            when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

            Page<PaymentFullResponseEvent> result = paymentService.getPayments(filter, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Test
    void shouldCalculateTotalForUser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            LocalDateTime from = LocalDateTime.now().minusDays(1);
            LocalDateTime to = LocalDateTime.now();
            BigDecimal expected = new BigDecimal("150.00");

            when(repository.getTotalAmountForUser(userPublicId, from, to)).thenReturn(expected);

            BigDecimal total = paymentService.getTotalForUser(userPublicId, from, to);

            assertThat(total).isEqualByComparingTo(expected);
            verify(repository, times(1)).getTotalAmountForUser(userPublicId, from, to);
        }
    }

    @Test
    void shouldCalculateTotalForAllUsers() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        BigDecimal expected = new BigDecimal("500.00");

        when(repository.getTotalAmount(from, to)).thenReturn(expected);

        BigDecimal total = paymentService.getTotalForAllUsers(from, to);

        assertThat(total).isEqualByComparingTo(expected);
        verify(repository, times(1)).getTotalAmount(from, to);
    }
}