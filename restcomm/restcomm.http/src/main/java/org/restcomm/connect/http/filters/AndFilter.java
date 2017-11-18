package org.restcomm.connect.http.filters;

import java.util.ArrayList;
import java.util.List;

public class AndFilter<T> extends CollectionFilter<T> {
    private List<CollectionFilter<T>> filters;

    @SafeVarargs
    public AndFilter ( final CollectionFilter<T>... filters ) {
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
