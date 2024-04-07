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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

class TinyUrlServiceTest {

    private TinyUrlService underTest;
    private TinyUrlDBService db;

    @BeforeEach
    public void setUp() throws Exception {
        this.db = new TinyUrlDBService();
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

    @AfterEach
    public void tearDown() throws Exception {
        this.db.close();
    }

    @Test
    public void shouldRedirect_validKey() {
        final String expectedURL = "myUrl";

        final LocalDateTime now = LocalDateTime.now();
        final TinyUrlCreateResponse resp = this.underTest.createUrl(
                expectedURL, now, now.plus(30, ChronoUnit.DAYS), 1);
        Assertions.assertThat(resp.created()).isTrue();
        Assertions.assertThat(resp.key()).hasSize(6);

        final TinyUrlProcessUrlResponse actual = this.underTest.processUrl(resp.key());
        Assertions.assertThat(actual.type()).isEqualTo(ProcessResponseType.VALID);
        Assertions.assertThat(actual.redirectTo()).isEqualTo(expectedURL);
    }

    @Test
    public void noRedirection_keyExpired() {
        final String expectedURL = "myUrl";

        final LocalDateTime now = LocalDateTime.now();
        final TinyUrlCreateResponse resp = this.underTest.createUrl(
                expectedURL, now, now.plus(10, ChronoUnit.NANOS), 1);
        Assertions.assertThat(resp.created()).isTrue();
        Assertions.assertThat(resp.key()).hasSize(6);

        final TinyUrlProcessUrlResponse actual = this.underTest.processUrl(resp.key());
        Assertions.assertThat(actual.type()).isEqualTo(ProcessResponseType.EXPIRED);
        Assertions.assertThat(actual.redirectTo()).isNull();
    }

    @Test
    public void noRedirection_notFound() {
        final TinyUrlProcessUrlResponse actual = this.underTest.processUrl("nonExistentKey");
        Assertions.assertThat(actual.type()).isEqualTo(ProcessResponseType.NOT_FOUND);
        Assertions.assertThat(actual.redirectTo()).isNull();
    }
}