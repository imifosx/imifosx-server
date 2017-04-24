package org.ideoholic.imifosx.portfolio.servicecharge.constants;

import java.util.HashMap;
import java.util.Map;

public enum GLExpenseTagsForServiceCharge {

	MOBILIZATION(1, "Mobilization"),
	SERVICING(2, "Servicing"),
	INVESTMENT(3, "Investment"),
    OVERHEADS(4, "Overheads"),
    PROVISIONS(5, "Provisions"),
    BFSERVICING(5, "BF-Servicing");

	private final Integer value;
	private final String code;

	private GLExpenseTagsForServiceCharge(final Integer value, final String code) {
		this.value = value;
		this.code = code;
	}

	public Integer getValue() {
		return this.value;
	}

	public String getCode() {
		return this.code;
	}

	private static final Map<Integer, GLExpenseTagsForServiceCharge> intToEnumMap = new HashMap<>();
	private static int minValue;
	private static int maxValue;
	static {
		int i = 0;
		for (final GLExpenseTagsForServiceCharge type : GLExpenseTagsForServiceCharge
				.values()) {
			if (i == 0) {
				minValue = type.value;
			}
			intToEnumMap.put(type.value, type);
			if (minValue >= type.value) {
				minValue = type.value;
			}
			if (maxValue < type.value) {
				maxValue = type.value;
			}
			i = i + 1;
		}
	}

	public static GLExpenseTagsForServiceCharge fromInt(final int i) {
		final GLExpenseTagsForServiceCharge type = intToEnumMap.get(Integer
				.valueOf(i));
		return type;
	}

	@Override
	public String toString() {
		return name().toString();
	}

}
