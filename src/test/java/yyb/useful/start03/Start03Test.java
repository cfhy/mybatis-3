package yyb.useful.start03;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;
import yyb.model.Blog;
import yyb.useful.start03.mapper.BlogMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yyb
 * @date 2019/5/30 15:24
 * @description 高级映射
 */
public class Start03Test {
    @Test
    public void test() throws IOException {
        String resource = "yyb/useful/start03/mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession session = sqlSessionFactory.openSession();
        try {
            BlogMapper mapper = session.getMapper(BlogMapper.class);
            Blog blog = mapper.selectBlogDetails(1);
            System.out.println(blog);
        } finally {
            session.close();
        }
    }

    /**
     * 动态代理测试
     */
    @Test
    public void testProxy(){
        MapperProxy mapperProxy=new MapperProxy();
        BlogMapper mapper =(BlogMapper)Proxy.newProxyInstance(BlogMapper.class.getClassLoader(), new Class[]{BlogMapper.class}, mapperProxy);
        Blog blog = mapper.selectBlogDetails(10);
    }

    /**
     * 泛型测试
     */
    @Test
    public void testGeneralType() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        Class c = arr.getClass();
        Method m = c.getMethod("add",Object.class);
        m.invoke(arr,"add_string");

        Type mySuperClass = new ArrayList<String>() {

        }.getClass().getGenericSuperclass();
        System.out.println(mySuperClass);
        //https://www.jianshu.com/p/f292b1bde719
        Type type = ((ParameterizedType) mySuperClass).getActualTypeArguments()[0];
        System.out.println(type);
    }
}
