package cn.bupt.zcc.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by 张城城 on 2018/4/26.
 */
public class ConnectManage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectManage.class);

    private volatile static ConnectManage connectManage;

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16,16, 600L, TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>());

    private CopyOnWriteArrayList<RpcClientHandler> connctedHandlers = new CopyOnWriteArrayList<>();

    private Map<InetSocketAddress,RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();


    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    protected long connectTimeoutMillis = 6000L;
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private volatile boolean isRuning = true;

    //私有构造方法
    private ConnectManage(){}

    public static ConnectManage getInstance(){

        if (connectManage==null){
            synchronized (ConnectManage.class){
                if (connectManage==null){
                    connectManage =new ConnectManage();
                }
            }
        }
        return connectManage;
    }

    public void  updateConnectServer(List<String> allServerAddress){
        if (allServerAddress!=null){
            if (allServerAddress.size()>0){
                HashSet<InetSocketAddress> newAllServerNodeSet = new HashSet<>();
                for (int i=0;i<allServerAddress.size();i++){
                    String[] array = allServerAddress.get(i).split(":");
                    if(array.length==2){
                        String host = array[0];
                        int port = Integer.valueOf(array[1]);
                        final InetSocketAddress remotePeer = new InetSocketAddress(host,port);
                        newAllServerNodeSet.add(remotePeer);
                    }
                }
                //添加新的可用的服务节点
                for (final InetSocketAddress remotePeer : newAllServerNodeSet){
                    if (!connectedServerNodes.keySet().contains(remotePeer)){
                        connectServerNode(remotePeer);
                    }
                }
                for (int i=0;i<connctedHandlers.size();i++){
                    RpcClientHandler handler =connctedHandlers.get(i);
                    SocketAddress remotePeer = handler.getSocketAddress();
                    if (!newAllServerNodeSet.contains(remotePeer)){
                        RpcClientHandler rpcClientHandler = connectedServerNodes.get(remotePeer);
                        rpcClientHandler.close();
                        connectedServerNodes.remove(remotePeer);
                        connctedHandlers.remove(handler);
                        i--;
                    }
                }
            }else {
                LOGGER.error("No available server node. All server nodes are down !!!");
                for (final RpcClientHandler clientHandler : connctedHandlers){
                    SocketAddress remotePeer = clientHandler.getSocketAddress();
                    RpcClientHandler handler = connectedServerNodes.get(remotePeer);
                    handler.close();
                    connectedServerNodes.remove(clientHandler);
                }
                connctedHandlers.clear();
            }
        }
    }

    private void connectServerNode(final InetSocketAddress remotePeer){
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup).
                        channel(NioSocketChannel.class).
                        handler(new RpcClientInitializer());
                ChannelFuture channelFuture = b.connect(remotePeer);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()){
                            LOGGER.debug("Successfully connect to remote server. remote peer = " + remotePeer);
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            addHandler(handler);
                        }
                    }
                });
            }
        });
    }
    private void addHandler(RpcClientHandler handler){
        connctedHandlers.add(handler);
        InetSocketAddress socketAddress = (InetSocketAddress) handler.getChannel().remoteAddress();
        connectedServerNodes.put(socketAddress,handler);
        signalAvailableHandler();
    }
    private void signalAvailableHandler(){
        lock.lock();
        try {
            connected.signalAll();
        }finally {
            lock.unlock();
        }
    }
    private boolean waitingForHandler() throws InterruptedException{
        lock.lock();
        try {
            return connected.await(this.connectTimeoutMillis,TimeUnit.SECONDS);
        }finally {
            lock.unlock();
        }
    }

    public RpcClientHandler chooseHandler(){
        CopyOnWriteArrayList<RpcClientHandler> handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connctedHandlers.clone();
        int size = handlers.size();
        while (isRuning && size<=0){
            try {
                boolean available = waitingForHandler();
                if (available){
                    handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connctedHandlers.clone();
                    size = handlers.size();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int index = (roundRobin.getAndAdd(1)+size) % size;
        return  handlers.get(index);
    }

}
