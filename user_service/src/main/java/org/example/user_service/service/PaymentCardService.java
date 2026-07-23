package org.example.user_service.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user_service.constant.AppConstraint;
import org.example.user_service.dto.CreatePaymentCardRequest;
import org.example.user_service.dto.PaymentCardResponse;
import org.example.user_service.dto.UpdateCardRequest;
import org.example.user_service.entity.PaymentCard;
import org.example.user_service.entity.User;
import org.example.user_service.mapper.PaymentCardMapper;
import org.example.user_service.repository.PaymentCardRepository;
import org.example.user_service.specification.PaymentCardSpecification;
import org.example.user_service.util.CardNumberGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserService userService;
    private final PaymentCardMapper paymentCardMapper;

    @Transactional
    public PaymentCardResponse createCard(String publicUserId, CreatePaymentCardRequest request) {
        log.info("Creating new card for user with publicId: {}", publicUserId);

        User user = userService.getUserEntityByPublicId(publicUserId);

        long cardCount = paymentCardRepository.countByUserId(user.getId());
        if (cardCount >= AppConstraint.MAX_CARDS_PER_USER.getValue()) {
            throw new IllegalStateException("User already has " + AppConstraint.MAX_CARDS_PER_USER.getValue() + " cards. Maximum limit reached.");
        }

        String generatedNumber = generateUniqueCardNumber();

        int expiryYears = AppConstraint.CARD_EXPIRY_YEARS.getValue();
        LocalDate expirationDate = LocalDate.now()
                .plusYears(expiryYears)
                .withDayOfMonth(YearMonth.from(LocalDate.now().plusYears(expiryYears)).lengthOfMonth());

        PaymentCard card = paymentCardMapper.toEntity(request, user);
        card.setNumber(generatedNumber);
        card.setExpirationDate(expirationDate);
        card.setActive(true);

        PaymentCard saved = paymentCardRepository.save(card);
        log.info("Card created successfully with id: {}", saved.getId());
        return paymentCardMapper.toResponse(saved);
    }

    @Transactional
    public void updateCardsHolderForUser(Long userId, String newHolder) {
        log.info("Updating holder name for all cards of userId: {}", userId);
        List<PaymentCard> cards = paymentCardRepository.findAllByUser_Id(userId);
        if (cards.isEmpty()) {
            log.debug("No cards found for user, skipping holder update.");
            return;
        }

        for (PaymentCard card : cards) {
            card.setHolder(newHolder);
        }
        paymentCardRepository.saveAll(cards);
        log.info("Updated {} cards to new holder: {}", cards.size(), newHolder);
    }

    @Transactional
    public PaymentCardResponse updateCard(Long cardId, UpdateCardRequest request) {
        log.info("Updating card with id: {}", cardId);

        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + cardId));

        paymentCardMapper.updateEntity(request, card);
        PaymentCard updated = paymentCardRepository.save(card);
        return paymentCardMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public Page<PaymentCardResponse> getCardsByUser(
            String publicUserId,
            String number,
            String holder,
            Boolean active,
            Pageable pageable) {

        userService.getUserEntityByPublicId(publicUserId);

        Specification<PaymentCard> spec = Specification
                .where(PaymentCardSpecification.hasNumber(number))
                .and(PaymentCardSpecification.hasHolder(holder))
                .and(PaymentCardSpecification.isActive(active));

        return paymentCardRepository.findAll(spec, pageable)
                .map(paymentCardMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PaymentCardResponse getCardById(Long cardId) {
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + cardId));
        return paymentCardMapper.toResponse(card);
    }

    @Transactional
    public void activateCard(Long cardId) {
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + cardId));
        card.setActive(true);
        paymentCardRepository.save(card);
        log.info("Card activated successfully: {}", cardId);
    }

    @Transactional
    public void deactivateCard(Long cardId) {
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + cardId));
        card.setActive(false);
        paymentCardRepository.save(card);
        log.info("Card deactivated successfully: {}", cardId);
    }


    /*I prefer not to use it, because in real bank application we won't delete sensitive data, I suppose.
    @Transactional
    public void deleteCard(Long cardId) {
        if (!paymentCardRepository.existsById(cardId)) {
            throw new EntityNotFoundException("Card not found with id: " + cardId);
        }
        paymentCardRepository.deleteById(cardId);
        log.info("Card deleted successfully: {}", cardId);
    }
    */

    private String generateUniqueCardNumber() {
        int maxRetries = AppConstraint.MAX_RETRIES_FOR_CARD_NUMBER.getValue();
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String candidate = CardNumberGenerator.generateRandomCardNumber();
            if (!paymentCardRepository.existsByNumber(candidate)) {
                return candidate;
            }
            log.debug("Card number collision on attempt {}/{}", attempt, maxRetries);
        }
        throw new IllegalStateException("Failed to generate a unique card number after " + maxRetries + " attempts.");
    }

    @Transactional(readOnly = true)
    public String getFullCardNumber(Long cardId) {
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + cardId));
        return card.getNumber();
    }
}