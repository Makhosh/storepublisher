package com.umutata.model;

public class result
{
    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public UploadFileRsp getUploadFileRsp() {
        return UploadFileRsp;
    }

    public void setUploadFileRsp(UploadFileRsp uploadFileRsp) {
        this.UploadFileRsp = uploadFileRsp;
    }

    private String resultCode;

    private UploadFileRsp UploadFileRsp;
}
