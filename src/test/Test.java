package test;

import base.boot.ApplicationBoot;
import base.dbconnection.DataSourceConnection;
import base.proxy.Executor;
import test.entity.User;
import test.repository.UserRepository;

import java.util.Date;
import java.util.List;


/**
 * @author Wonder Chen
 */
public class Test {
    public static void main(String[] args) {
        ApplicationBoot.initialize();
        UserRepository userRepository = Executor.getHandle(UserRepository.class);
//        List<User> users = userRepository.findUsersById(1,"123456");
//        int count = userRepository.count();
        User user = new User();
        user.setCreateTime(new Date());
        user.setEnable(true);
        user.setPassword("9999999");
        user.setUsername("testA");
//        user.setId(10);
        Object saveUser = userRepository.save(user);
        System.out.println();
    }
}
