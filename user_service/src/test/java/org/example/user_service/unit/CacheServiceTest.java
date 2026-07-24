package org.example.user_service.unit;

import org.example.user_service.service.CacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private CacheService cacheService;

    @Test
    void evictUserCache_shouldCallEvict() {
        when(cacheManager.getCache("users")).thenReturn(cache);

        cacheService.evictUserCache("public-123");

        verify(cache).evict("public-123");
    }
}