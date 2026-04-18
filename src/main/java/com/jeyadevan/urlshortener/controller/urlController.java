package com.jeyadevan.urlshortener.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.jeyadevan.urlshortener.common.LocationUtil;
import com.jeyadevan.urlshortener.dto.FullUrl;
import com.jeyadevan.urlshortener.dto.ShortUrl;
import com.jeyadevan.urlshortener.model.clickEventEntity;
import com.jeyadevan.urlshortener.model.urlEntity;
import com.jeyadevan.urlshortener.services.analyticsService;
import com.jeyadevan.urlshortener.services.cacheService;
import com.jeyadevan.urlshortener.services.urlService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class urlController {
    Logger logger = LoggerFactory.getLogger(urlController.class);
    private final urlService urlService;
    private final cacheService cacheService;
    private final analyticsService analyticsService;
    private final LocationUtil locationUtil;

    private ResponseEntity<Void> createRedirectResponse(FullUrl fullUrl) {
        try {
            URI uri = new URI(fullUrl.getOriginalUrl());
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setLocation(uri);
            return new ResponseEntity<>(httpHeaders, HttpStatus.FOUND);
        } catch (URISyntaxException e) {
            logger.error("Invalid URL syntax: {}", fullUrl.getOriginalUrl(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Autowired
    public urlController(urlService urlService, cacheService cacheService, analyticsService analyticsService, LocationUtil locationUtil) {
        this.urlService = urlService;
        this.cacheService = cacheService;
        this.analyticsService = analyticsService;
        this.locationUtil = locationUtil;

    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("URL Shortener is running!");
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortUrl> shortenUrl(@RequestBody FullUrl fullUrl) {
        logger.info("Received request to shorten URL: {}", fullUrl.getOriginalUrl());
        ShortUrl shortUrl = urlService.generateShortUrl(fullUrl);
        logger.info("Generated short URL: {}", shortUrl.getShortUrl());
        return ResponseEntity.ok(shortUrl);
    }

    @GetMapping("/{shortUrl:[a-zA-Z0-9]+}")
    public ResponseEntity<Void> redirectToFullUrl(@PathVariable String shortUrl, HttpServletRequest request){
        logger.debug("Received request to redirect short URL: {}", shortUrl);
        // 1) check Redis cache first for better performance
        urlEntity cached = cacheService.getUrlFromCache(shortUrl);
        if (cached != null) {
            logger.info("Cache hit for short URL: {}", shortUrl);
            String location = locationUtil.getLocationFromIP(request);
            String deviceType = request.getHeader("User-Agent");
            analyticsService.logClickEvent(shortUrl, location, deviceType);
            return createRedirectResponse(new FullUrl(cached.getOriginalUrl()));
        }

        urlEntity urlEntity = urlService.getUrlEntity(shortUrl);

        if (urlEntity != null) {
            logger.info("Redirecting to original URL: {}", urlEntity.getOriginalUrl());
            cacheService.putUrlInCache(urlEntity); // cache the result for future requests
            String location = locationUtil.getLocationFromIP(request);
            String deviceType = request.getHeader("User-Agent");
            analyticsService.logClickEvent(shortUrl, location, deviceType);
            return createRedirectResponse(new FullUrl(urlEntity.getOriginalUrl()));
        } else {
            logger.warn("No original URL found for short URL: {}", shortUrl);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/analytics/{shortUrl:[a-zA-Z0-9]+}")
    public ResponseEntity<?> getAnalytics(@PathVariable String shortUrl) {
        logger.info("Received request for analytics of short URL: {}", shortUrl);
        logger.info("Fetching analytics for short URL: {}", shortUrl);

        List<clickEventEntity> events = analyticsService.getClickEventsByUrlId(shortUrl);

        if (events == null || events.isEmpty()) {
            logger.warn("No analytics found for short URL: {}", shortUrl);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No analytics found");
        }

        Map<String, Object> response = new HashMap<>();

        response.put("totalClicks", events.size());

        response.put("locationStats", analyticsService.getLocationCounts(shortUrl));
        response.put("deviceStats", analyticsService.getDeviceCounts(shortUrl));

        response.put("recentClicks", events);

        return ResponseEntity.ok(response);
    }


}