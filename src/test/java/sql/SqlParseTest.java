package sql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.sql.parser.SQLExprParser;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.druid.util.JdbcUtils;
import org.junit.Test;

import java.util.*;

/**
 * @author liuxin
 * 2022/3/26 01:28
 * @link https://github.com/alibaba/druid/wiki/SQL-Parser
 */
public class SqlParseTest {

    @Test
    public void set() {
        String sql = "select * from t where t.id in (select id from users)";
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        SQLStatement sqlStatement = sqlStatements.get(0);
        System.out.println(sqlStatement.getChildren());
    }

    @Test
    public void set2() {
        String sql = "select u.id,u.name from user as u where u.id = 1 and u.name = 'xi' and u.age = ?";
        // 新建 MySQL Parser
        SQLStatementParser parser = new MySqlStatementParser(sql);
        // 使用Parser解析生成AST，这里SQLStatement就是AST
        SQLStatement statement = parser.parseStatement();
        // 使用visitor来访问AST
        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
//        statement.accept(visitor);
        // 从visitor中拿出你所关注的信息
//        System.out.println(visitor.getColumns());
        List<TableStat.Condition> conditions = visitor.getConditions();
//        System.out.println(conditions);
        if (statement instanceof SQLSelectStatement) {
            SQLSelectStatement sqlSelectStatement = (SQLSelectStatement) statement;
            SQLSelect select = sqlSelectStatement.getSelect();
            System.out.println(select);
            SQLSelectQueryBlock queryBlock = select.getQueryBlock();
            List<SQLSelectItem> selectList = queryBlock.getSelectList();
            for (SQLSelectItem sqlSelectItem : selectList) {
                String alias = sqlSelectItem.getAlias();
                SQLExpr expr = sqlSelectItem.getExpr();
            }
            SQLTableSource from = queryBlock.getFrom();
            System.out.println(from);
            SQLExpr where = queryBlock.getWhere();
            System.out.println(where);
            List<SQLObject> childrens = where.getChildren();
            for (SQLObject children : childrens) {
                if (children instanceof SQLIdentifierExpr) {
                    System.out.println(((SQLIdentifierExpr) children));
                }
                if (children instanceof SQLBinaryOpExpr) {
                    SQLBinaryOpExpr children1 = (SQLBinaryOpExpr) children;
                    SQLExpr left = children1.getLeft();
                    if (left instanceof SQLIdentifierExpr) {
                        System.out.println(left);
                    }
                    if (left instanceof SQLPropertyExpr) {
                        SQLPropertyExpr right1 = (SQLPropertyExpr) left;
                        System.out.println(right1);
                    }
                    SQLExpr right = children1.getRight();
                    if (right instanceof SQLIntegerExpr) {
                        System.out.println(((SQLIntegerExpr) right));
                    }
                    if (right instanceof SQLCharExpr) {
                        System.out.println(((SQLCharExpr) right));
                    }
                    //
                    if (right instanceof SQLVariantRefExpr) {
                        System.out.println(right);
                    }

                }
            }
        }
    }

    public static <T> T cast(Object o, Class<T> type) {
        return ((T) o);
    }

    @Test
    public void SQLSelect() {
        String sql = "select id as 'userId',name as 'userName',age as 'userAge' from users where age = 18";
        SQLStatementParser parser = new MySqlStatementParser(sql);
        SQLStatement sqlStatement = parser.parseStatement();

        System.out.println(SQLUtils.toSQLString(sqlStatement, "mysql"));
        // db类型
        String dbType = sqlStatement.getDbType();
        System.out.println("dbType:" + dbType);
        SQLSelectStatement sqlSelectStatement = cast(sqlStatement, SQLSelectStatement.class);
        SQLSelect select = sqlSelectStatement.getSelect();
        SQLSelectQuery query = select.getQuery();
        MySqlSelectQueryBlock mysqlQueryBlock = cast(query, MySqlSelectQueryBlock.class);
        // 这里的sql,没有用到别名,所以是一个SQLIdentifierExpr
        List<SQLSelectItem> selectList = mysqlQueryBlock.getSelectList();
        for (SQLSelectItem sqlSelectItem : selectList) {
            String queryColumnAlias = sqlSelectItem.getAlias();
            SQLExpr expr = sqlSelectItem.getExpr();
            if (expr instanceof SQLIdentifierExpr) {
                SQLIdentifierExpr columnIdentifier = cast(expr, SQLIdentifierExpr.class);
                String queryColumnName = columnIdentifier.getName();
                System.out.println("列名:" + queryColumnName + ",别名:" + queryColumnAlias);
            }
        }
        // where age = 18
        SQLExpr where = mysqlQueryBlock.getWhere();
        List<SQLObject> childrenList = where.getChildren();
        for (SQLObject sqlObject : childrenList) {
            if (sqlObject instanceof SQLBinaryOpExpr){
                SQLBinaryOpExpr whereItem = cast(sqlObject, SQLBinaryOpExpr.class);
                // 这里没有用到别名就是 SQLIdentifierExpr
                SQLExpr left = whereItem.getLeft();
                // 数字类型是
                SQLExpr right = whereItem.getRight();
                if (left instanceof SQLIdentifierExpr){
                    String name = cast(left, SQLIdentifierExpr.class).getName();
                    System.out.println(name);
                }
                if (right instanceof SQLIntegerExpr){
                    Object value = cast(right, SQLIntegerExpr.class).getValue();
                    System.out.println(value);
                }
            }
        }

    }

    @Test
    public void sec() {
        String sql = "select * from users";
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("id", 12L);
        conditions.put("name", "liu");
        System.out.println(search(sql, conditions));
    }

    public String search(String sql, Map<String, Object> conditions) {
        List<Map<String, Object>> result = new ArrayList<>();
        // SQLParserUtils.createSQLStatementParser可以将sql装载到Parser里面
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcUtils.MYSQL);
        // parseStatementList的返回值SQLStatement本身就是druid里面的语法树对象
        List<SQLStatement> stmtList = parser.parseStatementList();


        SQLStatement stmt = stmtList.get(0);
        if (stmt instanceof SQLSelectStatement) {
            // convert conditions to 'and' statement
            StringBuffer constraintsBuffer = new StringBuffer();
            Set<String> keys = conditions.keySet();
            Iterator<String> keyIter = keys.iterator();
            if (keyIter.hasNext()) {
                constraintsBuffer.append(keyIter.next()).append(" = ?");
            }
            while (keyIter.hasNext()) {
                constraintsBuffer.append(" AND ").append(keyIter.next()).append(" = ?");
            }
            SQLExprParser constraintsParser = SQLParserUtils.createExprParser(constraintsBuffer.toString(), JdbcUtils.MYSQL);
            SQLExpr constraintsExpr = constraintsParser.expr();

            SQLSelectStatement selectStmt = (SQLSelectStatement) stmt;
            // 拿到SQLSelect 通过在这里打断点看对象我们可以看出这是一个树的结构
            SQLSelect sqlselect = selectStmt.getSelect();
            SQLSelectQueryBlock query = (SQLSelectQueryBlock) sqlselect.getQuery();
            SQLExpr whereExpr = query.getWhere();
            // 修改where表达式
            if (whereExpr == null) {
                query.setWhere(constraintsExpr);
            } else {
                SQLBinaryOpExpr newWhereExpr = new SQLBinaryOpExpr(whereExpr, SQLBinaryOperator.BooleanAnd, constraintsExpr);
                query.setWhere(newWhereExpr);
            }
            sqlselect.setQuery(query);
            sql = sqlselect.toString();

        }
        return sql;
    }

}
