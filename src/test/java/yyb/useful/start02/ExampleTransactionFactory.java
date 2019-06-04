package yyb.useful.start02;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

/**
 * @author yyb
 * @date 2019/6/3 15:06
 * @description 使用 TransactionFactory 接口的实现类的完全限定名或类型别名代替它们。
 */
public class ExampleTransactionFactory implements TransactionFactory {
    @Override
    public void setProperties(Properties props) {

    }

    @Override
    public Transaction newTransaction(Connection conn) {
        return new ExampleTransaction();
    }

    @Override
    public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
        return null;
    }
}
