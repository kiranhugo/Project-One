package org.mifosplatform.finance.paymentsgateway.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.mifosplatform.commands.domain.CommandWrapper;
import org.mifosplatform.commands.service.CommandWrapperBuilder;
import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.finance.paymentsgateway.data.RecurringPaymentTransactionTypeConstants;
import org.mifosplatform.finance.paymentsgateway.domain.PaymentGatewayConfiguration;
import org.mifosplatform.finance.paymentsgateway.domain.PaymentGatewayConfigurationRepository;
import org.mifosplatform.finance.paymentsgateway.domain.PaymentGatewayRepository;
import org.mifosplatform.finance.paymentsgateway.domain.PaypalRecurringBilling;
import org.mifosplatform.finance.paymentsgateway.domain.PaypalRecurringBillingRepository;
import org.mifosplatform.finance.paymentsgateway.exception.PaymentGatewayConfigurationException;
import org.mifosplatform.infrastructure.configuration.domain.ConfigurationConstants;
import org.mifosplatform.infrastructure.configuration.domain.ConfigurationRepository;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.data.EnumOptionData;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.infrastructure.core.service.DateUtils;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.organisation.message.domain.BillingMessageRepository;
import org.mifosplatform.organisation.message.domain.BillingMessageTemplateRepository;
import org.mifosplatform.portfolio.client.domain.ClientRepository;
import org.mifosplatform.portfolio.order.data.OrderStatusEnumaration;
import org.mifosplatform.portfolio.order.domain.Order;
import org.mifosplatform.portfolio.order.domain.OrderRepository;
import org.mifosplatform.portfolio.order.domain.StatusTypeEnum;
import org.mifosplatform.portfolio.order.exceptions.OrderNotFoundException;
import org.mifosplatform.scheduledjobs.scheduledjobs.data.EventActionData;
import org.mifosplatform.workflow.eventaction.domain.EventAction;
import org.mifosplatform.workflow.eventaction.domain.EventActionRepository;
import org.mifosplatform.workflow.eventaction.service.EventActionConstants;
import org.mifosplatform.workflow.eventaction.service.EventActionReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import urn.ebay.api.PayPalAPI.ManageRecurringPaymentsProfileStatusReq;
import urn.ebay.api.PayPalAPI.ManageRecurringPaymentsProfileStatusRequestType;
import urn.ebay.api.PayPalAPI.ManageRecurringPaymentsProfileStatusResponseType;
import urn.ebay.api.PayPalAPI.PayPalAPIInterfaceServiceService;
import urn.ebay.api.PayPalAPI.SetExpressCheckoutReq;
import urn.ebay.api.PayPalAPI.SetExpressCheckoutRequestType;
import urn.ebay.api.PayPalAPI.SetExpressCheckoutResponseType;
import urn.ebay.api.PayPalAPI.UpdateRecurringPaymentsProfileReq;
import urn.ebay.api.PayPalAPI.UpdateRecurringPaymentsProfileRequestType;
import urn.ebay.api.PayPalAPI.UpdateRecurringPaymentsProfileResponseType;
import urn.ebay.apis.CoreComponentTypes.BasicAmountType;
import urn.ebay.apis.eBLBaseComponents.AckCodeType;
import urn.ebay.apis.eBLBaseComponents.BillingAgreementDetailsType;
import urn.ebay.apis.eBLBaseComponents.BillingCodeType;
import urn.ebay.apis.eBLBaseComponents.CurrencyCodeType;
import urn.ebay.apis.eBLBaseComponents.ErrorType;
import urn.ebay.apis.eBLBaseComponents.ManageRecurringPaymentsProfileStatusRequestDetailsType;
import urn.ebay.apis.eBLBaseComponents.PaymentActionCodeType;
import urn.ebay.apis.eBLBaseComponents.PaymentDetailsType;
import urn.ebay.apis.eBLBaseComponents.SetExpressCheckoutRequestDetailsType;
import urn.ebay.apis.eBLBaseComponents.StatusChangeActionType;
import urn.ebay.apis.eBLBaseComponents.UpdateRecurringPaymentsProfileRequestDetailsType;

