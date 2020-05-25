package com.loststars.quickbuy.service;

import java.util.List;

import com.loststars.quickbuy.error.BusinessException;
import com.loststars.quickbuy.service.model.ItemModel;

public interface ItemService {

    public ItemModel createItem(ItemModel itemModel) throws BusinessException;
    
    public ItemModel getItemById(Integer id) throws BusinessException;
    
    public ItemModel getItemByIdInCache(Integer id) throws BusinessException;
    
    public List<ItemModel> listItems();
    
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException;
    
    public boolean increaseSales(Integer itemId, Integer amount) throws BusinessException;
    
    public Integer getStock(Integer itemId);
    
    public Integer getSales(Integer itemId);
}
