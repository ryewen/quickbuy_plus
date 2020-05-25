package com.loststars.quickbuy.dataobject;

public class StockLogDO {
    
    public static final int STATUS_INIT = 1;
    
    public static final int STATUS_COMMIT = 2;
    
    public static final int STATUS_ROLLBACK = 3;
    
    private String id;

    private Integer itemId;

    private Integer amount;

    private Integer status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}