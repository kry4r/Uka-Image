package com.uka.image.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uka.image.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * User mapper interface
 * 
 * @author Uka Team
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * Find user by username
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    User findByUsername(@Param("username") String username);

    /**
     * Find user by email
     */
    @Select("SELECT * FROM users WHERE email = #{email} AND deleted = 0")
    User findByEmail(@Param("email") String email);

    /**
     * Check if username exists
     */
    @Select("SELECT COUNT(*) FROM users WHERE username = #{username} AND deleted = 0")
    int countByUsername(@Param("username") String username);

    /**
     * Check if email exists
     */
    @Select("SELECT COUNT(*) FROM users WHERE email = #{email} AND deleted = 0")
    int countByEmail(@Param("email") String email);
}