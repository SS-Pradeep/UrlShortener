package com.jeyadevan.urlshortener.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.jeyadevan.urlshortener.dto.DeviceCount;
import com.jeyadevan.urlshortener.dto.LocationCount;
import com.jeyadevan.urlshortener.model.clickEventEntity;

public interface clickEventRepository extends MongoRepository<clickEventEntity, String> {
    List<clickEventEntity> findByUrlId(String urlId);

    // Aggregation query to get count by location
    @Aggregation(pipeline = {
        "{ $match: { urlId: ?0 } }",
        "{ $group: { _id: '$location', count: { $sum: 1 } } }"
    })
    List<LocationCount> countClicksByLocation(String urlId);

    // Aggregation query to get count by device type
    @Aggregation(pipeline = {
        "{ $match: { urlId: ?0 } }",
        "{ $group: { _id: '$deviceType', count: { $sum: 1 } } }"
    })
    
    List<DeviceCount> countClicksByDeviceType(String urlId);
}
