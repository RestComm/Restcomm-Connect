/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.connect.rvd.upgrade;

import org.junit.Assert;
import org.junit.Test;
import org.restcomm.connect.rvd.exceptions.InvalidProjectVersion;
import org.restcomm.connect.rvd.upgrade.UpgradeService;
import org.restcomm.connect.rvd.upgrade.UpgradeService.UpgradabilityStatus;

/**
 * @author Orestis Tsakiridis
 */
public class UpgradeServiceTest {
    @Test
    public void testVariousUpgradabilityCases() throws InvalidProjectVersion {
        Assert.assertEquals(UpgradabilityStatus.UPGRADABLE, UpgradeService.checkUpgradability("rvd714","1.6"));
        Assert.assertEquals(UpgradabilityStatus.NOT_NEEDED, UpgradeService.checkUpgradability("1.0","1.1"));
        Assert.assertEquals(UpgradabilityStatus.NOT_NEEDED, UpgradeService.checkUpgradability("1.1","1.5") );
        Assert.assertEquals(UpgradabilityStatus.UPGRADABLE, UpgradeService.checkUpgradability("1.5","1.6"));
        Assert.assertEquals(UpgradabilityStatus.NOT_NEEDED, UpgradeService.checkUpgradability("1.1","1.5"));
        Assert.assertEquals(UpgradabilityStatus.UPGRADABLE, UpgradeService.checkUpgradability("1.1","1.6"));
        Assert.assertEquals(UpgradabilityStatus.NOT_SUPPORTED, UpgradeService.checkUpgradability("INVALID_VERSION", "1.6"));
    }
}
