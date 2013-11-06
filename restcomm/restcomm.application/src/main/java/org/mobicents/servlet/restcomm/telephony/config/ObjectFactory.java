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
package org.mobicents.servlet.restcomm.telephony.config;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class ObjectFactory {
    private final ClassLoader loader;

    public ObjectFactory() {
        super();
        loader = getClass().getClassLoader();
    }

    public ObjectFactory(final ClassLoader loader) {
        super();
        this.loader = loader;
    }

    public Object getObjectInstance(final String name) throws ObjectInstantiationException {
        try {
            final Class<?> klass = loader.loadClass(name);
            return klass.newInstance();
        } catch (final ClassNotFoundException exception) {
            throw new ObjectInstantiationException(exception);
        } catch (final InstantiationException exception) {
            throw new ObjectInstantiationException(exception);
        } catch (final IllegalAccessException exception) {
            throw new ObjectInstantiationException(exception);
        }
    }
}
