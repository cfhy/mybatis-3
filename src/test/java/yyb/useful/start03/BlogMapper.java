package yyb.useful.start03;


import yyb.model.Blog;

/**
 * @author yyb
 * @date 2019/5/30 15:43
 * @description
 */
public interface BlogMapper {
   Blog selectBlogDetails(int id);
}
