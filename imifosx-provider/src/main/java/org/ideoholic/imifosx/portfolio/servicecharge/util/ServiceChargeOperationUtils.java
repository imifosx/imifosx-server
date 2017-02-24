package org.ideoholic.imifosx.portfolio.servicecharge.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class ServiceChargeOperationUtils {

	public static BigDecimal divideAndMultiplyNonZeroValues(BigDecimal operand, BigDecimal divisor, BigDecimal multiplicand) {
		if (operand == null) {
			return BigDecimal.ONE;
		}
		if (divisor != null && !divisor.equals(BigDecimal.ZERO)) {
			operand = operand.divide(divisor, RoundingMode.HALF_UP);
		}
		if (multiplicand != null) {
			operand = operand.multiply(multiplicand);
		}
		return operand;
	}

	public static String convertMapToHTMLTable(Map<String, List<String>> map) {
		StringBuffer sb = new StringBuffer();
		sb.append("<table table style=\"width:100%\" border=5pt>");
		for (String key : map.keySet()) {
			sb.append("<tr>");
			sb.append("<td>");
			sb.append(key);
			sb.append("</td>");
			for (String element : map.get(key)) {
				sb.append("<td>");
				sb.append(element);
				sb.append("</td>");
			}
			sb.append("</tr>");

		}
		sb.append("</table>");
		return sb.toString();
	}

	public static String convertMapToHTMLTable(Map<String, List<BigDecimal>> map, StringBuffer appendHTML) {
		StringBuffer sb = new StringBuffer();
		if (null == appendHTML) {
			sb.append("<table table style=\"width:100%\" border=5pt>");
		}
		for (String key : map.keySet()) {
			sb.append("<tr>");
			sb.append("<td>");
			sb.append(key);
			sb.append("</td>");
			for (BigDecimal element : map.get(key)) {
				sb.append("<td>");
				if (element != null)
					sb.append(element.toPlainString());
				else
					sb.append(StringUtils.EMPTY);
				sb.append("</td>");
			}
			sb.append("</tr>");

		}
		sb.append("</table>");
		return sb.toString();
	}
}
