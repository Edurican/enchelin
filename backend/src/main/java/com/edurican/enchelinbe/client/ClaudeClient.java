package com.edurican.enchelinbe.client;

import com.edurican.enchelinbe.service.SummaryJson;

import java.util.List;

public interface ClaudeClient {
    SummaryJson generate(String restaurantName, String category, List<ReviewForSummary> reviews);
}
