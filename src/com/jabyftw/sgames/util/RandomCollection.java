package com.jabyftw.sgames.util;

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/*
 SOURCE: http://stackoverflow.com/questions/6409652/random-weighted-selection-java-framework
 */
public class RandomCollection <E> {

    private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
    private final Random random = new Random();
    private double total = 0;

    public RandomCollection() {
    }

    public void add(double weight, E result) {
        if(weight <= 0) {
            return;
        }
        total += weight;
        map.put(total, result);
    }

    public E next() {
        double value = random.nextDouble() * total;
        return map.ceilingEntry(value).getValue();
    }
}
