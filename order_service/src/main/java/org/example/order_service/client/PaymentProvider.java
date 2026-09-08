package org.example.order_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.example.order_service.dto.PaymentFullResponseEvent;
import org.example.order_service.dto.PaymentRequestEvent;
import org.example.order_service.exception.PaymentServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProvider {

    private final PaymentServiceClient paymentServiceClient;

    @Value("${internal.service.token}")
    private String internalToken;

    @Retryable(
            value = {Exception.class},
            maxAttemptsExpression = "${retry.maxAttempts:3}",
            backoff = @Backoff(
                    delayExpression = "${retry.delay:500}",
                    multiplierExpression = "${retry.multiplier:2}"
            )
    )
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackCreatePayment")
    public PaymentFullResponseEvent createPayment(PaymentRequestEvent request) {
        log.info("Calling Payment Service for order: {}", request.getOrderPublicId());
        return paymentServiceClient.createPayment(request, internalToken);
    }

    public PaymentFullResponseEvent fallbackCreatePayment(PaymentRequestEvent request) {
        log.error("Payment service unavailable after retries. Order not created: {}", request.getOrderPublicId());
        throw new PaymentServiceUnavailableException("Payment service temporarily unavailable. Please try again later.");
    }
}