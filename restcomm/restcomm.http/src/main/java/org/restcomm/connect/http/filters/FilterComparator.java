/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.connect.http.filters;

import java.util.Comparator;

/**
 * Created by ddhuy on 2017/11/18.
 */
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
