package com.mongoplus.enums;

/**
 * 管道枚举
 *
 * @author anwen
 */
public enum AggregateEnum {

    ADD_FIELDS("$addFields"),

    SET("$set"),

    BUCKET("$bucket"),

    BUCKET_AUTO("$bucketAuto"),

    MATCH("$match"),

    PROJECT("$project"),

    SORT("$sort"),

    LOOKUP("$lookup"),

    FACET("$facet"),

    GRAPH_LOOKUP("$graphLookup"),

    GROUP("$group"),

    UNION_WITH("$unionWith"),

    UNWIND("$unwind"),

    MERGE("$merge"),

    REPLACE_ROOT("$replaceRoot"),
    
    MERGE_OBJECTS("$mergeObjects"),

    EACH("$each"),

    POSITION("$position"),

    SLICE("$slice"),

    ;

    AggregateEnum(String value) {
        this.value = value;
    }

    private final String value;

    public String getValue() {
        return value;
    }
}
