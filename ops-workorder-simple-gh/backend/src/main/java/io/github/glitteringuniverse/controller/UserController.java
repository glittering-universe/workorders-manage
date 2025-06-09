package io.github.glitteringuniverse.controller;

import io.github.glitteringuniverse.entity.User;
import io.github.glitteringuniverse.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176"}, allowCredentials = "true")
public class UserController {
    
    private final UserService userService;
    
    @PostMapping("/login")
    public Optional<User> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        return userService.authenticate(username, password);
    }
    
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
    
    @GetMapping("/approvers")
    public List<User> getApprovers() {
        return userService.getApprovers();
    }
    
    @GetMapping("/operators")
    public List<User> getOperators() {
        return userService.getOperators();
    }
    
    @GetMapping("/by-department")
    public List<User> getUsersByDepartment(@RequestParam String department) {
        return userService.getUsersByDepartment(User.Department.valueOf(department));
    }
    
    @GetMapping("/by-role")
    public List<User> getUsersByRole(@RequestParam String role) {
        return userService.getUsersByRole(User.Role.valueOf(role));
    }
    
    @GetMapping("/by-level")
    public List<User> getUsersByLevel(@RequestParam String level) {
        return userService.getUsersByOrganizationLevel(level);
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }
    
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }
    
    @PostMapping("/{id}/disable")
    public void disableUser(@PathVariable Long id) {
        userService.disableUser(id);
    }
    
    @PostMapping("/{id}/reset-password")
    public void resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String newPassword = request.get("password");
        userService.resetPassword(id, newPassword);
    }
    
    @PostMapping("/encode-password")
    public Map<String, String> encodePassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String encoded = userService.encodePassword(password);
        Map<String, String> result = new HashMap<>();
        result.put("encoded", encoded);
        return result;
    }
}
