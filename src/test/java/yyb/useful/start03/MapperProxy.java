package yyb.useful.start03;

import yyb.model.Blog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author yyb
 * @date 2019/7/3 09:45
 * @description
 */
public class MapperProxy implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("aaa");
        return new Blog();
    }
}
