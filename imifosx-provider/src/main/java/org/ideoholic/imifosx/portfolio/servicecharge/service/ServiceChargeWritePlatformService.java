/**
 * 
 */
package org.ideoholic.imifosx.portfolio.servicecharge.service;

import org.ideoholic.imifosx.portfolio.servicecharge.data.ServiceChargeData;

/**
 * @author Musaib_2
 *
 */
public interface ServiceChargeWritePlatformService {
	
	ServiceChargeData createCharge(ServiceChargeData serviceChargeData);

	ServiceChargeData updateCharge(ServiceChargeData serviceChargeData);

	ServiceChargeData deleteCharge(ServiceChargeData serviceChargeData);
}