import com.google.gson.JsonObject;
import com.paypal.exception.ClientActionRequiredException;
import com.paypal.exception.HttpErrorException;
import com.paypal.exception.InvalidCredentialException;
import com.paypal.exception.InvalidResponseDataException;
import com.paypal.exception.MissingCredentialException;
import com.paypal.exception.SSLConfigurationException;
import com.paypal.sdk.exceptions.OAuthException;

@Service
public class PaymentGatewayRecurringWritePlatformServiceImpl implements PaymentGatewayRecurringWritePlatformService {

	private final PlatformSecurityContext context;
    private final PaymentGatewayRepository paymentGatewayRepository;
    private final FromJsonHelper fromApiJsonHelper;
    private final PortfolioCommandSourceWritePlatformService writePlatformService;
    private final PaymentGatewayConfigurationRepository paymentGatewayConfigurationRepository;
    private final BillingMessageTemplateRepository billingMessageTemplateRepository;
	private final BillingMessageRepository messageDataRepository;
	private final ClientRepository clientRepository;
	private final ConfigurationRepository configurationRepository;
	private final PaypalRecurringBillingRepository paypalRecurringBillingRepository;
	private final EventActionReadPlatformService eventActionReadPlatformService;
	private final EventActionRepository eventActionRepository;
	private final OrderRepository orderRepository;
   
   
    @Autowired
    public PaymentGatewayRecurringWritePlatformServiceImpl(final PlatformSecurityContext context,final PaymentGatewayRepository paymentGatewayRepository,
    		final FromJsonHelper fromApiJsonHelper,final PortfolioCommandSourceWritePlatformService writePlatformService,
    		final PaymentGatewayConfigurationRepository paymentGatewayConfigurationRepository,
    		final BillingMessageTemplateRepository billingMessageTemplateRepository,final BillingMessageRepository messageDataRepository,
    		final ClientRepository clientRepository, final EventActionRepository eventActionRepository,
    		final ConfigurationRepository configurationRepository,final EventActionReadPlatformService eventActionReadPlatformService,
    		final PaypalRecurringBillingRepository paypalRecurringBillingRepository, final OrderRepository orderRepository){
    	
    	this.context=context;
    	this.paymentGatewayRepository=paymentGatewayRepository;
    	this.fromApiJsonHelper=fromApiJsonHelper;
    	this.writePlatformService = writePlatformService;
    	this.paymentGatewayConfigurationRepository = paymentGatewayConfigurationRepository;
    	this.billingMessageTemplateRepository = billingMessageTemplateRepository;
    	this.messageDataRepository = messageDataRepository;
    	this.clientRepository = clientRepository;
    	this.eventActionRepository = eventActionRepository;
    	this.configurationRepository = configurationRepository;
    	this.eventActionReadPlatformService = eventActionReadPlatformService;
    	this.paypalRecurringBillingRepository = paypalRecurringBillingRepository;
    	this.orderRepository = orderRepository;
    }
    
