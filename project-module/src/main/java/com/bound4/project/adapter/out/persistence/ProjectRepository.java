package com.bound4.project.adapter.out.persistence;

import com.bound4.project.domain.Project;
import com.bound4.project.domain.ProjectStatus;
import com.bound4.project.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUser(User user);

    List<Project> findByUserAndStatus(User user, ProjectStatus status);

    @Query("SELECT p FROM Project p WHERE p.user = :user AND p.status = :status")
    Page<Project> findByUserAndStatus(@Param("user") User user, 
                                    @Param("status") ProjectStatus status, 
                                    Pageable pageable);

    List<Project> findByStatus(ProjectStatus status);

    @Query("SELECT p FROM Project p WHERE p.status = :status")
    Page<Project> findByStatus(@Param("status") ProjectStatus status, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.name LIKE %:namePattern%")
    List<Project> findByNameContaining(@Param("namePattern") String namePattern);

    @Query("SELECT p FROM Project p WHERE p.user = :user AND p.name LIKE %:namePattern%")
    List<Project> findByUserAndNameContaining(@Param("user") User user, 
                                            @Param("namePattern") String namePattern);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.user = :user AND p.status = :status")
    long countByUserAndStatus(@Param("user") User user, @Param("status") ProjectStatus status);

    @Query("SELECT p FROM Project p WHERE p.createdAt >= :fromDate")
    List<Project> findProjectsCreatedAfter(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT p FROM Project p WHERE p.updatedAt >= :fromDate")
    List<Project> findProjectsUpdatedAfter(@Param("fromDate") LocalDateTime fromDate);

    Optional<Project> findByIdAndUser(Long id, User user);

    @Query("SELECT p FROM Project p WHERE p.user.id = :userId")
    List<Project> findByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Project p WHERE p.user.id = :userId AND p.status = :status")
    List<Project> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ProjectStatus status);

    boolean existsByUserAndName(User user, String name);

    @Query("SELECT p FROM Project p JOIN FETCH p.user WHERE p.id = :id")
    Optional<Project> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT p FROM Project p JOIN FETCH p.user WHERE p.status = :status")
    List<Project> findByStatusWithUser(@Param("status") ProjectStatus status);

    @Query("SELECT p FROM Project p JOIN FETCH p.user WHERE p.user = :user AND p.status = :status")
    List<Project> findByUserAndStatusWithUser(@Param("user") User user, @Param("status") ProjectStatus status);
}