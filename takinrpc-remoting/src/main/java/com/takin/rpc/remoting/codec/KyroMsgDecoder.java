package com.takin.rpc.remoting.codec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.google.common.base.Stopwatch;
import com.takin.rpc.remoting.netty4.RemotingProtocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class KyroMsgDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(KyroMsgDecoder.class);

    public static final int HEAD_LENGTH = 4;

    private final Kryo kryo = new Kryo();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //        Stopwatch watch = Stopwatch.createStarted();

        //        Input input = new Input(new ByteBufInputStream(in));
        //        Object result = kryo.readClassAndObject(input);
        //        out.add(result);
        //        input.close();
        if (in.readableBytes() < HEAD_LENGTH) { //这个HEAD_LENGTH是我们用于表示头长度的字节数。  由于Encoder中我们传的是一个int类型的值，所以这里HEAD_LENGTH的值为4.
            return;
        }
        in.markReaderIndex(); //我们标记一下当前的readIndex的位置
        int dataLength = in.readInt(); // 读取传送过来的消息的长度。ByteBuf 的readInt()方法会让他的readIndex增加4
        if (dataLength < 0) { // 我们读到的消息体长度为0，这是不应该出现的情况，这里出现这情况，关闭连接。
            ctx.close();
        }

        if (in.readableBytes() < dataLength) { //读到的消息体长度如果小于我们传送过来的消息长度，则resetReaderIndex. 这个配合markReaderIndex使用的。把readIndex重置到mark的地方
            in.resetReaderIndex();
            return;
        }

        byte[] body = new byte[dataLength]; //传输正常
        in.readBytes(body);

        RemotingProtocol o = convertToObject(body); //将byte数据转化为我们需要的对象
        out.add(o);
        //        logger.info("kyro decoder use:" + watch.toString());
    }

    private RemotingProtocol convertToObject(byte[] body) {
        Input input = null;
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(body);
            input = new Input(bais);
            return kryo.readObject(input, RemotingProtocol.class);
        } catch (KryoException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                input.close();
            }
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

}
