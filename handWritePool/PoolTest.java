package handWritePool;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class PoolTest {
    public static void main(String[] args) throws Exception{
        Properties pro=new Properties();
        pro.load(PoolTest.class.getClassLoader().getResourceAsStream("jdbc-JL.properties"));
        final DataSource dataSource=new BasicDataSource().getDataSource(pro);
        for (int i=0;i<300;++i){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Connection conn=dataSource.getConnection();
                         Thread.sleep(100);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
