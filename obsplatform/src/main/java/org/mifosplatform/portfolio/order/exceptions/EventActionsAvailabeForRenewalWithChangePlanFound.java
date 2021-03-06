package org.mifosplatform.portfolio.order.exceptions;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;


public class EventActionsAvailabeForRenewalWithChangePlanFound extends AbstractPlatformDomainRuleException {

    public EventActionsAvailabeForRenewalWithChangePlanFound(Long clientId, Long orderId) {
        super("error.msg.change.plan.found", "Change Plan Already scheduled for this clientId "+clientId +" and orderId"+orderId,clientId,orderId);
    }
    
   
}
