package com.mongoplus.annotation.datasource;

import java.lang.annotation.*;

/**
 * 多数据源注解
 *
 * @author JiaChaoYang
 **/
@Target({ElementType.METHOD,ElementType.TYPE})
//运行时注解
@Retention(RetentionPolicy.RUNTIME)
//表明这个注解应该被 javadoc工具记录
//生成文档
@Documented
public @interface MongoDs {

    /**
     * 数据源名称
     * @author JiaChaoYang
    */
    String value();

    /**
     * 数据源处理类，需要实现{@link com.mongoplus.handlers.DataSourceHandler}接口
     * @author JiaChaoYang
    */
    Class<?> dsHandler() default Void.class;

}
