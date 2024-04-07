package com.acabra.data;

import java.time.LocalDateTime;

public record UrlData(String key, String url, int user, LocalDateTime created, LocalDateTime expires) {

}
