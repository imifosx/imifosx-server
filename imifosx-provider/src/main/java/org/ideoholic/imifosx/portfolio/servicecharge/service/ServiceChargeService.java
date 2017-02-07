package org.ideoholic.imifosx.portfolio.servicecharge.service;

import org.ideoholic.imifosx.infrastructure.core.service.Page;
import org.ideoholic.imifosx.infrastructure.core.service.SearchParameters;
import org.ideoholic.imifosx.portfolio.loanaccount.data.LoanAccountData;
import org.ideoholic.imifosx.portfolio.loanaccount.service.LoanReadPlatformServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceChargeService {

	
	
	private final LoanReadPlatformServiceImpl loanReadPlatformServiceImpl;
	
	@Autowired
	public ServiceChargeService(LoanReadPlatformServiceImpl loanReadPlatformServiceImpl) {
		this.loanReadPlatformServiceImpl = loanReadPlatformServiceImpl; 
	}
	
	public void getAllLoansData(){
		System.out.println("entered into getallloansdata");
		final SearchParameters searchParameters = SearchParameters.forLoans(null, null, 0, -1, null, null,
                null);
		Page<LoanAccountData> loanAccountData = null;
		try {
			loanAccountData = loanReadPlatformServiceImpl.retrieveAll(searchParameters);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Account number is "+loanAccountData.getTotalFilteredRecords());
	}
}
