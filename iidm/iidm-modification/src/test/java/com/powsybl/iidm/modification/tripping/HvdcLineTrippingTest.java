/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.modification.tripping;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class HvdcLineTrippingTest {

    @Test
    public void lineTrippingTest() {
        Network network = HvdcTestNetwork.createLcc();
        HvdcLine hvdcLine = network.getHvdcLine("L");
        Terminal terminal1 = hvdcLine.getConverterStation1().getTerminal();
        Terminal terminal2 = hvdcLine.getConverterStation2().getTerminal();

        assertTrue(terminal1.isConnected());
        assertTrue(terminal2.isConnected());

        HvdcLineTripping tripping = new HvdcLineTripping("L");
        tripping.apply(network);

        assertFalse(terminal1.isConnected());
        assertFalse(terminal2.isConnected());

        terminal1.connect();
        terminal2.connect();
        assertTrue(terminal1.isConnected());
        assertTrue(terminal2.isConnected());

        new HvdcLineTripping("L", "VL1").apply(network);

        assertFalse(terminal1.isConnected());
        assertTrue(terminal2.isConnected());

        terminal1.connect();
        terminal2.connect();
        assertTrue(terminal1.isConnected());
        assertTrue(terminal2.isConnected());

        new HvdcLineTripping("L", "VL2").apply(network);

        assertTrue(terminal1.isConnected());
        assertFalse(terminal2.isConnected());
    }

    @Test(expected = PowsyblException.class)
    public void unknownHvdcLineTrippingTest() {
        Network network = HvdcTestNetwork.createLcc();

        HvdcLineTripping tripping = new HvdcLineTripping("unknownHvdcLine");
        tripping.apply(network);
    }

    @Test(expected = PowsyblException.class)
    public void unknownVoltageLevelTrippingTest() {
        Network network = HvdcTestNetwork.createLcc();

        HvdcLineTripping tripping = new HvdcLineTripping("L", "unknownVoltageLevel");
        tripping.apply(network);
    }
}
