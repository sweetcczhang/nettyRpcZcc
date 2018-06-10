package cn.bupt.zcc.client;

import cn.bupt.zcc.common.RpcRequest;
import cn.bupt.zcc.common.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by 张城城 on 2018/4/25.
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientHandler.class);

    private ConcurrentHashMap<String,RpcFuture>  pendingRpc = new ConcurrentHashMap<>();

    private volatile Channel channel;

    private SocketAddress socketAddress;

    public Channel getChannel() {
        return channel;
    }

    public SocketAddress getSocketAddress(){
        return socketAddress;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        super.channelActive(ctx);
        this.socketAddress =this.channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse) throws Exception {
        String requestId = rpcResponse.getRequestId();
        RpcFuture rpcFuture = pendingRpc.get(requestId);
        if (rpcFuture!=null){
            pendingRpc.remove(requestId);
            rpcFuture.done(rpcResponse);
        }
    }

    public RpcFuture sendRequest(RpcRequest rpcRequest){
        final CountDownLatch downLatch = new CountDownLatch(1);
        RpcFuture rpcFuture = new RpcFuture(rpcRequest);
        pendingRpc.put(rpcRequest.getRequestId(),rpcFuture);
        channel.writeAndFlush(rpcRequest);
        channel.writeAndFlush(rpcRequest).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                downLatch.countDown();
            }
        });
        try {
            downLatch.await();
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
        return rpcFuture;
    }
    public void close(){
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
}
