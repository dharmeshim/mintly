package com.familyexpense.tracker.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ResponseEntity<List<Item>> getItems(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(itemService.searchItems(search));
    }

    public record CreateItemRequest(
        @NotBlank String name,
        @NotNull Long categoryId,
        @NotBlank String unit
    ) {}

    @PostMapping
    public ResponseEntity<Item> createItem(@Valid @RequestBody CreateItemRequest request) {
        Item item = itemService.createItem(request.name(), request.categoryId(), request.unit());
        return ResponseEntity.ok(item);
    }
}
