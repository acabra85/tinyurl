package com.acabra;

import com.acabra.response.TinyUrlCreateResponse;
import com.acabra.response.TinyUrlDeleteResponse;
import com.acabra.response.TinyUrlProcessUrlResponse;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class Controller {

    private final TinyUrlService urlService;

    public Controller(TinyUrlService urlService) throws SQLException {
        this.urlService = urlService;
    }

    public TinyUrlCreateResponse handleCreateUrl(
            String originalUrl, LocalDateTime expiration, int userId
    ) {
        final LocalDateTime timestamp = LocalDateTime.now();
        return this.urlService.createUrl(
                originalUrl,
                timestamp,
                expiration,
                userId);
    }

    public TinyUrlDeleteResponse handleDeleteUrl(String urlKey) {
        return this.urlService.deleteUrl();
    }

    public TinyUrlProcessUrlResponse processUrl(String urlKey) {
        return this.urlService.processUrl(urlKey);
    }
}
