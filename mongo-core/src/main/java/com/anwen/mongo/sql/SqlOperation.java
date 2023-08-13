package com.anwen.mongo.sql;

import cn.hutool.core.collection.CollUtil;
import com.anwen.mongo.annotation.collection.CollectionName;
import com.anwen.mongo.conditions.interfaces.aggregate.project.Projection;
import com.anwen.mongo.conditions.interfaces.condition.CompareCondition;
import com.anwen.mongo.conditions.interfaces.condition.Order;
import com.anwen.mongo.conn.ConnectMongoDB;
import com.anwen.mongo.constant.IdAutoConstant;
import com.anwen.mongo.constant.SqlOperationConstant;
import com.anwen.mongo.convert.Converter;
import com.anwen.mongo.convert.DocumentMapperConvert;
import com.anwen.mongo.domain.InitMongoCollectionException;
import com.anwen.mongo.domain.MongoQueryException;
import com.anwen.mongo.enums.*;
import com.anwen.mongo.model.*;
import com.anwen.mongo.support.SFunction;
import com.anwen.mongo.toolkit.BeanMapUtilByReflect;
import com.anwen.mongo.toolkit.StringUtils;
import com.anwen.mongo.toolkit.codec.RegisterCodecUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static com.anwen.mongo.toolkit.BeanMapUtilByReflect.checkTableField;

/**
 * @Description: sql执行
 * @BelongsProject: mongo
 * @BelongsPackage: com.anwen.mongo.sql
 * @Author: JiaChaoYang
 * @CreateTime: 2023-02-16 20:35
 * @Version: 1.0
 */
@Data
@Log4j2
public class SqlOperation<T> {

    private Map<String, MongoCollection<Document>> collectionMap = new HashMap<>();

    private List<SlaveDataSource> slaveDataSources;

    private BaseProperty baseProperty;

    private MongoClient mongoClient;

    // 实例化 ConnectMongoDB 对象，用于保存连接
    private ConnectMongoDB connectMongoDB;

    private Class<T> mongoEntity;

    private String createIndex = null;

    private String collectionName;

    private Boolean isItAutoId;

    public void setMongoEntity(Class<T> mongoEntity) {
        this.mongoEntity = mongoEntity;
    }

    public void init(Class<?> clazz) {
        String tableName = clazz.getSimpleName().toLowerCase();
        if (clazz.isAnnotationPresent(CollectionName.class)) {
            CollectionName annotation = clazz.getAnnotation(CollectionName.class);
            tableName = annotation.value();
            String dataSource = annotation.dataSource();
            if (StringUtils.isNotBlank(dataSource)) {
                Optional<SlaveDataSource> matchingSlave = slaveDataSources.stream()
                        .filter(slave -> Objects.equals(dataSource, slave.getSlaveName()))
                        .findFirst();
                if (matchingSlave.isPresent()) {
                    SlaveDataSource slave = matchingSlave.get();
                    baseProperty.setHost(slave.getHost());
                    baseProperty.setPort(slave.getPort());
                    baseProperty.setDatabase(slave.getDatabase());
                    baseProperty.setUsername(slave.getUsername());
                    baseProperty.setPassword(slave.getPassword());
                } else {
                    throw new InitMongoCollectionException("No matching slave data source configured");
                }
            }
        }
        try {
            connectMongoDB = new ConnectMongoDB(mongoClient, baseProperty.getDatabase(), tableName);
            MongoCollection<Document> collection = connectMongoDB.open();
            collectionMap.put(tableName, collection);
        } catch (MongoException e) {
            log.error("Failed to connect to MongoDB: {}", e.getMessage(), e);
        }
    }

    public Boolean doSave(T entity) {
        try {
            InsertOneResult insertOneResult = getCollection(entity).insertOne(processIdField(entity));
            return insertOneResult.wasAcknowledged();
        } catch (Exception e) {
            log.error("save fail , error info : {}", e.getMessage(), e);
            return false;
        }
    }

    public Boolean doSave(String collectionName, Map<String, Object> entityMap) {
        try {
            InsertOneResult insertOneResult = getCollection(collectionName).insertOne(new Document(entityMap));
            return insertOneResult.wasAcknowledged();
        } catch (Exception e) {
            log.error("save fail , error info : {}", e.getMessage(), e);
            return false;
        }
    }

