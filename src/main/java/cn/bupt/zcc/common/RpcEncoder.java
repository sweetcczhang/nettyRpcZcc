package cn.bupt.zcc.common;

import cn.bupt.zcc.util.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 对发送到服务端的数据进行编码
 * Created by 张城城 on 2018/4/22.
 */
public class RpcEncoder extends MessageToByteEncoder {

    private Class<?> clazz;

    public RpcEncoder(Class<?> clazz){
        this.clazz = clazz;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
            if(clazz.isInstance(o)){
                byte[] data = SerializationUtil.serialize(o);
                byteBuf.writeInt(data.length);
                byteBuf.writeBytes(data);
            }
    }
}
