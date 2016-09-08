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

import org.apache.commons.lang.NotImplementedException;
import org.mobicents.servlet.restcomm.dao.OrgIdentityDao;
import org.mobicents.servlet.restcomm.entities.OrgIdentity;
import org.mobicents.servlet.restcomm.entities.Sid;

import java.util.List;

/**
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class OrgIdentityDaoMock implements OrgIdentityDao {
    List<OrgIdentity> orgIdentities;

    public OrgIdentityDaoMock(List<OrgIdentity> orgIdentities) {
        this.orgIdentities = orgIdentities;
    }

    @Override
    public void addOrgIdentity(OrgIdentity orgIdentity) {
        for (OrgIdentity oi: orgIdentities) {
            if (oi.getSid().toString().equals(orgIdentity.getSid().toString()) || oi.getName().equals(orgIdentity.getName()))
                throw new RuntimeException("OrgIdentity already exists wuth such name or sid");
        }
        orgIdentities.add(orgIdentity);
    }

    @Override
    public OrgIdentity getOrgIdentity(Sid sid) {
        for (OrgIdentity oi: orgIdentities) {
            if (oi.getSid().toString().equals(sid.toString()))
                return oi;
        }
        return null;
    }

    @Override
    public OrgIdentity getOrgIdentityByName(String name) {
        for (OrgIdentity oi: orgIdentities) {
            if (oi.getName().equals(name))
                return oi;
        }
        return null;    }

    @Override
    public OrgIdentity getOrgIdentityByOrganizationSid(Sid organizationSid) {
        for (OrgIdentity oi: orgIdentities) {
            if (oi.getOrganizationSid().toString().equals(organizationSid.toString()))
                return oi;
        }
        return null;
    }

    @Override
    public List<OrgIdentity> getOrgIdentities() {
        throw new NotImplementedException();
    }

    @Override
    public void removeOrgIdentity(Sid sid) {
        for (OrgIdentity oi: orgIdentities) {
            if (oi.getSid().toString().equals(sid.toString())) {
                orgIdentities.remove(oi);
                return;
            }
        }
    }

    @Override
    public void updateOrgIdentity(OrgIdentity orgIdentity) {
        for (OrgIdentity oi: orgIdentities) {
            if (oi.getSid().toString().equals(orgIdentity.getSid().toString())) {
                orgIdentities.remove(oi);
                orgIdentities.add(orgIdentity);
                return;
            }
        }
    }
}
