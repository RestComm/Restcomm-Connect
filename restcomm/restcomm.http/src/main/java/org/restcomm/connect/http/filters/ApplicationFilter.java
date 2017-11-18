/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.restcomm.connect.http.filters;

import java.util.Comparator;

import org.restcomm.connect.dao.entities.Application;

/**
 * Created by ddhuy on 2017/11/18.
 */
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
    }
}
