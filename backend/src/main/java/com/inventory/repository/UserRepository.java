package com.inventory.repository;

import com.inventory.entity.User;
import com.inventory.enums.Role;
import com.inventory.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleAndLocationContainingIgnoreCase(Role role, String location);

    List<User> findByStatus(UserStatus status);

    long countByRole(Role role);

    long countByRoleAndLocation(Role role, String location);

    List<User> findByRoleAndStatus(Role role, UserStatus status);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
            "(:location IS NULL OR LOWER(u.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> searchUsers(@Param("role") Role role,
                           @Param("location") String location,
                           @Param("status") UserStatus status,
                           @Param("search") String search);

    @Query("SELECT u.location, COUNT(u) FROM User u WHERE u.role = :role GROUP BY u.location")
    List<Object[]> countByRoleGroupedByLocation(@Param("role") Role role);
}
