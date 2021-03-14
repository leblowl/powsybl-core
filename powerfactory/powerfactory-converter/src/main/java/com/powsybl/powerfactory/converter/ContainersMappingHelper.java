/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.powerfactory.converter;

import com.google.common.primitives.Ints;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.util.ContainersMapping;
import com.powsybl.powerfactory.model.DataObject;
import com.powsybl.powerfactory.model.Project;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
final class ContainersMappingHelper {

    private ContainersMappingHelper() {
    }

    private static final class Edge {

        private final DataObject obj1;

        private final DataObject obj2;

        private final DataObject obj3;

        private final boolean transformer;

        private final double r;

        private final double x;

        private Edge(DataObject obj1, DataObject obj2, DataObject obj3, boolean transformer, double r, double x) {
            this.obj1 = Objects.requireNonNull(obj1);
            this.obj2 = Objects.requireNonNull(obj2);
            this.obj3 = obj3;
            this.transformer = transformer;
            this.r = r;
            this.x = x;
        }

        private DataObject getObj1() {
            return obj1;
        }

        private DataObject getObj2() {
            return obj2;
        }

        public DataObject getObj3() {
            return obj3;
        }

        public boolean isTransformer() {
            return transformer;
        }

        public double getR() {
            return r;
        }

        public double getX() {
            return x;
        }
    }

    static ContainersMapping create(Project project, List<DataObject> elmTerms) {
        List<DataObject> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        Map<DataObject, List<DataObject>> branchesByCubicleId = new HashMap<>();
        for (DataObject elmTerm : elmTerms) {
            nodes.add(elmTerm);
            for (DataObject staCubic : elmTerm.getChildren("StaCubic")) {
                DataObject connectedObj = staCubic.findObjectAttributeValue("obj_id").orElse(null);
                if (connectedObj != null) {
                    if (connectedObj.getDataClassName().equals("ElmTr2")
                            || connectedObj.getDataClassName().equals("ElmTr3")
                            || connectedObj.getDataClassName().equals("ElmLne")
                            || connectedObj.getDataClassName().equals("ElmCoup")) {
                        nodes.add(staCubic);
                        edges.add(new Edge(elmTerm, staCubic, null, false, 0, 0));
                        branchesByCubicleId.computeIfAbsent(connectedObj, k -> new ArrayList<>())
                                .add(staCubic);
                    }
                }
            }
        }

        for (Map.Entry<DataObject, List<DataObject>> e : branchesByCubicleId.entrySet()) {
            DataObject connectedObj = e.getKey();
            List<DataObject> staCubics = e.getValue();
            if (staCubics.size() == 2) {
                switch (connectedObj.getDataClassName()) {
                    case "ElmTr2":
                        edges.add(new Edge(staCubics.get(0), staCubics.get(1), null, true, Double.MAX_VALUE, Double.MAX_VALUE));
                        break;
                    case "ElmLne":
                        float dline = connectedObj.getFloatAttributeValue("dline");
                        DataObject typLne = connectedObj.getObjectAttributeValue("typ_id");
                        float rline = typLne.getFloatAttributeValue("rline");
                        float xline = typLne.getFloatAttributeValue("xline");
                        double r = rline * dline;
                        double x = xline * dline;
                        edges.add(new Edge(staCubics.get(0), staCubics.get(1), null, false, r, x));
                        break;
                    case "ElmCoup":
                        edges.add(new Edge(staCubics.get(0), staCubics.get(1), null, false, 0, 0));
                        break;
                    default:
                        throw new AssertionError();
                }
            } else if (staCubics.size() == 3) {
                if (connectedObj.getDataClassName().equals("ElmTr3")) {
                    edges.add(new Edge(staCubics.get(0), staCubics.get(1), staCubics.get(2), true, Double.MAX_VALUE, Double.MAX_VALUE));
                } else {
                    throw new AssertionError();
                }
            } else {
                throw new PowsyblException(connectedObj.getName() + " should be connected at both sides");
            }
        }

        Function<Set<Integer>, String> busesToVoltageLevelId = new Function<Set<Integer>, String>() {

            private int noNameVoltageLevelCount = 0;

            @Override
            public String apply(Set<Integer> ids) {
                List<DataObject> objs = ids.stream()
                        .map(id -> project.getObjectById(id).orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                Set<DataObject> containers = objs.stream()
                        .map(obj -> {
                            String className = obj.getParent().getDataClassName();
                            if (className.equals("ElmSubstat") || className.equals("ElmTrfstat")) {
                                return obj.getParent();
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                String voltageLevelId = null;

                if (containers.size() == 1) {
                    float uknom = objs.stream()
                            .map(obj -> obj.findFloatAttributeValue("uknom").orElse(null))
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElseThrow(AssertionError::new);

                    DataObject container = containers.iterator().next();
                    float unom = container.getFloatAttributeValue("Unom");
                    if (uknom == unom) { // check terminal nominal voltage is consistent with container one
                        voltageLevelId = container.getName();
                    }
                }

                if (voltageLevelId == null) { // automatic naming
                    return "VL" + noNameVoltageLevelCount++;
                }

                return voltageLevelId;
            }
        };

        return ContainersMapping.create(nodes, edges,
            obj -> Ints.checkedCast(obj.getId()),
            edge -> Ints.checkedCast(edge.getObj1().getId()),
            edge -> Ints.checkedCast(edge.getObj2().getId()),
            edge -> edge.getObj3() != null ? Ints.checkedCast(edge.getObj3().getId()) : 0,
            Edge::getR,
            Edge::getX,
            Edge::isTransformer,
            busesToVoltageLevelId,
            value -> "S" + value);
    }
}