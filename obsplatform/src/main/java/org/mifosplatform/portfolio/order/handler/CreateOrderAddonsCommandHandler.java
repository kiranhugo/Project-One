/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.order.handler;

import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.portfolio.order.service.OrderAddOnsWritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CreateOrderAddonsCommandHandler implements NewCommandSourceHandler {

    private final OrderAddOnsWritePlatformService writePlatformService;

    @Autowired
    public CreateOrderAddonsCommandHandler(final OrderAddOnsWritePlatformService writePlatformService) {
        this.writePlatformService = writePlatformService;
    }

    
    @Override
    public CommandProcessingResult processCommand(final JsonCommand command) {

        return this.writePlatformService.createOrderAddons(command,command.entityId());
    }
}