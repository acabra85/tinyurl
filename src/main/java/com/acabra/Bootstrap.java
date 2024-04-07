package com.acabra;

import com.acabra.data.UrlData;
import org.ehcache.Cache;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.sql.SQLException;

public class Bootstrap {

    public static void main(String[] args) throws SQLException {
        TinyUrlDBService db = new TinyUrlDBService();
        KeyGenerationService kgs = new KeyGenerationService(6);
        Cache<String, UrlData> urlCache =
                (CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("preConfigured",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                        String.class,
                                        UrlData.class,
                                        ResourcePoolsBuilder.heap(100)
                                )
                                .build())
                .build(true))
                .createCache(
                        "myCache",
                        CacheConfigurationBuilder
                                .newCacheConfigurationBuilder(
                                        String.class, UrlData.class, ResourcePoolsBuilder.heap(100)
                                ).build());
        new Controller(new TinyUrlService(db, kgs, urlCache));
    }
}
