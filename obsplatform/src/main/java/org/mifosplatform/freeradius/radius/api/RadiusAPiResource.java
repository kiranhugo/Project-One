package org.mifosplatform.freeradius.radius.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.freeradius.radius.data.RadiusServiceData;
import org.mifosplatform.freeradius.radius.service.RadiusReadPlatformService;
import org.mifosplatform.infrastructure.core.api.ApiRequestParameterHelper;
import org.mifosplatform.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.mifosplatform.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.portfolio.plan.data.PlanData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author hugo
 * this api class used to create,update and delete diff radius details 
 */
@Path("/freeradius")
@Component
@Scope("singleton")
public class RadiusAPiResource {
	
	private static final Set<String> RESPONSE_DATA_PARAMETERS = new HashSet<String>(Arrays.asList("id", "planCode", "plan_description", "startDate","isHwReq",
            "endDate", "status", "service_code", "service_description","Period", "charge_code", "charge_description","servicedata","contractPeriod","provisionSystem",
            "service_type", "charge_type", "allowedtypes","selectedservice","bill_rule","billiingcycle","servicedata","services","statusname","planstatus","volumeTypes"));

	private final String resourceNameForPermissions = "RADIUS";
	private final PlatformSecurityContext context;
	 private final ApiRequestParameterHelper apiRequestParameterHelper;
	private final RadiusReadPlatformService radiusReadPlatformService;
	 private final DefaultToApiJsonSerializer<RadiusServiceData> toApiJsonSerializer;
	
	@Autowired
	public RadiusAPiResource(final PlatformSecurityContext context,final ApiRequestParameterHelper apiRequestParameterHelper,
			final PortfolioCommandSourceWritePlatformService commandSourceWritePlatformService,final RadiusReadPlatformService radiusReadPlatformService,
			final DefaultToApiJsonSerializer<RadiusServiceData> toApiJsonSerializer) {
		
		this.context = context;
		this.radiusReadPlatformService = radiusReadPlatformService;
		this.toApiJsonSerializer = toApiJsonSerializer;
		this.apiRequestParameterHelper = apiRequestParameterHelper;
	}

	/**
	 * @param uriInfo
	 * @return retrieved all nas details
	 */
	@GET
	@Path("nas")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveAllNasDetails(@Context final UriInfo uriInfo) {

		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		final String nasData = this.radiusReadPlatformService.retrieveAllNasDetails();
		return nasData;
	}
	
	
	@POST
	@Path("nas")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String createNas(final String apiRequestBodyAsJson) {

		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		final String nasData = this.radiusReadPlatformService.createNas(apiRequestBodyAsJson);
		return nasData;
	}
	
	@GET
	@Path("nas/{nasId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveNasDetail(@PathParam("nasId") final Long nasId) {

		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		final String radServiceData = this.radiusReadPlatformService.retrieveNasDetail(nasId);
		return radServiceData;
	}
	
	
	@DELETE
	@Path("nas/{nasId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String deleteNasDetail(@PathParam("nasId") final Long nasId) {

		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		final String radServiceData = this.radiusReadPlatformService.deleteNasDetail(nasId);
		return radServiceData;
	}
	
	@GET
	@Path("radservice")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveAllRadserviceDetails(@Context final UriInfo uriInfo,@QueryParam("attribute") final String attribute) {

		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		final String radServiceData = this.radiusReadPlatformService.retrieveAllRadServiceDetails(attribute);
		return radServiceData;
	}
	
	@GET
	@Path("radservice/{radServiceId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveRadserviceDetail(@PathParam("radServiceId") final Long radServiceId) {

		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		final String radServiceData = this.radiusReadPlatformService.retrieveRadServiceDetail(radServiceId);
		return radServiceData;
	}
	
	
	@POST
	@Path("radservice")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String createRadService(final String apiRequestBodyAsJson) {

		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		final String radServiceData = this.radiusReadPlatformService.createRadService(apiRequestBodyAsJson);
		return radServiceData;
	}
	
	@DELETE
	@Path("radservice/{radServiceId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String deleteRadService(@PathParam("radServiceId") final Long radServiceId) {

		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		final String radServiceData = this.radiusReadPlatformService.deleteRadService(radServiceId);
		return radServiceData;
	}
	
	@GET
	@Path("raduser2/template")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveRadserviceTemplateData(@Context final UriInfo uriInfo) {

		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());
		final List<RadiusServiceData> radServiceTemplateData = this.radiusReadPlatformService.retrieveRadServiceTemplateData();
		return this.toApiJsonSerializer.serialize(settings, radServiceTemplateData, RESPONSE_DATA_PARAMETERS);
	}



}
