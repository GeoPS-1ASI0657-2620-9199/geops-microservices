package com.geopslabs.geops.notification.application.services;

import org.springframework.transaction.support.*;
public class TestTransactions implements TransactionOperations {
    @Override
    public <T> T execute(TransactionCallback<T> action) { return action.doInTransaction(new SimpleTransactionStatus()); }
}
