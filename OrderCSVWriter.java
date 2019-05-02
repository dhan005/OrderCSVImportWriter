package com.intershop.adapter.orderexport.csv.service.internal;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.intershop.adapter.orderexport.xml.builder.OrderBuilder;
import com.intershop.beehive.app.capi.App;
import com.intershop.component.order.capi.OrderBO;
import com.intershop.component.order.capi.OrderProductLineItemBO;
import com.intershop.component.payment.capi.PaymentBO;
import com.intershop.component.payment.capi.PaymentHistoryEntryBO;
import com.intershop.sellside.appbase.b2c.capi.shipping.OrderShippingBucketBO;
import com.intershop.sellside.appbase.b2c.internal.order.OrderBOPaymentExtensionImpl;
import com.intershop.sellside.appbase.b2c.internal.order.OrderBOShippingBucketExtensionImpl;

public class OrderCSVWriter
{
    public static final String COMPONENT_NAME_ORDER_BUILDER = "OrderExportXML-Order";
    private OrderBuilder builder;
    private App app;
    private PrintWriter pw;
    private CSVFormat format; 
    private CSVPrinter  printer;

    public OrderCSVWriter(App appContext)
    {

    }

    public void printHeaders(FileWriter fw) throws IOException
    {
        format = CSVFormat.RFC4180.withDelimiter('|');
        printer = new CSVPrinter(fw, format);
        List<String> header = new ArrayList<>();
        header.add("Company Number");
        header.add("FCCustomerID");
        header.add("Store Number");
        header.add("Customer Type");
        header.add("OrderNumber");
        header.add("StartDate");
        header.add("Payment Type");
        header.add("UPC Code");
        header.add("Season");
        header.add("Currency Code");
        header.add("Unit Price");
        header.add("Quantity");
        header.add("Ship Method");
        header.add("ShipToName");
        header.add("ShipToID");
        header.add("BillToName");
        header.add("BillToID");
        header.add("Customer PO Number");
        header.add("OrderDate");
        header.add("OrderType");
        header.add("MessageToMerchant");
        header.add("DropShipFlag");
        header.add("DSAddress1");
        header.add("DSAddress2");
        header.add("DSAddress3");
        header.add("DSZipCode");
        header.add("DSState");
        header.add("DSCountry");
        //phase 2 headers included
        header.add("Request ID");
        header.add("Auth Token");
        header.add("Auth Transaction ");
        header.add("Auth Result");
        header.add("Auth Reason Code");
        header.add("Auth Amount");
        header.add("Auth Code");
        header.add("Auth Date Time");
        header.add("AVS Code");
        header.add("CV Code");
        header.add("Bill First Name");
        header.add("Bill Last Name");
        header.add("Bill Address 1");
        header.add("Bill Address 2");
        header.add("Bill Address 3");
        header.add("Bill Zip Code");
        header.add("Bill State");
        header.add("Bill Country");
        header.add("Bill email");
        header.add("Bill Phone Number");
        header.add("DSName");
        header.add("DSCity");
        header.add("DSPhone");
        // header not needed
        //printer.printRecord(header);
    }

    public void printOrders(FileWriter fw, Collection<OrderBO> orders) throws IOException
    {
        for (OrderBO order:orders){
            printSingleOrder(order,fw);
        }
    }

