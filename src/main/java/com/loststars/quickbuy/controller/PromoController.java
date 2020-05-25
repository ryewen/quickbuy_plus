package com.loststars.quickbuy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.loststars.quickbuy.error.BusinessException;
import com.loststars.quickbuy.response.CommonReturnType;
import com.loststars.quickbuy.service.PromoService;

@RestController
@RequestMapping("/promo")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")
public class PromoController extends BaseController {

    @Autowired
    private PromoService promoService;
    
    @GetMapping("/publishpromo")
    @ResponseBody
    public CommonReturnType publishPromo(@RequestParam("id") Integer id) throws BusinessException {
        promoService.publishPromo(id);
        return CommonReturnType.createSuccess(null);
    }
}
