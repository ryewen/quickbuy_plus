package com.loststars.quickbuy.service;

import com.loststars.quickbuy.error.BusinessException;
import com.loststars.quickbuy.service.model.OrderModel;

public interface OrderService {

    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException;
}
