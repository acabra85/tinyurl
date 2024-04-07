package com.acabra.response;

public record TinyUrlCreateResponse (boolean created, String key) {
    public static TinyUrlCreateResponse ok(String key) {
        return new TinyUrlCreateResponse(true, key);
    }

    public static TinyUrlCreateResponse fail() {
        return new TinyUrlCreateResponse(false, null);
    }
}
