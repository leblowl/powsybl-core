/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.tck.extensions;

import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.BranchObservability;
import com.powsybl.iidm.network.extensions.BranchObservabilityAdder;
import com.powsybl.iidm.network.test.BatteryNetworkFactory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public abstract class AbstractBranchObservabilityTest {

    @Test
    public void test() {
        Network network = BatteryNetworkFactory.create();
        Line line = network.getLine("NHV1_NHV2_1");
        assertNotNull(line);
        line.newExtension(BranchObservabilityAdder.class)
                .withObservable(true)
                .withStandardDeviationP1(0.02d)
                .withStandardDeviationP2(0.04d)
                .withRedundantP1(true)
                .withRedundantP2(false)
                .withStandardDeviationQ1(0.5d)
                .withStandardDeviationQ2(1.0d)
                .withRedundantQ1(true)
                .withRedundantQ2(false)
                .add();
        BranchObservability<Line> branchObservability = line.getExtension(BranchObservability.class);
        assertEquals("branchObservability", branchObservability.getName());
        assertEquals("NHV1_NHV2_1", branchObservability.getExtendable().getId());

        assertTrue(branchObservability.isObservable());
        branchObservability.setObservable(false);
        assertFalse(branchObservability.isObservable());

        // P
        assertEquals(0.02d, branchObservability.getQualityP1().getStandardDeviation(), 0d);
        branchObservability.getQualityP1().setStandardDeviation(0.03d);
        assertEquals(0.03d, branchObservability.getQualityP1().getStandardDeviation(), 0d);
        assertEquals(0.04d, branchObservability.getQualityP2().getStandardDeviation(), 0d);
        branchObservability.getQualityP2().setStandardDeviation(0.08d);
        assertEquals(0.08d, branchObservability.getQualityP2().getStandardDeviation(), 0d);

        assertTrue(branchObservability.getQualityP1().isRedundant());
        branchObservability.getQualityP1().setRedundant(false);
        assertFalse(branchObservability.getQualityP1().isRedundant());
        assertFalse(branchObservability.getQualityP2().isRedundant());
        branchObservability.getQualityP2().setRedundant(true);
        assertTrue(branchObservability.getQualityP2().isRedundant());

        // Q
        assertEquals(0.5d, branchObservability.getQualityQ1().getStandardDeviation(), 0d);
        branchObservability.getQualityQ1().setStandardDeviation(0.6d);
        assertEquals(0.6d, branchObservability.getQualityQ1().getStandardDeviation(), 0d);
        assertEquals(1.0d, branchObservability.getQualityQ2().getStandardDeviation(), 0d);
        branchObservability.getQualityQ2().setStandardDeviation(1.01d);
        assertEquals(1.01d, branchObservability.getQualityQ2().getStandardDeviation(), 0d);

        assertTrue(branchObservability.getQualityQ1().isRedundant());
        branchObservability.getQualityQ1().setRedundant(false);
        assertFalse(branchObservability.getQualityQ1().isRedundant());
        assertFalse(branchObservability.getQualityQ2().isRedundant());
        branchObservability.getQualityQ2().setRedundant(true);
        assertTrue(branchObservability.getQualityQ2().isRedundant());
    }

    @Test
    public void testMissingQuality() {
        Network network = BatteryNetworkFactory.create();
        Line line = network.getLine("NHV1_NHV2_1");
        assertNotNull(line);
        line.newExtension(BranchObservabilityAdder.class)
                .add();
        BranchObservability<Line> branchObservability = line.getExtension(BranchObservability.class);
        assertEquals("branchObservability", branchObservability.getName());
        assertEquals("NHV1_NHV2_1", branchObservability.getExtendable().getId());

        assertNull(branchObservability.getQualityP1());
        assertNull(branchObservability.getQualityP2());
        assertNull(branchObservability.getQualityQ1());
        assertNull(branchObservability.getQualityQ2());

        // P1
        assertSame(branchObservability, branchObservability.setQualityP1(0.03d));
        assertEquals(0.03d, branchObservability.getQualityP1().getStandardDeviation(), 0d);
        assertSame(branchObservability, branchObservability.setQualityP1(0.04d));
        assertEquals(0.04d, branchObservability.getQualityP1().getStandardDeviation(), 0d);

        assertFalse(branchObservability.getQualityP1().isRedundant());
        branchObservability.getQualityP1().setRedundant(true);
        assertTrue(branchObservability.getQualityP1().isRedundant());

        // P2
        assertSame(branchObservability, branchObservability.setQualityP2(0.031d));
        assertEquals(0.031d, branchObservability.getQualityP2().getStandardDeviation(), 0d);
        assertSame(branchObservability, branchObservability.setQualityP2(0.041d));
        assertEquals(0.041d, branchObservability.getQualityP2().getStandardDeviation(), 0d);

        assertFalse(branchObservability.getQualityP2().isRedundant());
        branchObservability.getQualityP2().setRedundant(true);
        assertTrue(branchObservability.getQualityP2().isRedundant());

        // Q1
        assertSame(branchObservability, branchObservability.setQualityQ1(0.6d));
        assertEquals(0.6d, branchObservability.getQualityQ1().getStandardDeviation(), 0d);
        assertSame(branchObservability, branchObservability.setQualityQ1(0.61d));
        assertEquals(0.61d, branchObservability.getQualityQ1().getStandardDeviation(), 0d);

        assertFalse(branchObservability.getQualityQ1().isRedundant());
        branchObservability.getQualityQ1().setRedundant(true);
        assertTrue(branchObservability.getQualityQ1().isRedundant());

        // Q2
        assertSame(branchObservability, branchObservability.setQualityQ2(0.6d));
        assertEquals(0.6d, branchObservability.getQualityQ2().getStandardDeviation(), 0d);
        assertSame(branchObservability, branchObservability.setQualityQ2(0.61d));
        assertEquals(0.61d, branchObservability.getQualityQ2().getStandardDeviation(), 0d);

        assertFalse(branchObservability.getQualityQ2().isRedundant());
        branchObservability.getQualityQ2().setRedundant(true);
        assertTrue(branchObservability.getQualityQ2().isRedundant());
    }
}
