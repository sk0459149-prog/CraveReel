package com.recipereels.service.impl;

import com.recipereels.repository.RecipeReelRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Records reel views in the background so a view request does not have to wait
 * for the database update to finish.
 */
@Component
public class ReelViewTracker {

    private final RecipeReelRepository reelRepository;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<Long, Long> recentViews = new HashMap<>();

    public ReelViewTracker(RecipeReelRepository reelRepository) {
        this.reelRepository = reelRepository;
    }

    public synchronized void recordView(Long reelId) {
        recentViews.merge(reelId, 1L, Long::sum);
        executor.submit(() -> reelRepository.incrementViewCount(reelId));
    }

    public synchronized long getRecentViewCount(Long reelId) {
        return recentViews.getOrDefault(reelId, 0L);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
