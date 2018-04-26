package org.chengy;

import com.google.common.base.Stopwatch;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BloomFilterTest {

    static int sizeofNumberSize = Integer.MAX_VALUE >> 9;
    static Random generator = new Random();

    @Test
    public void numberTest() {
        System.out.println(Integer.MAX_VALUE >> 10);
    }

    public static void main(String[] args) {

        int error = 0;
        HashSet<Integer> hashSet = new HashSet<>();
        BloomFilter<Integer> filter = BloomFilter.create(Funnels.integerFunnel(), sizeofNumberSize);
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i = 0; i < sizeofNumberSize; i++) {
            int number = generator.nextInt();
            if (filter.mightContain(number) != hashSet.contains(number)) {
                error++;
            }
            filter.put(number);
            hashSet.add(number);
        }
        long duration = stopwatch.stop().elapsed(TimeUnit.SECONDS);
        System.out.println("duration time " + duration + "s");
        System.out.println("Error count: " + error + ", error rate = " + String.format("%f", (float) error / (float) sizeofNumberSize));

    }
}
