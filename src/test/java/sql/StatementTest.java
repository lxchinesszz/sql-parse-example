package sql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.*;
import org.junit.Test;

import java.util.List;

/**
 * @author liuxin
 * 2022/3/26 20:21
 */
public class StatementTest {

    @Test
    public void statement() {
        System.out.println(SQLUtils.parseSingleMysqlStatement("select * from users") instanceof SQLSelectStatement);
        System.out.println(SQLUtils.parseSingleMysqlStatement("insert into users(id,name,age) values (1,'孙悟空',500)") instanceof SQLInsertStatement);
        System.out.println(SQLUtils.parseSingleMysqlStatement("update users set name = '唐僧' where id = 1 ") instanceof SQLUpdateStatement);
        System.out.println(SQLUtils.parseSingleMysqlStatement("delete from users where id = 1") instanceof SQLDeleteStatement);
    }

    @Test
    public void SQLDeleteStatement(){
        SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement("delete from users where id = 1");
        SQLDeleteStatement sqlDeleteStatement = Utils.cast(sqlStatement, SQLDeleteStatement.class);
        sqlDeleteStatement.addCondition(SQLUtils.toSQLExpr("name = '孙悟空'"));
//        DELETE FROM users
//        WHERE id = 1
//        AND name = '孙悟空'
        System.out.println(SQLUtils.toSQLString(sqlDeleteStatement));
    }


    @Test
    public void SQLDeleteStatement2(){
        SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement("delete from users where id = 1");
        SQLDeleteStatement sqlDeleteStatement = Utils.cast(sqlStatement, SQLDeleteStatement.class);
        SQLExpr where = sqlDeleteStatement.getWhere();
        SQLBinaryOpExpr sqlBinaryOpExpr = Utils.cast(where, SQLBinaryOpExpr.class);
//        DELETE FROM users
//        WHERE id = 2
        sqlBinaryOpExpr.setRight(SQLUtils.toSQLExpr("2"));
        System.out.println(SQLUtils.toSQLString(sqlDeleteStatement));
    }

    @Test
    public void SQLUpdateStatement() {
        SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement("update users u set u.name = '唐僧',age = 18 where u.id = 1 ");
        SQLUpdateStatement sqlUpdateStatement = Utils.cast(sqlStatement, SQLUpdateStatement.class);
        List<SQLUpdateSetItem> setItems = sqlUpdateStatement.getItems();
        for (SQLUpdateSetItem setItem : setItems) {
            SQLExpr column = setItem.getColumn();
            if (column instanceof SQLPropertyExpr) {
                SQLPropertyExpr sqlPropertyExpr = Utils.cast(column, SQLPropertyExpr.class);
                SQLExpr value = setItem.getValue();
                Utils.print("column:{},列owner:{},value:{}", sqlPropertyExpr.getName(), sqlPropertyExpr.getOwnernName(), value);
            }
            if (column instanceof SQLIdentifierExpr) {
                SQLExpr value = setItem.getValue();
                Utils.print("column:{},value:{}", column, value);
            }
        }
        SQLExpr where = sqlUpdateStatement.getWhere();
        Utils.startParse("解析where", Utils::parseWhere, where);
    }
}
