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

import org.restcomm.connect.commons.exceptions.RollingUpgradeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RollingUpgradeTracker is responsible for choosing an implementation of a (upgraded) feature using
 * RollingUpgradeStates. It uses hardcoded mappings between global and domain states but is designed to
 * also support version information from flyway engine. Edit the constructor in order to add/edit hardcoded
 * mappings.
 *
 * State mapping examples:
 *
 * GLOBAL_000 -> ACCOUNT_PASSWORD_001, FRIDNELYNAME_RENAME_A
 * GLOBAL_001 -> ACCOUNT_PASSWORD_002, FRIENDLYNAME_RENAME_B
 * GLOBAL_002 -> ACCOUNT_PASSWORD_003
 *
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RollingUpgradeTracker {

    RollingUpgradeState currentGlobalState;
    Map<RollingUpgradeState, List<RollingUpgradeState>> globalStateMappings = new HashMap<RollingUpgradeState, List<RollingUpgradeState>>();
    Map<Class<? extends RollingUpgradeStage>, Set<RollingUpgradeState>> statesByBaseType = new HashMap<Class<? extends RollingUpgradeStage>,Set<RollingUpgradeState>>();

    public RollingUpgradeTracker(RollingUpgradeState globalUpgradeState) {
        this.currentGlobalState = globalUpgradeState;
        registerUpgradeStage(RollingUpgradeState.GLOBAL_000, RollingUpgradeState.ACCOUNT_PASSWORD_000);
        registerUpgradeStage(RollingUpgradeState.GLOBAL_001, RollingUpgradeState.ACCOUNT_PASSWORD_001);
    }


    /**
     * Returned the effective state based on the baseType (the upgradable class at stake), state mappings
     * and current global state.
     *
     * @param baseTypeClass
     * @return a state identifier of null if none is defined
     */
    public RollingUpgradeState getState(Class<? extends RollingUpgradeStage> baseTypeClass) {
        // these are available states as declared by annotated types. If global state maps to one of these, we 're set
        Set<RollingUpgradeState> typeStates = statesByBaseType.get(baseTypeClass);
        List<RollingUpgradeState> mappedDomainStates = globalStateMappings.get(currentGlobalState);
        // iterate through all active domain states and search if any of them is included in the typeStates.
        for (RollingUpgradeState domainState: mappedDomainStates) {
            if (typeStates.contains(domainState)) {
                // looks like there is an implementation of baseType that is annotated with domainState
                return domainState;
            }
        }
        return null; // no implementation of baseType annotated with a mapped domainState found
    }

    // utility method for adding hardcoded global->domain state mappings in the constructor
    void registerUpgradeStage(RollingUpgradeState globalState, RollingUpgradeState stageState ) {
        List<RollingUpgradeState> registeredStages = globalStateMappings.get(globalState);
        if (registeredStages == null) {
            registeredStages = new ArrayList<RollingUpgradeState>();
        }
        registeredStages.add(stageState);
    }

    /**
     * Register a base type along the states annotated in its implementations.
     *
     * @param baseType
     * @param states
     */
    public void registerBaseType(Class<? extends RollingUpgradeStage> baseType, Set<RollingUpgradeState> states) {
        if ( statesByBaseType.containsKey(baseType) )
            throw new RollingUpgradeException("Error registering base type '" + baseType.getClass().getName() + "'. It's already there.");
        statesByBaseType.put(baseType, states);
    }

}
