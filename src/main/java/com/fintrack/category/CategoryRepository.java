package com.fintrack.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByUserIdIsNull();

    List<Category> findByUserId(UUID userId);

    Optional<Category> findByIdAndUserIdIsNull(UUID id);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
}
