/*
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
package org.mobicents.servlet.restcomm.dao;

import java.util.List;

import org.mobicents.servlet.restcomm.entities.AvailablePhoneNumber;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public interface AvailablePhoneNumbersDao {
    void addAvailablePhoneNumber(AvailablePhoneNumber availablePhoneNumber);

    List<AvailablePhoneNumber> getAvailablePhoneNumbers();

    List<AvailablePhoneNumber> getAvailablePhoneNumbersByAreaCode(String areaCode);

    List<AvailablePhoneNumber> getAvailablePhoneNumbersByPattern(String pattern);

    List<AvailablePhoneNumber> getAvailablePhoneNumbersByRegion(String region);

    List<AvailablePhoneNumber> getAvailablePhoneNumbersByPostalCode(int postalCode);

    void removeAvailablePhoneNumber(String phoneNumber);
}
