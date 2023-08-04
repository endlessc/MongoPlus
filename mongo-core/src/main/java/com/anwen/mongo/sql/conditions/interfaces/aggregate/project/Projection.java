package com.anwen.mongo.sql.conditions.interfaces.aggregate.project;

import com.anwen.mongo.sql.support.SFunction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author JiaChaoYang
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Projection {

    private String column;

    private Integer value;

    public static <T> Projection chain(SFunction<T,Object> column, Boolean value){
        return Projection.builder().column(column.getFieldNameLine()).value(value ? 1 : 0).build();
    }

    public static <T> Projection chain(SFunction<T,Object> column, Integer value){
        return Projection.builder().column(column.getFieldNameLine()).value(value).build();
    }

}
