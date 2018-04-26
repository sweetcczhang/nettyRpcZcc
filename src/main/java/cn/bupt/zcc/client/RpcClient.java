package cn.bupt.zcc.client;

import cn.bupt.zcc.registry.ServiceDiscovery;

import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by 张城城 on 2018/4/21.
 */
public class RpcClient {

    private String serverAddress; //服务地址

    private ServiceDiscovery serviceDiscovery; //服务发现

    private  static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16,16,600L, TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(65536));

    public RpcClient(String serverAddress){
        this.serverAddress =serverAddress;
    }

    public RpcClient(ServiceDiscovery serviceDiscovery){
        this.serviceDiscovery=serviceDiscovery;
    }

    public static <T> T createProxy(Class<T> interfaceClass){
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T>(interfaceClass));
    }

    public static void submit(Runnable task){
        threadPoolExecutor.submit(task);
    }

    public void stop(){
        threadPoolExecutor.shutdown();
    }



}
