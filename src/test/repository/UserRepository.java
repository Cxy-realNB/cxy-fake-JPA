package test.repository;

import base.annotation.Param;
import base.annotation.SQL;
import base.repository.BaseRepository;

import java.util.List;

/**
 * @author Wonder Chen
 */
public interface UserRepository<User> extends BaseRepository<User> {
    @SQL(value = "select * from user where id = :id and password = :password",nativeSQL = true)
    List<User> findUsersById(@Param("id")long id, @Param("password")String password);

}
