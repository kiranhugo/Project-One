package org.mifosplatform.portfolio.service.service;

import java.util.Collection;
import java.util.List;

import org.mifosplatform.infrastructure.core.data.EnumOptionData;
import org.mifosplatform.portfolio.plan.data.ServiceData;
import org.mifosplatform.portfolio.service.data.ServiceMasterData;
import org.mifosplatform.portfolio.service.data.ServiceMasterOptionsData;

public interface ServiceMasterReadPlatformService {
	

	List<ServiceData> retrieveAllServices(String serviceType);
	
	 Collection<ServiceMasterData> retrieveAllServiceMasterData() ;

	List<ServiceMasterOptionsData> retrieveServices();

	ServiceMasterOptionsData retrieveIndividualService(Long serviceId);

	List<EnumOptionData> retrieveServicesTypes();

	List<EnumOptionData> retrieveServiceUnitType();
}
