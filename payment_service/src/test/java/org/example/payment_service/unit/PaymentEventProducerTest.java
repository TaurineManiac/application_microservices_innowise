package org.example.payment_service.unit;

import org.example.payment_service.dto.PaymentStatusEvent;
import org.example.payment_service.entity.Payment;
import org.example.payment_service.enums.PaymentStatus;
import org.example.payment_service.kafka.PaymentEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

    @Mock
    private KafkaTemplate<String, PaymentStatusEvent> kafkaTemplate;

    @InjectMocks
    private PaymentEventProducer producer;

    @Test
    void shouldSendPaymentStatusEvent() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .orderPublicId(orderId)
                .userPublicId(userId)
                .amount(new BigDecimal("50.00"))
                .status(PaymentStatus.COMPLETED)
                .build();

        producer.sendPaymentStatusEvent(payment);

        PaymentStatusEvent expectedEvent = new PaymentStatusEvent(orderId, userId, PaymentStatus.COMPLETED, new BigDecimal("50.00"));
        verify(kafkaTemplate).send("payment_status_events", orderId.toString(), expectedEvent);
    }
}