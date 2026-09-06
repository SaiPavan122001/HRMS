package com.hrms.employee;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "profiles", indexes = {
    @Index(name = "idx_profiles_user_id", columnList = "user_id"),
    @Index(name = "idx_profiles_manager_id", columnList = "manager_id"),
    @Index(name = "idx_profiles_department", columnList = "department")})
public class Profile {

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Column(nullable = false)
  private String email;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(name = "employee_id", unique = true, length = 32)
  private String employeeId;

  @Column(length = 128)
  private String department;

  @Column(name = "job_title", length = 128)
  private String jobTitle;

  @Column(name = "phone", length = 32)
  private String phone;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manager_id")
  private Profile manager;

  @Column(name = "hire_date")
  private LocalDate hireDate;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  private Long version;

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }
  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }
  public String getEmployeeId() { return employeeId; }
  public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
  public String getDepartment() { return department; }
  public void setDepartment(String department) { this.department = department; }
  public String getJobTitle() { return jobTitle; }
  public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public Profile getManager() { return manager; }
  public void setManager(Profile manager) { this.manager = manager; }
  public LocalDate getHireDate() { return hireDate; }
  public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Long getVersion() { return version; }
}
