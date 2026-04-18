package com.jeyadevan.urlshortener.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jeyadevan.urlshortener.repository.clickEventRepository;
import com.jeyadevan.urlshortener.dto.LocationCount;
import com.jeyadevan.urlshortener.dto.DeviceCount;
import com.jeyadevan.urlshortener.model.clickEventEntity;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class analyticsService {

    Logger logger = LoggerFactory.getLogger(analyticsService.class);

    private final clickEventRepository clickEventRepository;

    @Autowired
    public analyticsService(clickEventRepository clickEventRepository) {
        this.clickEventRepository = clickEventRepository;
    }

    public List<clickEventEntity> getClickEventsByUrlId(String urlId) {
        logger.info("Fetching click events for URL ID: {}", urlId);
        return clickEventRepository.findByUrlId(urlId);
    }

    public void logClickEvent(String urlId, String location, String deviceType) {
        logger.info("Logging click event for URL ID: {}, Location: {}, Device Type: {}", urlId, location, deviceType);
        clickEventEntity clickEvent = new clickEventEntity(urlId, location, deviceType);
        clickEventRepository.save(clickEvent);
    }

    public List<LocationCount> getLocationCounts(String urlId) {
        logger.info("Fetching location counts for URL ID: {}", urlId);
        return clickEventRepository.countClicksByLocation(urlId);
    }

    public List<DeviceCount> getDeviceCounts(String urlId) {
        logger.info("Fetching device counts for URL ID: {}", urlId);
        return clickEventRepository.countClicksByDeviceType(urlId);
    }

}