package org.example.user_service.unit;

import org.example.user_service.dto.CreatePaymentCardRequest;
import org.example.user_service.dto.PaymentCardResponse;
import org.example.user_service.dto.UpdateCardRequest;
import org.example.user_service.entity.PaymentCard;
import org.example.user_service.entity.User;
import org.example.user_service.mapper.PaymentCardMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCardMapperTest {

    private final PaymentCardMapper mapper = PaymentCardMapper.INSTANCE;

    @Test
    void shouldMapEntityToResponse() {
        PaymentCard card = PaymentCard.builder()
                .id(1L)
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(5))
                .active(true)
                .build();

        PaymentCardResponse response = mapper.toResponse(card);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNumber()).isEqualTo("1234567890123456");
    }

    @Test
    void shouldMapCreateRequestAndUserToEntity() {
        User user = User.builder().id(1L).build();

        CreatePaymentCardRequest request = CreatePaymentCardRequest.builder()
                .holder("John Doe")
                .build();

        PaymentCard card = mapper.toEntity(request, user);

        assertThat(card).isNotNull();
        assertThat(card.getHolder()).isEqualTo("John Doe");
        assertThat(card.getUser()).isEqualTo(user);
        assertThat(card.getActive()).isTrue();
        assertThat(card.getId()).isNull();
        assertThat(card.getNumber()).isNull();
    }

    @Test
    void shouldUpdateEntityFromUpdateRequest() {
        PaymentCard card = PaymentCard.builder()
                .id(1L)
                .holder("Old")
                .active(true)
                .build();

        UpdateCardRequest request = UpdateCardRequest.builder()
                .holder("New Holder")
                .build();

        mapper.updateEntity(request, card);

        assertThat(card.getHolder()).isEqualTo("New Holder");
        assertThat(card.getActive()).isTrue();
    }
}