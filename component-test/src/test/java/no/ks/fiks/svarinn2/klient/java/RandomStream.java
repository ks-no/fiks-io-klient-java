package no.ks.fiks.svarinn2.klient.java;

import java.io.InputStream;
import java.util.Random;

class RandomStream extends InputStream {

    private final int maxSize;
    private int currentSize = 0;
    private Random rn = new Random(0);

    public RandomStream(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public int read() {
        if (currentSize > maxSize)
            return -1;

        currentSize += 1;
        return (byte) rn.nextInt(256);
    }
}
