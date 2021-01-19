/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.psse.model.pf.io;

import com.powsybl.psse.model.io.AbstractRecordGroup;
import com.powsybl.psse.model.pf.PsseFixedShunt;

import static com.powsybl.psse.model.PsseVersion.Major.V33;
import static com.powsybl.psse.model.PsseVersion.Major.V35;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 * @author José Antonio Marqués <marquesja at aia.es>
 */
class FixedBusShuntData extends AbstractRecordGroup<PsseFixedShunt> {

    public FixedBusShuntData() {
        super(PowerFlowRecordGroup.FIXED_BUS_SHUNT);
        withFieldNames(V33, "i", "id", "status", "gl", "bl");
        withFieldNames(V35, "ibus", "shntid", "stat", "gl", "bl");
        withQuotedFields("shntid", "id");
    }

    @Override
    public Class<PsseFixedShunt> psseTypeClass() {
        return PsseFixedShunt.class;
    }
}