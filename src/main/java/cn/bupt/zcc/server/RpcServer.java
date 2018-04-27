package cn.bupt.zcc.server;

import cn.bupt.zcc.client.ObjectProxy;
import cn.bupt.zcc.common.RpcDecoder;
import cn.bupt.zcc.common.RpcEncoder;
import cn.bupt.zcc.common.RpcRequest;
import cn.bupt.zcc.common.RpcResponse;
import cn.bupt.zcc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by 张城城 on 2018/4/27.
 */
public class RpcServer implements ApplicationContextAware,InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    private ServiceRegistry serviceRegistry;

    private String serviceAddress;

    private Map<String,Object> handlerMap = new HashMap<>();

    private volatile static ThreadPoolExecutor threadPoolExecutor;

    public RpcServer(String serviceAddress){
        this.serviceAddress =serviceAddress;
    }

    public RpcServer(String serviceAddress, ServiceRegistry serviceRegistry){
        this.serviceAddress = serviceAddress;
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)){
            for (Object serviceBean : serviceBeanMap.values()){
                String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
                handlerMap.put(interfaceName,serviceBean);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup,workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(65536,0,4,0,0))
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    .addLast(new RpcEncoder(RpcResponse.class))
                                    .addLast(new RpcServerHandler(handlerMap));
                        }
                    }).option(ChannelOption.SO_BACKLOG,128)
            .childOption(ChannelOption.SO_KEEPALIVE,true);

            String[] array = serviceAddress.split(":");
            String host = array[0];
            int port = Integer.valueOf(array[1]);
            ChannelFuture future = bootstrap.bind(host,port).sync();
            LOGGER.debug("Service started on port {}",port);
            if (serviceRegistry!=null){
                serviceRegistry.registry(serviceAddress);
            }
            future.channel().closeFuture().sync();
        }finally {

            workGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void submit(Runnable runnable){
        if (threadPoolExecutor==null){
            synchronized (RpcServer.class){
                if (threadPoolExecutor==null){
                    threadPoolExecutor = new ThreadPoolExecutor(16,16,600L, TimeUnit.SECONDS, new LinkedBlockingQueue());
                }
            }
        }
        threadPoolExecutor.submit(runnable);
    }

}