	@SuppressWarnings("rawtypes")
	@Override
	public String paypalRecurringVerification(HttpServletRequest request) throws IllegalStateException, ClientProtocolException, IOException, JSONException {

		PaymentGatewayConfiguration pgConfig = this.paymentGatewayConfigurationRepository.findOneByName(ConfigurationConstants.PAYPAL_PAYMENTGATEWAY);
		
		if (null == pgConfig || null == pgConfig.getValue() || !pgConfig.isEnabled()) {
			throw new PaymentGatewayConfigurationException(ConfigurationConstants.PAYPAL_PAYMENTGATEWAY);
		}
		
		JSONObject pgConfigJsonObj = new JSONObject(pgConfig.getValue());
		String paypalUrl = pgConfigJsonObj.getString("paypalUrl");
		//String paypalEmailId = pgConfigJsonObj.getString("paypalEmailId");
		
		String[] urlData = paypalUrl.split("\\?");
		
		//2. Prepare 'notify-validate' command with exactly the same parameters
		Enumeration en = request.getParameterNames();
		StringBuilder cmd = new StringBuilder("cmd=_notify-validate");
		String paramName;
		String paramValue;
		while (en.hasMoreElements()) {

			paramName = (String) en.nextElement();
			paramValue = request.getParameter(paramName);
			
			if (!"password".equalsIgnoreCase(paramName) && !"username".equalsIgnoreCase(paramName) && !"rm".equalsIgnoreCase(paramName)) {
				cmd.append("&").append(paramName).append("=").append(URLEncoder.encode(paramValue, request.getParameter("charset")));
				
			} /*else {
				cmd.append("&").append(paramName).append("=").append(URLEncoder.encode(paramValue, request.getParameter("charset")));
			}*/
		}
		 
		//3. Post above command to Paypal IPN URL {@link IpnConfig#ipnUrl}
		URL u = new URL(urlData[0].trim());
		HttpsURLConnection uc = (HttpsURLConnection) u.openConnection();
		uc.setDoOutput(true);
		uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		uc.setRequestProperty("Host", u.getHost());
		//uc.setRequestProperty("Host", "www.sandbox.paypal.com");
		uc.setRequestMethod("POST");
		PrintWriter pw = new PrintWriter(uc.getOutputStream());
		pw.println(cmd.toString());
		pw.close();
		 
		//4. Read response from Paypal
		BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
		String res = in.readLine();
		in.close(); 
		return res;

	}

	/*@Override
	public String getAccessToken(String data) {
		// TODO Auto-generated method stub
		try {
			
			PaymentGatewayConfiguration pgConfig = this.paymentGatewayConfigurationRepository.findOneByName(ConfigurationConstants.PAYPAL_RECURRING_PAYMENT_DETAILS);
			
			if (null == pgConfig || null == pgConfig.getValue() || !pgConfig.isEnabled()) {
				throw new PaymentGatewayConfigurationException(ConfigurationConstants.PAYPAL_RECURRING_PAYMENT_DETAILS);
			}
			
			JSONObject pgConfigObject = new JSONObject(pgConfig);
			String username = pgConfigObject.getString("userName");
			String password = pgConfigObject.getString("password");
			String signature = pgConfigObject.getString("signature");
			String serverType = pgConfigObject.getString("serverType");
			
			JSONObject object = new JSONObject(data);
			
			String currencyCode = object.getString("currencyCode");
			Double amount = object.getDouble("amount");
			String returnUrl = object.getString("returnUrl");
			
			PaymentDetailsType paymentDetails = new PaymentDetailsType();
			paymentDetails.setPaymentAction(PaymentActionCodeType.fromValue("Sale"));
		
			BasicAmountType orderTotal = new BasicAmountType();
			orderTotal.setCurrencyID(CurrencyCodeType.fromValue(currencyCode));
			orderTotal.setValue(amount.toString());
			paymentDetails.setOrderTotal(orderTotal);
			List<PaymentDetailsType> paymentDetailsList = new ArrayList<PaymentDetailsType>();
			paymentDetailsList.add(paymentDetails);
		
			SetExpressCheckoutRequestDetailsType setExpressCheckoutRequestDetails = new SetExpressCheckoutRequestDetailsType();
			setExpressCheckoutRequestDetails.setReturnURL(returnUrl);
			setExpressCheckoutRequestDetails.setCancelURL(returnUrl);
			
			setExpressCheckoutRequestDetails.setPaymentDetails(paymentDetailsList);
			
			BillingAgreementDetailsType billingAgreement = new BillingAgreementDetailsType(BillingCodeType.fromValue("RecurringPayments"));
			billingAgreement.setBillingAgreementDescription("recurringbilling");
			List<BillingAgreementDetailsType> billList = new ArrayList<BillingAgreementDetailsType>();
			billList.add(billingAgreement);
			setExpressCheckoutRequestDetails.setBillingAgreementDetails(billList);
		
			SetExpressCheckoutRequestType setExpressCheckoutRequest = new SetExpressCheckoutRequestType(setExpressCheckoutRequestDetails);
			setExpressCheckoutRequest.setVersion("104.0");
		
			SetExpressCheckoutReq setExpressCheckoutReq = new SetExpressCheckoutReq();
			setExpressCheckoutReq.setSetExpressCheckoutRequest(setExpressCheckoutRequest);
		
			Map<String, String> sdkConfig = new HashMap<String, String>();
			
			
			sdkConfig.put(RecurringPaymentTransactionTypeConstants.SERVER_MODE, serverType);
			sdkConfig.put(RecurringPaymentTransactionTypeConstants.API_USERNAME, username);
			sdkConfig.put(RecurringPaymentTransactionTypeConstants.API_PASSWORD, password);
			sdkConfig.put(RecurringPaymentTransactionTypeConstants.API_SIGNATURE, signature);
			
			PayPalAPIInterfaceServiceService service = new PayPalAPIInterfaceServiceService(sdkConfig);
				
			SetExpressCheckoutResponseType setExpressCheckoutResponse = service.setExpressCheckout(setExpressCheckoutReq);
			
			String ack = setExpressCheckoutResponse.getAck().getValue();
			
			object = new JSONObject(); 
			
			if(setExpressCheckoutResponse != null && ack.equalsIgnoreCase(
					RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_SUCCESS)){
				
				object.put("result", ack);
				object.put("accessToken", setExpressCheckoutResponse.getToken());
				
				return object.toString();
			
			} else if (setExpressCheckoutResponse != null && ack.equalsIgnoreCase(
					RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE)) {
				
				ErrorType errorType = setExpressCheckoutResponse.getErrors().get(0);
				
				object.put("result", ack);
				object.put("error", errorType.getErrorCode());
				object.put("description", errorType.getLongMessage());
				
				return object.toString();
			
			} else{
				
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}*/

