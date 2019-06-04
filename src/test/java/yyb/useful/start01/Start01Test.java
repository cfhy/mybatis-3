package yyb.useful.start01;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;
import yyb.model.Blog;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author yyb
 * @date 2019/5/30 15:24
 * @description
 */
public class Start01Test {
    @Test
    public void test() throws IOException {
        String resource = "yyb/start01/mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession session = sqlSessionFactory.openSession();
        try {
            //1.老的使用方式
            Blog blog = (Blog) session.selectOne("yyb.start01.BlogMapper.selectBlog", 101);
            //2.新的使用方式
            BlogMapper mapper = session.getMapper(BlogMapper.class);
            Blog blog1 = mapper.selectBlog(101);
        } finally {
            session.close();
        }
    }
}
