package com.example.tasktracker.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequests {
    private static final Sort BY_CREATION = Sort.by("createdAt").and(Sort.by("id"));
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private PageRequests() {
    }

    public static Pageable byCreation(Pageable pageable){
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), BY_CREATION);
    }

    public static Pageable newestFirst(Pageable pageable){
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
    }
}
