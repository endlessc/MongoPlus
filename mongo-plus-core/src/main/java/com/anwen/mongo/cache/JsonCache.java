package com.anwen.mongo.cache;

import com.alibaba.fastjson.serializer.SerializeConfig;
import com.anwen.mongo.json.LocalDateTimeSerializer;

import java.time.LocalDateTime;

/**
 * @author JiaChaoYang
 **/
public class JsonCache {

    public final static SerializeConfig config = new SerializeConfig();

    // 注册自定义的序列化器
    static {
        config.put(LocalDateTime .class, new LocalDateTimeSerializer());
    }

}