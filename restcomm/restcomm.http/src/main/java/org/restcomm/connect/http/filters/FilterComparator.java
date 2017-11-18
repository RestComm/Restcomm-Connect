package org.restcomm.connect.http.filters;

import java.util.Comparator;

public final class FilterComparator {

    public static Comparator<String> STRING_EQUALS = new Comparator<String>() {
        @Override
        public int compare ( String o1, String o2 ) {
            return o1.compareTo(o2);
        }
    };

    public static Comparator<String> STRING_EQUALS_IGNORE_CASE = new Comparator<String>() {
        @Override
        public int compare ( String o1, String o2 ) {
            return o1.compareToIgnoreCase(o2);
        }
    };

    public static Comparator<String> STRING_CONTAINS = new Comparator<String>() {
        @Override
        public int compare ( String o1, String o2 ) {
            return o1.contains(o2) ? 1 : 0;
        }
    };

    public static Comparator<Integer> INTEGER_EQUALS = new Comparator<Integer>() {
        @Override
        public int compare ( Integer o1, Integer o2 ) {
            return o1.intValue() == o2.intValue() ? 1 : 0;
        }
    };

    public static Comparator<Integer> INTEGER_GREATER_THAN = new Comparator<Integer>() {
        @Override
        public int compare ( Integer o1, Integer o2 ) {
            return o1.intValue() > o2.intValue() ? 1 : 0;
        }
    };

    public static Comparator<Integer> INTEGER_LESS_THAN = new Comparator<Integer>() {
        @Override
        public int compare ( Integer o1, Integer o2 ) {
            return o1.intValue() > o2.intValue() ? 1 : 0;
        }
    };
}
