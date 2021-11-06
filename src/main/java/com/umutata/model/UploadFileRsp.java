package com.umutata.model;

import java.util.List;

public class UploadFileRsp {
    public String getIfSuccess() {
        return ifSuccess;
    }

    public void setIfSuccess(String ifSuccess) {
        this.ifSuccess = ifSuccess;
    }

    String ifSuccess;

    public List<FileInfo> getFileInfoList() {
        return fileInfoList;
    }

    public void setFileInfoList(List<FileInfo> fileInfoList) {
        this.fileInfoList = fileInfoList;
    }

    List<FileInfo> fileInfoList;
}
