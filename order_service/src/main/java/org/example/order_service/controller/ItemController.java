package org.example.order_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order_service.dto.CreateItemRequest;
import org.example.order_service.dto.ItemResponse;
import org.example.order_service.dto.UpdateItemPriceRequest;
import org.example.order_service.dto.UpdateItemStatusRequest;
import org.example.order_service.enums.ItemStatus;
import org.example.order_service.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody CreateItemRequest request) {
        log.info("REST request to create item: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.createItem(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItem(@PathVariable Long id) {
        log.info("REST request to get item by id: {}", id);
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ItemResponse>> getAllItems(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) ItemStatus status,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("REST request to get all items with filters");
        return ResponseEntity.ok(itemService.getAllItems(name, minPrice, maxPrice, status, pageable));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> updateItemStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateItemStatusRequest request) {

        log.info("REST request to update status of item {} to {}", id, request.getStatus());
        return ResponseEntity.ok(itemService.updateItemStatus(id, request));
    }

    @PatchMapping("/{id}/price")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> updateItemPrice(
            @PathVariable Long id,
            @Valid @RequestBody UpdateItemPriceRequest request) {

        log.info("REST request to update price of item {} to {}", id, request.getPrice());
        return ResponseEntity.ok(itemService.updateItemPrice(id, request));
    }
}