package sql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author liuxin
 * 2022/3/26 20:35
 */
public class Utils {

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o, Class<T> type) {
        return ((T) o);
    }

    public static <T> void startParse(String taskName, Consumer<T> consumer, T args) {
        System.out.println("---------------------" + taskName + "--------------------");
        consumer.accept(args);
    }

    public static void print(String format, Object... args) {
        System.err.println(MessageFormatter.arrayFormat(format, args).getMessage());
    }

    public static SQLSelectQuery parseSQLSelectQuery(String sql) {
        SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement(sql);
        SQLSelectStatement sqlSelectStatement = Utils.cast(sqlStatement, SQLSelectStatement.class);
        SQLSelect select = sqlSelectStatement.getSelect();
        return select.getQuery();
    }

    public static void parseSQLBinaryOpExpr(SQLBinaryOpExpr conditionBinary) {
        SQLExpr conditionExpr = conditionBinary.getLeft();
        SQLExpr conditionValueExpr = conditionBinary.getRight();
        // 左边有别名所以是SQLPropertyExpr
        if (conditionExpr instanceof SQLPropertyExpr) {
            SQLPropertyExpr conditionColumnExpr = cast(conditionExpr, SQLPropertyExpr.class);
            // 右边根据类型进行转换 id是SQLIntegerExpr name是SQLCharExpr
            SQLValuableExpr conditionColumnValue = cast(conditionValueExpr, SQLValuableExpr.class);
            print("条件列名:{},条件别名:{},条件值:{}", conditionColumnExpr.getName(), conditionColumnExpr.getOwnernName(), conditionColumnValue);
        }
        // 如果没有别名
        if (conditionExpr instanceof SQLIdentifierExpr) {
            SQLIdentifierExpr conditionColumnExpr = cast(conditionExpr, SQLIdentifierExpr.class);
            SQLValuableExpr conditionColumnValue = cast(conditionValueExpr, SQLValuableExpr.class);
            print("条件列名:{},条件值:{}", conditionColumnExpr.getName(), conditionColumnValue);
        }
    }

    /**
     * 判断where要
     * 1. 注意是SQLBinaryOpExpr(id = 1) or (u.id = 1) 需要注意是否使用了别名<br>
     * 2. 注意如果只有一个查询添加 where本身就是一个SQLBinaryOpExpr，如果是多个就要用 where.getChildren()<br></>
     * 如果有别名: SQLPropertyExpr(name = id , ownerName = u)<br>
     * 如果没别名: SQLIdentifierExpr(name = id) <br></>
     * 值对象: SQLValuableExpr
     *
     * @param where 条件对象
     */
    public static void parseWhere(SQLExpr where) {
        if (where instanceof SQLBinaryOpExpr) {
            parseSQLBinaryOpExpr(cast(where, SQLBinaryOpExpr.class));
        } else {
            List<SQLObject> childrenList = where.getChildren();
            for (SQLObject sqlObject : childrenList) {
                // 包含了 left 和 right
                SQLBinaryOpExpr conditionBinary = cast(sqlObject, SQLBinaryOpExpr.class);
                parseSQLBinaryOpExpr(conditionBinary);
            }
        }

    }
}
