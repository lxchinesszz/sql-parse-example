package sql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectGroupByClause;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import org.junit.Test;

import java.util.List;

/**
 * @author liuxin
 * 2022/3/26 23:37
 */
public class SQLSelectGroupByTest {

    @Test
    public void groupBy() {
        SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement("select name,count(1) as count from users group by name,age having count > 2");
        SQLSelectStatement selectStatement = Utils.cast(sqlStatement, SQLSelectStatement.class);
        SQLSelect select = selectStatement.getSelect();
        SQLSelectQueryBlock query = Utils.cast(select.getQuery(), SQLSelectQueryBlock.class);
        SQLSelectGroupByClause groupBy = query.getGroupBy();
        List<SQLExpr> items = groupBy.getItems();
        for (SQLExpr item : items) {
            // group by name
            // group by age
            SQLIdentifierExpr groupByColumn = Utils.cast(item, SQLIdentifierExpr.class);
            Utils.print("group by {}", groupByColumn);
        }
    }

    @Test
    public void having() {
        SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement("select name,count(1) as count from users group by name,age having count > 2");
        SQLSelectStatement selectStatement = Utils.cast(sqlStatement, SQLSelectStatement.class);
        SQLSelect select = selectStatement.getSelect();
        SQLSelectQueryBlock query = Utils.cast(select.getQuery(), SQLSelectQueryBlock.class);
        SQLSelectGroupByClause groupBy = query.getGroupBy();
        SQLExpr having = groupBy.getHaving();
        // 因为只有一个条件,所以having就是SQLBinaryOpExpr
        SQLBinaryOpExpr havingExpr = Utils.cast(having, SQLBinaryOpExpr.class);
        // 没有使用别名,所以就是SQLIdentifierExpr
        SQLExpr left = havingExpr.getLeft();
        SQLIdentifierExpr leftExpr = Utils.cast(left, SQLIdentifierExpr.class);
        // 数字类型就是
        SQLExpr right = havingExpr.getRight();
        SQLValuableExpr rightValue = Utils.cast(right, SQLValuableExpr.class);
        SQLBinaryOperator operator = havingExpr.getOperator();
        // left:count, operator:>,right:2
        Utils.print("left:{}, operator:{},right:{}", leftExpr.getName(), operator.name, rightValue.getValue());
    }


}
