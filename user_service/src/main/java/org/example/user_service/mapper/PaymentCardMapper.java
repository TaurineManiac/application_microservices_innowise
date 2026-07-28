package org.example.user_service.mapper;

import org.example.user_service.dto.CreatePaymentCardRequest;
import org.example.user_service.dto.PaymentCardResponse;
import org.example.user_service.dto.UpdateCardRequest;
import org.example.user_service.entity.PaymentCard;
import org.example.user_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    PaymentCardMapper INSTANCE = Mappers.getMapper(PaymentCardMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "number", ignore = true)
    @Mapping(target = "expirationDate", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "user", source = "user")
    PaymentCard toEntity(CreatePaymentCardRequest request, User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "number", ignore = true)
    @Mapping(target = "expirationDate", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    void updateEntity(UpdateCardRequest request, @MappingTarget PaymentCard card);

    PaymentCardResponse toResponse(PaymentCard card);
}
