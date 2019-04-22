package handWritePool;

import java.sql.Connection;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.Date;

/*
ScheduleAtFixedRate 每次执行时间为上一次任务开始起向后推一个时间间隔  ScheduleAtFixedRate 是基于固定时间间隔进行任务调度

连接池实现的主类
定义了四个属性：数据源、最大空闲时间的超时时间、存储初始化连接池的队列、存储连接池额外生成的连接的队列
为了保证高并发，使用的类型都是高并发链表
-------------------------------------------------------------------------------------------------------
方法解析：
1、通过数据源构造连接池对象，初始化数据成员：在这里将最大空闲时间的超时时间使用线程池，保证任务是并发执行,互不影响。
   设置线程池为守护线程，系统退出自动回收线程池的资源

2、初始化连接池功能：根据配置的最大空闲时间，启动定时清理连接池的任务，通过守护线程来启动线程池，进程关闭资源自动回收
   创建初始的连接到存储初始化连接池的队列
3、定时清理连接池的任务：遍历额外生成的连接队列，寻找不处于空闲状态的连接；
                        计算该连接连接开始的空闲时间起点与此刻时间的时间差，将该时间差与最大空闲时间进行比较，
                        如果时间差大于最大的空闲时间，则将该连接从额外生成的连接队列中删除，并进行释放连接操作
4、从连接池中获取连接：先从initSizeConnQueue初始连接队列里面寻找空闲的连接；
                      没有再从maxSizeConnQueue连接队列里面寻找空闲连接；
                      如果没有空闲连接，则创建新的连接返回；
                      如果maxSizeConnQueue连接队列已满，则循环检查直到有空闲的连接产生
 */
public class ConnectionPoolImpl {
      //连接数据库使用的数据源的信息
    private PoolDataSource poolDataSource;

    //用来记录maxIdleTime最大空闲时间的超时时间，释放多余的连接，连接池保证初始的连接量就可以了
    private ScheduledExecutorService idleTimeTask;

    //存储initPoolSize的连接，因为连接池最起码保证这些连接是存活的，因此单独存放
    private ConcurrentLinkedQueue<PoolConnectionImpl> initSizeConnQueue;

    //存储连接池中额外生成的连接，由于这里面的连接超时之后，需要回收，因此使用高并发链表存储
    private ConcurrentLinkedQueue<PoolConnectionImpl> maxSizeConnQueue;

    //通过数据源构造连接池对象，初始化数据成员
    public ConnectionPoolImpl(PoolDataSource poolDataSource){
        this.poolDataSource=poolDataSource;
        this.idleTimeTask=Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t=Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);//设置线程池为守护线程，系统退出自动回收线程池的资源
                return t;
            }
        });
        this.initSizeConnQueue=new ConcurrentLinkedQueue<>();
        this.maxSizeConnQueue=new ConcurrentLinkedQueue<>();
    }

    //初始化连接池功能
    public void init(){
        //根据配置的最大空闲时间，启动定时清理连接池的任务，通过守护线程来启动线程池，进程关闭资源自动回收
        idleTimeTask.scheduleAtFixedRate(new CleanPoolTask(),poolDataSource.getMaxIdleTime(),
                                          poolDataSource.getMaxPoolSize(),TimeUnit.SECONDS);
        //创建初始的连接
        for(int i=0;i<poolDataSource.getInitPoolSize();++i){
            initSizeConnQueue.add(new PoolConnectionImpl());
        }
    }

    //定时清理连接池的任务
    private class CleanPoolTask implements Runnable{
        @Override
        public void run() {
            System.out.println("执行连接池释放空间连接的任务！");
            Iterator<PoolConnectionImpl> it=maxSizeConnQueue.iterator();
            while (it.hasNext()){
                PoolConnectionImpl conn=it.next();
                if(conn.isIdle()){
                    long idleTime=new Date().getTime()-conn.getEndUseTime();
                    //连接超时，连接需要被释放
                    System.out.println("idleTime"+idleTime);
                    System.out.println("spend time："+poolDataSource.getMaxIdleTime()*1000);
                    if(idleTime>poolDataSource.getMaxIdleTime()*1000){
                        //从集合中删除该项
                        it.remove();
                        //释放连接
                        conn.releaseConnection();
                        System.out.println("连接NO："+conn.getConnectionNo()+"被释放了");
                    }
                }
            }
        }
    }

    //从连接池中获取连接
    public Connection getConnection(){
        /*
        1、先从initSizeConnQueue初始连接队列里面寻找空闲的连接
        2、没有再从maxSizeConnQueue连接队列里面寻找空闲连接
        3、如果没有空闲连接，则创建新的连接返回
        4、如果maxSizeConnQueue连接队列已满，则循环检查直到有空闲的连接产生
         */
        PoolConnectionImpl conn=null;
        int initQueueSize=initSizeConnQueue.size();
        Iterator<PoolConnectionImpl> it=initSizeConnQueue.iterator();
        while (it.hasNext()){
            conn=it.next();
            if(conn.isIdle()){
                conn.setIdle(false);
                System.out.println("从主连接池拿了连接NO："+conn.getConnectionNo());
                return conn;
            }
        }
        it=maxSizeConnQueue.iterator();
        while (it.hasNext()){
            conn=it.next();
            if(conn.isIdle()){
                conn.setIdle(false);
                System.out.println("从备用连接池拿了连接NO："+conn.getConnectionNo());
                return conn;
            }
        }
        if(maxSizeConnQueue.size()<
                       poolDataSource.getMaxPoolSize()-poolDataSource.getInitPoolSize()){
            conn=new PoolConnectionImpl();
            maxSizeConnQueue.add(conn);
            conn.setIdle(false);
            return conn;
        }
        //如果maxSizeConnQueue连接队列已满，则循环检测直到有空闲的连接产生
        for(;;){
            //优先使用初始的连接队列，其次使用备用连接队列
            it=initSizeConnQueue.iterator();
            while (it.hasNext()){
                conn=it.next();
                if(conn.isIdle()){
                    conn.setIdle(false);
                    System.out.println("等待完成从主连接池拿了连接NO："+conn.getConnectionNo());
                }
            }
            it=maxSizeConnQueue.iterator();
            while (it.hasNext()){
                conn=it.next();
                if(conn.isIdle()){
                    conn.setIdle(false);
                    System.out.println("等待完成从备用连接池拿了连接NO："+conn.getConnectionNo());
                    return conn;
                }
            }
        }
    }
}
