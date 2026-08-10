package org.example.order_service.repository;

import org.example.order_service.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    @Override
    Optional<Item> findById(Long id);

    @Override
    Page<Item> findAll(Pageable pageable);

    @Override
    Page<Item> findAll(Specification<Item> spec, Pageable pageable);

    boolean existsById(Long id);
}