package com.recipereels.analytics;

import java.util.List;
import java.util.Map;

public interface RecipeAnalyticsService {
    List<Map<String, Object>> getTopReels(int limit);
}
