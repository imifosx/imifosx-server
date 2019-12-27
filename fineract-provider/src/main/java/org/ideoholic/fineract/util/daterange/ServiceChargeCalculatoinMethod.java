/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ideoholic.fineract.util.daterange;

import java.util.HashMap;
import java.util.Map;

public enum ServiceChargeCalculatoinMethod {
    MONTHLY(1, "MONTHLY", 12), QUARTERLY(2, "QUARTERLY", 4), YEARLY(3, "YEARLY", 1);

    private final int rangeCode;
    private final String systemEnvParamString;
    private final int scale;

    ServiceChargeCalculatoinMethod(final int rangeCode, final String envParamString, int scale) {
        this.rangeCode = rangeCode;
        this.systemEnvParamString = envParamString;
        this.scale = scale;
    }

    private static final Map<String, ServiceChargeCalculatoinMethod> values = new HashMap<>();
    static {
        for (final ServiceChargeCalculatoinMethod type : ServiceChargeCalculatoinMethod.values()) {
            values.put(type.systemEnvParamString, type);
        }
    }

    public static ServiceChargeCalculatoinMethod fromRangeCode(int rangeCode) {
        ServiceChargeCalculatoinMethod code = null;
        for (final ServiceChargeCalculatoinMethod type : ServiceChargeCalculatoinMethod.values()) {
            if (type.rangeCode == rangeCode) {
                code = type;
                break;
            }
        }
        return code;
    }

    public static ServiceChargeCalculatoinMethod fromString(String methodKey) {
        return values.get(methodKey);
    }

    public int getScale() {
        return this.scale;
    }

    public int getScaleForDailyCalculation() {
        int multiplier = 0, addendum = 5;
        switch (this.rangeCode) {
            case 1:
                multiplier = 30;
            break;
            case 2:
                multiplier = 90;
            break;
            case 3:
                multiplier = 360;
            break;
        }
        return (this.scale * multiplier) + addendum;
    }

}
