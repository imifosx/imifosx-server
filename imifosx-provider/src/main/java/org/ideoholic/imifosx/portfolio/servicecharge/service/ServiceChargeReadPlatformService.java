/**
 * 
 */
package org.ideoholic.imifosx.portfolio.servicecharge.service;

import java.util.Collection;

import org.ideoholic.imifosx.portfolio.servicecharge.data.ServiceChargeData;

/**
 * @author Musaib_2
 *
 */
public interface ServiceChargeReadPlatformService {

    Collection<ServiceChargeData> retrieveAllCharges();

    ServiceChargeData retrieveCharge(Long chargeId);
    
}
