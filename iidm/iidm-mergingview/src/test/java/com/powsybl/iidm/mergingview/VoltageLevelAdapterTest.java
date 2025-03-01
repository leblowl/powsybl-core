/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.mergingview;

import com.google.common.collect.Iterables;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.NoEquipmentNetworkFactory;
import com.powsybl.iidm.network.util.ShortIdDictionary;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class VoltageLevelAdapterTest {

    private MergingView mergingView;
    private Network networkRef;

    @Before
    public void setUp() {
        mergingView = MergingView.create("VoltageLevelTest", "iidm");
        networkRef = NoEquipmentNetworkFactory.create();
        mergingView.merge(networkRef);
    }

    @Test
    public void baseTests() {
        final String vlId = "vl1";
        VoltageLevel vlExpected = networkRef.getVoltageLevel(vlId);
        VoltageLevel vlActual = mergingView.getVoltageLevel(vlId);

        // Setter / Getter
        assertTrue(vlActual instanceof BusBreakerVoltageLevelAdapter);
        assertSame(mergingView, vlActual.getNetwork());
        assertSame(mergingView.getSubstation("sub"), vlActual.getSubstation().orElse(null));
        assertSame(mergingView.getIdentifiable(vlId), vlActual);
        assertEquals(vlExpected.getContainerType(), vlActual.getContainerType());

        vlActual.setHighVoltageLimit(300.0);
        assertEquals(300.0, vlActual.getHighVoltageLimit(), 0.0);
        vlActual.setLowVoltageLimit(200.0);
        assertEquals(200.0, vlActual.getLowVoltageLimit(), 0.0);
        vlActual.setNominalV(500.0);
        assertEquals(500.0, vlActual.getNominalV(), 0.0);
        assertEquals(vlExpected.getShuntCompensatorCount(), vlActual.getShuntCompensatorCount());
        assertEquals(vlExpected.getLccConverterStationCount(), vlActual.getLccConverterStationCount());

        // VscConverterStation
        vlActual.newVscConverterStation()
                    .setId("C1")
                    .setName("Converter1")
                    .setConnectableBus("busA")
                    .setBus("busA")
                    .setLossFactor(0.011f)
                    .setVoltageSetpoint(405.0)
                    .setVoltageRegulatorOn(true)
                    .setReactivePowerSetpoint(123)
                    .setEnsureIdUnicity(false)
                 .add();
        vlActual.getVscConverterStations().forEach(b -> {
            assertTrue(b instanceof VscConverterStationAdapter);
            assertNotNull(b);
        });
        assertEquals(vlExpected.getVscConverterStationCount(), vlActual.getVscConverterStationCount());
        assertEquals(vlExpected.getVscConverterStationStream().count(), vlActual.getVscConverterStationStream().count());

        // Battery
        Battery battery = vlActual.newBattery()
                    .setId("BAT")
                    .setConnectableBus("busA")
                    .setBus("busA")
                    .setMaxP(9999.99)
                    .setMinP(-9999.99)
                    .setP0(15)
                    .setQ0(-15)
                .add();
        vlActual.getBatteries().forEach(b -> {
            assertTrue(b instanceof BatteryAdapter);
            assertNotNull(b);
        });
        assertEquals(vlExpected.getBatteryCount(), vlActual.getBatteryCount());
        assertEquals(vlExpected.getBatteryStream().count(), vlActual.getBatteryStream().count());

        // Generator
        vlActual.newGenerator()
                    .setId("GEN").setVoltageRegulatorOn(true)
                    .setConnectableBus("busA")
                    .setBus("busA")
                    .setMaxP(9999.99)
                    .setMinP(-9999.99)
                    .setTargetV(25.5)
                    .setTargetP(600.05)
                    .setTargetQ(300.5)
                    .setEnsureIdUnicity(true)
                .add();
        vlActual.getGenerators().forEach(g -> {
            assertTrue(g instanceof GeneratorAdapter);
            assertNotNull(g);
        });
        assertEquals(vlExpected.getGeneratorCount(), vlActual.getGeneratorCount());
        assertEquals(vlExpected.getGeneratorStream().count(), vlActual.getGeneratorStream().count());

        // Load
        vlActual.newLoad()
                    .setId("LOAD")
                    .setConnectableBus("busA")
                    .setBus("busA")
                    .setP0(9999.99)
                    .setQ0(-9999.99)
                    .setLoadType(LoadType.FICTITIOUS)
                    .setEnsureIdUnicity(true)
                .add();
        vlActual.getLoads().forEach(l -> {
            assertTrue(l instanceof LoadAdapter);
            assertNotNull(l);
        });
        assertEquals(vlExpected.getLoadCount(), vlActual.getLoadCount());
        assertEquals(vlExpected.getLoadStream().count(), vlActual.getLoadStream().count());

        // ShuntCompensator
        vlActual.newShuntCompensator()
                    .setId("SHUNT")
                    .setConnectableBus("busA")
                    .setBus("busA")
                    .setSectionCount(1)
                    .newLinearModel()
                        .setBPerSection(1e-5)
                        .setMaximumSectionCount(1)
                    .add()
                .add();
        vlActual.getShuntCompensators().forEach(s -> {
            assertTrue(s instanceof ShuntCompensatorAdapter);
            assertNotNull(s);
        });
        assertEquals(vlExpected.getShuntCompensatorCount(), vlActual.getShuntCompensatorCount());
        assertEquals(vlExpected.getShuntCompensatorStream().count(), vlActual.getShuntCompensatorStream().count());

        // StaticVarCompensator
        vlActual.newStaticVarCompensator()
                    .setId("svc1")
                    .setName("scv1")
                    .setConnectableBus("busA")
                    .setBus("busA")
                    .setBmin(0.0002)
                    .setBmax(0.0008)
                    .setRegulationMode(StaticVarCompensator.RegulationMode.VOLTAGE)
                    .setRegulatingTerminal(mergingView.getLoad("LOAD").getTerminal())
                    .setVoltageSetpoint(390.0)
                    .setReactivePowerSetpoint(1.0)
                    .setEnsureIdUnicity(false)
                .add();
        vlActual.getStaticVarCompensators().forEach(s -> {
            assertTrue(s instanceof StaticVarCompensatorAdapter);
            assertNotNull(s);
        });
        assertEquals(vlExpected.getStaticVarCompensatorCount(), vlActual.getStaticVarCompensatorCount());
        assertEquals(vlExpected.getStaticVarCompensatorStream().count(), vlActual.getStaticVarCompensatorStream().count());

        // LccConverterStation
        vlActual.newLccConverterStation()
                    .setId("C2")
                    .setName("Converter2")
                    .setConnectableBus("busA")
                    .setBus("busA")
                    .setLossFactor(0.011f)
                    .setPowerFactor(0.5f)
                    .setEnsureIdUnicity(false)
                .add();
        vlActual.getLccConverterStations().forEach(b -> {
            assertTrue(b instanceof LccConverterStationAdapter);
            assertNotNull(b);
        });
        assertEquals(vlExpected.getLccConverterStationCount(), vlActual.getLccConverterStationCount());
        assertEquals(vlExpected.getLccConverterStationStream().count(), vlActual.getLccConverterStationStream().count());

        // Line
        mergingView.newLine()
                .setId("l1")
                .setName("line1")
                .setR(1.0)
                .setX(2.0)
                .setG1(3.0)
                .setG2(4.0)
                .setB1(5.0)
                .setB2(6.0)
                .setVoltageLevel1("vl1")
                .setVoltageLevel2("vl2")
                .setBus1("busA")
                .setBus2("busB")
                .setConnectableBus1("busA")
                .setConnectableBus2("busB")
                .add();
        vlActual.getLines().forEach(b -> {
            assertTrue(b instanceof LineAdapter);
            assertNotNull(b);
        });
        assertEquals(vlExpected.getLineCount(), vlActual.getLineCount());
        assertEquals(vlExpected.getLineStream().count(), vlActual.getLineStream().count());
        assertEquals(Iterables.size(vlExpected.getLines()), Iterables.size(vlActual.getLines()));

        // TwoWindingsTransformer
        TwoWindingsTransformer twoWindingsTransformer = vlActual.getSubstation().get().newTwoWindingsTransformer()
                .setId("2wt")
                .setName("TwoWindingsTransformer")
                .setR(1.0)
                .setX(2.0)
                .setG(3.0)
                .setB(4.0)
                .setRatedU1(5.0)
                .setRatedU2(6.0)
                .setRatedS(7.0)
                .setVoltageLevel1("vl1")
                .setVoltageLevel2("vl2")
                .setConnectableBus1("busA")
                .setConnectableBus2("busB")
            .add();
        vlActual.getTwoWindingsTransformers().forEach(b -> {
            assertTrue(b instanceof TwoWindingsTransformerAdapter);
            assertNotNull(b);
        });
        assertEquals(vlExpected.getTwoWindingsTransformerCount(), vlActual.getTwoWindingsTransformerCount());
        assertEquals(vlExpected.getTwoWindingsTransformerStream().count(), vlActual.getTwoWindingsTransformerStream().count());
        assertEquals(Iterables.size(vlExpected.getTwoWindingsTransformers()), Iterables.size(vlActual.getTwoWindingsTransformers()));

        // ThreeWindingsTransformer
        ThreeWindingsTransformer threeWindingsTransformer = vlActual.getSubstation().get().newThreeWindingsTransformer()
                .setId("3wt")
                .setName("ThreeWindingsTransformer")
                .newLeg1()
                .setR(1.3)
                .setX(1.4)
                .setG(1.6)
                .setB(1.7)
                .setRatedU(1.1)
                .setRatedS(1.2)
                .setVoltageLevel("vl1")
                .setBus("busA")
                .add()
                .newLeg2()
                .setR(2.03)
                .setX(2.04)
                .setG(0.0)
                .setB(0.0)
                .setRatedU(2.05)
                .setRatedS(2.06)
                .setVoltageLevel("vl2")
                .setBus("busB")
                .add()
                .newLeg3()
                .setR(3.3)
                .setX(3.4)
                .setG(0.0)
                .setB(0.0)
                .setRatedU(3.5)
                .setRatedS(3.6)
                .setVoltageLevel("vl2")
                .setBus("busB")
                .add()
                .add();
        vlActual.getThreeWindingsTransformers().forEach(b -> {
            assertTrue(b instanceof ThreeWindingsTransformerAdapter);
            assertNotNull(b);
        });
        assertEquals(vlExpected.getThreeWindingsTransformerCount(), vlActual.getThreeWindingsTransformerCount());
        assertEquals(vlExpected.getThreeWindingsTransformerStream().count(), vlActual.getThreeWindingsTransformerStream().count());
        assertEquals(Iterables.size(vlExpected.getThreeWindingsTransformers()), Iterables.size(vlActual.getThreeWindingsTransformers()));

        // Switch
        vlActual.getSwitches().forEach(s -> {
            assertTrue(s instanceof SwitchAdapter);
            assertNotNull(s);
        });
        assertEquals(vlExpected.getSwitchCount(), vlActual.getSwitchCount());

        // DanglingLine
        DanglingLine dl = vlActual.newDanglingLine()
                .setId("DLL1")
                .setName("DLL1")
                .setR(1)
                .setX(2)
                .setG(3)
                .setB(4)
                .setP0(5)
                .setQ0(6)
                .setUcteXnodeCode("code")
                .setBus("busA")
                .setConnectableBus("busA")
            .add();
        vlActual.getDanglingLines().forEach(s -> {
            assertTrue(s instanceof DanglingLineAdapter);
            assertNotNull(s);
        });
        assertEquals(vlExpected.getDanglingLineCount(), vlActual.getDanglingLineCount());
        assertEquals(vlExpected.getDanglingLineStream().count(), vlActual.getDanglingLineStream().count());

        // Topology : Bus kind
        assertEquals(vlExpected.getTopologyKind(), vlActual.getTopologyKind());
        TopologyVisitor visitor = mock(TopologyVisitor.class);
        vlActual.visitEquipments(visitor);
        verify(visitor, times(1)).visitBattery(any(Battery.class));
        verify(visitor, times(1)).visitGenerator(any(Generator.class));
        verify(visitor, times(1)).visitLoad(any(Load.class));
        verify(visitor, times(1)).visitShuntCompensator(any(ShuntCompensator.class));
        verify(visitor, times(1)).visitStaticVarCompensator(any(StaticVarCompensator.class));
        verify(visitor, times(2)).visitHvdcConverterStation(any(HvdcConverterStation.class));
        verify(visitor, times(1)).visitDanglingLine(any(DanglingLine.class));

        TestUtil.notImplemented(() -> vlActual.printTopology());
        TestUtil.notImplemented(() -> vlActual.printTopology(System.out, mock(ShortIdDictionary.class)));
        TestUtil.notImplemented(() -> {
            try {
                vlActual.exportTopology(mock(Writer.class), new Random(0L));
                vlActual.exportTopology(mock(Writer.class));
            } catch (IOException e) {
                // Ignored
            }
        });

        assertSame(battery, vlActual.getConnectable("BAT", Battery.class));
        assertEquals(1, Iterables.size(vlActual.getConnectables(Battery.class)));
        assertSame(dl, vlActual.getConnectable("DLL1", DanglingLine.class));
        assertEquals(1, Iterables.size(vlActual.getConnectables(DanglingLine.class)));
        assertEquals(1, Iterables.size(vlActual.getConnectables(Line.class)));
        assertEquals(1, Iterables.size(vlActual.getConnectables(TwoWindingsTransformer.class)));
        assertEquals(1, Iterables.size(vlActual.getConnectables(ThreeWindingsTransformer.class)));
        assertEquals(11, Iterables.size(vlActual.getConnectables()));
        assertEquals(11, vlActual.getConnectableStream().count());

        MergingView cgm = MergingViewFactory.createCGM(null);
        VoltageLevel vl1 = cgm.getVoltageLevel("VL1");
        VoltageLevel vl2 = cgm.getVoltageLevel("VL2");

        assertNull(cgm.getDanglingLine("DL1"));
        assertNull(vl1.getConnectable("DL1", DanglingLine.class));
        assertEquals(0, Iterables.size(vl1.getConnectables(DanglingLine.class)));

        assertNull(cgm.getDanglingLine("DL2"));
        assertNull(vl2.getConnectable("DL2", DanglingLine.class));
        assertEquals(0, Iterables.size(vl2.getConnectables(DanglingLine.class)));

        assertNotNull(cgm.getLine("DL1 + DL2"));
        assertEquals(1, Iterables.size(vl1.getConnectables(Line.class)));
        assertEquals(1, vl1.getConnectableCount(Line.class));
        assertNotNull(vl1.getConnectable("DL1 + DL2", Line.class));
        assertEquals(1, Iterables.size(vl2.getConnectables(Line.class)));
        assertEquals(1, vl2.getConnectableCount(Line.class));
        assertNotNull(vl2.getConnectable("DL1 + DL2", Line.class));
    }

    @Test
    public void busViewTests() {
        MergingView cgm = MergingViewFactory.createCGM(null);

        // Buses in VL.BusView, Terminal.BusView and Network.BusView are shared
        busViewTests(cgm, "VL1", "VL1_1", "LOAD1");
        // FIXME(mathbagu): In Bus/Breaker voltage level, the definition of a bus in the BusView is: branchCount >= 1. The DL are not considered as branch, bus as feeder
        // busViewTests(cgm, "VL2", "VL2_1", "LOAD2");
    }

    private void busViewTests(MergingView cgm, String voltageLevelId, String busId, String loadId) {
        VoltageLevel vl = cgm.getVoltageLevel(voltageLevelId);
        assertEquals(1, Iterables.size(vl.getBusView().getBuses()));
        assertEquals(1, vl.getBusView().getBusStream().count());
        Bus bus = vl.getBusView().getBus(busId);
        assertNotNull(bus);
        Bus cgmBus = cgm.getBusView().getBus(busId);
        assertNotNull(cgmBus);
        Bus terminalBus = cgm.getLoad(loadId).getTerminal().getBusView().getBus();
        assertNotNull(terminalBus);
        assertSame(bus, cgmBus);
        assertSame(bus, terminalBus);
    }

    @Test
    public void busBreakerViewTests() {
        final VoltageLevel voltageLevelBB = mergingView.getVoltageLevel("vl1");
        final VoltageLevel.BusBreakerView bbv = voltageLevelBB.getBusBreakerView();
        assertTrue(bbv instanceof BusBreakerVoltageLevelAdapter.BusBreakerViewAdapter);

        final String switchId = "BBV_SW1";
        final String busId1 = "BBV_B1";
        final String busId2 = "BBV_B2";

        final Bus busB1 = voltageLevelBB.getBusBreakerView().newBus()
                    .setId(busId1)
                    .setName(busId1)
                    .setEnsureIdUnicity(false)
                .add();
        final Bus busB2 = voltageLevelBB.getBusBreakerView().newBus()
                    .setId(busId2)
                    .setName(busId2)
                    .setEnsureIdUnicity(false)
                .add();
        final Switch switchSW1 = bbv.newSwitch()
                                        .setId(switchId)
                                        .setName(switchId)
                                        .setEnsureIdUnicity(true)
                                        .setBus1(busId1)
                                        .setBus2(busId2)
                                        .setOpen(true)
                                        .setFictitious(true)
                                    .add();

        assertSame(busB1, bbv.getBus(busId1));
        bbv.getBuses().forEach(s -> {
            assertTrue(s instanceof BusAdapter);
            assertNotNull(s);
        });
        assertEquals(3, bbv.getBusStream().count());

        assertSame(switchSW1, bbv.getSwitch(switchId));
        bbv.getSwitches().forEach(s -> {
            assertTrue(s instanceof SwitchAdapter);
            assertNotNull(s);
        });
        assertEquals(bbv.getSwitchCount(), bbv.getSwitchStream().count());

        assertSame(busB1, bbv.getBus1(switchId));
        assertSame(busB2, bbv.getBus2(switchId));

        // Buses in VL.BusView, Terminal.BusView and Network.BusView are shared
        MergingView cgm = MergingViewFactory.createCGM(null);
        busBreakerViewTests(cgm, "VL1", 1, "VL1_1", "LOAD1");
        busBreakerViewTests(cgm, "VL2", 3, "BUS3_2", "LOAD2");

        // Not implemented
        TestUtil.notImplemented(() -> bbv.removeBus(""));
        TestUtil.notImplemented(bbv::removeAllBuses);
        TestUtil.notImplemented(() -> bbv.removeSwitch(""));
        TestUtil.notImplemented(bbv::removeAllSwitches);
    }

    private void busBreakerViewTests(MergingView cgm, String voltageLevelId, int busCount, String busId, String loadId) {
        VoltageLevel vl = cgm.getVoltageLevel(voltageLevelId);
        assertEquals(busCount, Iterables.size(vl.getBusBreakerView().getBuses()));
        assertEquals(busCount, vl.getBusBreakerView().getBusStream().count());
        Bus bus = vl.getBusBreakerView().getBus(busId);
        assertNotNull(bus);
        Bus cgmBus = cgm.getBusBreakerView().getBus(busId);
        assertNotNull(cgmBus);
        Bus terminalBus = cgm.getLoad(loadId).getTerminal().getBusBreakerView().getBus();
        assertNotNull(terminalBus);
        assertSame(bus, cgmBus);
        assertSame(bus, terminalBus);
    }

    @Test
    public void nodeBreakerViewTests() {
        // adder
        final VoltageLevel voltageLevelNB = mergingView.getSubstation("sub").newVoltageLevel()
                                                                                    .setTopologyKind(TopologyKind.NODE_BREAKER)
                                                                                    .setTopologyKind(TopologyKind.NODE_BREAKER.name())
                                                                                    .setId("nbVL")
                                                                                    .setName("nbVL_name")
                                                                                    .setNominalV(200.0)
                                                                                    .setLowVoltageLimit(100.0)
                                                                                    .setHighVoltageLimit(200.0)
                                                                                    .setEnsureIdUnicity(false)
                                                                                 .add();

        final VoltageLevel.NodeBreakerView nbv = voltageLevelNB.getNodeBreakerView();
        assertTrue(nbv instanceof NodeBreakerVoltageLevelAdapter.NodeBreakerViewAdapter);

        nbv.newInternalConnection()
               .setNode1(0)
               .setNode2(1)
            .add();
        nbv.getInternalConnections().forEach(Assert::assertNotNull);
        assertEquals(nbv.getInternalConnectionCount(), nbv.getInternalConnectionStream().count());
        assertEquals(Collections.singletonList(0), nbv.getNodesInternalConnectedTo(1));
        assertEquals(Collections.singletonList(1), nbv.getNodeInternalConnectedToStream(0).boxed().collect(Collectors.toList()));

        final Switch switchSW1 = nbv.newSwitch()
                                        .setId("NBV_SW1")
                                        .setName("NBV_SW1")
                                        .setEnsureIdUnicity(true)
                                        .setNode1(0)
                                        .setNode2(1)
                                        .setKind(SwitchKind.LOAD_BREAK_SWITCH)
                                        .setKind(SwitchKind.LOAD_BREAK_SWITCH.name())
                                        .setOpen(true)
                                        .setRetained(true)
                                        .setFictitious(true)
                                    .add();

        final Switch breakerBK1 = nbv.newBreaker()
                                         .setId("NBV_BK1")
                                         .setName("NBV_BK1")
                                         .setEnsureIdUnicity(true)
                                         .setNode1(0)
                                         .setNode2(1)
                                         .setOpen(true)
                                         .setRetained(true)
                                         .setFictitious(true)
                                      .add();

        final Switch breakerDIS1 = nbv.newDisconnector()
                                         .setId("NBV_DIS1")
                                         .setName("NBV_DIS1")
                                         .setEnsureIdUnicity(true)
                                         .setNode1(0)
                                         .setNode2(1)
                                         .setOpen(true)
                                         .setRetained(true)
                                         .setFictitious(true)
                                      .add();

        assertEquals(0, nbv.getNode1("NBV_SW1"));
        assertEquals(1, nbv.getNode2("NBV_SW1"));
        assertSame(nbv.getTerminal(0), nbv.getTerminal1("NBV_SW1"));
        assertSame(nbv.getTerminal(1), nbv.getTerminal2("NBV_SW1"));

        assertSame(switchSW1, nbv.getSwitch("NBV_SW1"));
        assertSame(breakerBK1, nbv.getSwitch("NBV_BK1"));
        assertSame(breakerDIS1, nbv.getSwitch("NBV_DIS1"));
        nbv.getSwitches().forEach(sw -> {
            if (Objects.nonNull(sw)) {
                assertTrue(sw instanceof SwitchAdapter);
            }
        });

        List<String> expectedSwitches = Arrays.asList("NBV_SW1", "NBV_BK1", "NBV_DIS1");
        assertEquals(expectedSwitches, nbv.getSwitchStream(0).map(Identifiable::getId).collect(Collectors.toList()));

        int i = 0;
        for (Switch aSwitch : nbv.getSwitches(1)) {
            assertEquals(expectedSwitches.get(i++), aSwitch.getId());
        }

        assertEquals(nbv.getSwitchCount(), nbv.getSwitchStream().count());

        final BusbarSection sjb1 = nbv.newBusbarSection()
                                          .setId("NBV_SJB1")
                                          .setName("NBV_SJB1")
                                          .setNode(0)
                                      .add();
        assertSame(sjb1, nbv.getBusbarSection("NBV_SJB1"));
        nbv.getBusbarSections().forEach(Assert::assertNotNull);
        assertEquals(nbv.getBusbarSectionCount(), nbv.getBusbarSectionStream().count());

        // Not implemented
        TestUtil.notImplemented(() -> nbv.removeSwitch(""));
        TestUtil.notImplemented(() -> nbv.traverse(0, null));
    }
}
