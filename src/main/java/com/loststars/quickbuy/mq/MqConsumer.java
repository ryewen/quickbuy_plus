package com.loststars.quickbuy.mq;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.loststars.quickbuy.dao.ItemDOMapper;
import com.loststars.quickbuy.dao.ItemStockDOMapper;

@Component
public class MqConsumer {

    private DefaultMQPushConsumer consumer;
    
    @Value("${mq.nameserver.addr}")
    private String namesrvAddr;
    
    @Value("${mq.topicname.order}")
    private String topicName;
    
    @Autowired
    private ItemDOMapper itemDOMapper;
    
    @Autowired
    private ItemStockDOMapper itemStockDOMapper;
    
    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer("order_consumer_group");
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.subscribe(topicName, "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                try {
                    String jsonStr = new String(msgs.get(0).getBody(), "utf-8");
                    Map<String, Object> map = JSON.parseObject(jsonStr);
                    Integer itemId = (Integer) map.get("itemId");
                    Integer amount = (Integer) map.get("amount");
                    itemStockDOMapper.updateStock(itemId, amount);
                    itemDOMapper.updateSales(itemId, amount);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
    }
}
