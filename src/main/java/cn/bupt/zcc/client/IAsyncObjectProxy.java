package cn.bupt.zcc.client;

/**
 * Created by 张城城 on 2018/4/26.
 */
public interface IAsyncObjectProxy {
    public RpcFuture call(String funcName,Object... args);
}
