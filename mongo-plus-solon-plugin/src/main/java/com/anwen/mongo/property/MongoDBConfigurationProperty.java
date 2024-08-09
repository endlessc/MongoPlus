package com.anwen.mongo.property;

import com.anwen.mongo.cache.global.PropertyCache;

/**
 * configuration属性配置
 *
 * @author JiaChaoYang
 **/
public class MongoDBConfigurationProperty {

    /**
     * banner打印
     * @date 2024/1/26 21:58
    */
    private Boolean banner = true;

    private Boolean ikun = false;

    /**
     * 存放自增id的集合
     * @date 2024/5/1 下午8:55
     */
    private String autoIdCollectionName;


    /**
     * 是否将Id字段的ObjectId转换为字段的类型
     * @date 2024/8/9 14:52
     */
    private Boolean objectIdConvertType = false;

    /**
     * 自动转换ObjectId
     * @date 2024/7/26 下午5:40
     */
    private Boolean autoConvertObjectId = true;

    public Boolean getAutoConvertObjectId() {
        return autoConvertObjectId;
    }

    public void setAutoConvertObjectId(Boolean autoConvertObjectId) {
        this.autoConvertObjectId = autoConvertObjectId;
        PropertyCache.autoConvertObjectId = autoConvertObjectId;
    }

    public String getAutoIdCollectionName() {
        return autoIdCollectionName;
    }

    public void setAutoIdCollectionName(String autoIdCollectionName) {
        PropertyCache.autoIdCollectionName = autoIdCollectionName;
        this.autoIdCollectionName = autoIdCollectionName;
    }

    public Boolean getIkun() {
        return ikun;
    }

    public void setIkun(Boolean ikun) {
        PropertyCache.ikun = ikun;
        this.ikun = ikun;
    }


    public Boolean getObjectIdConvertType() {
        return objectIdConvertType;
    }

    public void setObjectIdConvertType(Boolean objectIdConvertType) {
        PropertyCache.objectIdConvertType = objectIdConvertType;
        this.objectIdConvertType = objectIdConvertType;
    }

    public Boolean getBanner() {
        return banner;
    }

    public void setBanner(Boolean banner) {
        this.banner = banner;
    }
}
