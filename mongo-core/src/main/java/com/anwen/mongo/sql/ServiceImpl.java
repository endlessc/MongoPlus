package com.anwen.mongo.sql;

import com.anwen.mongo.event.SqlOperationInitializedEvent;
import com.anwen.mongo.sql.query.LambdaQueryChainWrapper;
import com.anwen.mongo.sql.model.PageParam;
import com.anwen.mongo.sql.model.PageResult;
import com.anwen.mongo.sql.support.SFunction;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author JiaChaoYang
 * 接口实现
 * @since 2023-02-09 14:13
 **/
public class ServiceImpl<T> implements IService<T>, ApplicationListener<SqlOperationInitializedEvent> {

    private SqlOperation<T> sqlOperation;

    private Class<T> eClass;

    @Override
    public void onApplicationEvent(SqlOperationInitializedEvent event) {
        sqlOperation = (SqlOperation<T>) event.getSqlOperation();
        sqlOperation.init(getEClass());
    }

    @Override
    public Class<T> getEClass() {
        if (eClass != null) {
            return eClass;
        }
        Type superClassType = getClass().getGenericSuperclass();
        ParameterizedType pt = (ParameterizedType) superClassType;
        Type genType = pt.getActualTypeArguments()[0];
        if (genType instanceof Class) {
            eClass = (Class<T>) genType;
        } else {
            eClass = (Class<T>) Object.class;
        }
        return eClass;
    }

    @Override
    public Boolean save(T entity) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doSave(entity);
    }

    @Override
    public Boolean save(Map<String, Object> entityMap,String tableName) {
        setMongoEntity();
        return null;
    }

    @Override
    public Boolean saveBatch(Collection<T> entityList) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doSaveBatch(entityList);
    }

    @Override
    public Boolean saveOrUpdate(T entity) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doSaveOrUpdate(entity);
    }

    @Override
    public Boolean saveOrUpdateBatch(Collection<T> entityList) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doSaveOrUpdateBatch(entityList);
    }

    @Override
    public Boolean updateById(T entity) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doUpdateById(entity);
    }

    @Override
    public Boolean updateBatchByIds(Collection<T> entityList) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doUpdateBatchByIds(entityList);
    }

    @Override
    public Boolean updateByColumn(T entity, SFunction<T, Object> column) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doUpdateByColumn(entity,column);
    }

    @Override
    public Boolean updateByColumn(T entity, String column) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doUpdateByColumn(entity,column);
    }

    @Override
    public Boolean removeById(Serializable id) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doRemoveById(id);
    }

    @Override
    public Boolean removeByColumn(SFunction<T, Object> column, String value) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doRemoveByColumn(column,value);
    }

    @Override
    public Boolean removeByColumn(String column, String value) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doRemoveByColumn(column,value);
    }

    @Override
    public Boolean removeBatchByIds(Collection<Object> idList) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doRemoveBatchByIds(idList);
    }

    @Override
    public List<T> list() {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doList();
    }

    @Override
    public List<Map<String, Object>> list(String tableName) {
        return sqlOperation.doList(tableName);
    }

    @Override
    public T one(LambdaQueryChainWrapper<T> lambdaQueryChainWrapper) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doOne(lambdaQueryChainWrapper.getCompareList());
    }

    @Override
    public List<T> list(LambdaQueryChainWrapper<T> lambdaQueryChainWrapper) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doList(lambdaQueryChainWrapper.getCompareList(),lambdaQueryChainWrapper.getOrderList());
    }

    @Override
    public PageResult<T> page(LambdaQueryChainWrapper<T> lambdaQueryChainWrapper, Integer pageNum, Integer pageSize){
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doPage(lambdaQueryChainWrapper.getCompareList(),lambdaQueryChainWrapper.getOrderList(), pageNum,pageSize);
    }

    @Override
    public PageResult<T> page(PageParam pageParam) {
        return page(pageParam.getPageNum(),pageParam.getPageSize());
    }

    @Override
    public PageResult<T> page(Integer pageNum, Integer pageSize) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doPage(new ArrayList<>(),new ArrayList<>(),pageNum,pageSize);
    }

    @Override
    public T getById(Serializable id) {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation.doGetById(id);
    }

    @Override
    public SqlOperation<T> getSqlOperation() {
        sqlOperation.setMongoEntity(getEClass());
        return sqlOperation;
    }

    private void setMongoEntity() {
        Class<T> actualTypeArgument = getEClass();
        // 根据实际情况进行设置
        sqlOperation.setMongoEntity(actualTypeArgument);
    }

}
