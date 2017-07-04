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

package org.restcomm.connect.interpreter.rcml.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * Library class with all supported languages.
 *
 * @author Dmitriy Nadolenko
 */
public class SupportedAsrLanguages {

    public static final String DEFAULT_LANGUAGE ="en-US";

    private static final String LANGUAGE_EN_GB = "en-GB";
    private static final String LANGUAGE_ES_ES = "es-ES";
    private static final String LANGUAGE_IT_IT = "it-IT";
    private static final String LANGUAGE_FR_FR = "fr-FR";
    private static final String LANGUAGE_PL_PL = "pl-PL";
    private static final String LANGUAGE_PT_PT = "pt-PT";

    public static final Set<String> languages = new HashSet();
    static
    {
        languages.add(DEFAULT_LANGUAGE);
        languages.add(LANGUAGE_EN_GB);
        languages.add(LANGUAGE_ES_ES);
        languages.add(LANGUAGE_IT_IT);
        languages.add(LANGUAGE_FR_FR);
        languages.add(LANGUAGE_PL_PL);
        languages.add(LANGUAGE_PT_PT);
    }

}
