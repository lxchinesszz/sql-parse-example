package sql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.*;
import org.junit.Test;

/**
 * @author liuxin
 * 2022/3/26 20:34
 */
public class SQLSelectQueryTest {

    /**
     * SQLSelectStatement包含一个SQLSelect，SQLSelect包含一个SQLSelectQuery。SQLSelectQuery有主要的两个派生类，
     * 分别是SQLSelectQueryBlock(单表sql查询)和SQLUnionQuery(联合查询)。
     */
    @Test
    public void SQLSelectQuery() {
        // true
        System.out.println(parseSQLSelectQuery("select * from users") instanceof SQLSelectQueryBlock);
        // true
        System.out.println(parseSQLSelectQuery("select name from users union select name from school") instanceof SQLUnionQuery);
    }

    public SQLSelectQuery parseSQLSelectQuery(String sql) {
        SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement(sql);
        SQLSelectStatement sqlSelectStatement = Utils.cast(sqlStatement, SQLSelectStatement.class);
        SQLSelect select = sqlSelectStatement.getSelect();
        return select.getQuery();
    }
}
