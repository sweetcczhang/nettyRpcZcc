package cn.bupt.zcc.registry;

/**
 * 服务注册中心，实现服务注册
 * Created by 张城城 on 2018/4/21.
 */
public interface ServiceRegistry {

    /**
     *实现服务注册
     * @param serviceName     服务名称
     * @param serviceAddress  服务地址
     */
    public void registry(String serviceName, String serviceAddress);
}
