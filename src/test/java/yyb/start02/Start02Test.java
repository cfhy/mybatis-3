package yyb.start02;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;
import yyb.model.Blog;
import yyb.start01.BlogMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author yyb
 * @date 2019/5/30 15:24
 * @description
 */
public class Start02Test {
    @Test
    public void test() throws IOException {
        String resource = "yyb/start02/mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//        为了指定创建哪种环境，只要将它作为可选的参数传递给 SqlSessionFactoryBuilder 即可。可以接受环境配置的两个方法签名是：
//
//        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader, environment);
//        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader, environment, properties);
        SqlSession session = sqlSessionFactory.openSession();

    }
}
