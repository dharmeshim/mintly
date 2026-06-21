package com.familyexpense.tracker.purchase;

import com.familyexpense.tracker.category.Category;
import com.familyexpense.tracker.category.CategoryService;
import com.familyexpense.tracker.item.Item;
import com.familyexpense.tracker.item.ItemService;
import com.familyexpense.tracker.profile.Profile;
import com.familyexpense.tracker.profile.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ProfileService profileService;
    private final ItemService itemService;
    private final CategoryService categoryService;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            ProfileService profileService,
            ItemService itemService,
            CategoryService categoryService
    ) {
        this.purchaseRepository = purchaseRepository;
        this.profileService = profileService;
        this.itemService = itemService;
        this.categoryService = categoryService;
    }

    public List<Purchase> getPurchases(
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            Long itemId,
            Long profileId
    ) {
        return purchaseRepository.findWithFilters(startDate, endDate, categoryId, itemId, profileId);
    }

    public Purchase getPurchaseById(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));
    }

    public Purchase createPurchase(Long profileId, CreatePurchaseRequest req) {
        Profile profile = profileService.getProfileById(profileId);
        Item item = resolveItem(req.itemId(), req.itemName(), req.categoryId(), req.categoryName(), req.categoryColor(), req.unit());

        Purchase purchase = new Purchase(
                item,
                profile,
                req.quantity(),
                req.rate(),
                req.purchaseDate(),
                req.shop(),
                req.paymentMode(),
                req.notes()
        );
        return purchaseRepository.save(purchase);
    }

    public Purchase updatePurchase(Long id, Long profileId, UpdatePurchaseRequest req) {
        Purchase purchase = getPurchaseById(id);

        if (req.quantity() != null) {
            purchase.setQuantity(req.quantity());
        }
        if (req.rate() != null) {
            purchase.setRate(req.rate());
        }
        if (req.purchaseDate() != null) {
            purchase.setPurchaseDate(req.purchaseDate());
        }
        if (req.shop() != null) {
            purchase.setShop(req.shop());
        }
        if (req.paymentMode() != null) {
            purchase.setPaymentMode(req.paymentMode());
        }
        if (req.notes() != null) {
            purchase.setNotes(req.notes());
        }

        if (req.itemId() != null || req.itemName() != null) {
            Item item = resolveItem(req.itemId(), req.itemName(), req.categoryId(), req.categoryName(), req.categoryColor(), req.unit());
            purchase.setItem(item);
        }

        return purchaseRepository.save(purchase);
    }

    public void deletePurchase(Long id) {
        Purchase purchase = getPurchaseById(id);
        purchaseRepository.delete(purchase);
    }

    private Item resolveItem(Long itemId, String itemName, Long categoryId, String categoryName, String categoryColor, String unit) {
        if (itemId != null) {
            return itemService.getItemById(itemId);
        }

        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item ID or Item Name is required");
        }

        Category category;
        if (categoryId != null) {
            category = categoryService.getCategoryById(categoryId);
        } else if (categoryName != null && !categoryName.isBlank()) {
            category = categoryService.getOrCreateCategory(categoryName, categoryColor != null ? categoryColor : "#808080");
        } else {
            throw new IllegalArgumentException("Category reference (ID or name) is required for a new item");
        }

        String itemUnit = (unit != null && !unit.isBlank()) ? unit : "piece";
        return itemService.getOrCreateItem(itemName, category.getId(), itemUnit);
    }
}
