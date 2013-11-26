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
package org.mobicents.servlet.restcomm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class ServiceLocator {
    private static final class SingletonHolder {
        private static final ServiceLocator instance = new ServiceLocator();
    }

    private final Map<Class<?>, Object> services;

    private ServiceLocator() {
        super();
        this.services = new ConcurrentHashMap<Class<?>, Object>();
    }

    public <T> T get(final Class<T> klass) {
        synchronized (klass) {
            final Object service = services.get(klass);
            if (service != null) {
                return klass.cast(service);
            } else {
                return null;
            }
        }
    }

    public static ServiceLocator getInstance() {
        return SingletonHolder.instance;
    }

    public <T> void set(final Class<T> klass, final T instance) {
        synchronized (klass) {
            services.put(klass, instance);
        }
    }
}
