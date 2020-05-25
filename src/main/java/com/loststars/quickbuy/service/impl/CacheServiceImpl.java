package com.loststars.quickbuy.service.impl;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.loststars.quickbuy.service.CacheService;

@Service
public class CacheServiceImpl implements CacheService {
    
    private Cache<String, Object> commonCache = null;
    
    @PostConstruct
    public void init() {
        commonCache = CacheBuilder.newBuilder()
                .initialCapacity(10)
                .maximumSize(100)
                .expireAfterWrite(60, TimeUnit.SECONDS).build();
    }

    @Override
    public void setCommonCache(String key, Object object) {
        commonCache.put(key, object);
    }

    @Override
    public Object getCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }

}
