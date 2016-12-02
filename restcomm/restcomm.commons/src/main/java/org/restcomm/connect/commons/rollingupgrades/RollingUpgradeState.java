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

/**
 * Holds identifiers for all rolling upgrade states supported by this version of restcomm.
 *
 * Two types of identifiers/states exist:
 *
 * 'Global' states refer to the upgrade status of a restcomm installation as a whole
 * 'Domain' states refer to the upgrade status of a specific feature that is comprised from several stages
 *
 * Each Global can map to many Domain states. One needs to check the which are the mapped Domain states to tell
 * which of the available upgrade implementations will effective. RollingUpgradeTracker is the class responsible
 * for doing this mapping.
 *
 * Domain state examples:
 *
 * ACCOUNT_PASSWORD_000, ACCOUNT_PASSWORD_001, ACCOUNT_PASSWORD_002: three various upgrade states for adding password, password_algorithm to the Account entity
 * FRIENDLYNAME_RENAME_A, FRIENDLYNAME_RENAME_B: two states for renaming a property in the entity/database table.
 *
 * Global state examples:
 *
 * GLOBAL_000
 * GLOBAL_001
 **
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public enum RollingUpgradeState {
    /*
    //First step of the upgrade, DB is in the new version and we have both new and old version Restcomm nodes
    @Deprecated
    UpgradeState_000,
    //Second step of the upgrade, DB is in the new version and have only new version Restcomm nodes
    UpgradeState_001,
    //Third step of the upgrade, DB is new version, only new version Restcomm nodes, used for cleanup
    UpgradeState_002
    */

    // Global states
    GLOBAL_000,
    GLOBAL_001,
    // Domain states
    ACCOUNT_PASSWORD_000, // reading/writing both to accounts with/without password & password_algorithm for backwards compatibility
    ACCOUNT_PASSWORD_001 // support read/writing to account with password & password_algorithm
}
