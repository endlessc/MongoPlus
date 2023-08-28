package com.anwen.mongo.config.log;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * @author JiaChaoYang
 * 日志属性
 * @since 2023-06-07 23:07
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Configuration
@Inject("${mongo-plus}")
public class MongoDBLogProperty {

    /**
     * 是否开启日志
     * @author: JiaChaoYang
     * @date: 2023/6/7 23:08
     **/
    private Boolean log = false;

    /**
     * 是否开启格式化sql<p style="color:red;">格式化之后，sql会很长很长<p/>
     * @author JiaChaoYang
     * @date 2023/8/29 0:52
     */
    private Boolean format = false;

}
