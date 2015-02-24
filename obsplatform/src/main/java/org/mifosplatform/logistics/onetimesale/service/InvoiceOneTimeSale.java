package org.mifosplatform.logistics.onetimesale.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.LocalDate;
import org.mifosplatform.billing.discountmaster.data.DiscountMasterData;
import org.mifosplatform.billing.discountmaster.domain.DiscountMaster;
import org.mifosplatform.billing.discountmaster.domain.DiscountMasterRepository;
import org.mifosplatform.finance.billingorder.commands.BillingOrderCommand;
import org.mifosplatform.finance.billingorder.data.BillingOrderData;
import org.mifosplatform.finance.billingorder.domain.Invoice;
import org.mifosplatform.finance.billingorder.service.BillingOrderReadPlatformService;
import org.mifosplatform.finance.billingorder.service.BillingOrderWritePlatformService;
import org.mifosplatform.finance.billingorder.service.GenerateBill;
import org.mifosplatform.finance.billingorder.service.GenerateBillingOrderService;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.logistics.onetimesale.data.OneTimeSaleData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author hugo
 *
 *invoice for device sale
 */
@Service
public class InvoiceOneTimeSale {

	private final GenerateBill generateBill;
	private final BillingOrderWritePlatformService billingOrderWritePlatformService;
	private final GenerateBillingOrderService generateBillingOrderService;
	private final DiscountMasterRepository discountMasterRepository;
	private final BillingOrderReadPlatformService billingOrderReadPlatformService;
	@Autowired
	public InvoiceOneTimeSale(final GenerateBill generateBill,final BillingOrderWritePlatformService billingOrderWritePlatformService,
			final GenerateBillingOrderService generateBillingOrderService,final DiscountMasterRepository discountMasterRepository,
			final BillingOrderReadPlatformService billingOrderReadPlatformService) {
		this.generateBill = generateBill;
		this.billingOrderWritePlatformService = billingOrderWritePlatformService;
		this.generateBillingOrderService = generateBillingOrderService;
		this.discountMasterRepository=discountMasterRepository;
		this.billingOrderReadPlatformService=billingOrderReadPlatformService; 
	}

/**
 * @param clientId
 * @param oneTimeSaleData
 * @param b 
 */
public CommandProcessingResult invoiceOneTimeSale(final Long clientId, final OneTimeSaleData oneTimeSaleData, boolean isWalletEnable) {
	
		 List<BillingOrderCommand> billingOrderCommands = new ArrayList<BillingOrderCommand>();

			
				BillingOrderData billingOrderData = new BillingOrderData(oneTimeSaleData.getId(),oneTimeSaleData.getClientId(),	new LocalDate().toDate(),
						oneTimeSaleData.getChargeCode(),oneTimeSaleData.getChargeType(),oneTimeSaleData.getTotalPrice(),oneTimeSaleData.getTaxInclusive());
				
				
				DiscountMaster discountMaster=this.discountMasterRepository.findOne(oneTimeSaleData.getDiscountId());
				
				DiscountMasterData discountMasterData=new DiscountMasterData(discountMaster.getId(),discountMaster.getDiscountCode(),discountMaster.getDiscountDescription(),
						discountMaster.getDiscountType(),discountMaster.getDiscountRate(),null,null);
				
			    discountMasterData = this.calculateDiscount(discountMasterData,billingOrderData.getPrice());
			    

			    BillingOrderCommand   billingOrderCommand = this.generateBill.getOneTimeBill(billingOrderData, discountMasterData);
				        
			    billingOrderCommands.add(billingOrderCommand);
				
				// calculation of invoice
				Invoice invoice = this.generateBillingOrderService.generateInvoice(billingOrderCommands);

				// To fetch record from client_balance table
				this.billingOrderWritePlatformService.updateClientBalance(invoice.getInvoiceAmount(),clientId,isWalletEnable);
				
				/* //office commision
				 AgreementData clientAgreement=this.billingOrderReadPlatformService.retriveClientOfficeDetails(clientId);
			     if(clientAgreement.getOfficeType().equalsIgnoreCase("Agent")&&clientAgreement.getId()!=null) {
				     this.billingOrderWritePlatformService.UpdateOfficeCommision(invoice,clientAgreement.getId());
		          }*/
				
				return new CommandProcessingResult(invoice.getId());


			}

		
	
