package yyb.useful.start03.generic;

/**
 * @author yyb
 * @date 2019/9/9 14:24
 * @description 将Dao<T>这个泛型参数化
 */
public class SubDao extends Dao<Student> {

    public SubDao() throws IllegalAccessException, InstantiationException {
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        SubDao dao=new SubDao();
    }
}
