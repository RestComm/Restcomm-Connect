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
	
package org.mobicents.servlet.restcomm.tts.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public final class LanguagesCode {
    
    private static LanguagesCode instance;
    
    private final Map<Languages, String> languages;
    
    private LanguagesCode() {
        languages = new ConcurrentHashMap<Languages, String>();
        load();
    }

    public static LanguagesCode getInstance(){
        if (instance == null)
            instance = new LanguagesCode();
        
        return instance;
    }
    
    
    private void load() {
        languages.put(Languages.ENGLISH, "en");
        languages.put(Languages.ENGLISH_UK,"en-uk");
        languages.put(Languages.SPANISH, "es-es");
        languages.put(Languages.SPANISH_CATALAN, "es-ca");
        
        
    }
    
    //Example: getLanguageCode("english");
    private String getLanguageCode(final Languages language){
        return languages.get(language);
    }
}
