package com.bryce.mobfarmtools.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WeightedRandomSelector<T> {

    private Map<T, Integer> itemsWithWeights;
    private int totalWeight;
    private final Random random = new Random();

    public WeightedRandomSelector(ArrayList<T> args, ArrayList<Integer> weights) {
        this.updateItems(args, weights);
    }

    public T selectRandomItem() {
        int randomNumber = random.nextInt(totalWeight);

        int runningWeightTotal = 0;
        for (Map.Entry<T, Integer> entry : itemsWithWeights.entrySet()) {
            runningWeightTotal += entry.getValue();
            if (runningWeightTotal > randomNumber) {
                return entry.getKey();
            }
        }

        throw new RuntimeException("Should not reach here");
    }

    public void updateItems(ArrayList<T> args, ArrayList<Integer> weights) {
        if (args.size() != weights.size()) {
            throw new IllegalArgumentException("Args and weights must be matching sizes.");
        }

        this.itemsWithWeights = new HashMap<>();
        for (int i = 0; i < args.size(); i++) {
            this.itemsWithWeights.put(args.get(i), weights.get(i));
        }

        this.totalWeight = itemsWithWeights.values().stream().mapToInt(Integer::intValue).sum();
    }
}