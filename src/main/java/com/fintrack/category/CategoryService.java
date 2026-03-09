package com.fintrack.category;

import com.fintrack.auth.User;
import com.fintrack.category.dto.CategoryRequest;
import com.fintrack.category.dto.CategoryResponse;
import com.fintrack.config.AuthHelper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuthHelper authHelper;

    public List<CategoryResponse> getAll() {
        User user = authHelper.getCurrentUser();
        List<CategoryResponse> categories = new ArrayList<>();

        categoryRepository.findByUserIdIsNull().stream()
                .map(this::toResponse)
                .forEach(categories::add);

        categoryRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .forEach(categories::add);

        return categories;
    }

    public CategoryResponse create(CategoryRequest request) {
        User user = authHelper.getCurrentUser();
        Category category = Category.builder()
                .name(request.getName())
                .icon(request.getIcon())
                .user(user)
                .build();
        categoryRepository.save(category);
        return toResponse(category);
    }

    public void delete(UUID id) {
        User user = authHelper.getCurrentUser();
        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found or cannot be deleted"));
        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .isDefault(category.getUser() == null)
                .build();
    }
}
