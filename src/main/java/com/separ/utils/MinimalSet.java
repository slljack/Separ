package com.separ.utils;

import java.util.ArrayList;
import java.util.TreeSet;

public class MinimalSet {
    private TreeSet<Integer> numberList;

    public MinimalSet() {
        numberList = new TreeSet<Integer>();
        numberList.add(-1);
    }

    public void add(int num) {
        synchronized (numberList) {
            numberList.add(num);

            var removeSet = new ArrayList<Integer>();
            var runner = numberList.first();
            while (numberList.contains(runner + 1)) {
                removeSet.add(runner);
                runner = runner + 1;
            }

            numberList.removeAll(removeSet);
        }
    }

    public boolean isEmpty() {
        return numberList.isEmpty();
    }

    public boolean contains(int search) {
        synchronized (numberList) {
            return search < numberList.first() || numberList.contains(search);
        }
    }

    public int next() {
        synchronized (numberList) {
            return numberList.first() + 1;
        }
    }
}
