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

package org.restcomm.connect.mgcp;

import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import org.mobicents.protocols.mgcp.jain.pkg.AUMgcpEvent;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.apache.commons.codec.binary.Hex;

import java.net.URI;
import java.util.List;

/**
 * Created by gdubina on 6/6/17.
 */
@Immutable
public class AsrSignal {

    public static final MgcpEvent REQUEST_ASR = MgcpEvent.factory("asr", AUMgcpEvent.END_SIGNAL + 1);

    private static final String SPACE_CHARACTER = " ";

    private final String driver;
    private final List<URI> initialPrompts;
    private final String endInputKey;
    private final long maximumRecTimer;
    private final long waitingInputTimer;
    private final long timeAfterSpeech;
    private final String hotWords;
    private final String lang;

    public AsrSignal(String driver, String lang, List<URI> initialPrompts, String endInputKey, long maximumRecTimer, long waitingInputTimer,
                     long timeAfterSpeech, String hotWords) {
        this.driver = driver;
        this.initialPrompts = initialPrompts;
        this.endInputKey = endInputKey;
        this.maximumRecTimer = maximumRecTimer;
        this.waitingInputTimer = waitingInputTimer;
        this.timeAfterSpeech = timeAfterSpeech;
        this.hotWords = hotWords;
        this.lang = lang;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        if (!initialPrompts.isEmpty()) {
            buffer.append("ip=");
            for (int index = 0; index < initialPrompts.size(); index++) {
                buffer.append(initialPrompts.get(index));
                if (index < initialPrompts.size() - 1) {
                    //https://github.com/RestComm/Restcomm-Connect/issues/1988
                    buffer.append(",");
                }
            }
        }

        if (buffer.length() > 0)
            buffer.append(SPACE_CHARACTER);
        buffer.append("dr=").append(driver);

        if (buffer.length() > 0)
            buffer.append(SPACE_CHARACTER);
        buffer.append("ln=").append(lang);

        if (endInputKey != null) {
            if (buffer.length() > 0)
                buffer.append(SPACE_CHARACTER);
            buffer.append("eik=").append(endInputKey);
        }
        if (maximumRecTimer > 0) {
            if (buffer.length() > 0)
                buffer.append(SPACE_CHARACTER);
            buffer.append("mrt=").append(maximumRecTimer * 10);
        }
        if (waitingInputTimer > 0) {
            if (buffer.length() > 0)
                buffer.append(SPACE_CHARACTER);
            buffer.append("wit=").append(waitingInputTimer * 10);
        }
        if (timeAfterSpeech > 0) {
            if (buffer.length() > 0)
                buffer.append(SPACE_CHARACTER);
            buffer.append("pst=").append(timeAfterSpeech * 10);
        }
        if (hotWords != null) {
            if (buffer.length() > 0)
                buffer.append(SPACE_CHARACTER);
            buffer.append("hw=").append(Hex.encodeHexString(hotWords.getBytes()));
        }
        return buffer.toString();
    }
}
