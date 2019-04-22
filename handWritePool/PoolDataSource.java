package handWritePool;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/*
实现数据源的类 实现DataSource这个类

在实现数据源这一类当中，给出8个属性：驱动路径名称、连接数据库的url、账号、密码、初始化连接池大小、
最大空闲时间、最大的连接池数量以及连接池的对象，在构造方法的实现中，加上了连接池负责连接所需要的
数据源信息（直接将this作为参数传进去，省去了在连接时进行数据源相关信息的获取，简化代码量）、
将连接池的对象进行启动和初始化（直接将this作为参数传进去）
 */
public class PoolDataSource implements DataSource {
    //定义8个属性
    private String driverName; //驱动路径的名称
    private String url;  //连接数据库的url
    private String username;//账号
    private String userpwd; //密码
    private Integer initPoolSize=10;//初始化连接池的大小
    private Integer maxIdleTime=0;//最大空闲时间
    private Integer maxPoolSize=1024;//最大连接池数量

    private ConnectionPoolImpl connectionPoolImpl;//连接池的对象

    public PoolDataSource(String driverName,String url,String username,String userpwd,Integer initPoolSize,Integer maxIdleTime,Integer maxPoolSize){
        this.driverName=driverName;
        this.url=url;
        this.username=username;
        this.userpwd=userpwd;
        this.initPoolSize=initPoolSize;
        this.maxIdleTime=maxIdleTime;
        this.maxPoolSize=maxPoolSize;

        //给连接设置数据源，因为PoolConnectionImpl负责创建连接需要数据源的相关信息
        PoolConnectionImpl.setPoolDataSource(this);

        //启动连接池的实现
        connectionPoolImpl=new ConnectionPoolImpl(this);
        connectionPoolImpl.init();
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserpwd() {
        return userpwd;
    }

    public void setUserpwd(String userpwd) {
        this.userpwd = userpwd;
    }

    public Integer getInitPoolSize() {
        return initPoolSize;
    }

    public void setInitPoolSize(Integer initPoolSize) {
        this.initPoolSize = initPoolSize;
    }

    public Integer getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(Integer maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public ConnectionPoolImpl getConnectionPoolImpl() {
        return connectionPoolImpl;
    }

    public void setConnectionPoolImpl(ConnectionPoolImpl connectionPoolImpl) {
        this.connectionPoolImpl = connectionPoolImpl;
    }

    /*
    从连接池获取有效的连接
     */
    @Override
    public Connection getConnection() throws SQLException {
        return connectionPoolImpl.getConnection();
    }

    /*
    暂不支持该方法的调用，用户调用需要抛出异常
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        new UnsupportedOperationException("暂不支持该方法实现");
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
