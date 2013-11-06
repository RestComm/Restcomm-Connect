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
package org.mobicents.servlet.restcomm.mgcp;

import java.net.URI;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Ignore;
import org.mobicents.servlet.restcomm.mgcp.PlayCollect;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 */
public final class PlayCollectTest {
    public PlayCollectTest() {
        super();
    }

    @Ignore
    @Test
    public void testFormatting() {
        // We will used the builder pattern to create this message.
        final PlayCollect.Builder builder = PlayCollect.builder();
        builder.addPrompt(URI.create("hello.wav"));
        builder.setClearDigitBuffer(true);
        builder.setMinNumberOfDigits(1);
        builder.setMaxNumberOfDigits(16);
        builder.setDigitPattern("0123456789*#");
        builder.setFirstDigitTimer(1000);
        builder.setInterDigitTimer(1000);
        builder.setEndInputKey("#");
        final PlayCollect playCollect = builder.build();
        final String result = playCollect.toString();
        assertTrue("ip=hello.wav cb=true mx=16 mn=1 dp=0123456789*# fdt=10 idt=10 eik=#".equals(result));
        System.out.println("Signal Parameters: " + result + "\n");
    }

    @Test
    public void testFormattingDefaults() {
        // We will used the builder pattern to create this message.
        final PlayCollect.Builder builder = PlayCollect.builder();
        builder.addPrompt(URI.create("hello.wav"));
        final PlayCollect playCollect = builder.build();
        final String result = playCollect.toString();
        assertTrue("ip=hello.wav".equals(result));
        System.out.println("Signal Parameters: " + result + "\n");
    }

    @Ignore
    @Test
    public void testFormattingWithNoPrompts() {
        // We will used the builder pattern to create this message.
        final PlayCollect.Builder builder = PlayCollect.builder();
        builder.setClearDigitBuffer(true);
        builder.setMinNumberOfDigits(1);
        builder.setMaxNumberOfDigits(16);
        builder.setDigitPattern("0123456789*#");
        builder.setFirstDigitTimer(1000);
        builder.setInterDigitTimer(1000);
        builder.setEndInputKey("#");
        final PlayCollect playCollect = builder.build();
        final String result = playCollect.toString();
        assertTrue("cb=true mx=16 mn=1 dp=0123456789*# fdt=10 idt=10 eik=#".equals(result));
        System.out.println("Signal Parameters: " + result + "\n");
    }

    @Ignore
    @Test
    public void testFormattingWithMultiplePrompts() {
        // We will used the builder pattern to create this message.
        final PlayCollect.Builder builder = PlayCollect.builder();
        builder.addPrompt(URI.create("hello.wav"));
        builder.addPrompt(URI.create("world.wav"));
        builder.setClearDigitBuffer(true);
        builder.setMinNumberOfDigits(1);
        builder.setMaxNumberOfDigits(16);
        builder.setDigitPattern("0123456789*#");
        builder.setFirstDigitTimer(1000);
        builder.setInterDigitTimer(1050);
        builder.setEndInputKey("#");
        final PlayCollect playCollect = builder.build();
        final String result = playCollect.toString();
        assertTrue("ip=hello.wav;world.wav cb=true mx=16 mn=1 dp=0123456789*# fdt=10 idt=11 eik=#".equals(result));
        System.out.println("Signal Parameters: " + result + "\n");
    }
}
