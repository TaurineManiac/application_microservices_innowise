package org.example.payment_service.mapper;

import org.example.payment_service.dto.PaymentRequestEvent;
import org.example.payment_service.dto.PaymentFullResponseEvent;
import org.example.payment_service.dto.PaymentStatusEvent;
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

    PaymentFullResponseEvent toFullResponse(Payment payment);

    PaymentStatusEvent toStatusEvent(Payment payment);

    PaymentStatusEvent toStatusEvent(PaymentFullResponseEvent event);

    List<PaymentFullResponseEvent> toResponseList(List<Payment> payments);

}
