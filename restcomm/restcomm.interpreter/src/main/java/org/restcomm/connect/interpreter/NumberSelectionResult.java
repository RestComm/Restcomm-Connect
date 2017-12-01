/*
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
package org.restcomm.connect.interpreter;

import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.IncomingPhoneNumberFilter;

public class NumberSelectionResult {

    IncomingPhoneNumber number;
    IncomingPhoneNumberFilter usedFilter;
    ResultType type;

    public NumberSelectionResult(IncomingPhoneNumber number, IncomingPhoneNumberFilter usedFilter, ResultType type) {
        this.number = number;
        this.usedFilter = usedFilter;
        this.type = type;
    }

    public IncomingPhoneNumber getNumber() {
        return number;
    }

    public IncomingPhoneNumberFilter getUsedFilter() {
        return usedFilter;
    }

    public ResultType getType() {
        return type;
    }
}
