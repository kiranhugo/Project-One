package org.mifosplatform.portfolio.property.service;

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;

public interface PropertyWriteplatformService {

	CommandProcessingResult createProperty(JsonCommand command);

}