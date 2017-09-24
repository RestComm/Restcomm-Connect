package org.restcomm.connect.dao.entities;

import org.restcomm.connect.commons.dao.Sid;

public class AccountPermission extends Permission {
    private boolean value;
    public AccountPermission(Sid sid, String name) {
        this(sid, name, false);
    }
    public AccountPermission(Sid sid, String name, Boolean value) {
        super(sid, name);
        this.value = value;
    }
    /**
     * @return the value
     */
    public boolean isAllowed() {
        return value==true;
    }

    /**
     * @return the value
     */
    public boolean getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(boolean value) {
        this.value = value;
    }

}
