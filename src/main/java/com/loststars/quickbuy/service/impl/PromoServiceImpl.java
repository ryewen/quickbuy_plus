package com.loststars.quickbuy.service.impl;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.loststars.quickbuy.dao.PromoDOMapper;
import com.loststars.quickbuy.dataobject.PromoDO;
import com.loststars.quickbuy.error.BusinessException;
import com.loststars.quickbuy.error.EmBusinessError;
import com.loststars.quickbuy.service.ItemService;
import com.loststars.quickbuy.service.PromoService;
import com.loststars.quickbuy.service.UserService;
import com.loststars.quickbuy.service.model.ItemModel;
import com.loststars.quickbuy.service.model.PromoModel;
import com.loststars.quickbuy.service.model.UserModel;

@Service
public class PromoServiceImpl implements PromoService {
    
    @Autowired
    private PromoDOMapper promoDOMapper;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ItemService itemService;
    
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) throws BusinessException {
        if (itemId == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        if (promoDO == null) return null;
        PromoModel promoModel = convertFromDO(promoDO);
        return promoModel;
    }
    
    private PromoModel convertFromDO(PromoDO promoDO) {
        if (promoDO == null) return null;
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        return promoModel;
    }

    @Override
    public String genenatePromoToken(Integer userId, Integer itemId, Integer promoId) throws BusinessException {
        if (userId == null || itemId == null || promoId == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        if (StringUtils.equals((String) redisTemplate.opsForValue().get("Stock_Empty_" + itemId), "true")) {
            return null;
        }
        Long result = redisTemplate.opsForValue().increment("Promo_Door_Count_" + promoId,-1);
        if (result < 0) {
            return null;
        }
        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel == null) return null;
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) return null;
        if (redisTemplate.hasKey("Stock_Empty_" + itemModel.getId())) throw new BusinessException(EmBusinessError.UNKNOW_ERROR, "商品已售罄");
        PromoModel promoModel = itemModel.getPromoModel();
        if (promoModel == null) return null;
        if (promoModel.getId() == promoId && promoModel.getStatus() == PromoModel.STATUS_NOW) {
            String promoToken = UUID.randomUUID().toString().replace("-", "");
            return promoToken;
        }
        return null;
    }

    @Override
    public void publishPromo(Integer promoId) throws BusinessException {
        if (promoId == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        Integer stock = itemService.getStock(promoDO.getItemId());
        if (stock == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        redisTemplate.opsForValue().set("Promo_Door_Count_" + promoId, stock * 5);
    }
}
