package com.acabra;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Random;

public class KeyGenerationService {

    private final int keyLen;
    private static final char[] VALS = "qwertyuiopasdfghjklzxcvbnm1234567890".toCharArray();
    private final Random random = new Random(
            LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond()
    );

    public KeyGenerationService(int keyLen) {
        this.keyLen = keyLen;
    }

    public String getNextKey() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyLen; i++) {
            sb.append(VALS[random.nextInt(VALS.length)]);
        }
        return sb.toString();
    }
}
