package com.soft.live.util;


import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * 返回结果封装类
 */
//@ApiModel(value = "响应类", description = "返回统一响应数据")
public class ResultMessage {
    /**
     * 定义jackson对象
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 响应业务状态
     */
//    @ApiModelProperty(value = "状态", notes = "响应状态码，成功：200，请求失败：400")
    private Integer status;

    /**
     * 响应消息
     */
//    @ApiModelProperty(value = "消息", notes = "响应消息，成功：ok")
    private String message;

    /**
     * 响应中的数据
     */
//    @ApiModelProperty(value = "数据", notes = "响应的数据结果集")
    private Object data;


    /**
     * 封装返回结果 携带结果
     *
     * @param status
     * @param message
     * @param data
     * @return
     */
    public static ResultMessage build(Integer status, String message, Object data) {
        return new ResultMessage(status, message, data);
    }

    public static ResultMessage ok(Object data) {
        return new ResultMessage(data);
    }

    public static ResultMessage ok() {
        return new ResultMessage(null);
    }

    public ResultMessage() {

    }

    public static ResultMessage build(Integer status, String message) {
        return new ResultMessage(status, message, null);
    }

    public ResultMessage(Integer status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public ResultMessage(Object data) {
        this.status = 200;
        this.message = "OK";
        this.data = data;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Object getdata() {
        return data;
    }

    public void setdata(Object data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
