package org.mifosplatform.portfolio.addons.service;

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;

public interface AddOnsWritePlatformService {

	CommandProcessingResult createAddons(JsonCommand command);

}
