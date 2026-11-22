package com.geopslabs.geops.catalog.domain.models;

import java.util.List;

/** One page of a listing: the rows asked for and the totals of the whole listing. */
public record ResultPage<T>(List<T> content, int page, long totalElements, int totalPages) {
    public ResultPage {
        content = List.copyOf(content);
    }
}
