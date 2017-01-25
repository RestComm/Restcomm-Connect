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

package org.restcomm.connect.rvd.logging;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.rvd.TestUtils;
import org.restcomm.connect.rvd.concurrency.LogRotationSemaphore;

import java.io.File;
import java.io.IOException;

/**
 * Unit testing for ProjectLogger class
 *
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class CustomLoggerTest {

    CustomLogger customLogger;
    File logDir;
    String basepath;

    @Before
    public void before() {
        logDir = TestUtils.createRandomDir("logtest");
        LogRotationSemaphore semaphore = new LogRotationSemaphore();
        basepath = logDir.getPath() + "/rvd";
        customLogger = new CustomLogger(logDir.getPath() + "/rvd", 1000, 3, semaphore );
    }

    @After
    public void after() throws IOException {
        FileUtils.deleteDirectory( logDir);
    }


    @Test
    public void testBasicLogging() throws IOException {
        customLogger.log("hello world").done();
        String content = FileUtils.readFileToString(new File(basepath + ".log"));
        Assert.assertTrue(content.endsWith("hello world\n"));
    }

    @Test
    public void testRotation() throws IOException {
        for (int i=0; i < 40; i ++) {
            customLogger.log(i + " - A really loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong message").done();
        }
        String content = FileUtils.readFileToString(new File(basepath + ".log"));
        Assert.assertTrue(content.contains("39 - A really"));
        Assert.assertTrue(content.contains("35 - A really"));
        content = FileUtils.readFileToString(new File(basepath + "-1.log"));
        Assert.assertTrue(content.contains("34 - A really"));
        Assert.assertTrue(content.contains("28 - A really"));
        content = FileUtils.readFileToString(new File(basepath + "-2.log"));
        Assert.assertTrue(content.contains("27 - A really"));
        Assert.assertTrue(content.contains("21 - A really"));
        content = FileUtils.readFileToString(new File(basepath + "-3.log"));
        Assert.assertTrue(content.contains("20 - A really"));
        Assert.assertTrue(content.contains("14 - A really"));
        Assert.assertFalse(new File(basepath + "-4.log").exists());
    }

    @Test
    public void test1MBSize() throws IOException {
        LogRotationSemaphore semaphore = new LogRotationSemaphore();
        customLogger = new CustomLogger(logDir.getPath() + "/rvd", 1000000, 3, semaphore );
        for (int i=0; i < 10000; i ++) {
            customLogger.log(i + " - A really loooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong message").done();
        }
        String content = FileUtils.readFileToString(new File(basepath + ".log"));
        Assert.assertTrue(content.contains("9999 - A really"));
        Assert.assertTrue(content.contains("6377 - A really"));
        content = FileUtils.readFileToString(new File(basepath + "-1.log"));
        Assert.assertTrue(content.contains("6376 - A really"));
        Assert.assertTrue(content.contains("] 0 - A really"));

    }

    // TODO
    //    @Test
    //    public void testMultithreadedRotation() {}
}
