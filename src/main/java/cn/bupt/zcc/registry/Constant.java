package cn.bupt.zcc.registry;

/**
 * Created by 张城城 on 2018/4/21.
 */
public interface Constant {

    int ZK_SESSION_TIMEOUT = 5000;

    String ZK_REGISTRY_PATH = "/registry";

    String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";
}
