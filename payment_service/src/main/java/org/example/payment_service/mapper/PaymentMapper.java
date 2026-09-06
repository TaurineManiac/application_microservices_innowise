package org.example.payment_service.mapper;

import org.example.payment_service.dto.PaymentRequestEvent;
import org.example.payment_service.dto.PaymentResponseEvent;
import org.example.payment_service.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper{

    PaymentMapper INSTANCE = Mappers.getMapper(PaymentMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    Payment toEntity(PaymentRequestEvent event);

    PaymentResponseEvent toResponse(Payment payment);

    List<PaymentResponseEvent> toResponseList(List<Payment> payments);

}
