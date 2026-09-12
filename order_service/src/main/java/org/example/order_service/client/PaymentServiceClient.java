package org.example.order_service.client;

import org.example.order_service.dto.PaymentFullResponseEvent;
import org.example.order_service.dto.PaymentRequestEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "payment-service", url = "${payment.service.url}")
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments/internal")
    PaymentFullResponseEvent createPayment(
            @RequestBody PaymentRequestEvent request,
            @RequestHeader("X-Internal-Token") String internalToken
    );
}