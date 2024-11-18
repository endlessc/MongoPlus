package com.anwen.mongo.cache.global;

import com.anwen.mongo.strategy.mapping.MappingStrategy;
import com.anwen.mongo.strategy.mapping.impl.BigIntegerMappingStrategy;
import com.anwen.mongo.strategy.mapping.impl.EnumMappingStrategy;
import com.anwen.mongo.strategy.mapping.impl.ObjectMappingStrategy;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class MappingCache {

    private static final Map<Class<?>, MappingStrategy<?>> mappingStrategyMap = new HashMap<>();

    static {
        mappingStrategyMap.put(BigInteger.class,new BigIntegerMappingStrategy());
        mappingStrategyMap.put(Object.class,new ObjectMappingStrategy());
        mappingStrategyMap.put(Enum.class, new EnumMappingStrategy());
    }

    public static MappingStrategy<?> getMappingStrategy(Class<?> clazz){
        return mappingStrategyMap.get(clazz);
    }

    public static void putMappingStrategy(Class<?> clazz,MappingStrategy<?> mappingStrategy){
        mappingStrategyMap.put(clazz,mappingStrategy);
    }

}