    private void printSingleOrder(OrderBO order, FileWriter fw) throws IOException
    {
        List<String> orderdata = new ArrayList<>();
        getInfoFromLineItems(orderdata,order);
        printer.printRecord(orderdata);
    }
    private void getInfoFromLineItems(List<String> orderdata, OrderBO order) throws IOException
    {
        String upcCode = "";
        String season = "";
        String currency = "";
        String price = "";
        String quantity = "";
        String shippingID="";
        boolean dropShip=false;
        String paymentType=calculatePaymentType(order);

        Collection<OrderProductLineItemBO> orderPLIs = (Collection<OrderProductLineItemBO>)order.getAllProductLineItemBOs();
        for (OrderProductLineItemBO lineitem : orderPLIs)
        {
            if(lineitem.getProductBO().getAttribute("UPCCode")!=null){
                if (upcCode != "")
                {
                    upcCode = upcCode + ";" + lineitem.getProductBO().getAttribute("UPCCode").toString();
                }
                else 
                {
                    upcCode = lineitem.getProductBO().getAttribute("UPCCode").toString();
                }
            }
            
            if(lineitem.getProductBO().getAttribute("barco_Season")!=null){
                if (season != "")
                {
                    season = season + ";" + lineitem.getProductBO().getAttribute("barco_Season").toString();
                }
                else
                {
                    season = lineitem.getProductBO().getAttribute("barco_Season").toString();
                }
            }
            

            if (currency != "")
            {
                currency = currency + ";" + lineitem.getPrice().getCurrencyMnemonic();
            }
            else
            {
                currency = lineitem.getPrice().getCurrencyMnemonic();
            }

            if (price != "")
            {
                price = price + ";" + lineitem.getProductBO().getPrice().getValue().toString();
            }
            else
            {
                price = lineitem.getProductBO().getPrice().getValue().toString();
            }

            if (quantity != "")
            {
                quantity = quantity + ";" + lineitem.getQuantity().getValue().toString();
            }
            else
            {
                quantity = lineitem.getQuantity().getValue().toString();
            }
            
            if (shippingID == "")
            {
                
                shippingID = order.getCommonShipToAddressBO().getAddressName();
                
            }
       
        }
        orderdata.add("01");
        String custNum = null;
        if (order.getCustomerBO() != null)
        {
            custNum = order.getCustomerBO().getCustomerNo();
        }
        orderdata.add(custNum);
        //For Now we only have single shipping address so for export keeping storeID & Ship ID as one. 
        //In Case of multiple shipping refer shippingID on Line 147
        if (!shippingID.equals("DoorShipAddress"))
        {
            if (order.isMultipleShipmentsSupported())
            {

                int sIndex = shippingID.indexOf("_");
                if (sIndex != -1)
                {
                    shippingID = shippingID.substring(sIndex + 1, shippingID.length());
                }
                else
                {
                    //Default invoice set shipping ID to empty string
                    sIndex = shippingID.indexOf("DefaultInvoice");
                    shippingID = "";
                }

                orderdata.add(shippingID);

            }
            else
            {
                String addrNm = order.getCommonShipToAddressBO().getAddressName();
                int aIndex = addrNm.indexOf("_");
                if (aIndex != -1)
                {
                    addrNm = addrNm.substring(aIndex + 1, addrNm.length());
                }
                orderdata.add(addrNm);
            }
        }
        else {
            orderdata.add("");
            dropShip=true;
        }
        orderdata.add("10");
        orderdata.add(order.getDocumentNo());
        orderdata.add(calculatedate(new Date())); //current Date
        orderdata.add(paymentType);
        orderdata.add(upcCode);
        orderdata.add(season);
        orderdata.add(currency);
        orderdata.add(price);
        orderdata.add(quantity);
        orderdata.add(getShippingMethod(order));
        orderdata.add(order.getBuyerFirstName()+" "+order.getBuyerLastName());
        if (!shippingID.equals("DoorShipAddress"))
        {
            if (order.isMultipleShipmentsSupported())
            {
                orderdata.add(shippingID);
            }
            else
            {
                String addrNm = order.getCommonShipToAddressBO().getAddressName();
                int aIndex = addrNm.indexOf("_");
                if (aIndex != -1)
                {
                    addrNm = addrNm.substring(aIndex + 1, addrNm.length());
                }
                else
                {
                    //Default invoice set shipping ID to empty string
                    aIndex = addrNm.indexOf("DefaultInvoice");
                    addrNm = "";
                }

                orderdata.add(addrNm);
            }
        }
        else
        {
            orderdata.add("");
        }
        orderdata.add(order.getInvoiceToAddressBO().getFirstName()+" "+order.getInvoiceToAddressBO().getLastName());
		
		String invAddrNm = order.getInvoiceToAddressBO().getAddressName();
		int iIndex = invAddrNm.indexOf("_");
		if(iIndex != -1)
		{
			invAddrNm = invAddrNm.substring(iIndex+1, invAddrNm.length());
		}
		else 
		{
		    //Default invoice set shipping ID to empty string
			iIndex = invAddrNm.indexOf("DefaultInvoice");
			invAddrNm = "";
		}
			
        orderdata.add(invAddrNm);
        
        if(order.getCustomAttributes().getAttribute("barco_po")!=null){
            orderdata.add(order.getCustomAttributes().getAttribute("barco_po").toString());
        }
        else {
            orderdata.add("");
        }
        orderdata.add(calculatedate(order.getCreationDate()));
        if (order.getCustomAttributes().getAttribute("barco_ordertype") != null)
        {
            String oType = (String) order.getCustomAttributes().getAttribute("barco_ordertype");
            String oTypeCode = null;
            
            if (oType.equalsIgnoreCase("Group"))
            {
                oTypeCode = "G";
            }
            else if (oType.equalsIgnoreCase("Hospital"))
            {
                oTypeCode = "H";
            }
            else if (oType.equalsIgnoreCase("At Once"))
            {
                oTypeCode = "A";
            }
            else if (oType.equalsIgnoreCase("Replenishment"))
            {
                oTypeCode = "F";
            }
            else
            {
                oTypeCode = oType;
            }
            
            orderdata.add(oTypeCode);
        }
        else
        {
            orderdata.add("");
        }
        if (order.getCustomAttributes().getAttribute("BusinessObjectAttributes#Order_MessageToMerchant") != null)
        {
            orderdata.add(order.getCustomAttributes().getAttribute("BusinessObjectAttributes#Order_MessageToMerchant").toString());
        }
        else
        {
            orderdata.add("");
        }
        if(dropShip){
            orderdata.add("DSHP");
            orderdata.add(order.getCommonShipToAddressBO().getAddressLine1());
            orderdata.add(order.getCommonShipToAddressBO().getAddressLine2());
            orderdata.add(order.getCommonShipToAddressBO().getAddressLine3());
            orderdata.add(order.getCommonShipToAddressBO().getPostalCode());
            orderdata.add(order.getCommonShipToAddressBO().getState());
            orderdata.add(order.getCommonShipToAddressBO().getCountryCode());
       }
        else {
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
        }
        OrderBOPaymentExtensionImpl pb = (OrderBOPaymentExtensionImpl)order.getExtension("Payment");
        PaymentBO ptb = pb.getPaymentBOs().iterator().next();
        Collection<PaymentHistoryEntryBO> pth=ptb.getPaymentTransaction().getPaymentHistoryEntryBOs("Authorize");
        if(!pth.isEmpty()) {
        PaymentHistoryEntryBO ph=pth.iterator().next();
        String requestId=ptb.getPaymentTransactionBO().getServiceTransactionID();
        String authToken=(String)ph.getLoggedData().get("AuthorizationToken");
        String authTransaction=(String)ph.getLoggedData().get("AuthTransNo");
        String authResult=(String)ph.getLoggedData().get("AuthResult");
        String authReasoncode=(String)ph.getLoggedData().get("AuthReasonCode");
        String authAmount=(String)ph.getLoggedData().get("AuthAmount");
        String authCode=(String)ph.getLoggedData().get("AuthCode");
        String authDate=(String)ph.getLoggedData().get("AuthDate");
        String authCVCode=(String)ph.getLoggedData().get("AuthCVCode");
        String authAVS=(String)ph.getLoggedData().get("AuthAVS");
        orderdata.add(requestId);
        orderdata.add(authToken);
        orderdata.add(authTransaction);
        orderdata.add(authResult);
        orderdata.add(authReasoncode);
        orderdata.add(authAmount);
        orderdata.add(authCode);
        orderdata.add(authDate);
        orderdata.add(authAVS);
        orderdata.add(authCVCode);
        orderdata.add(order.getInvoiceToAddressBO().getFirstName());
        orderdata.add(order.getInvoiceToAddressBO().getLastName());
        orderdata.add(order.getInvoiceToAddressBO().getAddressLine1());
        orderdata.add(order.getInvoiceToAddressBO().getAddressLine2());
        orderdata.add(order.getInvoiceToAddressBO().getAddressLine3());
        orderdata.add(order.getInvoiceToAddressBO().getPostalCode());
        orderdata.add(order.getInvoiceToAddressBO().getState());
        orderdata.add(order.getInvoiceToAddressBO().getCountry());
        orderdata.add(order.getInvoiceToAddressBO().getEMail());
        orderdata.add(order.getInvoiceToAddressBO().getPhoneHome());
        }
        else
        {
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
        }
        
        
        if (dropShip)
        {
            orderdata.add(order.getCommonShipToAddressBO().getFirstName());
            orderdata.add(order.getCommonShipToAddressBO().getCity());
            orderdata.add(order.getCommonShipToAddressBO().getPhoneHome());
        }
        else {
            orderdata.add("");
            orderdata.add("");
            orderdata.add("");
        }
    }
    private String calculatePaymentType(OrderBO order)
    {
        String paymentType = null;
        OrderBOPaymentExtensionImpl pb = (OrderBOPaymentExtensionImpl)order.getExtension("Payment");
        PaymentBO ptb = pb.getPaymentBOs().iterator().next();
        paymentType = ptb.getPaymentServiceBO().getPaymentServiceConfigurationID();
        
        if (paymentType.equals("ISH_INVOICE"))
            return "PO";
        else if(paymentType.equals("CC_ON_FILE"))
            return "CRC";
        else if(paymentType.equals("CYBERSOURCE_CREDITCARD"))
            return "CRC";
        else
            return "";
    }

    private String getShippingMethod(OrderBO order)
    {
        OrderBOShippingBucketExtensionImpl shipExt = (OrderBOShippingBucketExtensionImpl) order.getExtension("ShippingBucket");
        Iterator itr = shipExt.getShippingBucketBOs().iterator();
        OrderShippingBucketBO osbBO = (OrderShippingBucketBO) itr.next();
        return osbBO.getSelectedShippingMethod().getDescription();
        
    }
    
    private String calculatedate(Date creationDate)
    {
        int year=creationDate.getYear()+1900;
        int date=creationDate.getDate();
        int month=creationDate.getMonth()+1;
        String datestring=String.valueOf(year)+"-"+String.valueOf(month)+"-"+String.valueOf(date);
        return datestring;
    }
    
}
