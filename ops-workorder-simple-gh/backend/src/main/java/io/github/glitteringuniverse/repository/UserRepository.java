package io.github.glitteringuniverse.repository;

import io.github.glitteringuniverse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    List<User> findByDepartment(User.Department department);
    
    List<User> findByRole(User.Role role);
    
    List<User> findByOrganizationLevel(String organizationLevel);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.department = :department")
    List<User> findByRoleAndDepartment(@Param("role") User.Role role, @Param("department") User.Department department);
    
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.role IN (:approverRole, :managerRole)")
    List<User> findApprovers(@Param("approverRole") User.Role approverRole, @Param("managerRole") User.Role managerRole);
    
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.role = :role")
    List<User> findOperators(@Param("role") User.Role role);
}
