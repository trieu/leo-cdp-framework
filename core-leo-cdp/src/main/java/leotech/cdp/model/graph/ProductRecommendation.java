package leotech.cdp.model.graph;

import java.util.Date;

import com.google.gson.annotations.Expose;

import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.marketing.TargetMediaUnit;

/**
 * Read-model for graph-based product recommendation
 * Loaded from ArangoDB AQL
 */
public class ProductRecommendation {

    @Expose
    private String productItemKey;

    @Expose
    private ProductItem productItem;

    @Expose
    private String targetMediaUnitId;

    @Expose
    private TargetMediaUnit targetMediaUnit;

    @Expose
    private int indexScore;

    @Expose
    private int eventScore;

    @Expose
    private Date updatedAt;

    // ===== getters / setters =====

    public String getProductItemKey() {
        return productItemKey;
    }

    public void setProductItemKey(String productItemKey) {
        this.productItemKey = productItemKey;
    }

    public ProductItem getProductItem() {
        return productItem;
    }

    public void setProductItem(ProductItem productItem) {
        this.productItem = productItem;
    }

    public String getTargetMediaUnitId() {
        return targetMediaUnitId;
    }

    public void setTargetMediaUnitId(String targetMediaUnitId) {
        this.targetMediaUnitId = targetMediaUnitId;
    }

    public TargetMediaUnit getTargetMediaUnit() {
        return targetMediaUnit;
    }

    public void setTargetMediaUnit(TargetMediaUnit targetMediaUnit) {
        this.targetMediaUnit = targetMediaUnit;
    }

    public int getIndexScore() {
        return indexScore;
    }

    public void setIndexScore(int indexScore) {
        this.indexScore = indexScore;
    }

    public int getEventScore() {
        return eventScore;
    }

    public void setEventScore(int eventScore) {
        this.eventScore = eventScore;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
