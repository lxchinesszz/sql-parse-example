> 本篇主要学习Druid 对Sql的语法解析。学习完之后,我们可以对任意sql进行解析,同时也可以基于AST语法树来生成sql语句。


## 一、AST

AST是abstract syntax tree的缩写，也就是抽象语法树。和所有的Parser一样，Druid Parser会生成一个抽象语法树。


在Druid中，AST节点类型主要包括SQLObject、SQLExpr、SQLStatement三种抽象类型。

```java 

interface SQLObject {}
interface SQLExpr extends SQLObject {}
interface SQLStatement extends SQLObject {}

interface SQLTableSource extends SQLObject {}
class SQLSelect extends SQLObject {}
class SQLSelectQueryBlock extends SQLObject {}
```

## 二、语法树解析

## 2.1 核心类介绍

### 2.1.1 SQLStatemment DQL & DML顶级抽象

- DQL 数据查询语言 select
- DML 数据操纵语言 insert update delete

最常用的Statement当然是SELECT/UPDATE/DELETE/INSERT，他们分别是

|核心类|说明|
|:--|:--|
|SQLSelectStatement|查询语句|
|SQLUpdateStatement|更新语句|
|SQLDeleteStatement|删除语句|
|SQLInsertStatement|新增语句|

```java 
@Test
public void statement() {
    // 以下全部 true
    System.out.println(SQLUtils.parseSingleMysqlStatement("select * from users") instanceof SQLSelectStatement);
    System.out.println(SQLUtils.parseSingleMysqlStatement("insert into users(id,name,age) values (1,'孙悟空',500)") instanceof SQLInsertStatement);
    System.out.println(SQLUtils.parseSingleMysqlStatement("update users set name = '唐僧' where id = 1 ") instanceof SQLUpdateStatement);
    System.out.println(SQLUtils.parseSingleMysqlStatement("delete from users where id = 1") instanceof SQLDeleteStatement);
}
```

### 2.1.2 SQLSelect SQL查询

