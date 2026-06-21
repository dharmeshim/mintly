package com.familyexpense.tracker.item;


import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
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

    public Item createItem(String name, String unit) {
        if (itemRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new IllegalArgumentException("Item with this name already exists");
        }
        Item item = new Item(name, unit);
        return itemRepository.save(item);
    }

    public Item getOrCreateItem(String name, String unit) {
        return itemRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> itemRepository.save(new Item(name, unit)));
    }
}
