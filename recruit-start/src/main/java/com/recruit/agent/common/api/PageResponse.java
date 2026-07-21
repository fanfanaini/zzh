package com.recruit.agent.common.api;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long total,
        int page,
        int size
) {
    public static <T> PageResponse<T> of(List<T> allItems, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 200));
        int fromIndex = Math.min((safePage - 1) * safeSize, allItems.size());
        int toIndex = Math.min(fromIndex + safeSize, allItems.size());
        return new PageResponse<>(allItems.subList(fromIndex, toIndex), allItems.size(), safePage, safeSize);
    }
}
