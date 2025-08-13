package leotech.cdp.model.asset;

import java.util.Date;

public class Voucher {

	String id;
	Date expiredDate;
	float promoPercentage = 0f;

	public Voucher() {
		// TODO Auto-generated constructor stub
	}

	public Voucher(String id, Date expiredDate, float promoPercentage) {
		super();
		this.id = id;
		this.expiredDate = expiredDate;
		this.promoPercentage = promoPercentage;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getExpiredDate() {
		return expiredDate;
	}

	public void setExpiredDate(Date expiredDate) {
		this.expiredDate = expiredDate;
	}

	public float getPromoPercentage() {
		return promoPercentage;
	}

	public void setPromoPercentage(float promoPercentage) {
		this.promoPercentage = promoPercentage;
	}

	@Override
	public int hashCode() {
		if (id != null) {
			return id.hashCode();
		}
		return 0;
	}
}
