package com.seckill.result;

import lombok.Data;

@Data
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    // 成功
    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 失败
    public static <T> Result<T> fail() {
        return fail("操作失败");
    }

    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

    // 未登录
    public static <T> Result<T> unauthorized() {
        Result<T> result = new Result<>();
        result.setCode(401);
        result.setMsg("请先登录");
        result.setData(null);
        return result;
    }
}
