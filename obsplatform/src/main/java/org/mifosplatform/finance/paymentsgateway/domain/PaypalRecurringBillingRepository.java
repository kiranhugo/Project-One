package org.mifosplatform.finance.paymentsgateway.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaypalRecurringBillingRepository extends
		JpaRepository<PaypalRecurringBilling, Long>,
		JpaSpecificationExecutor<PaypalRecurringBilling> {

	@Query("from PaypalRecurringBilling paypalRecurringBilling where paypalRecurringBilling.subscriberId =:subscriberId")
	PaypalRecurringBilling findOneBySubscriberId(@Param("subscriberId") String subscriberId);

}
