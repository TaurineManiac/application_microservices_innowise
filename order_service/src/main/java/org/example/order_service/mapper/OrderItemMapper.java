package org.example.order_service.mapper;

import org.example.order_service.dto.OrderItemResponse;
import org.example.order_service.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    OrderItemMapper INSTANCE = Mappers.getMapper(OrderItemMapper.class);

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemName", source = "item.name")
    @Mapping(target = "itemPrice", source = "itemsPriceAtMomentOfOrder")
    OrderItemResponse toResponse(OrderItem orderItem);
}