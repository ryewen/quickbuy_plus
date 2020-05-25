package com.loststars.quickbuy.service;

import com.loststars.quickbuy.error.BusinessException;
import com.loststars.quickbuy.service.model.UserModel;

public interface UserService {

    public UserModel getUserById(Integer id);
    
    public UserModel getUserByIdInCache(Integer id);
    
    public void registerUser(UserModel userModel) throws BusinessException;
    
    public UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;
}
