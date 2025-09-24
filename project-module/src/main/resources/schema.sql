-- RBAC 시스템 스키마 정의

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('A', 'B', 'C', 'D')),
    subscription_plan VARCHAR(50) NOT NULL CHECK (subscription_plan IN ('BASIC', 'PRO')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role),
    INDEX idx_user_subscription_plan (subscription_plan)
);

-- 프로젝트 테이블
CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_project_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_project_user_id (user_id),
    INDEX idx_project_status (status),
    INDEX idx_project_name (name),
    INDEX idx_project_created_at (created_at)
);

-- 권한 테이블
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource VARCHAR(50) NOT NULL CHECK (resource IN ('PROJECT', 'USER', 'PERMISSION', 'ROLE_POLICY', 'SYSTEM')),
    action VARCHAR(50) NOT NULL CHECK (action IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'MANAGE', 'EXECUTE')),
    role VARCHAR(50) NOT NULL CHECK (role IN ('A', 'B', 'C', 'D')),
    subscription_plan VARCHAR(50) NOT NULL CHECK (subscription_plan IN ('BASIC', 'PRO')),
    allowed BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_permission_resource_action_role_plan UNIQUE (resource, action, role, subscription_plan),
    INDEX idx_permission_role (role),
    INDEX idx_permission_subscription_plan (subscription_plan),
    INDEX idx_permission_resource (resource),
    INDEX idx_permission_allowed (allowed)
);

-- 역할 정책 테이블
CREATE TABLE IF NOT EXISTS role_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(50) NOT NULL CHECK (role IN ('A', 'B', 'C', 'D')),
    resource VARCHAR(50) NOT NULL CHECK (resource IN ('PROJECT', 'USER', 'PERMISSION', 'ROLE_POLICY', 'SYSTEM')),
    action VARCHAR(50) NOT NULL CHECK (action IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'MANAGE', 'EXECUTE')),
    allowed BOOLEAN NOT NULL DEFAULT TRUE,
    subscription_plan VARCHAR(50) NOT NULL CHECK (subscription_plan IN ('BASIC', 'PRO')),
    reason VARCHAR(200),
    is_system_policy BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_policy_role_resource_action_plan UNIQUE (role, resource, action, subscription_plan),
    INDEX idx_role_policy_role (role),
    INDEX idx_role_policy_subscription_plan (subscription_plan),
    INDEX idx_role_policy_resource (resource),
    INDEX idx_role_policy_allowed (allowed),
    INDEX idx_role_policy_system (is_system_policy)
);

-- 감사 로그 테이블
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL CHECK (action IN ('USER_ROLE_UPDATED', 'ROLE_POLICY_UPDATED', 'ROLE_POLICY_CREATED', 'ROLE_POLICY_DELETED', 'POLICY_BACKUP_CREATED', 'POLICY_BACKUP_RESTORED', 'PERMISSION_GRANTED', 'PERMISSION_REVOKED', 'USER_CREATED', 'USER_DELETED', 'SYSTEM_SETTING_CHANGED')),
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    admin_user_id BIGINT NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    target_user_id BIGINT,
    target_user_email VARCHAR(255),
    old_value TEXT,
    new_value TEXT,
    reason VARCHAR(500),
    client_ip VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_admin_user_id (admin_user_id),
    INDEX idx_audit_target_user_id (target_user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_created_at (created_at),
    INDEX idx_audit_entity (entity_type, entity_id)
);

-- 사용자 알림 테이블
CREATE TABLE IF NOT EXISTS user_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('ROLE_CHANGED', 'PERMISSION_CHANGED', 'SYSTEM_NOTICE', 'POLICY_UPDATED', 'ACCOUNT_SUSPENDED', 'ACCOUNT_REACTIVATED', 'SUBSCRIPTION_CHANGED', 'SECURITY_ALERT')),
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    admin_user_id BIGINT,
    reference_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    INDEX idx_notification_user_id (user_id),
    INDEX idx_notification_user_id_read (user_id, is_read),
    INDEX idx_notification_type (type),
    INDEX idx_notification_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스 추가 설명
-- 1. users 테이블: 이메일 기반 로그인, 역할별 검색, 구독 플랜별 검색을 위한 인덱스
-- 2. projects 테이블: 사용자별 프로젝트 조회, 상태별 필터링, 이름 검색, 생성일 정렬을 위한 인덱스  
-- 3. permissions 테이블: 역할별 권한 조회, 구독 플랜별 권한 조회, 리소스별 권한 조회를 위한 인덱스
-- 4. role_policies 테이블: 역할별 정책 조회, 구독 플랜별 정책 조회, 시스템 정책 구분을 위한 인덱스
-- 5. audit_logs 테이블: 관리자별 로그 조회, 대상 사용자별 로그 조회, 액션별 로그 조회를 위한 인덱스
-- 6. user_notifications 테이블: 사용자별 알림 조회, 읽지 않은 알림 조회, 타입별 알림 조회를 위한 인덱스