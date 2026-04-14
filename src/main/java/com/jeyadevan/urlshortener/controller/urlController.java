package com.jeyadevan.urlshortener.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.net.URISyntaxException;

import com.jeyadevan.urlshortener.dto.FullUrl;
import com.jeyadevan.urlshortener.dto.ShortUrl;
import com.jeyadevan.urlshortener.services.urlService;
import com.jeyadevan.urlshortener.services.cacheService;
import com.jeyadevan.urlshortener.model.urlEntity;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@RestController
public class urlController {
    Logger logger = LoggerFactory.getLogger(urlController.class);
    private final urlService urlService;
    private final cacheService cacheService;
    
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
    public urlController(urlService urlService, cacheService cacheService) {
        this.urlService = urlService;
        this.cacheService = cacheService;
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

    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirectToFullUrl(@PathVariable String shortUrl){
               // Todo: Use a separate service for storing metadata like location, timestamp, click-count update etc. for analytics and monitoring purposes
        logger.debug("Received request to redirect short URL: {}", shortUrl);
        FullUrl fullUrl=null;
        // 1) check Redis cache first for better performance
        urlEntity cached = cacheService.getUrlFromCache(shortUrl);
        if (cached != null) {
            logger.info("Cache hit for short URL: {}", shortUrl);
            fullUrl = new FullUrl(cached.getOriginalUrl());
            return createRedirectResponse(fullUrl);
        }

        urlEntity urlEntity = urlService.getUrlEntity(shortUrl);

        if (urlEntity != null) {
            logger.info("Redirecting to original URL: {}", urlEntity.getOriginalUrl());
            cacheService.putUrlInCache(urlEntity); // cache the result for future requests
            return createRedirectResponse(new FullUrl(urlEntity.getOriginalUrl()));
        } else {
            logger.warn("No original URL found for short URL: {}", shortUrl);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }



}