package org.mifosplatform.finance.depositandrefund.handler;
import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.finance.depositandrefund.service.RefundWritePlatformService;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateRefundAmountCommandHandler implements NewCommandSourceHandler {

	private final RefundWritePlatformService writePlatformService;

	@Autowired
	public CreateRefundAmountCommandHandler(
			final RefundWritePlatformService writePlatformService) {
		this.writePlatformService = writePlatformService;
	}

	@Transactional
	@Override
	public CommandProcessingResult processCommand(final JsonCommand command) {

		return this.writePlatformService.createRefund(command,command.entityId());
	}
	
	
}

