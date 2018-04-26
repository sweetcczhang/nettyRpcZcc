package cn.bupt.zcc.common;

import cn.bupt.zcc.util.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 对服务器端传回的数据进行解码
 * Created by 张城城 on 2018/4/22.
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> clazz;

    public RpcDecoder(Class<?> clazz){
        this.clazz = clazz;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        //判断byteBuf中数据的长度，如果长度小于4则结束，说明服务器返回数据有误
        if (byteBuf.readableBytes()<4){
            return;
        }

        byteBuf.markReaderIndex();
        int datalength = byteBuf.readInt();
        if (byteBuf.readableBytes()<datalength){
            byteBuf.resetReaderIndex();
            return;
        }
        byte[] data = new byte[datalength];
        byteBuf.readBytes(data);
        Object obj = SerializationUtil.deserialize(data,clazz);
        list.add(obj);
    }
}
