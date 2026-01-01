package com.example.scanidcard;

import java.io.Serializable;

/**
 * 身份证识别结果（从腾讯云 OCR 的 Response 节点中解析出来）
 */
public class IdentifyResult implements Serializable {

    // 0 表示成功；非 0 表示失败（本项目自定义）
    private int errorcode;
    private String errormsg;

    // 身份证正面字段
    private String name;
    private String sex;
    private String nation;
    private String birth;
    private String address;
    private String idNum;

    // 身份证反面字段
    private String authority;
    private String validDate;

    // 其他信息
    private String requestId;
    private String advancedInfo;
    private String rawJson;

    public int getErrorcode() {
        return errorcode;
    }

    public void setErrorcode(int errorcode) {
        this.errorcode = errorcode;
    }

    public String getErrormsg() {
        return errormsg;
    }

    public void setErrormsg(String errormsg) {
        this.errormsg = errormsg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public String getBirth() {
        return birth;
    }

    public void setBirth(String birth) {
        this.birth = birth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIdNum() {
        return idNum;
    }

    public void setIdNum(String idNum) {
        this.idNum = idNum;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getValidDate() {
        return validDate;
    }

    public void setValidDate(String validDate) {
        this.validDate = validDate;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getAdvancedInfo() {
        return advancedInfo;
    }

    public void setAdvancedInfo(String advancedInfo) {
        this.advancedInfo = advancedInfo;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }
}
