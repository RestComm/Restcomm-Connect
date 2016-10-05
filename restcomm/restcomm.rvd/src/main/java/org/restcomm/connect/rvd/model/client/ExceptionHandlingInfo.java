package org.restcomm.connect.rvd.model.client;

import java.util.List;

public class ExceptionHandlingInfo {

    public static class ExceptionMapping {
        private String exceptionName;
        private String next;
        public ExceptionMapping(String exceptionName, String next) {
            super();
            this.exceptionName = exceptionName;
            this.next = next;
        }
        public String getExceptionName() {
            return exceptionName;
        }
        public String getNext() {
            return next;
        }

    }

    private List<ExceptionMapping> exceptionMappings;
    private String defaultNext;

    public ExceptionHandlingInfo(List<ExceptionMapping> exceptionMappings, String defaultNext) {
        super();
        this.exceptionMappings = exceptionMappings;
        this.defaultNext = defaultNext;
    }
}
