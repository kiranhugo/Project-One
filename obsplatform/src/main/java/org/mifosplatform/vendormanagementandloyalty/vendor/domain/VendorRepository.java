package org.mifosplatform.vendormanagementandloyalty.vendor.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VendorRepository  extends
JpaRepository<Vendor, Long>,
JpaSpecificationExecutor<Vendor>{

}

