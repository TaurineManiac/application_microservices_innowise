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
import org.example.user_service.exception.GenerationException;
import org.example.user_service.mapper.PaymentCardMapper;
import org.example.user_service.repository.PaymentCardRepository;
import org.example.user_service.repository.UserRepository;
import org.example.user_service.specification.PaymentCardSpecification;
import org.example.user_service.util.CardNumberGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final CacheService cacheService;
    private final PaymentCardMapper paymentCardMapper;

    @Transactional
    public PaymentCardResponse createCard(UUID publicUserId, CreatePaymentCardRequest request) {
        log.info("Creating new card for user with publicId: {}", publicUserId);

        User user = userRepository.findByPublicId(publicUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with publicId: " + publicUserId));

        long cardCount = paymentCardRepository.countByUserId(user.getId());
        if (cardCount >= AppConstraint.MAX_CARDS_PER_USER.getValue()) {
            throw new IllegalStateException("User already has " + AppConstraint.MAX_CARDS_PER_USER.getValue() + " cards. Maximum limit reached.");
        }

        int expiryYears = AppConstraint.CARD_EXPIRY_YEARS.getValue();
        LocalDate expirationDate = LocalDate.now()
                .plusYears(expiryYears)
                .withDayOfMonth(YearMonth.from(LocalDate.now().plusYears(expiryYears)).lengthOfMonth());

        int maxRetries = AppConstraint.MAX_RETRIES_FOR_CARD_NUMBER.getValue();
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {

                String generatedNumber = CardNumberGenerator.generateRandomCardNumber();

                PaymentCard card = paymentCardMapper.toEntity(request, user);
                card.setNumber(generatedNumber);
                card.setExpirationDate(expirationDate);
                card.setActive(true);

                PaymentCard saved = paymentCardRepository.save(card);
                cacheService.evictUserCache(publicUserId);

                log.info("Card created successfully with id: {} (attempt {})", saved.getId(), attempt);
                return paymentCardMapper.toResponse(saved);

            } catch (DataIntegrityViolationException ex) {
                log.warn("Card number collision on attempt {}/{}, retrying...", attempt, maxRetries);

                if (attempt == maxRetries) {
                    log.error("Failed to generate unique card number after {} attempts", maxRetries);
                    throw new GenerationException("Failed to generate unique card number. Please try again later.");
                }
            }
        }
        throw new GenerationException("Failed to generate unique card number. Please try again later.");
    }

    @Transactional
    public void updateCardsHolderForUser(Long userId, UUID publicId, String newHolder) {
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

        cacheService.evictUserCache(publicId);

        log.info("Updated {} cards to new holder: {}", cards.size(), newHolder);
    }

    @Transactional
    public PaymentCardResponse updateCard(Long cardId, UpdateCardRequest request) {
        log.info("Updating card with id: {}", cardId);

        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + cardId));

        paymentCardMapper.updateEntity(request, card);
        PaymentCard updated = paymentCardRepository.save(card);

        cacheService.evictUserCache(updated.getUser().getPublicId());

        return paymentCardMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public Page<PaymentCardResponse> getCardsByUser(
            UUID publicUserId,
            String number,
            String holder,
            Boolean active,
            Pageable pageable) {

        if (!userRepository.existsByPublicId(publicUserId)) {
            throw new EntityNotFoundException("User not found with publicId: " + publicUserId);
        }

        Specification<PaymentCard> spec = Specification
                .where(PaymentCardSpecification.hasNumber(number))
                .and(PaymentCardSpecification.belongsToUser(publicUserId))
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
        cacheService.evictUserCache(card.getUser().getPublicId());
        log.info("Card activated successfully: {}", cardId);
    }

    @Transactional
    public void deactivateCard(Long cardId) {
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + cardId));
        card.setActive(false);
        paymentCardRepository.save(card);
        cacheService.evictUserCache(card.getUser().getPublicId());
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

    @Transactional(readOnly = true)
    public String getFullCardNumber(Long cardId) {
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + cardId));
        return card.getNumber();
    }
}