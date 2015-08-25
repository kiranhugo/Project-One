package org.mifosplatform.portfolio.allocation.service;

import java.util.List;

import org.mifosplatform.logistics.onetimesale.data.AllocationDetailsData;

public interface AllocationReadPlatformService {

	List<AllocationDetailsData> getTheHardwareItemDetails(Long orderId,String serialNumber);

	List<AllocationDetailsData> retrieveHardWareDetailsByItemCode(Long clientId, String itemCode);

	List<String> retrieveHardWareDetails(Long clientId);

	AllocationDetailsData getDisconnectedHardwareItemDetails(Long orderId,Long clientId);

	List<AllocationDetailsData> retrieveHardWareDetailsByServiceMap(Long clientId, Long serviceId);

	AllocationDetailsData getDisconnectedOrderHardwareDetails(Long orderId,Long serviceId, Long clientId);


}
