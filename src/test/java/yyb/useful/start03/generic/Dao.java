package yyb.useful.start03.generic;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author yyb
 * @date 2019/9/9 14:23
 * @description
 */
public class Dao <T>
{
    public Dao() throws IllegalAccessException, InstantiationException {
        Type superclass = getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = null;
        if (superclass instanceof ParameterizedType) {
            parameterizedType = (ParameterizedType) superclass;
            Type[] typeArray = parameterizedType.getActualTypeArguments();
            if (typeArray != null && typeArray.length > 0) {
                Class clazz=(Class)typeArray[0];
                Student s = (Student)clazz.newInstance();
                System.out.println(s);
            }
        }
    }
}
