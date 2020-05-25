package com.loststars.quickbuy.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loststars.quickbuy.dao.ItemDOMapper;
import com.loststars.quickbuy.dao.ItemStockDOMapper;
import com.loststars.quickbuy.dataobject.ItemDO;
import com.loststars.quickbuy.dataobject.ItemStockDO;
import com.loststars.quickbuy.error.BusinessException;
import com.loststars.quickbuy.error.EmBusinessError;
import com.loststars.quickbuy.service.ItemService;
import com.loststars.quickbuy.service.PromoService;
import com.loststars.quickbuy.service.model.ItemModel;
import com.loststars.quickbuy.service.model.PromoModel;
import com.loststars.quickbuy.validator.ValidationResult;
import com.loststars.quickbuy.validator.ValidatorImpl;

@Service
public class ItemServiceImpl implements ItemService {
    
    @Autowired
    private ValidatorImpl validatorImpl;
    
    @Autowired
    private ItemDOMapper itemDOMapper;
    
    @Autowired
    private ItemStockDOMapper itemStockDOMapper;
    
    @Autowired
    private PromoService promoService;
    
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        ValidationResult validationResult = validatorImpl.validate(itemModel);
        if (validationResult.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, validationResult.getErrMsg());
        }
        ItemDO itemDO = convertFromModel(itemModel);
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());
        ItemStockDO itemStockDO = convertStockFromModel(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);
        return itemModel;
    }

    private ItemDO convertFromModel(ItemModel itemModel) {
        if (itemModel == null) return null;
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel, itemDO);
        return itemDO;
    }
    
    private ItemStockDO convertStockFromModel(ItemModel itemModel) {
        if (itemModel == null) return null;
        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());
        return itemStockDO;
    }

    @Override
    public ItemModel getItemById(Integer id) throws BusinessException {
        if (id == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if (itemDO == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
        ItemModel itemModel = convertFromDO(itemDO, itemStockDO);
        PromoModel promoModel = promoService.getPromoByItemId(id);
        if (promoModel != null && promoModel.getStatus().intValue() != PromoModel.STATUS_END) {
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }
    
    private ItemModel convertFromDO(ItemDO itemDO, ItemStockDO itemStockDO) {
        if (itemDO == null || itemStockDO == null) return null;
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setStock(itemStockDO.getStock());
        return itemModel;
    }

    @Override
    public List<ItemModel> listItems() {
        String key = "Items";
        List<ItemModel> itemModels = (List<ItemModel>) redisTemplate.opsForValue().get(key);
        if (itemModels == null) {
            List<ItemDO> itemDOs = itemDOMapper.listItems();
            itemModels = itemDOs.stream().map((itemDO)->{
                ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
                ItemModel itemModel = convertFromDO(itemDO, itemStockDO);
                return itemModel;
            }).collect(Collectors.toList());
            redisTemplate.opsForValue().set(key, itemModels);
            redisTemplate.expire(key, 30, TimeUnit.MINUTES);
        }
        return itemModels;
    }

    @Override
    //@Transactional(rollbackFor = Exception.class)
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        if (itemId == null || amount == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        //int affectedRow = itemStockDOMapper.updateStock(itemId, amount);
        //if (affectedRow == 0) throw new BusinessException(EmBusinessError.ITEM_STOCK_LIMIT);
        Long result = redisTemplate.opsForValue().increment("Stock_" + itemId, amount * -1);
        if (result >= 0) {
//            if (mqProducer.asyncReduceStock(itemId, amount)) {
//                return true;
//            } else {
//                redisTemplate.opsForValue().increment("Stock_" + itemId, amount);
//                return false;
//            }
            if (result == 0) {
                redisTemplate.opsForValue().set("Stock_Empty_" + itemId, "true");
            }
            return true;
        } else {
            redisTemplate.opsForValue().increment("Stock_" + itemId, amount);
            return false;
        }
    }

    @Override
    //@Transactional(rollbackFor = Exception.class)
    public boolean increaseSales(Integer itemId, Integer amount) throws BusinessException {
        if (itemId == null || amount == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        //int affectedRow = itemDOMapper.updateSales(itemId, amount);
        //if (affectedRow == 0) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        redisTemplate.opsForValue().increment("Sales_" + itemId, amount);
//        if (mqProducer.asyncIncreaseSales(itemId, amount)) {
//            return true;
//        } else {
//            redisTemplate.opsForValue().increment("Sales_" + itemId, amount * -1);
//            return false;
//        }
        return true;
    }

    @Override
    public ItemModel getItemByIdInCache(Integer id) throws BusinessException {
        String key = "ItemModel_" + id;
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get(key);
        if (itemModel == null) {
            itemModel = getItemById(id);
            redisTemplate.opsForValue().set(key, itemModel);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
            redisTemplate.opsForValue().set("Stock_" + id, itemModel.getStock());
            redisTemplate.expire("Stock_" + id, 1, TimeUnit.HOURS);
            redisTemplate.opsForValue().set("Sales_" + id, itemModel.getSales());
            redisTemplate.expire("Sales_" + id, 1, TimeUnit.HOURS);
        } else {
            itemModel.setStock(getStock(id));
            itemModel.setSales(getSales(id));
        }
        return itemModel;
    }

    @Override
    public Integer getStock(Integer itemId) {
        String key = "Stock_" + itemId;
        Integer stock = (Integer) redisTemplate.opsForValue().get(key);
        if (stock == null) {
            stock = itemStockDOMapper.selectByItemId(itemId).getStock();
            if (stock == null) return null;
            redisTemplate.opsForValue().set(key, stock);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        }
        return stock;
    }

    @Override
    public Integer getSales(Integer itemId) {
        String key = "Sales_" + itemId;
        Integer sales = (Integer) redisTemplate.opsForValue().get(key);
        if (sales == null) {
            sales = itemDOMapper.selectByPrimaryKey(itemId).getSales();
            if (sales == null) return null;
            redisTemplate.opsForValue().set(key, sales);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        }
        return sales;
    }
}
