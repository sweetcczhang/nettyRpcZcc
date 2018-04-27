package cn.bupt.zcc.client;

import cn.bupt.zcc.common.RpcRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 创建代理类
 * Created by 张城城 on 2018/4/25.
 */
public class ObjectProxy<T> implements InvocationHandler,IAsyncObjectProxy{

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectProxy.class);
    private Class<T> clazz;
    public ObjectProxy(Class<T> clazz){
        this.clazz =clazz;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //如果调用的是Object的方法就直接在本地执行不需要执行远程的方法
        if (Object.class == method.getDeclaringClass()){
            String name = method.getName();
            if ("equals".equals(name)){
                return proxy == args[0];
            }else if ("hashCode".equals(name)){
                return System.identityHashCode(proxy);
            }else if ("toString".equals(name)){
                return proxy.getClass().getName()+"@"+Integer.toHexString(System.identityHashCode(proxy)) +
                        ", with InvocationHandler" + this;
            }else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }
        //构造数据交换对象，发送请求
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterType(method.getParameterTypes());
        request.setParameters(args);
        LOGGER.debug(method.getDeclaringClass().getName());
        LOGGER.debug(method.getName());
        for (int i=0; i<method.getParameterTypes().length;i++){
            LOGGER.debug(method.getParameterTypes()[i].getName());
        }
        for (int i=0; i<args.length;i++){
            LOGGER.debug(args[i].toString());
        }
        RpcClientHandler handler = ConnectManage.getInstance().chooseHandler();
        RpcFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture.get();
    }

    @Override
    public RpcFuture call(String funcName, Object... args) {
        return null;
    }

    private Class<?> getClassType(Object obj){
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName){
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
        }

        return classType;
    }
}
