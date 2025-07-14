package org.example.aiedabackend.vo;

public class Response<T> {
    private String code;
    private String message;
    private T data;

    public Response(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Response<T> buildSuccess(T data) {
        return new Response<>("200", "操作成功", data);
    }

    public static <T> Response<T> buildFailure(String message, String status) {
        return new Response<>(status, message, null);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
