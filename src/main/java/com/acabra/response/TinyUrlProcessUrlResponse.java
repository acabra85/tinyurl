package com.acabra.response;

public record TinyUrlProcessUrlResponse(ProcessResponseType type, String key, String redirectTo) {
}

