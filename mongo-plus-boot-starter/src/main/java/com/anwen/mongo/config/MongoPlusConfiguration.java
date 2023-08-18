package com.anwen.mongo.config;

import com.anwen.mongo.config.log.MongoDBLogProperty;
import com.anwen.mongo.event.SqlOperationInitializedEvent;
import com.anwen.mongo.execute.SqlOperation;
import com.anwen.mongo.log.CustomMongoDriverLogger;
import com.anwen.mongo.mapper.MongoPlusMapMapper;
import com.anwen.mongo.toolkit.UrlJoint;
import com.anwen.mongo.transactional.MongoTransactionalAspect;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;

/**
 * @author JiaChaoYang
 * 连接配置
 * @since 2023-02-09 14:27
 **/
@Log4j2
@EnableConfigurationProperties(value = {MongoDBConnectProperty.class, MongoDBLogProperty.class})
public class MongoPlusConfiguration extends MongoAutoConfiguration{

    /**
     * 自定义事件
     * @author JiaChaoYang
     * @date 2023/6/26/026 22:06
     */
    @Resource
    private ApplicationEventPublisher eventPublisher;

    private final MongoDBLogProperty mongoDBLogProperty;

    final
    MongoDBConnectProperty mongoDBConnectProperty;

    private MongoClient mongoClient;

    private SqlOperation<?> sqlOperation;

    @Override
    public MongoClient mongo(ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers, MongoClientSettings settings) {
        return this.mongoClient;
    }

    public MongoPlusConfiguration(MongoDBConnectProperty mongoDBConnectProperty,MongoDBLogProperty mongoDBLogProperty) {
        this.mongoDBConnectProperty = mongoDBConnectProperty;
        this.mongoDBLogProperty = mongoDBLogProperty;
    }

    @Bean
    @ConditionalOnMissingBean
    @PostConstruct
    public <T> SqlOperation<T> sqlOperation() {
        if (sqlOperation != null){
            return (SqlOperation<T>) this.sqlOperation;
        }
        SqlOperation<T> sqlOperation = new SqlOperation<>();
        sqlOperation.setSlaveDataSources(mongoDBConnectProperty.getSlaveDataSource());
        sqlOperation.setBaseProperty(mongoDBConnectProperty);
//        sqlOperation.setIsItAutoId(mongoDBLogProperty.getIsItAutoId());
        UrlJoint urlJoint = new UrlJoint(mongoDBConnectProperty);
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(urlJoint.jointMongoUrl()));
        if (mongoDBLogProperty.getLog()){
            builder.addCommandListener(new CustomMongoDriverLogger());
        }
        this.mongoClient = MongoClients.create(builder.build());
        sqlOperation.setMongoClient(this.mongoClient);
        // 发布自定义事件通知其他类，sqlOperation已完成初始化
        eventPublisher.publishEvent(new SqlOperationInitializedEvent(sqlOperation));
        this.sqlOperation = sqlOperation;
        return sqlOperation;
    }

    @Bean
    public MongoPlusMapMapper mongoPlusMapMapper(SqlOperation<Map<String,Object>> sqlOperation){
        return new MongoPlusMapMapper(sqlOperation);
    }

    @Bean
    public MongoTransactionalAspect mongoTransactionalAspect(){
        return new MongoTransactionalAspect(mongoClient);
    }

}
