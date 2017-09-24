/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.restcomm.connect.dao.entities;

import org.apache.shiro.authz.permission.WildcardPermission;
import org.restcomm.connect.commons.dao.Sid;

public class Permission extends WildcardPermission{
    private final Sid sid;
    private String name;
    public Permission(Sid sid, String name){
        super(name);
        this.sid = sid;
        this.name = name;


    }

    /**
     * @return the sid
     */
    public Sid getSid() {
        return sid;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param new name
     */
    public void setName(String name) {
        this.name = name;
    }
    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }

//    @Override
//    public boolean implies(org.apache.shiro.authz.Permission p) {
//
//        return super.implies(p);
//    }
}
