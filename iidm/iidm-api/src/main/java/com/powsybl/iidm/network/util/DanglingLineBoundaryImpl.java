/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.util;

import com.powsybl.iidm.network.*;

import java.util.Objects;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public class DanglingLineBoundaryImpl implements Boundary {
    // for SV use: side represents the network side, that is always
    // Side.ONE for a dangling line.

    private final DanglingLine parent;

    public DanglingLineBoundaryImpl(DanglingLine parent) {
        this.parent = Objects.requireNonNull(parent);
    }

    @Override
    public double getV() {
        if (valid(parent.getP0(), parent.getQ0())) {
            DanglingLineData danglingLineData = new DanglingLineData(parent, true);
            return danglingLineData.getBoundaryBusU();
        }

        Terminal t = parent.getTerminal();
        Bus b = t.getBusView().getBus();
        return new SV(t.getP(), t.getQ(), getV(b), getAngle(b), Branch.Side.ONE).otherSideU(parent, true);
    }

    @Override
    public double getAngle() {
        if (valid(parent.getP0(), parent.getQ0())) {
            DanglingLineData danglingLineData = new DanglingLineData(parent, true);
            return Math.toDegrees(danglingLineData.getBoundaryBusTheta());
        }
        Terminal t = parent.getTerminal();
        Bus b = t.getBusView().getBus();
        return new SV(t.getP(), t.getQ(), getV(b), getAngle(b), Branch.Side.ONE).otherSideA(parent, true);
    }

    @Override
    public double getP() {
        if (valid(parent.getP0(), parent.getQ0())) {
            return -parent.getP0();
        }
        Terminal t = parent.getTerminal();
        Bus b = t.getBusView().getBus();
        return new SV(t.getP(), t.getQ(), getV(b), getAngle(b), Branch.Side.ONE).otherSideP(parent, true);
    }

    @Override
    public double getQ() {
        if (valid(parent.getP0(), parent.getQ0())) {
            return -parent.getQ0();
        }
        Terminal t = parent.getTerminal();
        Bus b = t.getBusView().getBus();
        return new SV(t.getP(), t.getQ(), getV(b), getAngle(b), Branch.Side.ONE).otherSideQ(parent, true);
    }

    @Override
    public Branch.Side getSide() {
        return null;
    }

    @Override
    public DanglingLine getConnectable() {
        return parent;
    }

    @Override
    public VoltageLevel getVoltageLevel() {
        return parent.getTerminal().getVoltageLevel();
    }

    private static double getV(Bus b) {
        return b == null ? Double.NaN : b.getV();
    }

    private static double getAngle(Bus b) {
        return b == null ? Double.NaN : b.getAngle();
    }

    private static boolean valid(double p0, double q0) {
        return !Double.isNaN(p0) && !Double.isNaN(q0);
    }
}
