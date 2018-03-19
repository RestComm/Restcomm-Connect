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
package org.restcomm.connect.sms.smpp;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;

public class RegexRemover {

    public static final char[] REGEX_SPECIAL_CHARS = {'*', '#', '^', '|', '.', '\\', '$', '[', ']'};

    /**
     * SMPP is not supporting organizations at the moment, disable regexes to
     * prevent an organization to capture all cloud traffic with a single regex.
     *
     * @param numbers
     */
    static void removeRegexes(List<IncomingPhoneNumber> numbers) {
        //use a list to prevent conc access during iteration
        List<IncomingPhoneNumber> toBeRemoved = new ArrayList();
        if (numbers != null) {
            for (IncomingPhoneNumber nAux : numbers) {
                if (StringUtils.containsAny(nAux.getPhoneNumber(), REGEX_SPECIAL_CHARS)) {
                    //mark this as to be removed later
                    toBeRemoved.add(nAux);
                }
            }
            //finally remove regexes
            for (IncomingPhoneNumber nAux : toBeRemoved) {
                //this is relying on Java default equals IncomingPhoneNumber
                numbers.remove(nAux);
            }
        }
    }
}
