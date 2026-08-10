package org.example.order_service.mapper;

import org.example.order_service.dto.OrderResponse;
import org.example.order_service.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "userInfo", ignore = true)
    OrderResponse toResponse(Order order);
}