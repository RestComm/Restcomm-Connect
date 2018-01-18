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
package org.restcomm.connect.dao.mybatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoUtils;
import org.restcomm.connect.dao.ProfileAssociationsDao;
import org.restcomm.connect.dao.entities.ProfileAssociation;

/**
 * @author maria-farooq@live.com (Maria Farooq)
 */
@ThreadSafe
public final class MybatisProfileAssociationsDao implements ProfileAssociationsDao {
    private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.ProfileAssociationsDao.";
    private final SqlSessionFactory sessions;

    public MybatisProfileAssociationsDao(final SqlSessionFactory sessions) {
        super();
        this.sessions = sessions;
    }

    @Override
    public ProfileAssociation getProfileAssociationByTargetSid(String targetSid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ProfileAssociation> getAllProfileAssociations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int addProfileAssociation(ProfileAssociation profileAssociation) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void updateProfileAssociation(ProfileAssociation profileAssociation) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteProfileAssociationByProfileSid(String sid) {
        // TODO Auto-generated method stub
        
    }

    private ProfileAssociation toProfileAssociations(final Map<String, Object> map) {
        final Sid profileSid = DaoUtils.readSid(map.get("profile_sid"));
        final Sid targetSid = DaoUtils.readSid(map.get("target_sid"));
        final DateTime dateCreated = DaoUtils.readDateTime(map.get("date_created"));
        final DateTime dateUpdated = DaoUtils.readDateTime(map.get("date_updated"));
        return new ProfileAssociation(profileSid, targetSid, dateCreated.toDate(), dateUpdated.toDate());
    }

    private Map<String, Object> toMap(final ProfileAssociation profileAssociation) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("profile_sid", DaoUtils.writeSid(profileAssociation.getProfileSid()));
        map.put("target_sid", DaoUtils.writeSid(profileAssociation.getTargetSid()));
        map.put("date_created", profileAssociation.getDateCreated());
        map.put("date_updated", profileAssociation.getDateCreated());
        return map;
    }
}
