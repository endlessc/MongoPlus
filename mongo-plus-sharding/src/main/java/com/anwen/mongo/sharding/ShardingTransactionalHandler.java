package com.anwen.mongo.sharding;

import com.anwen.mongo.annotation.transactional.MongoTransactional;
import com.anwen.mongo.cache.global.DataSourceNameCache;
import com.anwen.mongo.context.MongoTransactionStatus;
import com.anwen.mongo.context.ShardingTransactionContext;
import com.anwen.mongo.handlers.TransactionHandler;
import com.anwen.mongo.manager.MongoTransactionalManager;
import com.anwen.mongo.toolkit.ArrayUtils;
import com.anwen.mongo.toolkit.ClassTypeUtil;
import com.mongodb.client.ClientSession;

import java.util.function.Supplier;

/**
 * @author anwen
 */
public class ShardingTransactionalHandler extends TransactionHandler {

    @Override
    public Object transaction(Supplier<Object> supplier, MongoTransactional mongoTransactional) {
        ClientSession session = MongoTransactionalManager.getTransaction(mongoTransactional);
        MongoTransactionStatus status = MongoTransactionalManager.getTransactionStatus(session, null);
        MongoTransactionalManager.startTransaction(status);
        String currentDataSource = DataSourceNameCache.getDataSource();
        ShardingTransactionContext.addResourcesTransactionStatus(currentDataSource,status);
        try {
            Object result = supplier.get();
            ShardingTransactionContext.commitCurrentAllTransaction();
            return result;
        } catch (Exception e) {
            handleTransactionException(mongoTransactional, e);
            throw e;
        } finally {
            ShardingTransactionContext.closeAllSession();
        }
    }

    public static void handleTransactionException(MongoTransactional mongoTransactional, Exception e) {
        Class<? extends Exception> eClass = e.getClass();
        boolean finished = processRollback(mongoTransactional, eClass, true)
                || processRollback(mongoTransactional, eClass, false);
        if (!finished) {
            ShardingTransactionContext.rollbackAllTransaction();
        }
    }

    public static boolean processRollback(MongoTransactional mongoTransactional, Class<? extends Exception> eClass, boolean isRollback) {
        Class<? extends Throwable>[] exceptionList = isRollback ? mongoTransactional.rollbackFor() : mongoTransactional.noRollbackFor();
        if (ArrayUtils.isEmpty(exceptionList)) {
            return false;
        }
        for (Class<? extends Throwable> exceptionType : exceptionList) {
            if (ClassTypeUtil.isTargetClass(exceptionType, eClass)) {
                if (isRollback) {
                    ShardingTransactionContext.rollbackAllTransaction();
                } else {
                    ShardingTransactionContext.commitCurrentAllTransaction();
                }
                return true;
            }
        }
        return false;
    }

}
