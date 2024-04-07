package com.acabra;

import com.acabra.data.UrlData;
import com.acabra.response.ProcessResponseType;
import com.acabra.response.TinyUrlCreateResponse;
import com.acabra.response.TinyUrlProcessUrlResponse;
import org.assertj.core.api.Assertions;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.ExpiryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

class TinyUrlServiceTest {

    private TinyUrlService underTest;

    @BeforeEach
    public void setUp() throws Exception {
        TinyUrlDBService db = new TinyUrlDBService();
        KeyGenerationService kgs = new KeyGenerationService(6);
        ExpiryPolicy<? super String, ? super UrlData> expiryPolicy =
                (ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMillis(200)));
        final CacheManager preConfigured = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("preConfigured",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                        String.class,
                                        UrlData.class,
                                        ResourcePoolsBuilder.heap(100)
                                ).build())
                .build(true);
        Cache<String, UrlData> urlCache =
                preConfigured
                        .createCache(
                                "myCache",
                                CacheConfigurationBuilder
                                        .newCacheConfigurationBuilder(
                                                String.class, UrlData.class, ResourcePoolsBuilder.heap(100)
                                        ).withExpiry(expiryPolicy).build());
        this.underTest = new TinyUrlService(db,kgs, urlCache);
    }
    @Test
    public void shouldCreateANewKey() {
        final String expectedURL = "myUrl";

        final TinyUrlCreateResponse resp = this.underTest.createUrl(
                expectedURL, LocalDateTime.now().plus(30, ChronoUnit.DAYS), 1);
        Assertions.assertThat(resp.created()).isTrue();
        Assertions.assertThat(resp.key()).hasSize(6);

        final TinyUrlProcessUrlResponse actual = this.underTest.processUrl(resp.key());
        Assertions.assertThat(actual.type()).isEqualTo(ProcessResponseType.VALID);
        Assertions.assertThat(actual.redirectTo()).isEqualTo(expectedURL);
    }
}