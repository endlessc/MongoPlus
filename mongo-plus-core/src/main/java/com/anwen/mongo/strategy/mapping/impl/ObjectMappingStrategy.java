package com.anwen.mongo.strategy.mapping.impl;

import com.anwen.mongo.strategy.mapping.MappingStrategy;

/**
 * Object类型映射器
 *
 * @author anwen
 */
public class ObjectMappingStrategy implements MappingStrategy<Object> {
    @Override
    public Object mapping(Object fieldValue) throws IllegalAccessException {
        return fieldValue.toString();
    }
}
