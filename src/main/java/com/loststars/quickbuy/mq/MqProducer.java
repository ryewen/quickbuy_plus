package com.loststars.quickbuy.mq;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.loststars.quickbuy.dao.StockLogDOMapper;
import com.loststars.quickbuy.dataobject.StockLogDO;
import com.loststars.quickbuy.error.BusinessException;
import com.loststars.quickbuy.service.OrderService;

@Component
public class MqProducer {
    
    private DefaultMQProducer producer;
    
    private TransactionMQProducer transactionMQProducer;
    
    @Value("${mq.nameserver.addr}")
    private String namesrvAddr;

    @Value("${mq.topicname.order}")
    private String topicName;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private StockLogDOMapper stockLogDOMapper;
    
    @PostConstruct
    public void init() throws MQClientException {
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(namesrvAddr);
        producer.start();
        transactionMQProducer = new TransactionMQProducer("transation_producer_group");
        transactionMQProducer.setNamesrvAddr(namesrvAddr);
        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                Map<String, Object> argMap = (Map<String, Object>) arg;
                try {
                    orderService.createOrder((Integer) argMap.get("userId"), (Integer) argMap.get("itemId"), (Integer) argMap.get("promoId"),
                            (Integer) argMap.get("amount"), (String) argMap.get("stockLogId"));
                } catch (BusinessException e) {
                    e.printStackTrace();
                    StockLogDO stockLogDO = new StockLogDO();
                    stockLogDO.setId((String) argMap.get("stockLogId"));
                    stockLogDO.setStatus(StockLogDO.STATUS_ROLLBACK);
                    stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }
            
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                try {
                    Map<String, Object> map = JSON.parseObject(new String(msg.getBody(), "utf-8"));
                    StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey((String) map.get("stockLogId"));
                    if (stockLogDO == null) return LocalTransactionState.UNKNOW;
                    if (stockLogDO.getStatus() == StockLogDO.STATUS_COMMIT) {
                        return LocalTransactionState.COMMIT_MESSAGE;
                    } else if (stockLogDO.getStatus() == StockLogDO.STATUS_ROLLBACK) {
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    } else {
                        return LocalTransactionState.UNKNOW;
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return LocalTransactionState.UNKNOW;
                }
            }
        });
        transactionMQProducer.start();
    }
    
    public boolean transactionCreateOrderAsyncStockAndSales(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws UnsupportedEncodingException, MQClientException {
        Map<String, Object> argMap = new HashMap<>();
        argMap.put("userId", userId);
        argMap.put("itemId", itemId);
        argMap.put("promoId", promoId);
        argMap.put("amount", amount);
        argMap.put("stockLogId", stockLogId);
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        bodyMap.put("stockLogId", stockLogId);
        Message message = new Message(topicName, "order", JSON.toJSONString(bodyMap).getBytes("utf-8"));
        TransactionSendResult result = transactionMQProducer.sendMessageInTransaction(message, argMap);
        if (result.getLocalTransactionState().equals(LocalTransactionState.COMMIT_MESSAGE))
            return true;
        else
            return false;
    }
}
