package com.acabra;

import com.acabra.data.UrlData;
import com.acabra.response.ProcessResponseType;
import com.acabra.response.TinyUrlCreateResponse;
import com.acabra.response.TinyUrlDeleteResponse;
import com.acabra.response.TinyUrlProcessUrlResponse;
import org.ehcache.Cache;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class TinyUrlService {

    private final KeyGenerationService kgs;
    private final Cache<String, UrlData> cache;
    private final TinyUrlDBService db;

    public TinyUrlService(TinyUrlDBService db, KeyGenerationService kgs, Cache<String, UrlData> cm) {
        this.kgs = kgs;
        this.db = db;
        this.cache = cm;
    }

    public TinyUrlCreateResponse createUrl(String originalUrl, LocalDateTime created, LocalDateTime expiration, int userId) {
        String key = kgs.getNextKey();
        while (db.hasKey(key)) {
            key = kgs.getNextKey();
        }
        LocalDateTime validExpiration = getOrDefaultExpiration(created, expiration);
        if (db.insertKey(key, originalUrl, created, validExpiration, userId)) {
            cache.put(key, new UrlData(key, originalUrl, userId, created, expiration));
            return TinyUrlCreateResponse.ok(key);
        }
        return TinyUrlCreateResponse.fail();
    }

    private LocalDateTime getOrDefaultExpiration(LocalDateTime now, LocalDateTime expiration) {
        if (expiration == null || expiration.isBefore(now)) {
            return now.plusDays(30);
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
        final LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        if (urlData.expires().isBefore(now)) {
            return new TinyUrlProcessUrlResponse(ProcessResponseType.EXPIRED, urlKey, null);
        }
        return new TinyUrlProcessUrlResponse(ProcessResponseType.VALID, urlKey, urlData.url());
    }

    public TinyUrlDeleteResponse deleteUrl() {
        return null;
    }
}
