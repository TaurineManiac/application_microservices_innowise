package org.example.user_service.unit;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    private UpdateCardRequest updateRequest;
    private PaymentCardResponse cardResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .publicId("user-123")
                .name("John")
                .surname("Doe")
                .build();

        card = PaymentCard.builder()
                .id(1L)
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(5))
                .active(true)
                .user(user)
                .build();

        createRequest = CreatePaymentCardRequest.builder()
                .holder("John Doe")
                .build();

        updateRequest = UpdateCardRequest.builder()
                .holder("Jonathan Doe")
                .build();

        cardResponse = PaymentCardResponse.builder()
                .id(1L)
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(5))
                .active(true)
                .build();
    }

    @Test
    void createCard_shouldGenerateAndSaveCard() {
        when(userRepository.findByPublicId(anyString())).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserId(anyLong())).thenReturn(0L);
        when(paymentCardRepository.existsByNumber(anyString())).thenReturn(false);
        when(paymentCardMapper.toEntity(any(CreatePaymentCardRequest.class), any(User.class))).thenReturn(card);
        when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(card);
        when(paymentCardMapper.toResponse(any(PaymentCard.class))).thenReturn(cardResponse);

        PaymentCardResponse result = paymentCardService.createCard("user-123", createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isEqualTo("1234567890123456");
        verify(paymentCardRepository).save(card);
        verify(cacheService).evictUserCache("user-123");
    }

    @Test
    void createCard_shouldThrow_whenMaxCardsReached() {
        when(userRepository.findByPublicId(anyString())).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserId(anyLong())).thenReturn(5L);

        assertThatThrownBy(() -> paymentCardService.createCard("user-123", createRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maximum limit reached");
    }

    @Test
    void getCardsByUser_shouldReturnPage() {
        when(userRepository.existsByPublicId(anyString())).thenReturn(true);
        Page<PaymentCard> page = new PageImpl<>(List.of(card));
        when(paymentCardRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page.map(c -> c));
        when(paymentCardMapper.toResponse(any(PaymentCard.class))).thenReturn(cardResponse);

        var result = paymentCardService.getCardsByUser("user-123", null, null, null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void updateCard_shouldUpdateAndEvictCache() {
        when(paymentCardRepository.findById(anyLong())).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(card);
        when(paymentCardMapper.toResponse(any(PaymentCard.class))).thenReturn(cardResponse);

        PaymentCardResponse result = paymentCardService.updateCard(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(paymentCardRepository).save(card);
        verify(cacheService).evictUserCache("user-123");
    }

    @Test
    void activateCard_shouldSetActiveTrue() {
        when(paymentCardRepository.findById(anyLong())).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(card);

        paymentCardService.activateCard(1L);

        assertThat(card.getActive()).isTrue();
        verify(cacheService).evictUserCache("user-123");
    }

    @Test
    void deactivateCard_shouldSetActiveFalse() {
        when(paymentCardRepository.findById(anyLong())).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(card);

        paymentCardService.deactivateCard(1L);

        assertThat(card.getActive()).isFalse();
        verify(cacheService).evictUserCache("user-123");
    }
}