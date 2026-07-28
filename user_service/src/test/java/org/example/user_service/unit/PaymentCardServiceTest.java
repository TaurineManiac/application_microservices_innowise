package org.example.user_service.unit;

import org.example.user_service.constant.AppConstraint;
import org.example.user_service.dto.CreatePaymentCardRequest;
import org.example.user_service.dto.PaymentCardResponse;
import org.example.user_service.dto.UpdateCardRequest;
import org.example.user_service.entity.PaymentCard;
import org.example.user_service.entity.User;
import org.example.user_service.mapper.PaymentCardMapper;
import org.example.user_service.repository.PaymentCardRepository;
import org.example.user_service.repository.UserRepository;
import org.example.user_service.service.CacheService;
import org.example.user_service.service.PaymentCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @InjectMocks
    private PaymentCardService paymentCardService;

    private User user;
    private PaymentCard card;
    private CreatePaymentCardRequest createRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .name("John")
                .surname("Doe")
                .active(true)
                .build();

        card = PaymentCard.builder()
                .id(1L)
                .number("4111111111111111")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(5))
                .active(true)
                .user(user)
                .build();

        createRequest = CreatePaymentCardRequest.builder()
                .holder("John Doe")
                .build();
    }

    @Test
    void createCard_shouldSaveAndReturnCard_whenUnderLimit() {
        when(userRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserId(anyLong())).thenReturn(2L);
        when(paymentCardMapper.toEntity(any(), any())).thenReturn(card);
        when(paymentCardRepository.save(any())).thenReturn(card);
        when(paymentCardMapper.toResponse(any())).thenReturn(PaymentCardResponse.builder().id(1L).build());

        PaymentCardResponse response = paymentCardService.createCard(user.getPublicId(), createRequest);

        assertThat(response.getId()).isEqualTo(1L);
        verify(cacheService).evictUserCache(user.getPublicId());

        verify(paymentCardRepository, never()).existsByNumber(anyString());
    }

    @Test
    void createCard_shouldRetryOnCardNumberCollision() {
        when(userRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserId(anyLong())).thenReturn(2L);
        when(paymentCardMapper.toEntity(any(), any())).thenReturn(card);

        when(paymentCardRepository.save(any(PaymentCard.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate card number"))
                .thenReturn(card);

        when(paymentCardMapper.toResponse(any())).thenReturn(PaymentCardResponse.builder().id(1L).build());

        PaymentCardResponse response = paymentCardService.createCard(user.getPublicId(), createRequest);

        assertThat(response.getId()).isEqualTo(1L);

        verify(paymentCardRepository, times(2)).save(any(PaymentCard.class));

        verify(paymentCardRepository, never()).existsByNumber(anyString());
    }

    @Test
    void createCard_shouldThrowIllegalStateException_whenMaxCardsReached() {
        when(userRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserId(anyLong())).thenReturn(5L);

        assertThatThrownBy(() -> paymentCardService.createCard(user.getPublicId(), createRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maximum limit reached");
    }

    @Test
    void updateCardsHolderForUser_shouldUpdateAllCards() {
        List<PaymentCard> cards = List.of(card);
        when(paymentCardRepository.findAllByUser_Id(anyLong())).thenReturn(cards);

        paymentCardService.updateCardsHolderForUser(user.getId(), user.getPublicId(), "New Name");

        verify(paymentCardRepository).saveAll(cards);
        verify(cacheService).evictUserCache(user.getPublicId());
        assertThat(card.getHolder()).isEqualTo("New Name");
    }

    @Test
    void activateCard_shouldEvictCache() {
        when(paymentCardRepository.findById(anyLong())).thenReturn(Optional.of(card));
        paymentCardService.activateCard(1L);
        verify(cacheService).evictUserCache(user.getPublicId());
    }

    @Test
    void deactivateCard_shouldEvictCache() {
        when(paymentCardRepository.findById(anyLong())).thenReturn(Optional.of(card));
        paymentCardService.deactivateCard(1L);
        verify(cacheService).evictUserCache(user.getPublicId());
    }
}