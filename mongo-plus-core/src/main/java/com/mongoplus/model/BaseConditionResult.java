package com.mongoplus.model;

import com.mongodb.BasicDBObject;

/**
 * 构建lambda条件结果
 *
 * @author JiaChaoYang
 **/
public class BaseConditionResult {

    /**
     * 条件策略
    */
    private BasicDBObject condition;

    /**
     * projection策略
    */
    private BasicDBObject projection;

    /**
     * 排序策略
    */
    private BasicDBObject sort;

    public BaseConditionResult(BasicDBObject condition, BasicDBObject projection, BasicDBObject sort) {
        this.condition = condition;
        this.projection = projection;
        this.sort = sort;
    }

    public BaseConditionResult() {
    }

    public BasicDBObject getCondition() {
        return condition;
    }

    public void setCondition(BasicDBObject condition) {
        this.condition = condition;
    }

    public BasicDBObject getProjection() {
        return projection;
    }

    public void setProjection(BasicDBObject projection) {
        this.projection = projection;
    }

    public BasicDBObject getSort() {
        return sort;
    }

    public void setSort(BasicDBObject sort) {
        this.sort = sort;
    }

    @Override
    public String toString() {
        return "BaseLambdaQueryResult{" +
                "condition=" + condition +
                ", projection=" + projection +
                ", sort=" + sort +
                '}';
    }
}
