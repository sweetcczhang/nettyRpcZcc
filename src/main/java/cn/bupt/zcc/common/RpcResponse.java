package cn.bupt.zcc.common;

/**
 * Created by 张城城 on 2018/4/21.
 */
public class RpcResponse {

    private String requestId;  // 请求id

    private String error;     // 错误描述

    private Object result;    // 响应结果

    public boolean isError(){
        return error!=null;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