SQLSelectStatement包含一个SQLSelect，SQLSelect包含一个SQLSelectQuery。SQLSelectQuery有主要的两个派生类，
分别是SQLSelectQueryBlock(单表sql查询)和SQLUnionQuery([union查询](https://www.w3school.com.cn/sql/sql_union.asp))。

```java 
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
```

### 2.1.3 SQLExpr

SQLExpr 是有几个实现类的。


```sql 
select id,name,age from users where id = 1 and name = '孙悟空';

select u.id, u.name from users as u where id = 1 and name = ?;
```


|核心类|举例|说明|适用范围|快速记忆|
|:--|:--|:--|:--|:--|
|SQLIdentifierExpr|id,name,age|SQLIdentifierExpr|查询字段或者where条件|唯一标记|
|SQLPropertyExpr|u.id,u.name|区别于SQLIdentifierExpr,适用于有别名的场景; SQLPropertyExpr.name = id, SQLPropertyExpr.owner = SQLIdentifierExpr = u）|查询字段或者where条件|有别名就是它|
|SQLBinaryOpExpr|id = 1, id > 5 |SQLBinaryOpExpr(left = SQLIdentifierExpr = id ,right = SQLValuableExpr = 1)|where条件|有操作符就是它|
|SQLVariantRefExpr|id = ?|变量|where条件|有变量符就是它|
|SQLIntegerExpr|id = 1|数字类型|值类型| - |
|SQLCharExpr|name = '孙悟空'|字符类型|值类型| - |

#### 2.1.3.1 SQLBinaryOpExpr

```java 
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
```

#### 2.1.3.2 SQLVariantRefExpr

```java 
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
```

### 2.1.4 SQLTableSource

常见的SQLTableSource包括SQLExprTableSource、SQLJoinTableSource、SQLSubqueryTableSource、SQLWithSubqueryClause.Entry


|核心类|举例|说明|快速记忆|
|:--|:--|:--|:--|:--|
|SQLExprTableSource|select * from emp where i = 3| name = SQLIdentifierExpr = emp| 单表查询 |
|SQLJoinTableSource|select * from emp e inner join org o on e.org_id = o.id| left = SQLExprTableSource(emp e),right = SQLExprTableSource(org o), condition = SQLBinaryOpExpr(e.org_id = o.id) | join 查询使用 |
|SQLSubqueryTableSource|select * from (select * from temp) a|from(...)是一个SQLSubqueryTableSource|子查询语句|
|SQLWithSubqueryClause| WITH RECURSIVE ancestors AS (SELECT * FROM org UNION SELECT f.* FROM org f, ancestors a WHERE f.id = a.parent_id ) SELECT * FROM ancestors; |ancestors AS (...) 是一个SQLWithSubqueryClause.Entry|with|


## 2.2 SQL语句解析示例

### 2.2.1 解析 Where

注意如果条件语句中只有一个条件,那么where就是一个 `SQLBinaryOpExpr`。
当条件大于2个,使用 `where.getChildren()`

```java 
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
```

### 2.2.2 解析 SQLSelectItem

解析查询的列信息

```java 
    /**
     * 解析查询字段,注意是否使用了别名.u.id as userId, u.name as userName, u.age as userAge<br>
     * userId（sqlSelectItem.getAlias）<br>
     * 如果有别名: u.id( id = SQLPropertyExpr.getName,u = SQLPropertyExpr.getOwnernName)<br>
     * 如果没别名: id(id = SQLIdentifierExpr.name)
     *
     * @param selectColumnList 查询字段
     */
    private void parseSQLSelectItem(List<SQLSelectItem> selectColumnList) {
        for (SQLSelectItem sqlSelectItem : selectColumnList) {
            // u.id as userId(selectColumnAlias)
            String selectColumnAlias = sqlSelectItem.getAlias();
            // u.id = SQLPropertyExpr
            SQLExpr expr = sqlSelectItem.getExpr();
            if (expr instanceof SQLPropertyExpr) {
                SQLPropertyExpr selectColumnExpr = cast(expr, SQLPropertyExpr.class);
                print("列名:{},别名:{},表别名:{}", selectColumnExpr.getName(), selectColumnAlias, selectColumnExpr.getOwnernName());
            }
            if (expr instanceof SQLIdentifierExpr) {
                SQLIdentifierExpr selectColumnExpr = cast(expr, SQLIdentifierExpr.class);
                print("列名:{},别名:{}", selectColumnExpr.getName(), selectColumnAlias);
            }
        }
    }
```

### 2.2.3 解析 SQLUpdateSetItem

```java 
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
```

## 三、语法树生成

前面的内容如果都搞清楚了,那么我们就能对sql进行解析,通知可以修改sql解析后的语法树,同时再将修改后的语法树,重新转换成sql

## 3.1 修改语法树

### 3.1.1 增加一个条件

```java 
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
```

### 3.1.2 修改一个条件值

将条件id = 1 修改成 id = 2

```java 
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
```

## 四、Visitor模式

访问者模式

所有的AST节点都支持Visitor模式，需要自定义遍历逻辑，可以实现相应的ASTVisitorAdapter派生类

```java 
    public static class CustomerMySqlASTVisitorAdapter extends MySqlASTVisitorAdapter {

        private final Map<String, SQLTableSource> ALIAS_MAP = new HashMap<String, SQLTableSource>();

        private final Map<String, SQLExpr> ALIAS_COLUMN_MAP = new HashMap<String, SQLExpr>();


        public boolean visit(SQLExprTableSource x) {
            String alias = x.getAlias();
            ALIAS_MAP.put(alias, x);
            return true;
        }

        @Override
        public boolean visit(MySqlSelectQueryBlock x) {
            List<SQLSelectItem> selectList = x.getSelectList();
            for (SQLSelectItem sqlSelectItem : selectList) {
                String alias = sqlSelectItem.getAlias();
                SQLExpr expr = sqlSelectItem.getExpr();
                ALIAS_COLUMN_MAP.put(alias, expr);
            }
            return true;
        }

        public Map<String, SQLTableSource> getAliasMap() {
            return ALIAS_MAP;
        }

        public Map<String, SQLExpr> getAliasColumnMap() {
            return ALIAS_COLUMN_MAP;
        }
    }

    @Test
    public void AliasVisitor() {
        String sql = "select u.id as userId, u.name as userName, age as userAge from users as u where u.id = 1 and u.name = '孙悟空' limit 2,10";
        // 解析SQL
        SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement(sql);
        CustomerMySqlASTVisitorAdapter customerMySqlASTVisitorAdapter = new CustomerMySqlASTVisitorAdapter();
        sqlStatement.accept(customerMySqlASTVisitorAdapter);
        // 表别名:{u=users}
        System.out.println("表别名:" + customerMySqlASTVisitorAdapter.getAliasMap());
        // 列别名{userName=u.name, userId=u.id, userAge=age}
        System.out.println("列别名" + customerMySqlASTVisitorAdapter.getAliasColumnMap());
    }
```
