package org.example.order_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order_service.dto.PaymentStatusEvent;
import org.example.order_service.dto.UpdateOrderStatusRequest;
import org.example.order_service.enums.OrderStatus;
import org.example.order_service.enums.PaymentStatus;
import org.example.order_service.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class PaymentStatusConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "payment_status_events", groupId = "order-service-group")
    public void handlePaymentStatus(PaymentStatusEvent event) {
        log.info("Received payment status event: order={}, status={}",
                event.getOrderPublicId(), event.getPaymentStatus());

        OrderStatus orderStatus = mapPaymentStatusToOrderStatus(event.getPaymentStatus());

        if (orderStatus == null) {
            log.warn("Unhandled payment status: {}, ignoring", event.getPaymentStatus());
            return;
        }

        orderService.updateOrder(
                event.getOrderPublicId(),
                UpdateOrderStatusRequest.builder()
                        .status(orderStatus)
                        .build()
        );
    }


    private OrderStatus mapPaymentStatusToOrderStatus(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case COMPLETED -> OrderStatus.COMPLETED;
            case FAILED -> OrderStatus.REJECTED;
            case REFUNDED, CANCELED -> OrderStatus.CANCELED;
            case PENDING, PROCESSING -> null;
        };
    }
}
