package org.mobicents.servlet.restcomm.identity.mocks;

import org.mobicents.servlet.restcomm.entities.Sid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A mock implementation of Organization dao to use with SSO extension until the real
 * one becomes ready.
 *
 * @author orestis.tsakiridis@gmail.com  - Orestis Tsakiridis
 */
public class OrganizationDao {
    List<Organization> organizations = new ArrayList<Organization>();

    public OrganizationDao(List<Organization> organizations) {
        this.organizations = organizations;
    }

    public OrganizationDao() {}

    Organization getOrganization(Sid organizationSid) {
        for (Organization org: organizations) {
            if (org.getSid().toString().equals(organizationSid.toString()))
                return org;
        }
        return null;
    }

    Organization getOrganizationByDomain(String domain) {
        for (Organization org: organizations) {
            if (org.getDomain().equals(domain)) {
                return org;
            }
        }
        return null;
    }

    void addOrganization(Organization added) {
        if (added == null)
            throw new IllegalArgumentException();
        for (Organization org: organizations) {
            if (org.getSid().equals(added.getSid().toString()))
                throw new RuntimeException("Organization sid already exists: " + added.getSid().toString());
            if (org.getDomain().equals(added.getDomain()))
                throw new RuntimeException("Organization domain already exists: " + added.getDomain());
        }
        organizations.add(added);
    }

    // singleton stuff
    static OrganizationDao instance;
    public static OrganizationDao getInstance() {
        if (instance == null) {
            instance = new OrganizationDao();
        }
        return instance;
    }
}
