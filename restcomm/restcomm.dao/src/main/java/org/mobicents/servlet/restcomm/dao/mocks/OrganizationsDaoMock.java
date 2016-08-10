/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.servlet.restcomm.dao.mocks;

import org.mobicents.servlet.restcomm.dao.OrganizationsDao;
import org.mobicents.servlet.restcomm.entities.Organization;
import org.mobicents.servlet.restcomm.entities.Sid;

import java.util.ArrayList;
import java.util.List;

/**
 * A mock implementation of Organization dao to use with SSO extension until the real
 * one is ready.
 *
 * @author orestis.tsakiridis@gmail.com  - Orestis Tsakiridis
 */
public class OrganizationsDaoMock implements OrganizationsDao {
    List<Organization> organizations = new ArrayList<Organization>();

    public OrganizationsDaoMock(List<Organization> organizations) {
        this.organizations = organizations;
    }

    public OrganizationsDaoMock() {}

    public Organization getOrganization(Sid organizationSid) {
        for (Organization org: organizations) {
            if (org.getSid().toString().equals(organizationSid.toString()))
                return org;
        }
        return null;
    }

    @Override
    public Organization getOrganization(String namespace) {
        for (Organization org: organizations) {
            if (org.getNamespace().equals(namespace)) {
                return org;
            }
        }
        return null;
    }

    @Override
    public List<Organization> getAllOrganizations() {
        return null;
    }

    @Override
    public void updateOrganization(Organization organization) {

    }

    @Override
    public void removeOrganization(Sid sid) {

    }

    @Override
    public void addOrganization(Organization added) {
        if (added == null)
            throw new IllegalArgumentException();
        for (Organization org: organizations) {
            if (org.getSid().equals(added.getSid().toString()))
                throw new RuntimeException("Organization sid already exists: " + added.getSid().toString());
            if (org.getNamespace().equals(added.getNamespace()))
                throw new RuntimeException("Organization domain already exists: " + added.getNamespace());
        }
        organizations.add(added);
    }

    // singleton stuff
    private static OrganizationsDaoMock instance;
    public static OrganizationsDaoMock getInstance() {
        if (instance == null) {
            instance = new OrganizationsDaoMock();
        }
        return instance;
    }
}