	@Override
	public void recurringEventUpdate(HttpServletRequest request) throws JSONException {
		
		/**
		 * Storing ProfileId, if not Stored in the b_recurring table 
		 */
		PaypalRecurringBilling paypalRecurringBilling = recurringSubscriberSignUp(request);
		
		/**
		 * @Param retrievePendingRecurringRequest() method is used for get the Pending Orders from b_event_action
		 */
		
		String jsonObj = request.getParameter(RecurringPaymentTransactionTypeConstants.CUSTOM);
		
		JSONObject obj = new JSONObject(jsonObj);
		
		Long clientId = obj.getLong(RecurringPaymentTransactionTypeConstants.CLIENTID);
		Long planId   = obj.getLong(RecurringPaymentTransactionTypeConstants.PLANID);
		String paytermCode = obj.getString(RecurringPaymentTransactionTypeConstants.PAYMENTCODE);
		Long contractPeriod = obj.getLong(RecurringPaymentTransactionTypeConstants.CONTRACTPERIOD);
		
		List<EventActionData> eventActionDatas = this.eventActionReadPlatformService.retrievePendingRecurringRequest(clientId);
		
		for(EventActionData eventActionData:eventActionDatas){
			
			System.out.println("EventAction Id:"+eventActionData.getId()+", PaymentGatewayId:"+eventActionData.getResourceId());
		
			EventAction eventAction = this.eventActionRepository.findOne(eventActionData.getId());
			
			if (eventAction.getActionName().equalsIgnoreCase(EventActionConstants.ACTION_NEW)) {
				
				JSONObject createOrder = new JSONObject(eventAction.getCommandAsJson());
				
				Long ePlanCode   = createOrder.getLong(RecurringPaymentTransactionTypeConstants.PLANCODE);
				String ePaytermCode = createOrder.getString(RecurringPaymentTransactionTypeConstants.PAYMENTCODE);
				Long eContractPeriod = createOrder.getLong(RecurringPaymentTransactionTypeConstants.CONTRACTPERIOD);
				
				if(planId == ePlanCode && paytermCode.equalsIgnoreCase(ePaytermCode) && contractPeriod == eContractPeriod){
					
					// creating order and assign Recurring Details.
					
					CommandWrapper commandRequest = new CommandWrapperBuilder().createOrder(clientId).withJson(createOrder.toString()).build();
					CommandProcessingResult resultOrder = this.writePlatformService.logCommandSource(commandRequest);

					if (resultOrder == null) {
						
						throw new PlatformDataIntegrityException("error.msg.client.order.creation", "Book Order Failed for ClientId:"	
								+ clientId, "Book Order Failed");
					}
					
					if(null != paypalRecurringBilling){
						createOrder.remove("start_date");
						eventAction.updateStatus('Y');
						eventAction.setTransDate(DateUtils.getLocalDateOfTenant().toDate());
						createOrder.put("start_date", DateUtils.getLocalDateOfTenant().toDate());
						eventAction.setCommandAsJson(createOrder.toString());
						this.eventActionRepository.save(eventAction);
						
						paypalRecurringBilling.setOrderId(resultOrder.resourceId());
						
						this.paypalRecurringBillingRepository.save(paypalRecurringBilling);
					}
					
					
				}
				
			} else{
				System.out.println("Does Not Implement the Code....");
			}
			
			this.eventActionRepository.save(eventAction);
		}

		
	}

