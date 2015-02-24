package org.mifosplatform.workflow.eventaction.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.java.dev.obs.beesmart.AddExternalBeesmartMethod;

import org.codehaus.jettison.json.JSONObject;
import org.mifosplatform.cms.eventmaster.domain.EventMaster;
import org.mifosplatform.cms.eventmaster.domain.EventMasterRepository;
import org.mifosplatform.cms.eventorder.domain.EventOrder;
import org.mifosplatform.cms.eventorder.domain.EventOrderRepository;
import org.mifosplatform.cms.eventorder.domain.EventOrderdetials;
import org.mifosplatform.crm.ticketmaster.data.TicketMasterData;
import org.mifosplatform.crm.ticketmaster.domain.TicketMaster;
import org.mifosplatform.crm.ticketmaster.domain.TicketMasterRepository;
import org.mifosplatform.crm.ticketmaster.service.TicketMasterReadPlatformService;
import org.mifosplatform.finance.billingorder.api.BillingOrderApiResourse;
import org.mifosplatform.infrastructure.core.service.DateUtils;
import org.mifosplatform.organisation.message.domain.BillingMessage;
import org.mifosplatform.organisation.message.domain.BillingMessageRepository;
import org.mifosplatform.organisation.message.domain.BillingMessageTemplate;
import org.mifosplatform.organisation.message.domain.BillingMessageTemplateConstants;
import org.mifosplatform.organisation.message.domain.BillingMessageTemplateRepository;
import org.mifosplatform.organisation.message.exception.EmailNotFoundException;
import org.mifosplatform.portfolio.association.data.AssociationData;
import org.mifosplatform.portfolio.association.exception.HardwareDetailsNotFoundException;
import org.mifosplatform.portfolio.association.service.HardwareAssociationReadplatformService;
import org.mifosplatform.portfolio.client.domain.Client;
import org.mifosplatform.portfolio.client.domain.ClientRepository;
import org.mifosplatform.portfolio.contract.data.SubscriptionData;
import org.mifosplatform.portfolio.contract.service.ContractPeriodReadPlatformService;
import org.mifosplatform.portfolio.order.domain.Order;
import org.mifosplatform.portfolio.order.domain.OrderRepository;
import org.mifosplatform.provisioning.processrequest.domain.ProcessRequest;
import org.mifosplatform.provisioning.processrequest.domain.ProcessRequestDetails;
import org.mifosplatform.provisioning.processrequest.domain.ProcessRequestRepository;
import org.mifosplatform.provisioning.provisioning.api.ProvisioningApiConstants;
import org.mifosplatform.useradministration.data.AppUserData;
import org.mifosplatform.useradministration.service.AppUserReadPlatformService;
import org.mifosplatform.workflow.eventaction.data.ActionDetaislData;
import org.mifosplatform.workflow.eventaction.data.EventActionProcedureData;
import org.mifosplatform.workflow.eventaction.domain.EventAction;
import org.mifosplatform.workflow.eventaction.domain.EventActionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventActionWritePlatformServiceImpl implements ActiondetailsWritePlatformService{
	
	
	
	private final OrderRepository orderRepository;
	private final TicketMasterRepository repository;
	private final ClientRepository clientRepository;
	private final EventOrderRepository eventOrderRepository;
	private final EventMasterRepository eventMasterRepository;
	private final EventActionRepository eventActionRepository;
	private final BillingMessageRepository messageDataRepository;
	private final AppUserReadPlatformService readPlatformService;
	private final BillingOrderApiResourse billingOrderApiResourse;
	private final ProcessRequestRepository processRequestRepository;
	private final BillingMessageTemplateRepository messageTemplateRepository;
	private final TicketMasterReadPlatformService ticketMasterReadPlatformService ;
    private final ActionDetailsReadPlatformService actionDetailsReadPlatformService;	
    private final ContractPeriodReadPlatformService contractPeriodReadPlatformService;
    private final HardwareAssociationReadplatformService hardwareAssociationReadplatformService;


	@Autowired
	public EventActionWritePlatformServiceImpl(final ActionDetailsReadPlatformService actionDetailsReadPlatformService,final EventActionRepository eventActionRepository,
			final HardwareAssociationReadplatformService hardwareAssociationReadplatformService,final ContractPeriodReadPlatformService contractPeriodReadPlatformService,
			final OrderRepository orderRepository,final TicketMasterRepository repository,final ProcessRequestRepository processRequestRepository,
			final BillingOrderApiResourse billingOrderApiResourse,final BillingMessageRepository messageDataRepository,final ClientRepository clientRepository,
			final BillingMessageTemplateRepository messageTemplateRepository,final EventMasterRepository eventMasterRepository,final EventOrderRepository eventOrderRepository,
			final TicketMasterReadPlatformService ticketMasterReadPlatformService,final AppUserReadPlatformService readPlatformService)
	{
		this.repository=repository;
		this.orderRepository=orderRepository;
		this.clientRepository=clientRepository;
		this.readPlatformService=readPlatformService;
		this.eventOrderRepository=eventOrderRepository;
		this.eventActionRepository=eventActionRepository;
		this.eventMasterRepository=eventMasterRepository;
		this.messageDataRepository=messageDataRepository;
		this.billingOrderApiResourse=billingOrderApiResourse;
		this.processRequestRepository=processRequestRepository;
		this.messageTemplateRepository=messageTemplateRepository;
		this.ticketMasterReadPlatformService=ticketMasterReadPlatformService;
        this.actionDetailsReadPlatformService=actionDetailsReadPlatformService;
        this.contractPeriodReadPlatformService=contractPeriodReadPlatformService;
        this.hardwareAssociationReadplatformService=hardwareAssociationReadplatformService;
	}
	
	
	
	@Override
	public String AddNewActions(List<ActionDetaislData> actionDetaislDatas,final Long clientId,final String resourceId,String ticketURL) {
    
  try{
    	
	if(actionDetaislDatas!=null){
	   EventAction eventAction=null;
			
	   	for(ActionDetaislData detailsData:actionDetaislDatas){
		      EventActionProcedureData actionProcedureData=this.actionDetailsReadPlatformService.checkCustomeValidationForEvents(clientId, detailsData.getEventName(),detailsData.getActionName(),resourceId);
			  JSONObject jsonObject=new JSONObject();

			  	if(actionProcedureData.isCheck()){
				    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
				    List<SubscriptionData> subscriptionDatas=this.contractPeriodReadPlatformService.retrieveSubscriptionDatabyContractType("Month(s)",1);
				   
				    switch(detailsData.getActionName()){
				  
				    case EventActionConstants.ACTION_SEND_EMAIL :
				    	

				    	   
				          TicketMasterData data = this.ticketMasterReadPlatformService.retrieveTicket(clientId,new Long(resourceId));
				          TicketMaster ticketMaster=this.repository.findOne(new Long(resourceId));
				          AppUserData user = this.readPlatformService.retrieveUser(new Long(data.getUserId()));
				          BillingMessageTemplate billingMessageTemplate = this.messageTemplateRepository.findByTemplateDescription(BillingMessageTemplateConstants.MESSAGE_TEMPLATE_TICKET_TEMPLATE);
				          String value=ticketURL+""+resourceId;
				          String removeUrl="<br/><b>URL : </b>"+"<a href="+value+">View Ticket</a>";
				         // removeUrl.replaceAll("(PARAMURL)", ticketURL+""+resourceId); 	
				        	if(detailsData.getEventName().equalsIgnoreCase(EventActionConstants.EVENT_CREATE_TICKET)){
				        	  	if(!user.getEmail().isEmpty()){
				        	  		BillingMessage billingMessage = new BillingMessage("CREATE TICKET", data.getProblemDescription()+"<br/>"
				        	  	    +ticketMaster.getDescription()+"\n"+removeUrl, "", user.getEmail(), user.getEmail(),
									 "Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
									 BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
				        	  		this.messageDataRepository.save(billingMessage);
				        	  	}else{
				        	  	   if(actionProcedureData.getEmailId().isEmpty()){
				        	  		   
				        	  			throw new EmailNotFoundException(new Long(data.getUserId()));
				        	  		}else{
				        	  			
				        	  			BillingMessage billingMessage = new BillingMessage("CREATE TICKET", data.getProblemDescription()+"<br/>"
				        	  		    +ticketMaster.getDescription()+"\n"+removeUrl, "", actionProcedureData.getEmailId(), actionProcedureData.getEmailId(),
										"Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
										BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
				        	  			this.messageDataRepository.save(billingMessage);
				        	  		}
				        	  	}
				        	
				        	}else if(detailsData.getEventName().equalsIgnoreCase(EventActionConstants.EVENT_EDIT_TICKET)){
				        	  		
				        	    if(!user.getEmail().isEmpty()){
				        	    	
				        	  		BillingMessage billingMessage = new BillingMessage("ADD COMMENT", data.getProblemDescription()+"<br/>"
				        	        +ticketMaster.getDescription()+"<br/>"+"COMMENT: "+data.getLastComment()+"<br/>"+removeUrl, "", user.getEmail(), user.getEmail(),
									"Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
									BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
				        	  		this.messageDataRepository.save(billingMessage);
				        	  	
				        	    }else{
				        	  		
				        	  		if(actionProcedureData.getEmailId().isEmpty()){
					        	  			throw new EmailNotFoundException(new Long(data.getUserId()));	
					        	  	}else{
					        	  		BillingMessage billingMessage = new BillingMessage("ADD COMMENT", data.getProblemDescription()+"<br/>"
					        	  	     +ticketMaster.getDescription()+"<br/>"+"COMMENT: \t"+data.getLastComment()+"<br/>"+removeUrl, "", actionProcedureData.getEmailId(),
					        	  	     actionProcedureData.getEmailId(),"Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
					        	  	     BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
						        	  		this.messageDataRepository.save(billingMessage);
					        	  	}
				        	  	}
				        	
				        	}else if(detailsData.getEventName().equalsIgnoreCase(EventActionConstants.EVENT_CLOSE_TICKET)){
				        		
				        	  	if(!user.getEmail().isEmpty()){
				        	  			BillingMessage billingMessage = new BillingMessage("CLOSED TICKET", data.getProblemDescription()+"<br/>"
				        	  			+ticketMaster.getDescription()+"<br/>"+"RESOLUTION: \t"+ticketMaster.getResolutionDescription()+"<br/>"+removeUrl, "", user.getEmail(), user.getEmail(),
										"Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
										BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
				        	  			this.messageDataRepository.save(billingMessage);
				        	  	}else{
				        	  		if(actionProcedureData.getEmailId().isEmpty()){
					        	  		throw new EmailNotFoundException(new Long(data.getUserId()));	
					        	  	}else{
					        	  		     BillingMessage billingMessage = new BillingMessage("CLOSED TICKET", data.getProblemDescription()+"<br/>"
					        	  		    +ticketMaster.getDescription()+"<br/>"+"RESOLUTION: \t"+ticketMaster.getResolutionDescription()+"<br/>"+removeUrl, "", actionProcedureData.getEmailId(),
					        	  	         actionProcedureData.getEmailId(),"Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
					        	  	       BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
						        	        this.messageDataRepository.save(billingMessage);
					        	  }
				        	  	}
				        	  }
				      
				       break;
				       
				    case EventActionConstants.ACTION_ACTIVE : 
				    	
				          AssociationData associationData=this.hardwareAssociationReadplatformService.retrieveSingleDetails(actionProcedureData.getOrderId());
				          		if(associationData ==null){
				          			throw new HardwareDetailsNotFoundException(actionProcedureData.getOrderId().toString());
				          		}
				          		jsonObject.put("renewalPeriod",subscriptionDatas.get(0).getId());	
				          		jsonObject.put("description","Order Renewal By Scheduler");
				          		eventAction=new EventAction(DateUtils.getDateOfTenant(), "CREATE", "PAYMENT",EventActionConstants.ACTION_RENEWAL.toString(),"/orders/renewal", 
			        			 Long.parseLong(resourceId), jsonObject.toString(),actionProcedureData.getOrderId(),clientId);
				          		this.eventActionRepository.save(eventAction);
				         break; 		

				    case EventActionConstants.ACTION_NEW :

				    	jsonObject.put("billAlign","false");
				    	jsonObject.put("contractPeriod",subscriptionDatas.get(0).getId());
				    	jsonObject.put("dateFormat","dd MMMM yyyy");
                        jsonObject.put("locale","en");
                        jsonObject.put("paytermCode","Monthly");
                        jsonObject.put("planCode",actionProcedureData.getPlanId());
                        jsonObject.put("isNewplan","true");
                        jsonObject.put("start_date",dateFormat.format(DateUtils.getDateOfTenant()));
                        eventAction=new EventAction(DateUtils.getDateOfTenant(), "CREATE", "PAYMENT",actionProcedureData.getActionName(),"/orders/"+clientId, 
                        		Long.parseLong(resourceId), jsonObject.toString(),null,clientId);
			        	this.eventActionRepository.save(eventAction);
			        	   
				    	break;
				    	
				    case EventActionConstants.ACTION_DISCONNECT :

			        	   eventAction=new EventAction(DateUtils.getDateOfTenant(), "CREATE", "PAYMENT",EventActionConstants.ACTION_ACTIVE.toString(),"/orders/reconnect/"+clientId, 
			        	   Long.parseLong(resourceId), jsonObject.toString(),actionProcedureData.getOrderId(),clientId);
			        	   this.eventActionRepository.save(eventAction);

			        	   break; 
			        	   
				    case EventActionConstants.ACTION_INVOICE : 
				    	  
			        	  jsonObject.put("dateFormat","dd MMMM yyyy");
			        	  jsonObject.put("locale","en");
			        	  jsonObject.put("systemDate",dateFormat.format(DateUtils.getDateOfTenant()));
			        	  	if(detailsData.IsSynchronous().equalsIgnoreCase("N")){
			        	  		eventAction=new EventAction(DateUtils.getDateOfTenant(), "CREATE",EventActionConstants.EVENT_ACTIVE_ORDER.toString(),
			        	  		EventActionConstants.ACTION_INVOICE.toString(),"/billingorder/"+clientId,Long.parseLong(resourceId),
			        	  		jsonObject.toString(),Long.parseLong(resourceId),clientId);
					        	this.eventActionRepository.save(eventAction);
			        	  	
			        	  	}else{
			            	 
			        	  		Order order=this.orderRepository.findOne(new Long(resourceId));
			        	  		jsonObject.put("dateFormat","dd MMMM yyyy");
			        	  		jsonObject.put("locale","en");
			        	  		jsonObject.put("systemDate",dateFormat.format(order.getStartDate()));
			        	  		this.billingOrderApiResourse.retrieveBillingProducts(order.getClientId(),jsonObject.toString());
			        	  	}
			        	  break;
			        	  
					default:
						break;
				    }
			  	}
				    
				    switch(detailsData.getActionName()){
				 
				    case EventActionConstants.ACTION_PROVISION_IT : 

				    	Client client=this.clientRepository.findOne(clientId);
			  	    	EventOrder eventOrder=this.eventOrderRepository.findOne(Long.valueOf(resourceId));
			  	    	EventMaster eventMaster=this.eventMasterRepository.findOne(eventOrder.getEventId());
			  	    	String response= AddExternalBeesmartMethod.addVodPackage(client.getOffice().getExternalId().toString(),client.getAccountNo(),
			  	    			eventMaster.getEventName());

			  	    	ProcessRequest processRequest=new ProcessRequest(Long.valueOf(0), eventOrder.getClientId(),eventOrder.getId(),ProvisioningApiConstants.PROV_BEENIUS,
								ProvisioningApiConstants.REQUEST_ACTIVATION_VOD,'Y','Y');
						List<EventOrderdetials> eventDetails=eventOrder.getEventOrderdetials();
						//EventMaster eventMaster=this.eventMasterRepository.findOne(eventOrder.getEventId());
						//JSONObject jsonObject=new JSONObject();
						jsonObject.put("officeUid",client.getOffice().getExternalId());
						jsonObject.put("subscriberUid",client.getAccountNo());
						jsonObject.put("vodUid",eventMaster.getEventName());
								
							for(EventOrderdetials details:eventDetails){
								ProcessRequestDetails processRequestDetails=new ProcessRequestDetails(details.getId(),details.getEventDetails().getId(),jsonObject.toString(),
										response,null,eventMaster.getEventStartDate(), eventMaster.getEventEndDate(),DateUtils.getDateOfTenant(),DateUtils.getDateOfTenant(),'N',
										ProvisioningApiConstants.REQUEST_ACTIVATION_VOD,null);
								processRequest.add(processRequestDetails);
							}
						this.processRequestRepository.save(processRequest);

						break;
						
				    case EventActionConstants.ACTION_SEND_PROVISION : 

				    	eventAction=new EventAction(DateUtils.getDateOfTenant(), "CLOSE", "Client",EventActionConstants.ACTION_SEND_PROVISION.toString(),"/processrequest/"+clientId, 
				    	Long.parseLong(resourceId),jsonObject.toString(),clientId,clientId);
				    	this.eventActionRepository.save(eventAction);
				  	
			        	break;
			        	
				    case EventActionConstants.ACTION_ACTIVE_LIVE_EVENT :
				    	 eventMaster=this.eventMasterRepository.findOne(Long.valueOf(resourceId));
				    	 
				    	 eventAction=new EventAction(eventMaster.getEventStartDate(),"Create","Live Event",EventActionConstants.ACTION_ACTIVE_LIVE_EVENT.toString(),
				    			 "/eventmaster",Long.parseLong(resourceId),jsonObject.toString(),Long.valueOf(0),Long.valueOf(0));
				    	 this.eventActionRepository.saveAndFlush(eventAction);
				    	 
				    	 eventAction=new EventAction(eventMaster.getEventEndDate(),"Disconnect","Live Event",EventActionConstants.ACTION_INACTIVE_LIVE_EVENT.toString(),
				    			 "/eventmaster",Long.parseLong(resourceId),jsonObject.toString(),Long.valueOf(0),Long.valueOf(0));
				    	 this.eventActionRepository.saveAndFlush(eventAction);
			      
				    	break; 	   	
			      
			       
				    	
				    }
				    	 	
				      /* if(detailsData.getActionName().equalsIgnoreCase(EventActionConstants.ACTION_SEND_EMAIL)){
				    	   
				          TicketMasterData data = this.ticketMasterReadPlatformService.retrieveTicket(clientId,new Long(resourceId));
				          TicketMaster ticketMaster=this.repository.findOne(new Long(resourceId));
				          AppUserData user = this.readPlatformService.retrieveUser(new Long(data.getUserId()));
				          BillingMessageTemplate billingMessageTemplate = this.messageTemplateRepository.findByTemplateDescription(BillingMessageTemplateConstants.MESSAGE_TEMPLATE_TICKET_TEMPLATE);
				          String value=ticketURL+""+resourceId;
				          String removeUrl="<br/><b>URL : </b>"+"<a href="+value+">View Ticket</a>";
				         // removeUrl.replaceAll("(PARAMURL)", ticketURL+""+resourceId); 	
				        	if(detailsData.getEventName().equalsIgnoreCase(EventActionConstants.EVENT_CREATE_TICKET)){
				        	  	if(!user.getEmail().isEmpty()){
				        	  		BillingMessage billingMessage = new BillingMessage("CREATE TICKET", data.getProblemDescription()+"<br/>"
				        	  	    +ticketMaster.getDescription()+"\n"+removeUrl, "", user.getEmail(), user.getEmail(),
									 "Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
									 BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
				        	  		this.messageDataRepository.save(billingMessage);
				        	  	}else{
				        	  	   if(actionProcedureData.getEmailId().isEmpty()){
				        	  		   
				        	  			throw new EmailNotFoundException(new Long(data.getUserId()));
				        	  		}else{
				        	  			BillingMessage billingMessage = new BillingMessage("CREATE TICKET", data.getProblemDescription()+"<br/>"
				        	  		    +ticketMaster.getDescription()+"\n"+removeUrl, "", actionProcedureData.getEmailId(), actionProcedureData.getEmailId(),
										"Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
										BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
				        	  			this.messageDataRepository.save(billingMessage);
				        	  		}
				        	  	}
				        	
				        	}else if(detailsData.getEventName().equalsIgnoreCase(EventActionConstants.EVENT_EDIT_TICKET)){
				        	  		
				        	    if(!user.getEmail().isEmpty()){
				        	  		BillingMessage billingMessage = new BillingMessage("ADD COMMENT", data.getProblemDescription()+"<br/>"
				        	        +ticketMaster.getDescription()+"<br/>"+"COMMENT: "+data.getLastComment()+"<br/>"+removeUrl, "", user.getEmail(), user.getEmail(),
									"Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
									BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
				        	  		this.messageDataRepository.save(billingMessage);
				        	  	}else{
				        	  		if(actionProcedureData.getEmailId().isEmpty()){
					        	  			throw new EmailNotFoundException(new Long(data.getUserId()));	
					        	  	}else{
					        	  		BillingMessage billingMessage = new BillingMessage("ADD COMMENT", data.getProblemDescription()+"<br/>"
					        	  	     +ticketMaster.getDescription()+"<br/>"+"COMMENT: \t"+data.getLastComment()+"<br/>"+removeUrl, "", actionProcedureData.getEmailId(),
					        	  	     actionProcedureData.getEmailId(),"Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
					        	  	     BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
						        	  		this.messageDataRepository.save(billingMessage);
					        	  	}
				        	  	}
				        	
				        	}else if(detailsData.getEventName().equalsIgnoreCase(EventActionConstants.EVENT_CLOSE_TICKET)){
				        	  	if(!user.getEmail().isEmpty()){
				        	  			BillingMessage billingMessage = new BillingMessage("CLOSED TICKET", data.getProblemDescription()+"<br/>"
				        	  			+ticketMaster.getDescription()+"<br/>"+"RESOLUTION: \t"+ticketMaster.getResolutionDescription()+"<br/>"+removeUrl, "", user.getEmail(), user.getEmail(),
										"Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
										BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
				        	  			this.messageDataRepository.save(billingMessage);
				        	  	}else{
				        	  		if(actionProcedureData.getEmailId().isEmpty()){
					        	  		throw new EmailNotFoundException(new Long(data.getUserId()));	
					        	  	}else{
					        	  		     BillingMessage billingMessage = new BillingMessage("CLOSED TICKET", data.getProblemDescription()+"<br/>"
					        	  		    +ticketMaster.getDescription()+"<br/>"+"RESOLUTION: \t"+ticketMaster.getResolutionDescription()+"<br/>"+removeUrl, "", actionProcedureData.getEmailId(),
					        	  	         actionProcedureData.getEmailId(),"Ticket:"+resourceId, BillingMessageTemplateConstants.MESSAGE_TEMPLATE_STATUS, billingMessageTemplate,
					        	  	       BillingMessageTemplateConstants.MESSAGE_TEMPLATE_MESSAGE_TYPE, null);
						        	        this.messageDataRepository.save(billingMessage);
					        	  }
				        	  	}
				        	  }
				      
				       }else if(actionProcedureData.getActionName().equalsIgnoreCase(EventActionConstants.ACTION_ACTIVE)){
				    	   
					          AssociationData associationData=this.hardwareAssociationReadplatformService.retrieveSingleDetails(actionProcedureData.getOrderId());
					          		if(associationData ==null){
					          			throw new HardwareDetailsNotFoundException(actionProcedureData.getOrderId().toString());
					          		}
					          		jsonObject.put("renewalPeriod",subscriptionDatas.get(0).getId());	
					          		jsonObject.put("description","Order Renewal By Scheduler");
					          		eventAction=new EventAction(new Date(), "CREATE", "PAYMENT",EventActionConstants.ACTION_RENEWAL.toString(),"/orders/renewal", 
				        			 Long.parseLong(resourceId), jsonObject.toString(),actionProcedureData.getOrderId(),clientId);
					          		this.eventActionRepository.save(eventAction);
				         
				       }else if(actionProcedureData.getActionName().equalsIgnoreCase(EventActionConstants.ACTION_NEW)){
				        	  
				        	   jsonObject.put("billAlign","false");
				        	   jsonObject.put("contractPeriod",subscriptionDatas.get(0).getId());
				        	   jsonObject.put("dateFormat","dd MMMM yyyy");
                               jsonObject.put("locale","en");
				        	   jsonObject.put("paytermCode","Monthly");
				        	   jsonObject.put("planCode",actionProcedureData.getPlanId());
				        	   jsonObject.put("isNewplan","true");
				        	   jsonObject.put("start_date",dateFormat.format(new Date()));
				        	   eventAction=new EventAction(new Date(), "CREATE", "PAYMENT",actionProcedureData.getActionName(),"/orders/"+clientId, 
					        			 Long.parseLong(resourceId), jsonObject.toString(),null,clientId);
				        	   this.eventActionRepository.save(eventAction);
				        	   
				      }else if(actionProcedureData.getActionName().equalsIgnoreCase(EventActionConstants.ACTION_DISCONNECT)){
				        	   
				        	   eventAction=new EventAction(new Date(), "CREATE", "PAYMENT",EventActionConstants.ACTION_ACTIVE.toString(),"/orders/reconnect/"+clientId, 
				        	   Long.parseLong(resourceId), jsonObject.toString(),actionProcedureData.getOrderId(),clientId);
				        	   this.eventActionRepository.save(eventAction);
				        	   	
				      }else if(detailsData.getActionName().equalsIgnoreCase(EventActionConstants.ACTION_INVOICE)){
				    	  
				        	  jsonObject.put("dateFormat","dd MMMM yyyy");
                              jsonObject.put("locale","en");
				        	  jsonObject.put("systemDate",dateFormat.format(new Date()));
				        	  	
				        	  if(detailsData.IsSynchronous().equalsIgnoreCase("N")){
				        		  
				        	  		eventAction=new EventAction(new Date(), "CREATE",EventActionConstants.EVENT_ACTIVE_ORDER.toString(),
				        	  		EventActionConstants.ACTION_INVOICE.toString(),"/billingorder/"+clientId,Long.parseLong(resourceId),
				        	  		jsonObject.toString(),Long.parseLong(resourceId),clientId);
						        	this.eventActionRepository.save(eventAction);
				        	  	
				        	  	}else{
				            	 
				        	  		Order order=this.orderRepository.findOne(new Long(resourceId));
				        	  		jsonObject.put("dateFormat","dd MMMM yyyy");
				        	  		jsonObject.put("locale","en");
				        	  		jsonObject.put("systemDate",dateFormat.format(order.getStartDate()));
				        	  		this.billingOrderApiResourse.retrieveBillingProducts(order.getClientId(),jsonObject.toString());
				        	  	}
				     
				      }else if(detailsData.getActionName().equalsIgnoreCase(EventActionConstants.ACTION_PROVISION_IT)){
			  	    	
			  	    	Client client=this.clientRepository.findOne(clientId);
			  	    	EventOrder eventOrder=this.eventOrderRepository.findOne(Long.valueOf(resourceId));
			  	    	EventMaster eventMaster=this.eventMasterRepository.findOne(eventOrder.getEventId());
			  	    	
			  	    	String response= AddExternalBeesmartMethod.addVodPackage(client.getOffice().getExternalId().toString(),client.getAccountNo(),
			  	    			eventMaster.getEventName());
			  	   
						ProcessRequest processRequest=new ProcessRequest(Long.valueOf(0), eventOrder.getClientId(),eventOrder.getId(),ProvisioningApiConstants.PROV_BEENIUS,
								ProvisioningApiConstants.REQUEST_ACTIVATION_VOD,'Y','Y');
						List<EventOrderdetials> eventDetails=eventOrder.getEventOrderdetials();
						//EventMaster eventMaster=this.eventMasterRepository.findOne(eventOrder.getEventId());
						//JSONObject jsonObject=new JSONObject();
						jsonObject.put("officeUid",client.getOffice().getExternalId());
						jsonObject.put("subscriberUid",client.getAccountNo());
						jsonObject.put("vodUid",eventMaster.getEventName());
								
							for(EventOrderdetials details:eventDetails){
								ProcessRequestDetails processRequestDetails=new ProcessRequestDetails(details.getId(),details.getEventDetails().getId(),jsonObject.toString(),
										response,null,eventMaster.getEventStartDate(), eventMaster.getEventEndDate(),new Date(),new Date(),'N',
										ProvisioningApiConstants.REQUEST_ACTIVATION_VOD,null);
								processRequest.add(processRequestDetails);
							}
						this.processRequestRepository.save(processRequest);
					
			  	    	
				      }
			  	    }if(detailsData.getActionName().equalsIgnoreCase(EventActionConstants.ACTION_SEND_PROVISION)){
		        	   
		        	   eventAction=new EventAction(new Date(), "CLOSE", "Client",EventActionConstants.ACTION_SEND_PROVISION.toString(),"/processrequest/"+clientId, 
		        	   Long.parseLong(resourceId),jsonObject.toString(),clientId,clientId);
		        	   this.eventActionRepository.save(eventAction);
			  	}*/
			
		}
	}
	     return null;
    }catch(Exception exception){
    	exception.printStackTrace();
    	return null;
    }

	}
}
