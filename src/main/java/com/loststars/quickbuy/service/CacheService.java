package com.loststars.quickbuy.service;

public interface CacheService {

    public void setCommonCache(String key, Object object);
    
    public Object getCommonCache(String key);
}
