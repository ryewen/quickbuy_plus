package com.loststars.quickbuy.service;

import com.loststars.quickbuy.error.BusinessException;
import com.loststars.quickbuy.service.model.PromoModel;

public interface PromoService {

    public PromoModel getPromoByItemId(Integer itemId) throws BusinessException;
    
    public String genenatePromoToken(Integer userId, Integer itemId, Integer promoId) throws BusinessException;
    
    public void publishPromo(Integer promoId) throws BusinessException;
}
