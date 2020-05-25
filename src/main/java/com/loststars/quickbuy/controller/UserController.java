package com.loststars.quickbuy.controller;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.loststars.quickbuy.controller.viewobject.UserVO;
import com.loststars.quickbuy.error.BusinessException;
import com.loststars.quickbuy.error.EmBusinessError;
import com.loststars.quickbuy.response.CommonReturnType;
import com.loststars.quickbuy.service.UserService;
import com.loststars.quickbuy.service.model.UserModel;

import sun.misc.BASE64Encoder;

@RestController
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class UserController extends BaseController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @GetMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam("id") Integer id) throws Exception {
        UserModel userModel = userService.getUserById(id);
        if (userModel == null) throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        return CommonReturnType.createSuccess(convertFromModel(userModel));
    }

    @PostMapping(value = "/getotp")
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam("telphone") String telphone, HttpServletRequest request) {
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 10000;
        String otpCode = String.valueOf(randomInt);
        request.getSession().setAttribute(telphone, otpCode);
        System.out.println("telphone=" + telphone + "&otpCode=" + otpCode);
        return CommonReturnType.createSuccess(null);
    }
    
    @PostMapping(value = "/register")
    @ResponseBody
    public CommonReturnType register(@RequestParam("telphone") String telphone, @RequestParam("otpCode") String otpCode,
                                     @RequestParam("password") String password, @RequestParam("age") Integer age,
                                     @RequestParam("gender") Byte gender, @RequestParam("name") String name,
                                     HttpServletRequest request) throws BusinessException, NoSuchAlgorithmException, UnsupportedEncodingException {
        if (otpCode == null || telphone == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        String optCodeInSession = (String) request.getSession().getAttribute(telphone);
        if (! StringUtils.equals(optCodeInSession, otpCode)) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "验证码错误");
        UserModel userModel = new UserModel();
        userModel.setTelphone(telphone);
        if (password == null) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        userModel.setEncrptPassword(encodeByMD5(password));
        userModel.setAge(age);
        userModel.setGender(gender);
        userModel.setName(name);
        userService.registerUser(userModel);
        return CommonReturnType.createSuccess(null);
    }
    
    @PostMapping(value = "/login")
    @ResponseBody
    public CommonReturnType login(@RequestParam("telphone") String telphone, @RequestParam("password") String password) throws BusinessException, NoSuchAlgorithmException, UnsupportedEncodingException {
        if (StringUtils.isEmpty(telphone) || StringUtils.isEmpty(password)) throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        UserModel userModel = userService.validateLogin(telphone, encodeByMD5(password));
        //HttpSession session = request.getSession();
        String uuidToken = UUID.randomUUID().toString().replace("-", "");
        String key = uuidToken + "-UserModel";
        redisTemplate.opsForValue().set(key, userModel);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
        //session.setAttribute("IS_LOGIN", true);
        //session.setAttribute("LOGIN_USER", userModel);
        return CommonReturnType.createSuccess(uuidToken);
    }
    
    public String encodeByMD5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (str == null) return null;
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();
        String newStr = base64Encoder.encode(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }
    
    private UserVO convertFromModel(UserModel userModel) {
        if (userModel == null) return null;
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }
}
