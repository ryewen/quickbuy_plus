package com.loststars.quickbuy.error;

public class BusinessException extends Exception implements CommonError {
    
    private EmBusinessError businessError;
    
    public BusinessException(EmBusinessError businessError) {
        this.businessError = businessError;
    }
    
    public BusinessException(EmBusinessError businessError, String errMsg) {
        this.businessError = businessError;
        businessError.setErrMsg(errMsg);
    }

    @Override
    public Integer getErrCode() {
        return businessError.getErrCode();
    }

    @Override
    public String getErrMsg() {
        return businessError.getErrMsg();
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.businessError.setErrMsg(errMsg);
        return this;
    }

}
