package org.example.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order_service.dto.CreateItemRequest;
import org.example.order_service.dto.ItemResponse;
import org.example.order_service.dto.UpdateItemPriceRequest;
import org.example.order_service.dto.UpdateItemStatusRequest;
import org.example.order_service.entity.Item;
import org.example.order_service.enums.ItemStatus;
import org.example.order_service.exception.EntityNotFoundException;
import org.example.order_service.mapper.ItemMapper;
import org.example.order_service.repository.ItemRepository;
import org.example.order_service.specification.ItemSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Transactional
    public ItemResponse createItem(CreateItemRequest request) {
        Item item = itemMapper.toEntity(request);
        if (item.getStatus() == null) {
            item.setStatus(ItemStatus.ACTIVE);
        }
        Item saved = itemRepository.save(item);
        log.info("Item created: {}", saved.getId());
        return itemMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + id));
        return itemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> getAllItems(String name, BigDecimal minPrice, BigDecimal maxPrice,
                                          ItemStatus status, Pageable pageable) {
        Specification<Item> spec = Specification
                .where(ItemSpecification.hasName(name))
                .and(ItemSpecification.priceBetween(minPrice, maxPrice))
                .and(ItemSpecification.hasStatus(status));

        return itemRepository.findAll(spec, pageable)
                .map(itemMapper::toResponse);
    }

    @Transactional
    public ItemResponse updateItemStatus(Long id, UpdateItemStatusRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + id));
        item.setStatus(request.getStatus());
        Item updated = itemRepository.save(item);
        log.info("Item {} status updated to {}", id, request.getStatus());
        return itemMapper.toResponse(updated);
    }

    @Transactional
    public ItemResponse updateItemPrice(Long id, UpdateItemPriceRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + id));
        item.setPrice(request.getPrice());
        Item updated = itemRepository.save(item);
        log.info("Item {} price updated to {}", id, request.getPrice());
        return itemMapper.toResponse(updated);
    }
}