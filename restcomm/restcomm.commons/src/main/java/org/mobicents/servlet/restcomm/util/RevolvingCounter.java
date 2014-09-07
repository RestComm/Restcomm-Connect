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
package org.mobicents.servlet.restcomm.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class RevolvingCounter {
    private final long start;
    private final long limit;
    private final AtomicLong count;
    private final Lock lock;

    public RevolvingCounter(final long limit) {
        this(0, limit);
    }

    public RevolvingCounter(final long start, final long limit) {
        super();
        this.start = start;
        this.limit = limit;
        this.count = new AtomicLong();
        this.count.set(start);
        this.lock = new ReentrantLock();
    }

    public long get() {
        long result = count.getAndIncrement();
        if (result >= limit) {
            while (!lock.tryLock()) { /* Spin */
            }
            if (count.get() >= limit) {
                result = start;
                count.set(start + 1);
                lock.unlock();
            } else {
                lock.unlock();
                result = get();
            }
        }
        return result;
    }
}
