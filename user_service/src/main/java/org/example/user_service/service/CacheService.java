package org.example.user_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final CacheManager cacheManager;

    public void evictUserCache(String publicId) {
        cacheManager.getCache("users").evict(publicId);
        log.info("Cache evicted for user: {}", publicId);
    }
}