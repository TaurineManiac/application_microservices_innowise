package org.example.payment_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.payment_service.dto.PaymentStatusEvent;
import org.example.payment_service.entity.Payment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String TOPIC = "payment_status_events";

    private final KafkaTemplate<String, PaymentStatusEvent> kafkaTemplate;

    public void sendPaymentStatusEvent(Payment payment) {
        PaymentStatusEvent event = PaymentStatusEvent.builder()
                .orderPublicId(payment.getOrderPublicId())
                .userPublicId(payment.getUserPublicId())
                .paymentStatus(payment.getStatus())
                .amount(payment.getAmount())
                .build();

        kafkaTemplate.send(TOPIC, payment.getOrderPublicId().toString(), event);
        log.info("Sent payment status event: order={}, status={}", payment.getOrderPublicId(), payment.getStatus());
    }
}