	// Discount Applicable Logic
	public boolean isDiscountApplicable(final DiscountMasterData discountMasterData) {
		boolean isDiscountApplicable = true;
		
		return isDiscountApplicable;

	}

	// Discount End Date calculation if null
	public Date getDiscountEndDateIfNull(final DiscountMasterData discountMasterData) {
		LocalDate discountEndDate = discountMasterData.getDiscountEndDate();
		if (discountMasterData.getDiscountEndDate() == null) {
			discountEndDate = new LocalDate(2099, 0, 01);
		}
		return discountEndDate.toDate();

	}
	
	// if is percentage
	public boolean isDiscountPercentage(final DiscountMasterData discountMasterData){
		boolean isDiscountPercentage = false;
		if(discountMasterData.getDiscountType().equalsIgnoreCase("percentage")){
																
			isDiscountPercentage = true;
		}
		return isDiscountPercentage;
	}
	
	// if is discount
	public boolean isDiscountFlat(final DiscountMasterData discountMasterData){
		boolean isDiscountFlat = false;
		if(discountMasterData.getDiscountType().equalsIgnoreCase("flat")){
			
			isDiscountFlat = true;
		}
		return isDiscountFlat;
	}
	

	// Discount calculation 
	public DiscountMasterData calculateDiscount(final DiscountMasterData discountMasterData, BigDecimal chargePrice){
		
		BigDecimal discountAmount=BigDecimal.ZERO;
		if(isDiscountPercentage(discountMasterData)){
			
			if(discountMasterData.getDiscountRate().compareTo(new BigDecimal(100)) ==-1 ||
			 discountMasterData.getDiscountRate().compareTo(new BigDecimal(100)) == 0){
				
			discountAmount = this.calculateDiscountPercentage(discountMasterData.getDiscountRate(), chargePrice);
			discountMasterData.setDiscountAmount(discountAmount);
			chargePrice = this.chargePriceNotLessThanZero(chargePrice, discountAmount);
			discountMasterData.setDiscountedChargeAmount(chargePrice);
			
			}
			
		}
		
		if(isDiscountFlat(discountMasterData)){
			
			BigDecimal netFlatAmount=this.calculateDiscountFlat(discountMasterData.getDiscountRate(), chargePrice);
			netFlatAmount=this.chargePriceNotLessThanZero(chargePrice, discountAmount);
			discountMasterData.setDiscountedChargeAmount(netFlatAmount);
			discountAmount = chargePrice.subtract(netFlatAmount);
			discountMasterData.setDiscountAmount(discountAmount);
			
		}
		return discountMasterData;
	
	}
	
	// Dicount Percent calculation
	public BigDecimal calculateDiscountPercentage(final BigDecimal discountRate,final BigDecimal chargePrice){
		
		return chargePrice.multiply(discountRate.divide(new BigDecimal(100))).setScale(Integer.parseInt(this.generateBill.roundingDecimal()), RoundingMode.HALF_UP);
	}
	
	// Discount Flat calculation
	public BigDecimal calculateDiscountFlat(final BigDecimal discountRate,final BigDecimal chargePrice){
		
		BigDecimal discountFlat=BigDecimal.ZERO;
		//check for chargeprice zero and discountrate greater than zero
		if(chargePrice.compareTo(BigDecimal.ZERO) == 1 ){
			discountFlat=chargePrice.subtract(discountRate).setScale(Integer.parseInt(this.generateBill.roundingDecimal()),RoundingMode.HALF_UP);
		}
		return discountFlat;
	}
	
	// to check price not less than zero
	public BigDecimal chargePriceNotLessThanZero(BigDecimal chargePrice,final BigDecimal discountPrice){
		
		chargePrice = chargePrice.subtract(discountPrice);
		if(chargePrice.compareTo(discountPrice) < 0){
			chargePrice = BigDecimal.ZERO;
		}
		return chargePrice;
		
	}
	

}
