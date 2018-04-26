package cn.bupt.zcc.client;

import cn.bupt.zcc.common.RpcDecoder;
import cn.bupt.zcc.common.RpcEncoder;
import cn.bupt.zcc.common.RpcRequest;
import cn.bupt.zcc.common.RpcResponse;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Created by 张城城 on 2018/4/25.
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel>{
    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new RpcEncoder(RpcRequest.class));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        pipeline.addLast(new RpcDecoder(RpcResponse.class));

    }
}
