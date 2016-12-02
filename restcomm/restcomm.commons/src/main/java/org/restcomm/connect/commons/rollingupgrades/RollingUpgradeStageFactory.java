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

package org.restcomm.connect.commons.rollingupgrades;

import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RollingUpgradeStageFactory<T extends RollingUpgradeStage> {
    List<Class<? extends T> > availableTypes = new ArrayList<Class<? extends T>>();
    List<RollingUpgrade> availableAnnotations = new ArrayList<RollingUpgrade>();
    Class<T> baseType;

    Class<? extends T> effectiveType;

    public RollingUpgradeStageFactory(String rootPackage, Class<T> baseType, RollingUpgradeTracker stateTracker) {
        this.baseType = baseType;
        scanPackage(rootPackage);
        registerTracker(stateTracker);
        RollingUpgradeState trackerSuggestedState = stateTracker.getState(baseType);
        this.effectiveType = getEffectiveType(trackerSuggestedState);
        clear(); // release some RAM (from this instance internal state keep only effectiveType that is needed for create())
    }

    // clears factory state that is not needed at the end of the constructor
    private void clear() {
        this.availableTypes = null;
        this.availableAnnotations = null;
    }

    private void scanPackage(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends T>> typesInPackage = reflections.getSubTypesOf(baseType);
        for ( Class<? extends T> type : typesInPackage) {
            availableTypes.add(type);
            availableAnnotations.add(type.getAnnotation(RollingUpgrade.class));
        }
    }

    /**
     * Determines the type that should be created from the factory. If an annotated type with the trackerSuggestedState is
     * found it is returned. Otherwise, if there is a default type (isDefault -> true) it is returned. If none applies, it
     * returned null.
     *
     * @param trackerSuggestedState
     * @return a Class object or null
     */
    private Class<? extends T> getEffectiveType(RollingUpgradeState trackerSuggestedState) {
        Class<? extends T> defaultType = null;
        for (int i = 0; i < availableTypes.size(); i++) {
            RollingUpgrade annot = availableAnnotations.get(i);
            RollingUpgradeState state = annot.state();
            // if the suggested state is found return immediately
            if (trackerSuggestedState != null && trackerSuggestedState.equals(annot.state()))
                return availableTypes.get(i);
            // at the same keep an eye for the DEFAULT state which has special meaning
            if (annot.isDefault())
                defaultType = availableTypes.get(i);
        }
        // looks like trackerSuggestedState was now found. Let's return the default state if any
        if (defaultType != null)
            return defaultType;
        return null;
    }

    /**
     * Informs the RollingUpgradeTracker about the base type and annotated states for the implementations
     * this factory creates.
     *
     * NOTE: call scanPackage first to have 'availableAnnotations' filled up
     *
     * @param tracker
     */
    void registerTracker(RollingUpgradeTracker tracker) {
        Set<RollingUpgradeState> baseTypeStates = new HashSet<RollingUpgradeState>();
        for ( RollingUpgrade annot: availableAnnotations) {
            RollingUpgradeState state = annot.state();
            if (state != null)
                baseTypeStates.add(annot.state());
        }
        tracker.registerBaseType(baseType, baseTypeStates);
    }

    public T create() {
        if ( this.effectiveType == null)
            throw new IllegalStateException("No appropriate class was found for " + baseType.getName());
        try {
            return effectiveType.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
