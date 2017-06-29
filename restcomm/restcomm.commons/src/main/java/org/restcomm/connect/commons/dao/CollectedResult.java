package org.restcomm.connect.commons.dao;

/**
 * Created by gdubina on 6/6/17.
 */
public class CollectedResult {

    private final String result;
    private final boolean isAsr;
    private final boolean isPartial;

    public CollectedResult(String result, boolean isAsr, boolean isPartial) {
        this.result = result;
        this.isAsr = isAsr;
        this.isPartial = isPartial;
    }

    public String getResult() {
        return result;
    }

    public boolean isAsr() {
        return isAsr;
    }

    public boolean isPartial() {
        return isPartial;
    }

    @Override
    public String toString() {
        return "CollectedResult{" +
                "result='" + result + '\'' +
                ", isAsr=" + isAsr +
                ", isPartial=" + isPartial +
                '}';
    }
}
