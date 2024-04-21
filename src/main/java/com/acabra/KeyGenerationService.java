package com.acabra;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.IntStream;

public class KeyGenerationService {

    private final int keyLen;

    private final long SEED = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond();
    private final byte[] SEED_BYTES = KeyGenerationService.longToBytes(SEED);
    public  final String LETTERS = "qwertyuiopasdfghjklzxcvbnm";
    private final char[] VALS = scramble(LETTERS + "1234567890", SEED_BYTES);

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    private static char[] scramble(String keys, byte[] seedBytes) {
        char [] scrambled = keys.toCharArray();
        ArrayList<Character> arr = new ArrayList<>();
        IntStream.range(0, keys.length()).forEach(i -> arr.add(keys.charAt(i)));
        SecureRandom random = new SecureRandom(seedBytes);
        int i = -1;
        while(!arr.isEmpty()) {
            scrambled[++i] = arr.remove(random.nextInt(arr.size()));
        }
        return scrambled;
    }

    private final Random random = new Random(SEED);

    public KeyGenerationService(int keyLen) {
        this.keyLen = keyLen;
    }

    public String getNextKey() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyLen; ++i) {
            sb.append(VALS[random.nextInt(VALS.length)]);
        }
        return sb.toString();
    }
}
