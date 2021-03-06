package com.rem.cs.data.jpa.repository;

import com.rem.cs.data.jpa.entity.Role;
import com.rem.cs.data.jpa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    @Query("select u from User as u where u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("select r from Role as r join r.users as u where u.id = :id")
    List<Role> findRolesById(@Param("id") String id);

    @Query("select count(u) from User as u where u.email = :email")
    long countByEmail(@Param("email") String email);
}
