package org.example.payment_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.payment_service.dto.PaymentFilterRequest;
import org.example.payment_service.dto.PaymentFullResponseEvent;
import org.example.payment_service.dto.PaymentRequestEvent;
import org.example.payment_service.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/internal")
    public ResponseEntity<PaymentFullResponseEvent> createPayment(
            @Valid @RequestBody PaymentRequestEvent request
    ){
        log.info("Internal request to create payment for order {}", request.getOrderPublicId());
        PaymentFullResponseEvent response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentFullResponseEvent>> getPayments(
            @ModelAttribute PaymentFilterRequest  filterRequest,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        log.info("REST request to get payments with filters");
        return ResponseEntity.ok(paymentService.getPayments(filterRequest, pageable));
    }

    @GetMapping("/total/user")
    @PreAuthorize("hasRole('ADMIN') or #userPublicId == authentication.principal")
    public ResponseEntity<BigDecimal> getTotalAmountOfUser(
            @RequestParam UUID userPublicId,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to
    ){
        return ResponseEntity.ok(paymentService.getTotalForUser(userPublicId, from, to));
    }

    @GetMapping("/total/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getTotalAll(
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to
    ){
        return ResponseEntity.ok(paymentService.getTotalForAllUsers(from, to));
    }
}
