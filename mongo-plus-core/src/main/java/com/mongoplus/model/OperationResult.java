package com.mongoplus.model;

import com.mongoplus.annotation.ID;

import java.io.Serializable;

/**
 * 数据变动记录对象
 *
 * @author anwen
 */
public class OperationResult implements Serializable {

    private static final long serialVersionUID = -6333002023958303277L;

    @ID
    private String id;

    /**
     * 操作类型
     *
     */
    private String operation;

    /**
     * 记录状态
     *
     */
    private boolean recordStatus;

    /**
     * 数据源名称
     *
     */
    private String datasourceName;

    /**
     * 数据库名
     *
     */
    private String databaseName;

    /**
     * 集合名
     *
     */
    private String collectionName;

    /**
     * 改动数据
     *
     */
    private String changedData;

    /**
     * 插件耗时
     *
     */
    private long cost;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public boolean isRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(boolean recordStatus) {
        this.recordStatus = recordStatus;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getChangedData() {
        return changedData;
    }

    public void setChangedData(String changedData) {
        this.changedData = changedData;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }
    @Override
    public String toString() {
        return "{" +
                "\"datasourceName\":\"" + datasourceName + "\"," +
                "\"databaseName\":\"" + databaseName + "\"," +
                "\"collectionName\":\"" + collectionName + "\"," +
                "\"operation\":\"" + operation + "\"," +
                "\"recordStatus\":\"" + recordStatus + "\"," +
                "\"cost(ms)\":" + cost + "," +
                "\"changedData\":" + changedData + "}";
    }

}
