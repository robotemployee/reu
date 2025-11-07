package com.robotemployee.reu.util;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class IntegerWeightedRandomList<K> extends ArrayList<Map.Entry<K, Integer>> {

    public K pickRandom(RandomSource randomSource) {
        if (isEmpty()) throw new IllegalStateException("Can't pick randomly from an empty list");
        int sum = 0;
        for (Map.Entry<K, Integer> entry : this) {
            int weight = entry.getValue();
            sum += weight;
        }

        int remainingCount = randomSource.nextInt(sum);

        K result = get(0).getKey();

        for (Map.Entry<K, Integer> entry : this) {
            int weight = entry.getValue();
            remainingCount -= weight;
            if (remainingCount > 0) continue;

            result = entry.getKey();
            break;
        }
        return result;
    }

    public K popRandom(RandomSource randomSource) {
        if (isEmpty()) throw new IllegalStateException("Can't pick randomly from an empty list");
        int sum = 0;
        for (Map.Entry<K, Integer> entry : this) {
            int weight = entry.getValue();
            sum += weight;
        }

        int remainingCount = randomSource.nextInt(sum);

        Map.Entry<K, Integer> resultEntry = get(0);

        for (Map.Entry<K, Integer> entry : this) {
            int weight = entry.getValue();
            remainingCount -= weight;
            if (remainingCount > 0) continue;

            resultEntry = entry;
            break;
        }
        remove(resultEntry);
        return resultEntry.getKey();
    }

    public void add(K result, Integer weight) {
        add(Map.entry(result, weight));
    }
}