	@Override
	public PaypalRecurringBilling recurringSubscriberSignUp(HttpServletRequest request) {
		// TODO Auto-generated method stub

		try {
			String ProfileId = request.getParameter(RecurringPaymentTransactionTypeConstants.SUBSCRID);
			String jsonObj = request.getParameter(RecurringPaymentTransactionTypeConstants.CUSTOM);

			PaypalRecurringBilling billing = this.paypalRecurringBillingRepository.findOneBySubscriberId(ProfileId);
			
			if (billing == null) {

				 JSONObject object = new JSONObject(jsonObj);
				 Long clientId = object.getLong(RecurringPaymentTransactionTypeConstants.CLIENTID);
				 
				 billing = new PaypalRecurringBilling(clientId, ProfileId);
				 this.paypalRecurringBillingRepository.save(billing);
			}

			return billing;
			
		} catch (JSONException e) {
			System.out.println("ProfileId Storing Failed");
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String createJsonForOnlineMethod(HttpServletRequest request) throws JSONException {
		
		String status;
		
		String paymentStatus = request.getParameter(RecurringPaymentTransactionTypeConstants.PAYMENTSTATUS);
		String currency = request.getParameter(RecurringPaymentTransactionTypeConstants.MCCURRENCY);
		BigDecimal amount = new BigDecimal(request.getParameter(RecurringPaymentTransactionTypeConstants.MCGROSS));
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
		String date = dateFormat.format(new Date());
		
		JsonObject jsonObj = new JsonObject();
		jsonObj.addProperty("paymentDate", date);
		jsonObj.addProperty("payerEmail", request.getParameter("payer_email"));
		jsonObj.addProperty("customer_name", request.getParameter("first_name"));
		jsonObj.addProperty("receiverEmail", request.getParameter("receiver_email"));
		jsonObj.addProperty("payerStatus", request.getParameter("payer_status"));
		jsonObj.addProperty("currency", currency);
		jsonObj.addProperty("paymentStatus", paymentStatus);
		
		JSONObject custom = new JSONObject(request.getParameter(RecurringPaymentTransactionTypeConstants.CUSTOM));
		Long clientId = custom.getLong(RecurringPaymentTransactionTypeConstants.CLIENTID);
		
		JSONObject jsonObject = new JSONObject();
		
		if(paymentStatus.equalsIgnoreCase(RecurringPaymentTransactionTypeConstants.COMPLETED)){
			
			status = RecurringPaymentTransactionTypeConstants.SUCCESS;
			
		} else if (paymentStatus.equalsIgnoreCase(RecurringPaymentTransactionTypeConstants.PENDING)) {
			
			status = RecurringPaymentTransactionTypeConstants.PENDING;
			String error = request.getParameter(RecurringPaymentTransactionTypeConstants.PENDINGREASON);
			jsonObj.addProperty("pendingReason", error);
			jsonObject.put("error", error);
		
		} else{
			status = paymentStatus;
		}
				
		jsonObject.put("source", RecurringPaymentTransactionTypeConstants.PAYPAL);
		jsonObject.put("transactionId", request.getParameter(RecurringPaymentTransactionTypeConstants.TRANSACTIONID));
		jsonObject.put("currency", currency);
		jsonObject.put("clientId", clientId);
		jsonObject.put("total_amount", amount);
		jsonObject.put("locale", "en");
		jsonObject.put("dateFormat", "dd MMMM yyyy");
		jsonObject.put("otherData", jsonObj.toString());
		jsonObject.put("status", status);
	
		return jsonObject.toString();
		
	}

	@Override
	public CommandProcessingResult updatePaypalRecurring(JsonCommand command) {
		// TODO Auto-generated method stub
		
		Map<String,Object> mapDetails = new HashMap<String,Object>();
		
		try {
			
			PaymentGatewayConfiguration pgConfigDetails = this.paymentGatewayConfigurationRepository.findOneByName(ConfigurationConstants.PAYPAL_PAYMENTGATEWAY);
			
			if (null == pgConfigDetails || null == pgConfigDetails.getValue() || !pgConfigDetails.isEnabled()) {
				throw new PaymentGatewayConfigurationException(ConfigurationConstants.PAYPAL_PAYMENTGATEWAY);
			}
			
			JSONObject object = new JSONObject(pgConfigDetails);
			
			String currencyCode = object.getString("currency_code");

			UpdateRecurringPaymentsProfileRequestDetailsType updateRecurringPaymentsProfileRequestDetails = new UpdateRecurringPaymentsProfileRequestDetailsType();

			Long orderId = command.longValueOfParameterNamed("orderId");
			
			PaypalRecurringBilling paypalRecurringBilling = this.paypalRecurringBillingRepository.findOneByOrderId(orderId);
			
			if(null == paypalRecurringBilling){
				throw new OrderNotFoundException(orderId);
			}
			
			updateRecurringPaymentsProfileRequestDetails.setProfileID(paypalRecurringBilling.getSubscriberId());
			
			if(command.hasParameter("billingCycles")){
				updateRecurringPaymentsProfileRequestDetails.setAdditionalBillingCycles(command.integerValueOfParameterNamed("billingCycles"));
			}

			if(command.hasParameter("totalAmount")){
				
				BasicAmountType amount = new BasicAmountType();

				amount.setCurrencyID(CurrencyCodeType.valueOf(currencyCode));

				amount.setValue(command.stringValueOfParameterNamed("totalAmount"));

				updateRecurringPaymentsProfileRequestDetails.setAmount(amount);
			}
			
			/* 2013-08-24T05:38:48Z */

			/*final String pattern = "yyyy-MM-dd'T'hh:mm:ssZ";

			SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

			Calendar calendar = new GregorianCalendar();
			
			TimeZone timeZone = calendar.getTimeZone();

			calendar.setTimeZone(timeZone);

			String date = dateFormat.format(calendar.getTime());

			updateRecurringPaymentsProfileRequestDetails.setBillingStartDate(date);*/
			

			updateRecurringPaymentsProfileRequestDetails.setMaxFailedPayments(5);

			UpdateRecurringPaymentsProfileRequestType updateRecurringPaymentsProfileRequest = new UpdateRecurringPaymentsProfileRequestType();

			updateRecurringPaymentsProfileRequest.setUpdateRecurringPaymentsProfileRequestDetails(updateRecurringPaymentsProfileRequestDetails);

			UpdateRecurringPaymentsProfileReq profile = new UpdateRecurringPaymentsProfileReq();

			profile.setUpdateRecurringPaymentsProfileRequest(updateRecurringPaymentsProfileRequest);

			// Create the PayPalAPIInterfaceServiceService service object to make the API call
			PayPalAPIInterfaceServiceService service = new PayPalAPIInterfaceServiceService(getMapConfigDetails());

			UpdateRecurringPaymentsProfileResponseType manageProfileStatusResponse = service.updateRecurringPaymentsProfile(profile);
			
			if (manageProfileStatusResponse.getAck().equals(AckCodeType.FAILURE)
					|| (manageProfileStatusResponse.getErrors() != null && manageProfileStatusResponse.getErrors().size() > 0)) {
		
				String error = manageProfileStatusResponse.getErrors().get(0).getLongMessage();
				
				mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
				mapDetails.put("error", error);
				
			} else {
				
				mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_SUCCESS);
				mapDetails.put("error", "");
			}	

		} catch (JSONException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			stackTraceToString(e);
			mapDetails.put("error", stackTraceToString(e));
		} catch (SSLConfigurationException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		} catch (InvalidCredentialException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		} catch (HttpErrorException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		} catch (InvalidResponseDataException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		} catch (ClientActionRequiredException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		} catch (MissingCredentialException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		} catch (OAuthException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		} catch (IOException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		} catch (InterruptedException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		} catch (ParserConfigurationException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		} catch (SAXException e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		} catch (Exception e) {
			mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
			mapDetails.put("error", stackTraceToString(e));
		}
		
		return new CommandProcessingResultBuilder().with(mapDetails).build();
	}

	private String stackTraceToString(Throwable e) {
		
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement element : e.getStackTrace()) {
			sb.append(element.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	private Map<String, String> getMapConfigDetails() throws JSONException {

		PaymentGatewayConfiguration pgConfig = this.paymentGatewayConfigurationRepository.findOneByName(ConfigurationConstants.PAYPAL_RECURRING_PAYMENT_DETAILS);

		if (null == pgConfig || null == pgConfig.getValue() || !pgConfig.isEnabled()) {
			throw new PaymentGatewayConfigurationException(ConfigurationConstants.PAYPAL_RECURRING_PAYMENT_DETAILS);
		}

		JSONObject pgConfigObject = new JSONObject(pgConfig.getValue());
		String username = pgConfigObject.getString("userName");
		String password = pgConfigObject.getString("password");
		String signature = pgConfigObject.getString("signature");
		String serverType = pgConfigObject.getString("serverType");

		Map<String, String> sdkConfig = new HashMap<String, String>();
		sdkConfig.put(RecurringPaymentTransactionTypeConstants.SERVER_MODE, serverType);
		sdkConfig.put(RecurringPaymentTransactionTypeConstants.API_USERNAME, username);
		sdkConfig.put(RecurringPaymentTransactionTypeConstants.API_PASSWORD, password);
		sdkConfig.put(RecurringPaymentTransactionTypeConstants.API_SIGNATURE, signature);
		
		return sdkConfig;
	}

	@Override
	public CommandProcessingResult updatePaypalProfileStatus(JsonCommand command) {

		Map<String,Object> mapDetails = new HashMap<String,Object>();
		
		try {

			Long orderId = command.longValueOfParameterNamed("orderId");
			String status = command.stringValueOfParameterNamed("recurringStatus");

			PaypalRecurringBilling paypalRecurringBilling = this.paypalRecurringBillingRepository.findOneByOrderId(orderId);

			if (null == paypalRecurringBilling) {
				throw new OrderNotFoundException(orderId);
			}

			ManageRecurringPaymentsProfileStatusRequestType request = new ManageRecurringPaymentsProfileStatusRequestType();

			ManageRecurringPaymentsProfileStatusRequestDetailsType details = new ManageRecurringPaymentsProfileStatusRequestDetailsType();

			request.setManageRecurringPaymentsProfileStatusRequestDetails(details);

			details.setProfileID(paypalRecurringBilling.getSubscriberId());

			details.setAction(StatusChangeActionType.valueOf(status));

			// Invoke the API
			ManageRecurringPaymentsProfileStatusReq wrapper = new ManageRecurringPaymentsProfileStatusReq();
			wrapper.setManageRecurringPaymentsProfileStatusRequest(request);

			// Create the PayPalAPIInterfaceServiceService service object to make the API call
			PayPalAPIInterfaceServiceService service = new PayPalAPIInterfaceServiceService(getMapConfigDetails());

			ManageRecurringPaymentsProfileStatusResponseType manageProfileStatusResponse = service.manageRecurringPaymentsProfileStatus(wrapper);

			if(manageProfileStatusResponse.getAck().equals(AckCodeType.SUCCESS) || 
					manageProfileStatusResponse.getAck().equals(AckCodeType.SUCCESSWITHWARNING)){
				
				mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_SUCCESS);
				mapDetails.put("acknoledgement", manageProfileStatusResponse.getAck());
				mapDetails.put("error", "");
			
			} else if (manageProfileStatusResponse.getAck().equals(AckCodeType.FAILURE) || 
					manageProfileStatusResponse.getAck().equals(AckCodeType.FAILUREWITHWARNING) ||
					 (manageProfileStatusResponse.getErrors() != null && manageProfileStatusResponse.getErrors().size() > 0)) {
		
				String error = manageProfileStatusResponse.getErrors().get(0).getLongMessage();
				
				mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
				mapDetails.put("acknoledgement", manageProfileStatusResponse.getAck());
				mapDetails.put("error", error);
				
			} else {
				mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
				mapDetails.put("acknoledgement", manageProfileStatusResponse.getAck());
				mapDetails.put("error", "");
			}	

			return new CommandProcessingResultBuilder().with(mapDetails).build();
			
		} catch (JSONException e) {		
			mapDetails.put("error", stackTraceToString(e));
		} catch (SSLConfigurationException e) {
			mapDetails.put("error", stackTraceToString(e));
		} catch (InvalidCredentialException e) {
			mapDetails.put("error", stackTraceToString(e));
		} catch (HttpErrorException e) {
			mapDetails.put("error", stackTraceToString(e));
		} catch (InvalidResponseDataException e) {
			mapDetails.put("error", stackTraceToString(e));
		} catch (ClientActionRequiredException e) {
			mapDetails.put("error", stackTraceToString(e));
		} catch (MissingCredentialException e) {
			mapDetails.put("error", stackTraceToString(e));
		} catch (OAuthException e) {
			mapDetails.put("error", stackTraceToString(e));
		} catch (IOException e) {
			mapDetails.put("error", stackTraceToString(e));
		} catch (InterruptedException e) {
			mapDetails.put("error", stackTraceToString(e));
		} catch (ParserConfigurationException e) {
			mapDetails.put("error", stackTraceToString(e));
		} catch (SAXException e) {
			mapDetails.put("error", stackTraceToString(e));
		} catch (Exception e) {
			mapDetails.put("error", stackTraceToString(e));
		}

		mapDetails.put("result", RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
		mapDetails.put("acknoledgement", "");
		
		return new CommandProcessingResultBuilder().with(mapDetails).build();
		
	}

	@Override
	public void disConnectOrder(HttpServletRequest request) {
		
		Long orderId = getOrderId(request);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
		
		String date = dateFormat.format(new Date());
		
		JSONObject object = new JSONObject();
		
		try {
			
			String status = getOrderStatus(orderId);
			
			if (!(status.equalsIgnoreCase(StatusTypeEnum.DISCONNECTED.toString()) || status.equalsIgnoreCase(StatusTypeEnum.PENDING.toString()))) {
				
				object.put("disconnectionDate", date);
				object.put("disconnectReason", "Number of Billing Cycles are Exceed at Paypal Recurring Billing");
				final CommandWrapper commandRequest = new CommandWrapperBuilder().updateOrder(orderId).withJson(object.toString()).build();
				final CommandProcessingResult result = this.writePlatformService.logCommandSource(commandRequest);

				if (result != null && result.resourceId() > 0) {
					System.out.println(RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_SUCCESS);
				} else {
					System.out.println(RecurringPaymentTransactionTypeConstants.RECURRING_PAYMENT_FAILURE);
				}
			}
			
		} catch (JSONException e) {
			System.out.println("JsonException: "+ e.getMessage());
		}
		
	}
	
	@Override
	public String getOrderStatus(Long orderId){
		
		Order order = this.orderRepository.findOne(orderId);
		
		EnumOptionData Enumstatus=OrderStatusEnumaration.OrderStatusType(order.getStatus().intValue());
		return Enumstatus.getValue();
	}
	
	@Override
	public Long getOrderId(HttpServletRequest request) {
		
		String profileId = request.getParameter(RecurringPaymentTransactionTypeConstants.SUBSCRID);
		
		PaypalRecurringBilling billing = this.paypalRecurringBillingRepository.findOneBySubscriberId(profileId);
		
		return billing.getOrderId();
	}


}
