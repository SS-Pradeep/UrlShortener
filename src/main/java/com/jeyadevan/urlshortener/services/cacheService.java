package com.jeyadevan.urlshortener.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.jeyadevan.urlshortener.model.urlEntity;

@Service
public class cacheService {

    private final RedisTemplate<String, urlEntity> redisTemplate;
    private static final String URL_CACHE_PREFIX = "url:";

    @Autowired
    public cacheService(RedisTemplate<String, urlEntity> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public urlEntity getUrlFromCache(String shortUrl) {
        return redisTemplate.opsForValue().get(URL_CACHE_PREFIX + shortUrl);
    }

    public void putUrlInCache(urlEntity entity) {
        if (entity == null || entity.getShortUrl() == null) {
            return;
        }
        redisTemplate
            .opsForValue()
            .set(URL_CACHE_PREFIX + entity.getShortUrl(), entity);
    }

    public void evictUrlFromCache(String shortUrl) {
        redisTemplate.delete(URL_CACHE_PREFIX + shortUrl);
    }
}