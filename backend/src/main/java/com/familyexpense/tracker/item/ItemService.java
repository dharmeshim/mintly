package com.familyexpense.tracker.item;

import com.familyexpense.tracker.category.Category;
import com.familyexpense.tracker.category.CategoryService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryService categoryService;

    public ItemService(ItemRepository itemRepository, CategoryService categoryService) {
        this.itemRepository = itemRepository;
        this.categoryService = categoryService;
    }

    public List<Item> searchItems(String query) {
        if (query == null || query.isBlank()) {
            return itemRepository.findAll();
        }
        return itemRepository.findByNameContainingIgnoreCase(query);
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
    }

    public Item createItem(String name, Long categoryId, String unit) {
        Category category = categoryService.getCategoryById(categoryId);
        if (itemRepository.findByNameIgnoreCaseAndCategoryId(name, categoryId).isPresent()) {
            throw new IllegalArgumentException("Item with this name already exists in this category");
        }
        Item item = new Item(name, category, unit);
        return itemRepository.save(item);
    }

    public Item getOrCreateItem(String name, Long categoryId, String unit) {
        Category category = categoryService.getCategoryById(categoryId);
        return itemRepository.findByNameIgnoreCaseAndCategoryId(name, categoryId)
                .orElseGet(() -> itemRepository.save(new Item(name, category, unit)));
    }
}
