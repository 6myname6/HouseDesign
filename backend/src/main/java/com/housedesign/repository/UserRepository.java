package com.housedesign.repository;

import com.housedesign.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户数据访问层。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 按用户名查询用户（登录用）。 */
    Optional<User> findByUsername(String username);

    /** 判断用户名是否已存在（注册查重用）。 */
    boolean existsByUsername(String username);
}
