package org.example.order_service.mapper;

import org.example.order_service.dto.CreateItemRequest;
import org.example.order_service.dto.ItemResponse;
import org.example.order_service.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemMapper INSTANCE = Mappers.getMapper(ItemMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    Item toEntity(CreateItemRequest request);

    ItemResponse toResponse(Item item);
}