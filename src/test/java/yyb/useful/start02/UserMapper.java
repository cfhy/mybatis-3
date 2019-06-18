package yyb.useful.start02;

import org.apache.ibatis.submitted.rounding.User;

import java.util.List;

/**
 * @author yyb
 * @date 2019/6/18 14:13
 * @description
 */
public interface UserMapper {
    List<User> getUser();
}
