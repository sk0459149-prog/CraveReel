package com.recipereels.analytics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RecipeAnalyticsServiceImpl implements RecipeAnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    public RecipeAnalyticsServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> getTopReels(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        String sql = """
                SELECT r.id, r.title, rr.view_count AS viewCount
                FROM recipe_reels rr
                JOIN recipes r ON r.id = rr.recipe_id
                WHERE rr.status = 'APPROVED'
                ORDER BY rr.view_count DESC, rr.created_at DESC
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, safeLimit);
    }
}