    public Boolean doSaveBatch(Collection<T> entityList) {
        try {
            InsertManyResult insertManyResult = getCollection(entityList.iterator().next()).insertMany(processIdFieldList(entityList));
            return insertManyResult.getInsertedIds().size() == entityList.size();
        } catch (Exception e) {
            log.error("saveBatch fail , error info : {}", e.getMessage(), e);
            return false;
        }
    }

    public Boolean doSaveBatch(String collectionName, Collection<Map<String, Object>> entityList) {
        try {
            InsertManyResult insertManyResult = getCollection(collectionName).insertMany(BeanMapUtilByReflect.mapListToDocumentList(entityList));
            return insertManyResult.getInsertedIds().size() == entityList.size();
        } catch (Exception e) {
            log.error("saveBatch fail , error info : {}", e.getMessage(), e);
            return false;
        }
    }


    public Boolean doSaveOrUpdate(T entity) {
        try {
            Class<?> entityClass = entity.getClass().getSuperclass();
            Field field = entityClass.getFields()[0];
            String id = String.valueOf(field.get(entity));
            if (doGetById(id) == null) return doSave(entity);
            return doUpdateById(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean doSaveOrUpdate(String collectionName, Map<String, Object> entityMap) {
        if (entityMap.containsKey(SqlOperationConstant._ID)) {
            return doUpdateById(collectionName, entityMap);
        }
        return doSave(collectionName, entityMap);
    }


    public Boolean doSaveOrUpdateBatch(Collection<T> entityList) {
        List<T> insertList = new ArrayList<>();
        for (Document document : getCollection(entityList.iterator().next()).find(
                Filters.in(SqlOperationConstant._ID, entityList.stream().map(entity -> {
                    try {
                        return String.valueOf(entity.getClass().getSuperclass().getMethod("getId").invoke(entity));
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList()))
        )) {
            insertList.add((T) document);
            entityList.remove(document);
        }
        if (!insertList.isEmpty()) {
            return doSaveBatch(insertList);
        }
        return doUpdateBatchByIds(entityList);
    }

    public Boolean doSaveOrUpdateBatch(String collectionName, Collection<Map<String, Object>> entityList) {
        List<Map<String,Object>> insertList = new ArrayList<>();
        for (Document document : getCollection(collectionName).find(
                Filters.in(SqlOperationConstant._ID, entityList.stream().map(entity -> entity.get(SqlOperationConstant._ID)).collect(Collectors.toList()))
        )) {
            insertList.add(document);
            entityList.remove(document);
        }
        if (!insertList.isEmpty()) {
            return doSaveBatch(collectionName,insertList);
        }
        return doUpdateBatchByIds(collectionName,entityList);
    }


    public Boolean doUpdateById(T entity) {
        UpdateResult updateResult;
        Document document = new Document(checkTableField(entity));
        BasicDBObject filter = new BasicDBObject(SqlOperationConstant._ID, new ObjectId(String.valueOf(document.get(SqlOperationConstant._ID))));
        document.remove(SqlOperationConstant._ID);
        BasicDBObject update = new BasicDBObject(SpecialConditionEnum.SET.getCondition(), document);
        updateResult = getCollection(entity).updateOne(filter, update);
        return updateResult.getModifiedCount() != 0;
    }

    public Boolean doUpdateById(String collectionName, Map<String, Object> entityMap) {
        if (!entityMap.containsKey(SqlOperationConstant._ID)) {
            throw new MongoException("_id undefined");
        }
        UpdateResult updateResult;
        BasicDBObject filter = new BasicDBObject(SqlOperationConstant._ID, new ObjectId(String.valueOf(entityMap.get(SqlOperationConstant._ID))));
        entityMap.remove(SqlOperationConstant._ID);
        BasicDBObject update = new BasicDBObject(SpecialConditionEnum.SET.getCondition(), new Document(entityMap));
        updateResult = getCollection(collectionName).updateOne(filter, update);
        return updateResult.getModifiedCount() != 0;
    }


    public Boolean doUpdateBatchByIds(Collection<T> entityList) {
        for (T entity : entityList) {
            UpdateResult updateResult;
            try {
                updateResult = getCollection(entity).updateMany(Filters.eq(SqlOperationConstant._ID, entity.getClass().getSuperclass().getMethod("getId").invoke(entity)), new Document(checkTableField(entity)));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            return updateResult.getModifiedCount() == entityList.size();
        }
        return false;
    }

    public Boolean doUpdateBatchByIds(String collectionName, Collection<Map<String, Object>> entityList) {
        for (Map<String, Object> entity : entityList) {
            UpdateResult updateResult = getCollection(collectionName).updateOne(Filters.eq(SqlOperationConstant._ID, new ObjectId(String.valueOf(entity.get(SqlOperationConstant._ID)))), new Document(entity));
            return updateResult.getModifiedCount() == entityList.size();
        }
        return false;
    }


    public Boolean doUpdateByColumn(T entity, SFunction<T, Object> column) {
        try {
            UpdateResult updateResult = getCollection(entity).updateOne(Filters.eq(column.getFieldName(), entity.getClass().getMethod(column.getFieldName()).invoke(entity)), new Document(checkTableField(entity)));
            return updateResult.getModifiedCount() > 0;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("update fail , fail info : {}", e.getMessage(), e);
            return false;
        }
    }


    public Boolean doUpdateByColumn(T entity, String column) {
        try {
            UpdateResult updateResult = getCollection(entity).updateOne(Filters.eq(column, entity.getClass().getMethod("get"+Character.toUpperCase(column.charAt(0))+column.substring(1)).invoke(entity)), new Document(checkTableField(entity)));
            return updateResult.getModifiedCount() > 0;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("update fail , fail info : {}", e.getMessage(), e);
            return false;
        }
    }

    public Boolean doUpdateByColumn(String collectionName,Map<String,Object> entityMap, String column) {
        UpdateResult updateResult = getCollection(collectionName).updateOne(Filters.eq(column, entityMap.get(column)), new Document(entityMap));
        return updateResult.getModifiedCount() > 0;
    }


    public Boolean doRemoveById(Serializable id) {
        return getCollection().deleteOne(Filters.eq(SqlOperationConstant._ID, id)).getDeletedCount() != 0;
    }

    public Boolean doRemoveById(String collectionName,Serializable id) {
        return getCollection(collectionName).deleteOne(Filters.eq(SqlOperationConstant._ID, new ObjectId(String.valueOf(id)))).getDeletedCount() != 0;
    }


    public Boolean doRemoveByColumn(SFunction<T, Object> column, String value) {
        return getCollection().deleteOne(Filters.eq(column.getFieldNameLine(), value)).getDeletedCount() != 0;
    }


    public Boolean doRemoveByColumn(String column, String value) {
        return getCollection().deleteOne(Filters.eq(column, value)).getDeletedCount() != 0;
    }

    public Boolean doRemoveByColumn(String collectionName,String column, String value) {
        return getCollection(collectionName).deleteOne(Filters.eq(column, value)).getDeletedCount() != 0;
    }


    public Boolean doRemoveBatchByIds(Collection<Serializable> idList) {
        return getCollection().deleteMany(Filters.in(SqlOperationConstant._ID, idList)).getDeletedCount() != 0;
    }

    public Boolean doRemoveBatchByIds(String collectionName,Collection<Serializable> idList) {
        return getCollection(collectionName).deleteMany(Filters.in(SqlOperationConstant._ID, idList)).getDeletedCount() != 0;
    }

    public List<T> doList() {
        MongoCollection<Document> collection = getCollection();
        if (StringUtils.isNotBlank(createIndex)) {
            collection.createIndex(new Document(createIndex, QueryOperatorEnum.TEXT.getValue()));
        }
        return DocumentMapperConvert.mapDocumentList(collection.find(),mongoEntity);
    }

    public List<Map<String, Object>> doList(String collectionName) {
        MongoCollection<Document> collection = getCollection(collectionName);
        if (StringUtils.isNotBlank(createIndex)) {
            collection.createIndex(new Document(createIndex, QueryOperatorEnum.TEXT.getValue()));
        }
        return Converter.convertDocumentToMap(collection.find(Map.class), Math.toIntExact(doCount(collectionName)));
    }

    public List<Map<String, Object>> doList(String collectionName, List<CompareCondition> compareConditionList, List<Order> orderList, List<Projection> projectionList) {
        return getLambdaQueryResult(collectionName, compareConditionList, orderList,projectionList);
    }

    public PageResult<Map<String, Object>> doPage(String collectionName, List<CompareCondition> compareConditionList, List<Order> orderList,List<Projection> projectionList, Integer pageNum, Integer pageSize) {
        return getLambdaQueryResultPage(collectionName, compareConditionList, orderList,projectionList, new PageParam(pageNum, pageSize));
    }

    public PageResult<Map<String,Object>> doPage(String collectionName,Integer pageNum,Integer pageSize){
        return getLambdaQueryResultPage(collectionName,new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new PageParam(pageNum,pageSize));
    }

    public Map<String, Object> doOne(String collectionName, List<CompareCondition> compareConditionList,List<Projection> projectionList) {
        List<Map<String, Object>> result = getLambdaQueryResult(collectionName, compareConditionList, new ArrayList<>(),projectionList);
        if (result.size() > 1) {
            throw new MongoQueryException("query result greater than one line");
        }
        return !result.isEmpty() ? result.get(0) : new HashMap<>();
    }

    public Map<String, Object> doLimitOne(String collectionName, List<CompareCondition> compareConditionList,List<Projection> projectionList) {
        List<Map<String, Object>> result = getLambdaQueryResult(collectionName, compareConditionList, new ArrayList<>(),projectionList);
        return !result.isEmpty() ? result.get(0) : new HashMap<>();
    }


    public T doGetById(String collectionName, Serializable id) {
        BasicDBObject byId = new BasicDBObject(SqlOperationConstant._ID, new BasicDBObject(SpecialConditionEnum.EQ.getCondition(), id));
        FindIterable<Document> iterable = getCollection(collectionName).find(byId);
        return (T) iterable.first();
    }

    public List<T> doGetByIds(String collectionName, Collection<Serializable> ids) {
        BasicDBObject query = new BasicDBObject(SqlOperationConstant._ID, new BasicDBObject(SpecialConditionEnum.IN.getCondition(), ids));
        FindIterable<Document> iterable = getCollection(collectionName).find(query);
        return DocumentMapperConvert.mapDocumentList(iterable, mongoEntity);
    }


    public List<T> doList(List<CompareCondition> compareConditionList, List<Order> orderList,List<Projection> projectionList) {
        return getLambdaQueryResult(compareConditionList, orderList,projectionList);
    }

    public T doOne(List<CompareCondition> compareConditionList,List<Projection> projectionList) {
        List<T> result = getLambdaQueryResult(compareConditionList, new ArrayList<>(),projectionList);
        if (result.size() > 1) {
            throw new MongoQueryException("query result greater than one line");
        }
        return !result.isEmpty() ? result.get(0) : null;
    }

    public T doLimitOne(List<CompareCondition> compareConditionList,List<Projection> projectionList) {
        List<T> result = getLambdaQueryResult(compareConditionList, new ArrayList<>(),projectionList);
        return !result.isEmpty() ? result.get(0) : null;
    }

    public PageResult<T> doPage(List<CompareCondition> compareConditionList, List<Order> orderList,List<Projection> projectionList, Integer pageNum, Integer pageSize) {
        return getLambdaQueryResultPage(compareConditionList, orderList,projectionList, new PageParam(pageNum, pageSize));
    }

    public T doGetById(Serializable id) {
        BasicDBObject byId = new BasicDBObject(SqlOperationConstant._ID, new BasicDBObject(SpecialConditionEnum.EQ.getCondition(), id));
        FindIterable<Document> iterable = getCollection().find(byId);
        return (T) iterable.first();
    }

    public List<T> doGetByIds(Collection<Serializable> ids) {
        BasicDBObject query = new BasicDBObject(SqlOperationConstant._ID, new BasicDBObject(SpecialConditionEnum.IN.getCondition(), ids));
        FindIterable<Document> iterable = getCollection().find(query);
        return DocumentMapperConvert.mapDocumentList(iterable, mongoEntity);
    }

    public Boolean doUpdate(List<CompareCondition> compareConditionList) {
        BasicDBObject queryBasic = buildQueryCondition(compareConditionList);
        BasicDBObject updateBasic = buildUpdateValue(compareConditionList);
        UpdateResult updateResult = getCollection().updateMany(queryBasic, new BasicDBObject() {{
            append(SpecialConditionEnum.SET.getCondition(), updateBasic);
        }});
        return updateResult.getModifiedCount() > 0;
    }

    public Boolean doUpdate(String collectionName,List<CompareCondition> compareConditionList){
        BasicDBObject queryBasic = buildQueryCondition(compareConditionList);
        BasicDBObject updateBasic = buildUpdateValue(compareConditionList);
        UpdateResult updateResult = getCollection(collectionName).updateMany(queryBasic, new BasicDBObject() {{
            append(SpecialConditionEnum.SET.getCondition(), updateBasic);
        }});
        return updateResult.getModifiedCount() > 0;
    }

    public Boolean doRemove(List<CompareCondition> compareConditionList) {
        BasicDBObject deleteBasic = buildQueryCondition(compareConditionList);
        DeleteResult deleteResult = getCollection().deleteMany(deleteBasic);
        return deleteResult.getDeletedCount() > 0;
    }

    public Boolean doRemove(String collectionName,List<CompareCondition> compareConditionList){
        BasicDBObject deleteBasic = buildQueryCondition(compareConditionList);
        DeleteResult deleteResult = getCollection(collectionName).deleteMany(deleteBasic);
        return deleteResult.getDeletedCount() > 0;
    }

    public long doCount(String collectionName,List<CompareCondition> compareConditionList){
        return getCollection(collectionName).countDocuments(buildQueryCondition(compareConditionList));
    }

    public long doCount(String collectionName){
        return getCollection(collectionName).countDocuments();
    }

    public long doCount(){
        return getCollection().countDocuments();
    }

    public long doCount(List<CompareCondition> compareConditionList){
        return getCollection().countDocuments(buildQueryCondition(compareConditionList));
    }

    public List<T> doAggregateList(List<BaseAggregate> aggregateList){
        List<BasicDBObject> basicDBObjectList = new ArrayList<>();
        aggregateList.forEach(aggregate -> {
            if (Objects.equals(aggregate.getType(), AggregateTypeEnum.MATCH.getType())){
                basicDBObjectList.add(new BasicDBObject("$" + aggregate.getType(), buildQueryCondition(((BaseMatchAggregate) aggregate.getPipeline()).getCompareConditionList())));
            }
        });
        AggregateIterable<Document> aggregateIterable = getCollection().aggregate(basicDBObjectList);
        return DocumentMapperConvert.mapDocumentList(aggregateIterable.iterator(),mongoEntity);
    }


    private List<T> getLambdaQueryResult(List<CompareCondition> compareConditionList, List<Order> orderList,List<Projection> projectionList) {
        return DocumentMapperConvert.mapDocumentList(baseLambdaQuery(compareConditionList, orderList,projectionList), mongoEntity);
    }

    private List<Map<String, Object>> getLambdaQueryResult(String collectionName, List<CompareCondition> compareConditionList, List<Order> orderList,List<Projection> projectionList) {
        return Converter.convertDocumentToMap(baseLambdaQuery(collectionName, compareConditionList, orderList,projectionList), Math.toIntExact(doCount(collectionName, compareConditionList)));
    }

    /**
     * 查询执行
     *
     * @author JiaChaoYang
     * @date 2023/6/25/025 1:51
     */
    private FindIterable<Document> baseLambdaQuery(List<CompareCondition> compareConditionList, List<Order> orderList,List<Projection> projectionList) {
        BasicDBObject sortCond = new BasicDBObject();
        orderList.forEach(order -> sortCond.put(order.getColumn(), order.getType()));
        MongoCollection<Document> collection = getCollection();
        BasicDBObject basicDBObject = buildQueryCondition(compareConditionList);
        if (StringUtils.isNotBlank(createIndex)) {
            collection.createIndex(new Document(createIndex, QueryOperatorEnum.TEXT.getValue()));
        }
        return collection.find(basicDBObject).projection(buildProjection(projectionList)).sort(sortCond);
    }

    private FindIterable<Map> baseLambdaQuery(String collectionName, List<CompareCondition> compareConditionList, List<Order> orderList,List<Projection> projectionList) {
        BasicDBObject sortCond = new BasicDBObject();
        orderList.forEach(order -> sortCond.put(order.getColumn(), order.getType()));
        MongoCollection<Document> collection = getCollection(collectionName);
        BasicDBObject basicDBObject = buildQueryCondition(compareConditionList);
        if (StringUtils.isNotBlank(createIndex)) {
            collection.createIndex(new Document(createIndex, QueryOperatorEnum.TEXT.getValue()));
        }
        return collection.find(basicDBObject,Map.class).projection(buildProjection(projectionList)).sort(sortCond);
    }

    private PageResult<T> getLambdaQueryResultPage(List<CompareCondition> compareConditionList, List<Order> orderList,List<Projection> projectionList, PageParam pageParams) {
        PageResult<T> pageResult = new PageResult<>();
        FindIterable<Document> documentFindIterable = baseLambdaQuery(compareConditionList, orderList,projectionList);
        long totalSize = doCount(compareConditionList);
        pageResult.setPageNum(pageParams.getPageNum());
        pageResult.setPageSize(pageParams.getPageSize());
        pageResult.setTotalSize(totalSize);
        pageResult.setTotalPages((totalSize + pageParams.getPageSize() - 1) / pageParams.getPageSize());
        pageResult.setContentData(DocumentMapperConvert.mapDocumentList(documentFindIterable.skip((pageParams.getPageNum() - 1) * pageParams.getPageSize()).limit(pageParams.getPageSize()), mongoEntity));
        return pageResult;
    }

    private PageResult<Map<String, Object>> getLambdaQueryResultPage(String collectionName, List<CompareCondition> compareConditionList, List<Order> orderList,List<Projection> projectionList, PageParam pageParams) {
        PageResult<Map<String, Object>> pageResult = new PageResult<>();
        FindIterable<Map> documentFindIterable = baseLambdaQuery(collectionName, compareConditionList, orderList,projectionList);
        long totalSize = doCount(collectionName,compareConditionList);
        pageResult.setPageNum(pageParams.getPageNum());
        pageResult.setPageSize(pageParams.getPageSize());
        pageResult.setTotalSize(totalSize);
        pageResult.setTotalPages((totalSize + pageParams.getPageSize() - 1) / pageParams.getPageSize());
        pageResult.setContentData(Converter.convertDocumentToMap(documentFindIterable.skip((pageParams.getPageNum() - 1) * pageParams.getPageSize()).limit(pageParams.getPageSize())));
        return pageResult;
    }

    private BasicDBObject buildProjection(List<Projection> projectionList){
        return new BasicDBObject(){{
            projectionList.forEach(projection -> {
                put(projection.getColumn(),projection.getValue());
            });
        }};
    }

    /**
     * 构建查询条件
     *
     * @author JiaChaoYang
     * @date 2023/6/25/025 1:48
     */
    private BasicDBObject buildQueryCondition(List<CompareCondition> compareConditionList) {
        return new BasicDBObject() {{
            compareConditionList.stream().filter(compareCondition -> compareCondition.getType() == CompareEnum.QUERY.getKey()).collect(Collectors.toList()).forEach(compare -> {
                if (Objects.equals(compare.getCondition(), QueryOperatorEnum.LIKE.getValue()) && StringUtils.isNotBlank(String.valueOf(compare.getValue()))) {
                    put(compare.getColumn(), new BasicDBObject(SpecialConditionEnum.REGEX.getCondition(), compare.getValue()));
                } else if (Objects.equals(compare.getLogicType(), LogicTypeEnum.OR.getKey())) {
                    if (CollUtil.isEmpty(compare.getChildCondition())) {
                        compare.setChildCondition(Collections.singletonList(compare));
                    }
                    put(SpecialConditionEnum.OR.getCondition(), buildOrQueryCondition(compare.getChildCondition()));
                } else if (Objects.equals(compare.getLogicType(), LogicTypeEnum.NOR.getKey())) {
                    put(SpecialConditionEnum.NOR.getCondition(), buildQueryCondition(compare.getChildCondition()));
                } else if (Objects.equals(compare.getLogicType(), LogicTypeEnum.ELEMMATCH.getKey())) {
                    put(SpecialConditionEnum.ELEM_MATCH.getCondition(), buildOrQueryCondition(compare.getChildCondition()));
                } else if (Objects.equals(compare.getCondition(), QueryOperatorEnum.TEXT.getValue())) {
                    put(SpecialConditionEnum.TEXT.getCondition(), new BasicDBObject(SpecialConditionEnum.SEARCH.getCondition(), compare.getValue()));
                    createIndex = compare.getColumn();
                } else {
                    put(compare.getColumn(), new BasicDBObject("$" + compare.getCondition(), compare.getValue()));
                }
            });
        }};
    }

    /**
     * 构建子条件
     *
     * @author JiaChaoYang
     * @date 2023/7/16 19:59
     */
    private List<BasicDBObject> buildOrQueryCondition(List<CompareCondition> compareConditionList) {
        List<BasicDBObject> basicDBObjectList = new ArrayList<>();
        compareConditionList.forEach(compare -> {
            BasicDBObject basicDBObject = new BasicDBObject();
            if (Objects.equals(compare.getCondition(), QueryOperatorEnum.LIKE.getValue()) && StringUtils.isNotBlank(String.valueOf(compare.getValue()))) {
                basicDBObject.put(compare.getColumn(), new BasicDBObject(SpecialConditionEnum.REGEX.getCondition(), compare.getValue()));
            } else if (Objects.equals(compare.getCondition(), QueryOperatorEnum.AND.getValue())) {
                basicDBObjectList.add(buildQueryCondition(compare.getChildCondition()));
            } else if (Objects.equals(compare.getCondition(), QueryOperatorEnum.TEXT.getValue())) {
                basicDBObject.put(SpecialConditionEnum.TEXT.getCondition(), new BasicDBObject(SpecialConditionEnum.SEARCH.getCondition(), compare.getValue()));
                createIndex = compare.getColumn();
            }else {
                basicDBObject.put(compare.getColumn(), new BasicDBObject("$" + compare.getCondition(), compare.getValue()));
            }
            basicDBObjectList.add(basicDBObject);
        });
        return basicDBObjectList;
    }

    /**
     * 构建更新值
     *
     * @author JiaChaoYang
     * @date 2023/7/9 22:16
     */
    private BasicDBObject buildUpdateValue(List<CompareCondition> compareConditionList) {
        return new BasicDBObject() {{
            compareConditionList.stream().filter(compareCondition -> compareCondition.getType() == CompareEnum.UPDATE.getKey()).collect(Collectors.toList()).forEach(compare -> {
                put(compare.getColumn(), compare.getValue());
            });
        }};
    }

    private MongoCollection<Document> getCollection(T entity) {
        return getCollection().withCodecRegistry(CodecRegistries.fromRegistries(RegisterCodecUtil.registerCodec(entity)));
    }

    private MongoCollection<Document> getCollection(Map<String, Object> entityMap, String collectionName) {
        return getCollection(collectionName).withCodecRegistry(CodecRegistries.fromRegistries(RegisterCodecUtil.registerCodec(entityMap)));
    }

    private MongoCollection<Document> getCollection() {
        createIndex = null;
        Class<?> clazz = mongoEntity;
        String collectionName = clazz.getSimpleName().toLowerCase();
        if (clazz.isAnnotationPresent(CollectionName.class)) {
            collectionName = clazz.getAnnotation(CollectionName.class).value();
        }
        return getCollection(collectionName);
    }

    private MongoCollection<Document> getCollection(String collectionName) {
        createIndex = null;
        this.collectionName = collectionName;
        // 检查连接是否需要重新创建
        if (!this.collectionMap.containsKey(collectionName)) {
            if (connectMongoDB == null || !Objects.equals(connectMongoDB.getCollection(), collectionName)){
                connectMongoDB = new ConnectMongoDB(mongoClient, baseProperty.getDatabase(), collectionName);
            }
            MongoCollection<Document> mongoCollection = connectMongoDB.open();
            this.collectionMap.put(collectionName, mongoCollection);
            return mongoCollection;
        }
        return this.collectionMap.get(collectionName);
    }

    private Document processIdField(T entity){
        Map<String, Object> tableFieldMap = checkTableField(entity);
        if (IdAutoConstant.IS_IT_AUTO_ID){
            long num = 1L;
            MongoCollection<Document> collection = getCollection("counters");
            Document query = new Document(SqlOperationConstant._ID, collectionName);
            Document update = new Document("$inc", new Document(SqlOperationConstant.AUTO_NUM, num));
            Document document = collection.findOneAndUpdate(query,update,new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
            if (document == null){
                Long finalNum = num;
                collection.insertOne(new Document(new HashMap<String,Object>(){{
                    put(SqlOperationConstant._ID,collectionName);
                    put(SqlOperationConstant.AUTO_NUM, finalNum);
                }}));
            }else {
                num = Long.parseLong(String.valueOf(document.get(SqlOperationConstant.AUTO_NUM)));
            }
            tableFieldMap.put(SqlOperationConstant._ID,num);
            tableFieldMap.remove(SqlOperationConstant.IS_IT_AUTO_ID);
        }
        return new Document(tableFieldMap);
    }

    private List<Document> processIdFieldList(Collection<T> entityList){
        return entityList.stream().map(this::processIdField).collect(Collectors.toList());
    }

}
