package io.github.glitteringuniverse.service;

import io.github.glitteringuniverse.entity.User;
import io.github.glitteringuniverse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 用户登录验证
     */
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getEnabled() && passwordEncoder.matches(password, user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
    
    /**
     * 创建用户
     */
    @Transactional
    public User createUser(User user) {
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * 根据部门获取用户
     */
    public List<User> getUsersByDepartment(User.Department department) {
        return userRepository.findByDepartment(department);
    }
    
    /**
     * 根据角色获取用户
     */
    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }
    
    /**
     * 获取审批人员列表
     */
    public List<User> getApprovers() {
        return userRepository.findApprovers(User.Role.APPROVER, User.Role.DEPT_MANAGER);
    }
    
    /**
     * 获取操作人员列表
     */
    public List<User> getOperators() {
        return userRepository.findOperators(User.Role.OPERATOR);
    }
    
    /**
     * 根据组织层级获取用户
     */
    public List<User> getUsersByOrganizationLevel(String level) {
        return userRepository.findByOrganizationLevel(level);
    }
    
    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id).orElseThrow();
        
        user.setRealName(userDetails.getRealName());
        user.setEmail(userDetails.getEmail());
        user.setPhone(userDetails.getPhone());
        user.setRole(userDetails.getRole());
        user.setDepartment(userDetails.getDepartment());
        user.setOrganizationLevel(userDetails.getOrganizationLevel());
        user.setEnabled(userDetails.getEnabled());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    /**
     * 禁用用户
     */
    @Transactional
    public void disableUser(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    /**
     * 密码编码
     */
    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }
    
    /**
     * 验证密码
     */
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
