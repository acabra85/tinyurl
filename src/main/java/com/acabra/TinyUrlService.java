package com.acabra;

import com.acabra.data.UrlData;
import com.acabra.response.ProcessResponseType;
import com.acabra.response.TinyUrlCreateResponse;
import com.acabra.response.TinyUrlDeleteResponse;
import com.acabra.response.TinyUrlProcessUrlResponse;
import org.ehcache.Cache;

import java.time.LocalDateTime;

public class TinyUrlService {

    private final KeyGenerationService kgs;
    private final Cache<String, UrlData> cache;
    private final TinyUrlDBService db;

    public TinyUrlService(TinyUrlDBService db, KeyGenerationService kgs, Cache<String, UrlData> cm) {
        this.kgs = kgs;
        this.db = db;
        this.cache = cm;
    }

    public TinyUrlCreateResponse createUrl(String originalUrl, LocalDateTime expiration, int userId) {
        String key = kgs.getNextKey();
        while (db.hasKey(key)) {
            key = kgs.getNextKey();
        }
        LocalDateTime created = LocalDateTime.now();
        LocalDateTime validExpiration = getOrDefaultExpiration(expiration);
        if (db.insertKey(key, originalUrl, created, validExpiration, userId)) {
            cache.put(key, new UrlData(key, originalUrl, userId, created, expiration));
            return TinyUrlCreateResponse.ok(key);
        }
        return TinyUrlCreateResponse.fail();
    }

    private LocalDateTime getOrDefaultExpiration(LocalDateTime expiration) {
        if (expiration == null || expiration.isBefore(LocalDateTime.now())) {
            return LocalDateTime.now().plusDays(30);
        }
        return expiration;
    }

    public TinyUrlProcessUrlResponse processUrl(String urlKey) {
        UrlData urlData = cache.get(urlKey);
        if (urlData == null) {
            urlData = db.getUrlData(urlKey);
            if (urlData == null) {
                return new TinyUrlProcessUrlResponse(ProcessResponseType.NOT_FOUND, urlKey, null);
            }
        }
        final ProcessResponseType type =
                urlData.expires().isBefore(LocalDateTime.now())
                        ? ProcessResponseType.EXPIRED
                        : ProcessResponseType.VALID;
        return new TinyUrlProcessUrlResponse(type, urlKey, urlData.url());
    }

    public TinyUrlDeleteResponse deleteUrl() {
        return null;
    }
}
