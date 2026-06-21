package com.familyexpense.tracker.purchase;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public ResponseEntity<List<Purchase>> getPurchases(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Long profileId
    ) {
        List<Purchase> purchases = purchaseService.getPurchases(startDate, endDate, itemId, profileId);
        return ResponseEntity.ok(purchases);
    }

    @PostMapping
    public ResponseEntity<Purchase> createPurchase(
            @AuthenticationPrincipal Long profileId,
            @Valid @RequestBody CreatePurchaseRequest request
    ) {
        Purchase purchase = purchaseService.createPurchase(profileId, request);
        return ResponseEntity.ok(purchase);
    }

    @GetMapping("/items/{itemId}/last")
    public ResponseEntity<Purchase> getLastPurchaseByItemId(@PathVariable Long itemId) {
        Purchase purchase = purchaseService.getLastPurchaseByItemId(itemId);
        if (purchase == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(purchase);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Purchase> updatePurchase(
            @PathVariable Long id,
            @AuthenticationPrincipal Long profileId,
            @RequestBody UpdatePurchaseRequest request
    ) {
        Purchase purchase = purchaseService.updatePurchase(id, profileId, request);
        return ResponseEntity.ok(purchase);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long id) {
        purchaseService.deletePurchase(id);
        return ResponseEntity.noContent().build();
    }
}
