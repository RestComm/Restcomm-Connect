package org.restcomm.connect.http.filters;

import java.util.Comparator;

import org.restcomm.connect.dao.entities.Application;

public abstract class ApplicationFilter extends CollectionFilter<Application> {

    public abstract boolean checkCondition ( Application application );

    public static class FriendlyNameFilter extends ApplicationFilter {
        private String friendlyName;
        private Comparator<String> comparator;

        public FriendlyNameFilter ( String friendlyName, Comparator<String> comparator ) {
            this.friendlyName = friendlyName;
            this.comparator = comparator;
        }

        @Override
        public boolean checkCondition ( Application application ) {
            if (this.friendlyName == null)
                return true;
            return (this.comparator.compare(this.friendlyName, application.getFriendlyName()) == 0);
        }

        public static Comparator<String> EQUALS = new Comparator<String>() {
            @Override
            public int compare ( String o1, String o2 ) {
                return o1.compareTo(o2);
            }
        };

        public static Comparator<String> EQUALS_IGNORE_CASE = new Comparator<String>() {
            @Override
            public int compare ( String o1, String o2 ) {
                return o1.compareToIgnoreCase(o2);
            }
        };
    }
}
