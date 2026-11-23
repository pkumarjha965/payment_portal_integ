package org.medix.model;

public record Product(
        String id,
        String name,
        String category,
        String description,
        Long price,
        Double rating,
        Integer reviewsCount,
        String imageUrl,
        boolean inStock,
        String badge
) {
}
