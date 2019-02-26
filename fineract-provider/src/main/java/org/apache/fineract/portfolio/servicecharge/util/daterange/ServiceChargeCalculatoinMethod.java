package org.apache.fineract.portfolio.servicecharge.util.daterange;

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

}
