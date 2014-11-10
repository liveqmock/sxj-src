package com.sxj.redis.output;

import java.nio.ByteBuffer;

import com.sxj.redis.codec.RedisCodec;
import com.sxj.redis.protocol.CommandOutput;

public class ListScanOutput<K, V> extends
        CommandOutput<K, V, ListScanResult<V>>
{
    
    public ListScanOutput(RedisCodec<K, V> codec)
    {
        super(codec, new ListScanResult<V>());
    }
    
    @Override
    public void set(ByteBuffer bytes)
    {
        if (output.getPos() == null)
        {
            output.setPos(((Number) codec.decodeValue(bytes)).longValue());
        }
        else
        {
            output.addValue(codec.decodeValue(bytes));
        }
    }
    
}
