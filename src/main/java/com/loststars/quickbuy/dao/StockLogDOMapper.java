package com.loststars.quickbuy.dao;

import com.loststars.quickbuy.dataobject.StockLogDO;
import com.loststars.quickbuy.dataobject.StockLogDOExample;
import java.util.List;

public interface StockLogDOMapper {
    int deleteByPrimaryKey(String id);

    int insert(StockLogDO record);

    int insertSelective(StockLogDO record);

    List<StockLogDO> selectByExample(StockLogDOExample example);

    StockLogDO selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(StockLogDO record);

    int updateByPrimaryKey(StockLogDO record);
}