package com.familyexpense.tracker.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByItemIdOrderByPurchaseDateDesc(Long itemId);

    List<Purchase> findByItemIdOrderByPurchaseDateAsc(Long itemId);

    @Query("SELECT p FROM Purchase p WHERE " +
           "(:startDate IS NULL OR p.purchaseDate >= :startDate) AND " +
           "(:endDate IS NULL OR p.purchaseDate <= :endDate) AND " +
           "(:categoryId IS NULL OR p.item.category.id = :categoryId) AND " +
           "(:itemId IS NULL OR p.item.id = :itemId) AND " +
           "(:profileId IS NULL OR p.profile.id = :profileId) " +
           "ORDER BY p.purchaseDate DESC, p.id DESC")
    List<Purchase> findWithFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("itemId") Long itemId,
            @Param("profileId") Long profileId
    );
}
