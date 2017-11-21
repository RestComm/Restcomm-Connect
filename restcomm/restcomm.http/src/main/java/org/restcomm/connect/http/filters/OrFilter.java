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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ddhuy on 2017/11/18.
 */
public class OrFilter<T> extends CollectionFilter<T> {
    private List<CollectionFilter<T>> filters;

    @SafeVarargs
    public OrFilter ( final CollectionFilter<T>... filters ) {
        this.filters = new ArrayList<CollectionFilter<T>>();
        for (CollectionFilter<T> filter : filters) {
            this.filters.add(filter);
        }
    }

    public void appendFilter ( CollectionFilter<T> filter ) {
        this.filters.add(filter);
    }

    public void removeFilter ( CollectionFilter<T> filter ) {
        this.filters.remove(filter);
    }

    @Override
    public boolean checkCondition ( T t ) {
        for (CollectionFilter<T> filter : this.filters) {
            if (!filter.checkCondition(t))
                return false;
        }
        return true;
    }
}
