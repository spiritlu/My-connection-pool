package handWritePool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ConcurrentSkipListSet;

/*
自定义的连接池的连接对象类型 （相当于connection的抽象描述）继承定义的抽象的连接池这个类
好处是：将想要实现的核心方法在该类进行具体的实现
-----------------------------------------------------------------------------------------------------
7个基本属性：数据源、和数据库保持的连接、未使用的序号、已经使用过的序号、当前连接的序号、
连接是否处于空闲状态、连接开始空闲的时间起点
关键点：将所有的序号设置为ConcurrentSkipListSet类型，不仅保证了线程安全，而且保证不重复这一特性。
------------------------------------------------------------------------------------------------------
方法详解：
1、初始化连接的相关信息：该连接还未使用，将连接开始空闲的时间起点设置为空，
创建新的数据库连接，获取到数据库的url、账号以及密码，并给连接分配相应的序号
（分配未使用过的序号，那么这个分配的序号即刻设置为使用过的，并把这个序号设置为当前连接的序号），将新创建
的连接设置为空闲的状态
2、setPoolDataSource设置数据源，主要的目的是为了设置未使用的全局序号，将未使用的序号设置为最大连接数量+1即可
3、将连接归还到线程池当中：将连接设置为空闲状态，重置连接的空闲的时间起点
4、释放连接资源：被释放的连接，需要把连接的序号归还给set集合并且释放数据库连接资源
 */
public class PoolConnectionImpl extends PoolConnection{
    //连接数据库用的数据源的信息
    private static PoolDataSource poolDataSource;

    //和数据库保持的连接
    private Connection connection;

    //全局标识连接的序号  未使用的序号
    private static ConcurrentSkipListSet<Integer> unuseConnectionNoSet;

    //全局标识连接的序号  已经使用的序号
    private static ConcurrentSkipListSet<Integer> usedConnectionNoSet;

    //连接的序号
    private int connectionNo;

    //连接是否处于空闲状态
    private boolean idle;

    //连接开始空闲时间的起点
    private Long endUseTime;

    //初始化连接信息
    public PoolConnectionImpl(){
        //如果连接还未使用，连接开始空闲的时间起点置为null
        this.endUseTime=null;

        //创建新的数据库连接，获取到数据源的url、账号以及密码。
        try {
            this.connection = DriverManager.getConnection(poolDataSource.getUrl(),
                                                           poolDataSource.getUsername(),
                                                           poolDataSource.getUserpwd());
        }catch (SQLException e){
            e.printStackTrace();
        }

        //给当前的连接分配一个全局的序号
        int no=unuseConnectionNoSet.pollFirst();
        usedConnectionNoSet.add(no);
        this.connectionNo=no;

        //将新创建的连接设置为空闲状态
        this.idle=true;
    }

    public static void setPoolDataSource(PoolDataSource poolDataSource){
        PoolConnectionImpl.poolDataSource=poolDataSource;

        //加载数据库驱动
        try {
            Class.forName(poolDataSource.getDriverName());
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }

        //根据最大连接数量，创建下标序号集合
        if(unuseConnectionNoSet==null){
            unuseConnectionNoSet=new ConcurrentSkipListSet<>();
            for(int i=0;i<poolDataSource.getMaxPoolSize();i++) {
                unuseConnectionNoSet.add(i+1);
            }
        }
        if (usedConnectionNoSet==null){
            unuseConnectionNoSet=new ConcurrentSkipListSet<>();
        }
    }
   /*
   将连接归还到线程池当中
    */
    @Override
    public void close() throws SQLException {
        idle=true;
        endUseTime=new Date().getTime();
        System.out.println("连接 NO："+connectionNo+"已归还到连接池");
    }
    /*
    释放连接的资源
     */
    public void releaseConnection(){
        //被释放的连接，需要把连接的序号归还给set集合
        usedConnectionNoSet.remove(connectionNo);
        unuseConnectionNoSet.add(connectionNo);
        try {
            //释放连接数据库的连接资源
            this.connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public int getConnectionNo() {
        return connectionNo;
    }

    public void setConnectionNo(int connectionNo) {
        this.connectionNo = connectionNo;
    }

    public Long getEndUseTime() {
        return endUseTime;
    }

    public void setEndUseTime(Long endUseTime) {
        this.endUseTime = endUseTime;
    }

    public boolean isIdle() {
        return idle;
    }

    public void setIdle(boolean idle) {
        this.idle = idle;
    }
}
