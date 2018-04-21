package cn.bupt.zcc.registry;

/**
 *实现服务发现的功能
 * Created by 张城城 on 2018/4/21.
 */
public interface ServiceDiscovery {
    /**
     *根据名称查找服务注册中心相应的服务名称
     * @param serviceName 服务名称
     * @return
     */
    public String discover(String serviceName);
}
