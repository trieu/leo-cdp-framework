package leotech.cdp.model.analytics;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.asset.ProductItem;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.XssFilterUtil;
import rfx.core.util.StringUtil;

/**
 *  Order Transaction for e-commerce, retail
 * 
 * @author tantrieuf31
 * @since 2024
 *
 */
public class OrderTransaction {
	String transactionId = "";
	double totalTransactionValue = 0, transactionDiscount = 0;
	String currencyCode = ProductItem.USD, paymentInfo = "", transactionStatus = "";
	
	double tax = 0, shippingValue = 0; ; 
	Map<String, Object> shippingInfo = new HashMap<String, Object>();
	
	Set<OrderedItem> orderedItems;
	Set<ServiceItem> serviceItems;

	/**
	 * @param createdAt
	 * @param formData
	 */
	public OrderTransaction(Date createdAt, MultiMap formData) {
		this.transactionId = HttpWebParamUtil.getString(formData, HttpParamKey.TRANSACTION_ID);
		this.transactionStatus = HttpWebParamUtil.getString(formData, HttpParamKey.TRANSACTION_STATUS, "");
		this.totalTransactionValue = HttpWebParamUtil.getDouble(formData, HttpParamKey.TRANSACTION_VALUE, 0);
		this.currencyCode = HttpWebParamUtil.getString(formData, HttpParamKey.TRANSACTION_CURRENCY, ProductItem.USD);
		setOrderedItems(createdAt, formData);
	}

	/**
	 * @param createdAt
	 * @param jsonData
	 */
	public OrderTransaction(Date createdAt, JsonObject jsonData, boolean computeTotalTransactionValue ) {
		this.transactionId = XssFilterUtil.safeGet(jsonData, HttpParamKey.TRANSACTION_ID);
		this.transactionStatus = XssFilterUtil.safeGet(jsonData, HttpParamKey.TRANSACTION_STATUS, "");
		this.totalTransactionValue = jsonData.getDouble(HttpParamKey.TRANSACTION_VALUE, 0d);
		this.transactionDiscount = jsonData.getDouble(HttpParamKey.TRANSACTION_DISCOUNT, 0d);
		this.currencyCode = XssFilterUtil.safeGet(jsonData, HttpParamKey.TRANSACTION_CURRENCY, ProductItem.USD);
		this.paymentInfo = XssFilterUtil.safeGet(jsonData, HttpParamKey.TRANSACTION_PAYMENT);
		
		this.tax = jsonData.getDouble(HttpParamKey.TRANSACTION_TAX, 0d);
		this.shippingValue = jsonData.getDouble(HttpParamKey.TRANSACTION_SHIPPING_VALUE, 0d);
		this.shippingInfo =   HttpWebParamUtil.getMapFromRequestParams(jsonData,HttpParamKey.TRANSACTION_SHIPPING_INFO); 
		
		// physical products
		setOrderedItems(createdAt, jsonData, computeTotalTransactionValue);
		
		// service items like Spa, Course, Training
		setServiceItems(createdAt, jsonData, computeTotalTransactionValue);
	}

	public String getTransactionId() {
		return transactionId;
	}
	
	public String getTransactionStatus() {
		return transactionStatus;
	}

	public double getTotalTransactionValue() {
		return totalTransactionValue;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public double getTransactionDiscount() {
		return transactionDiscount;
	}

	public String getPaymentInfo() {
		return paymentInfo;
	}
	
	public double getTax() {
		return tax;
	}
	
	public double getShippingValue() {
		return shippingValue;
	}

	public Map<String, Object> getShippingInfo() {
		return shippingInfo;
	}

	public Set<OrderedItem> getOrderedItems() {
		return orderedItems != null ? orderedItems : new HashSet<OrderedItem>(0);
	}
	
	

	public Set<ServiceItem> getServiceItems() {
		return serviceItems;
	}

	/**
	 * get purchased items from MultiMap
	 * 
	 * @param params
	 * @return
	 */
	public final void setOrderedItems(Date createdAt, MultiMap formData) {
		try {
			String jsonStr = StringUtil.decodeUrlUTF8(formData.get(HttpParamKey.SHOPPING_CART_ITEMS));
			if (StringUtil.isNotEmpty(jsonStr)) {
				JsonArray list = new JsonArray(jsonStr);
				if (list != null) {
					int size = list.size();
					double value = 0;
					Set<OrderedItem> items = new HashSet<>(size);
					for (int i = 0; i < size; i++) {
						JsonObject obj = list.getJsonObject(i);
						try {
							OrderedItem item = new OrderedItem(createdAt, obj);
							items.add(item);
							value += item.getTransactionValue();
						} catch (Exception e) {
							e.printStackTrace();
							System.err.println("getShoppingItems, get error on JsonObject \n " + obj);
						}
					}
					this.orderedItems = items;
					if(this.totalTransactionValue == 0 && value != 0) {
						this.totalTransactionValue = value;
					}
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * get purchased items from MultiMap
	 * 
	 * @param params
	 * @return
	 */
	public final void setOrderedItems(Date createdAt, JsonObject formData, boolean computeTotalTransactionValue) {
		JsonArray list = formData.getJsonArray(HttpParamKey.SHOPPING_CART_ITEMS);
		if (list != null) {
			int size = list.size();
			double value = 0;
			Set<OrderedItem> items = new HashSet<>(size);
			for (int i = 0; i < size; i++) {
				JsonObject obj = list.getJsonObject(i);
				OrderedItem item = new OrderedItem(createdAt, obj);
				if(item.isValid()) {
					items.add(item);
					value += item.getTransactionValue();
				}
			}
			this.orderedItems = items;
			if(computeTotalTransactionValue && this.totalTransactionValue == 0 && value != 0) {
				this.totalTransactionValue = value;
			}
		}
	}
	
	
	/**
	 * @param createdAt
	 * @param formData
	 * @param computeTotalTransactionValue
	 */
	public final void setServiceItems(Date createdAt, JsonObject formData, boolean computeTotalTransactionValue) {
		JsonArray list = formData.getJsonArray(HttpParamKey.SERVICE_ITEMS);
		if (list != null) {
			int size = list.size();
			double value = 0;
			Set<ServiceItem> items = new HashSet<>(size);
			for (int i = 0; i < size; i++) {
				JsonObject obj = list.getJsonObject(i);
				ServiceItem item = new ServiceItem(createdAt, obj);
				if(item.isValid()) {
					items.add(item);
					value += item.getTransactionValue();
				}
			}
			this.serviceItems = items;
			if(computeTotalTransactionValue && this.totalTransactionValue == 0 && value != 0) {
				this.totalTransactionValue = value;
			}
		}
	}

	/**
	 * @param jsonStr
	 * @return
	 */
	public static Set<OrderedItem> parseJsonToGetOrderedItems(String jsonStr) {
		Set<OrderedItem> shoppingItems = null;
		try {
			if (StringUtil.isNotEmpty(jsonStr)) {
				System.out.println("parseJsonToGetOrderedItems " + jsonStr);
				Gson gson = new Gson();
				Type strMapType = new TypeToken<ArrayList<OrderedItem>>() {}.getType();
				List<OrderedItem> items = gson.fromJson(jsonStr, strMapType);
				Predicate<? super OrderedItem> predicate = item -> {
					item.buildKey();
					return StringUtil.isNotEmpty(item.getItemId());
				};
				Set<OrderedItem> itemSet = items.stream().filter(predicate).collect(Collectors.toSet());
				shoppingItems = itemSet;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return shoppingItems != null ? shoppingItems : new HashSet<OrderedItem>(0);
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
