package org.example.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user_service.dto.CreatePaymentCardRequest;
import org.example.user_service.dto.PaymentCardResponse;
import org.example.user_service.dto.UpdateCardRequest;
import org.example.user_service.service.PaymentCardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{publicUserId}/cards")
@RequiredArgsConstructor
@Slf4j
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or #publicUserId.toString() == authentication.principal")
    public ResponseEntity<PaymentCardResponse> createCard(
            @PathVariable UUID publicUserId,
            @Valid @RequestBody CreatePaymentCardRequest request) {

        log.info("REST request to create card for user with publicId: {}", publicUserId);
        PaymentCardResponse response = paymentCardService.createCard(publicUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or #publicUserId.toString() == authentication.principal")
    public ResponseEntity<Page<PaymentCardResponse>> getCardsByUser(
            @PathVariable UUID publicUserId,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String holder,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("REST request to get cards for user with publicId: {} with filters - number: {}, holder: {}, active: {}",
                publicUserId, number, holder, active);

        Page<PaymentCardResponse> page = paymentCardService.getCardsByUser(
                publicUserId, number, holder, active, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentCardResponse> getCardById(
            @PathVariable UUID publicUserId,
            @PathVariable Long cardId) {

        log.info("REST request to get card by id: {} for user: {}", cardId, publicUserId);
        PaymentCardResponse response = paymentCardService.getCardById(cardId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN') or #publicUserId.toString() == authentication.principal")
    public ResponseEntity<PaymentCardResponse> updateCard(
            @PathVariable UUID publicUserId,
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateCardRequest request) {

        log.info("REST request to update card with id: {} for user: {}", cardId, publicUserId);
        PaymentCardResponse response = paymentCardService.updateCard(cardId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN') or #publicUserId.toString() == authentication.principal")
    public ResponseEntity<Void> activateCard(
            @PathVariable UUID publicUserId,
            @PathVariable Long cardId) {

        log.info("REST request to activate card with id: {} for user: {}", cardId, publicUserId);
        paymentCardService.activateCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{cardId}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or #publicUserId.toString() == authentication.principal")
    public ResponseEntity<Void> deactivateCard(
            @PathVariable UUID publicUserId,
            @PathVariable Long cardId) {

        log.info("REST request to deactivate card with id: {} for user: {}", cardId, publicUserId);
        paymentCardService.deactivateCard(cardId);
        return ResponseEntity.noContent().build();
    }
}