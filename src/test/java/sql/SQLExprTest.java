package sql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import org.junit.Test;

import java.util.List;

/**
 * @author liuxin
 * 2022/3/26 20:57
 */
public class SQLExprTest {

    /**
     * select id,name,age from users where id = 1 and name = '孙悟空'
     * - id,name,age 这里SQLExpr
     * - id = 1 和 name = 孙悟空 也是SQLExpr
     */
    @Test
    public void SQLExpr() {
        String sql = "select id,u.name,age from users as u where id = 1 and name = '孙悟空'"; // 解析SQL
        SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement(sql);
        // 因为我们的sql是一个查询语句,所以根据我们上面的介绍就是一个SQLSelectStatement
        SQLSelectStatement sqlSelectStatement = Utils.cast(sqlStatement, SQLSelectStatement.class);
        // 从查询语句顶级抽象中获取查询对象
        SQLSelect select = sqlSelectStatement.getSelect();
        SQLSelectQuery query = select.getQuery();
        // SQLSelectQuery 有两个实现 SQLSelectQueryBlock 和 SQLUnionQuery。这里我们先用SQLSelectQueryBlock举例
        SQLSelectQueryBlock queryBlock = Utils.cast(query, SQLSelectQueryBlock.class);
        // 首先我们拿到语句的from对象
        // 首先我们先拿到查询的字段,这里因为用到了别名
        List<SQLSelectItem> selectColumnList = queryBlock.getSelectList();
        for (SQLSelectItem sqlSelectItem : selectColumnList) {
            // id
            SQLExpr expr = sqlSelectItem.getExpr();
            System.out.println(expr);
        }
        // 拿到查询条件 u.id = 1 and u.name = '孙悟空' // 因为使用到了别名
        SQLExpr where = queryBlock.getWhere();
        List<SQLObject> childrenList = where.getChildren();
        for (SQLObject sqlObject : childrenList) {
            System.out.println(sqlObject);
        }
    }

    /**
     * 操作符相关: SQLBinaryOpExpr
     */
    @Test
    public void SQLBinaryOpExpr() {
        String sql = "select * from users where id > 1 and age = 18";
        SQLSelectQuery sqlSelectQuery = Utils.parseSQLSelectQuery(sql);
        SQLSelectQueryBlock selectQueryBlock = Utils.cast(sqlSelectQuery, SQLSelectQueryBlock.class);
        SQLExpr where = selectQueryBlock.getWhere();
        List<SQLObject> conditions = where.getChildren();
        // [id > 1 , age = 18] 出现了操作符所以是SQLBinaryOpExpr
        for (SQLObject condition : conditions) {
            SQLBinaryOpExpr conditionExpr = Utils.cast(condition, SQLBinaryOpExpr.class);
            SQLBinaryOperator operator = conditionExpr.getOperator();
            SQLIdentifierExpr conditionColumn = Utils.cast(conditionExpr.getLeft(), SQLIdentifierExpr.class);
            SQLValuableExpr conditionColumnValue = Utils.cast(conditionExpr.getRight(), SQLValuableExpr.class);
            Utils.print("条件字段:{},操作符号:{},条件值:{}", conditionColumn.getName(), operator.name, conditionColumnValue);
        }
    }

    @Test
    public void SQLVariantRefExpr() {
        String sql = "select * from users where id = ? and name = ?";
        SQLSelectQuery sqlSelectQuery = Utils.parseSQLSelectQuery(sql);
        SQLSelectQueryBlock selectQueryBlock = Utils.cast(sqlSelectQuery, SQLSelectQueryBlock.class);
        SQLExpr where = selectQueryBlock.getWhere();
        List<SQLObject> conditions = where.getChildren();
        // [id = ?] 出现了变量符,所以要用SQLVariantRefExpr
        for (SQLObject condition : conditions) {
            SQLBinaryOpExpr conditionExpr = Utils.cast(condition, SQLBinaryOpExpr.class);
            SQLBinaryOperator operator = conditionExpr.getOperator();
            SQLIdentifierExpr conditionColumn = Utils.cast(conditionExpr.getLeft(), SQLIdentifierExpr.class);
            SQLVariantRefExpr conditionColumnValue = Utils.cast(conditionExpr.getRight(), SQLVariantRefExpr.class);
            int index = conditionColumnValue.getIndex();
            Utils.print("条件字段:{},操作符号:{},索引位:{}", conditionColumn.getName(), operator.name, index);
        }
    }
}
