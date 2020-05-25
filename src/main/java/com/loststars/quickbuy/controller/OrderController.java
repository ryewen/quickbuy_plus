package com.loststars.quickbuy.controller;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import com.google.common.util.concurrent.RateLimiter;
import com.loststars.quickbuy.util.CodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import com.loststars.quickbuy.dao.StockLogDOMapper;
import com.loststars.quickbuy.dataobject.StockLogDO;
import com.loststars.quickbuy.error.BusinessException;
import com.loststars.quickbuy.error.EmBusinessError;
import com.loststars.quickbuy.mq.MqProducer;
import com.loststars.quickbuy.response.CommonReturnType;
import com.loststars.quickbuy.service.PromoService;
import com.loststars.quickbuy.service.model.OrderModel;
import com.loststars.quickbuy.service.model.UserModel;
import sun.misc.BASE64Encoder;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")
public class OrderController extends BaseController {
    
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    
    @Autowired
    private MqProducer producer;
    
    @Autowired
    private StockLogDOMapper stockLogDOMapper;
    
    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(20);
        orderCreateRateLimiter = RateLimiter.create(300);
    }

    @GetMapping(value = "/generateverifycode")
    @ResponseBody
    public CommonReturnType generateCodeAndPic(@RequestParam("token") String token, HttpServletResponse response) throws BusinessException, IOException {
        if (token == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token + "-UserModel");
        if (userModel == null) throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        Map<String, Object> codePicMap = CodeUtil.generateCodeAndPic();
        String code = (String) codePicMap.get("code");
        String key = "Code_" + token;
        redisTemplate.opsForValue().set(key, code);
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
        //ImageIO.write((RenderedImage) codePicMap.get("codePic"), "jpeg", response.getOutputStream());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write((BufferedImage) codePicMap.get("codePic"), "jpeg", byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        BASE64Encoder base64Encoder = new BASE64Encoder();
        String encodedStr = base64Encoder.encode(bytes);
        return CommonReturnType.createSuccess(encodedStr);
    }
    
    @PostMapping(value = "/generatetoken")
    @ResponseBody
    public CommonReturnType generatePromoToken(@RequestParam("itemId") Integer itemId, @RequestParam("promoId") Integer promoId,
            @RequestParam("token") String token, @RequestParam("verifyCode") String verifyCode) throws BusinessException {
        if (itemId == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        if (StringUtils.isEmpty(token)) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token + "-UserModel");
        if (userModel == null) throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        if (promoId == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        if (verifyCode == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        String codeKey = "Code_" + token;
        String codeInRedis = (String) redisTemplate.opsForValue().get(codeKey);
        if (! StringUtils.equals(verifyCode, codeInRedis)) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "验证码错误");
        String promoToken = promoService.genenatePromoToken(userModel.getId(), itemId, promoId);
        if (promoToken == null) throw new BusinessException(EmBusinessError.UNKNOW_ERROR);
        String key = "PromoToken_itemId_" + itemId + "_promoId_" + promoId + "_userId_" + userModel.getId();
        redisTemplate.opsForValue().set(key, promoToken);
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
        return CommonReturnType.createSuccess(promoToken);
    }

    @PostMapping(value = "/createorder")
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam("itemId") Integer itemId, @RequestParam(name = "promoId", required = false) Integer promoId,
            @RequestParam("amount") Integer amount, @RequestParam("token") String token, @RequestParam("promoToken") String promoToken) throws BusinessException, ExecutionException, InterruptedException {
        if (itemId == null || amount == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        if (StringUtils.isEmpty(token)) throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        if (! orderCreateRateLimiter.tryAcquire()) throw new BusinessException(EmBusinessError.UNKNOW_ERROR, "秒杀活动火爆");
        //OrderModel orderModel = new OrderModel();
        String key = token + "-UserModel";
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(key);
        if (userModel == null) throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        if (promoId != null) {
            if (promoToken == null) throw new BusinessException(EmBusinessError.UNKNOW_ERROR, "秒杀令牌不能为空");
            String promoTokenKey = "PromoToken_itemId_" + itemId + "_promoId_" + promoId + "_userId_" + userModel.getId();
            String promoTokenInRedis = (String) redisTemplate.opsForValue().get(promoTokenKey);
            if (promoTokenInRedis == null) throw new BusinessException(EmBusinessError.UNKNOW_ERROR, "未获得秒杀令牌");
            if (! promoToken.equals(promoTokenInRedis)) throw new BusinessException(EmBusinessError.UNKNOW_ERROR, "秒杀令牌错误");
        }
        //orderModel.setItemId(itemId);
        //orderModel.setUserId(userModel.getId());
        //orderModel.setAmount(amount);
        //orderService.createOrder(userModel.getId(), itemId, promoId, amount);
        StockLogDO stockLogDO = new StockLogDO();
        stockLogDO.setId(UUID.randomUUID().toString().replace("-", ""));
        stockLogDO.setItemId(itemId);
        stockLogDO.setAmount(amount);
        stockLogDO.setStatus(StockLogDO.STATUS_INIT);

        Future<Object> future = executorService.submit(() -> {
            stockLogDOMapper.insertSelective(stockLogDO);
            try {
                if (! producer.transactionCreateOrderAsyncStockAndSales(userModel.getId(), itemId, promoId, amount, stockLogDO.getId()))
                    throw new BusinessException(EmBusinessError.UNKNOW_ERROR, "发送异步下单错误");
            } catch (UnsupportedEncodingException | MQClientException e) {
                e.printStackTrace();
                throw new BusinessException(EmBusinessError.UNKNOW_ERROR);
            }
            return null;
        });
        future.get();

//        stockLogDOMapper.insertSelective(stockLogDO);
//        try {
//            if (! producer.transactionCreateOrderAsyncStockAndSales(userModel.getId(), itemId, promoId, amount, stockLogDO.getId()))
//                throw new BusinessException(EmBusinessError.UNKNOW_ERROR, "发送异步下单错误");
//        } catch (UnsupportedEncodingException | MQClientException e) {
//            e.printStackTrace();
//            throw new BusinessException(EmBusinessError.UNKNOW_ERROR);
//        }
        return CommonReturnType.createSuccess(null);
    }
}
