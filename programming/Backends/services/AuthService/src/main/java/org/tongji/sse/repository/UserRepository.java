package org.tongji.sse.repository;

// 导入用户实体
import org.tongji.sse.entity.User;
// Spring Data JPA
import org.springframework.data.jpa.repository.JpaRepository;
// Java Optional类
import java.util.Optional;

/**
 * 用户数据访问接口
 * Spring Data JPA会自动实现这个接口的所有方法
 * 我们只需要定义方法签名，Spring会生成SQL查询
 */
// 继承JpaRepository<User, Long>表示：
// 1. 操作User实体
// 2. 主键类型是Long
public interface UserRepository extends JpaRepository<User, Long> {
    // 根据用户名查找用户，返回Optional避免空指针
    Optional<User> findByUsername(String username);
    
    // 检查用户名是否存在
    boolean existsByUsername(String username);
    
    // 检查邮箱是否存在
    boolean existsByEmail(String email);
    
    // 检查手机号是否存在
    boolean existsByPhoneNumber(String phoneNumber);
    
    // 根据手机号查找用户
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    // 根据邮箱查找用户
    Optional<User> findByEmail(String email);
}