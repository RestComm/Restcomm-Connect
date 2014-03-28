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
package org.mobicents.servlet.restcomm.interpreter.rcml;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com (George Vagenas)
 */
@Immutable
public final class Verbs {
    public static final String dial = "Dial";
    public static final String enqueue = "Enqueue";
    public static final String fax = "Fax";
    public static final String gather = "Gather";
    public static final String hangup = "Hangup";
    public static final String leave = "Leave";
    public static final String pause = "Pause";
    public static final String play = "Play";
    public static final String record = "Record";
    public static final String redirect = "Redirect";
    public static final String reject = "Reject";
    public static final String say = "Say";
    public static final String sms = "Sms";
    //USSD verbs
    public static final String ussdLanguage = "Language";
    public static final String ussdMessage = "UssdMessage";
    public static final String ussdCollect = "UssdCollect";

    public Verbs() {
        super();
    }

    public static boolean isVerb(final Tag tag) {
        final String name = tag.name();
        if (dial.equals(name))
            return true;
        if (enqueue.equals(name))
            return true;
        if (fax.equals(name))
            return true;
        if (gather.equals(name))
            return true;
        if (hangup.equals(name))
            return true;
        if (leave.equals(name))
            return true;
        if (pause.equals(name))
            return true;
        if (play.equals(name))
            return true;
        if (record.equals(name))
            return true;
        if (redirect.equals(name))
            return true;
        if (reject.equals(name))
            return true;
        if (say.equals(name))
            return true;
        if (sms.equals(name))
            return true;
        if (ussdLanguage.equals(name))
            return true;
        if (ussdMessage.equals(name))
            return true;
        if (ussdCollect.equals(name))
            return true;
        return false;
    }
}
