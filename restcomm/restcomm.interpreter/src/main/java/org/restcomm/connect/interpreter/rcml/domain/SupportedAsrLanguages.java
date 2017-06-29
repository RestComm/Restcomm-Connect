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
