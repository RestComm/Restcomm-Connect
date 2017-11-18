package org.restcomm.connect.http.filters;

import java.util.ArrayList;
import java.util.List;

public abstract class CollectionFilter<T> {

    public abstract boolean checkCondition ( T t );

    public List<T> filter ( List<T> list ) {
        List<T> filteredList = new ArrayList<T>();
        for (T t : list) {
            if (this.checkCondition(t))
                filteredList.add(t);
        }
        return filteredList;
    }
}
