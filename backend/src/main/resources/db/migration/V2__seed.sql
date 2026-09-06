-- ============================================================================
-- HRMS Flyway V2 - seed data (from the original Supabase seeds)
-- ============================================================================

-- Default permissions (module, action)
INSERT INTO permissions (module, action, description) VALUES
  ('attendance','view','View attendance records'),
  ('attendance','create','Mark attendance (check-in/out)'),
  ('attendance','edit','Edit attendance records'),
  ('attendance','delete','Delete attendance records'),
  ('attendance','approve','Approve attendance corrections'),
  ('timesheets','view','View timesheet entries'),
  ('timesheets','create','Create timesheet entries'),
  ('timesheets','edit','Edit timesheet entries'),
  ('timesheets','delete','Delete timesheet entries'),
  ('timesheets','approve','Approve/reject timesheets'),
  ('leave_management','view','View leave requests'),
  ('leave_management','create','Apply for leave'),
  ('leave_management','edit','Edit leave requests'),
  ('leave_management','delete','Cancel leave requests'),
  ('leave_management','approve','Approve/reject leave requests'),
  ('role_management','view','View roles and permissions'),
  ('role_management','create','Create new roles'),
  ('role_management','edit','Edit roles and permissions'),
  ('role_management','delete','Delete roles'),
  ('role_management','assign','Assign roles to users');

-- System roles
INSERT INTO roles (name, description, is_system_role) VALUES
  ('Super Admin','Full system control including role customization', true),
  ('HR Admin','Manage Attendance, Leave Policies, Employee Timesheets', true),
  ('Manager','View team Attendance, Approve team Timesheets and Leave requests', true),
  ('Employee','Mark own Attendance, Submit own Timesheets, Apply for Leave', true);

-- Super Admin: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name='Super Admin'), id FROM permissions;

-- HR Admin
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name='HR Admin'), p.id FROM permissions p
WHERE p.module IN ('attendance','timesheets','leave_management')
  AND p.action IN ('view','create','edit','approve');

-- Manager
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name='Manager'), p.id FROM permissions p
WHERE (p.module='attendance' AND p.action IN ('view','edit'))
   OR (p.module='timesheets' AND p.action IN ('view','approve'))
   OR (p.module='leave_management' AND p.action IN ('view','approve'));

-- Employee
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name='Employee'), p.id FROM permissions p
WHERE (p.module='attendance' AND p.action IN ('view','create'))
   OR (p.module='timesheets' AND p.action IN ('view','create','edit'))
   OR (p.module='leave_management' AND p.action IN ('view','create','edit'));

-- Default leave types
INSERT INTO leave_types (name, description, max_days_per_year, is_carry_forward) VALUES
  ('Annual Leave','Annual vacation leave',25,true),
  ('Sick Leave','Medical leave for illness',15,false),
  ('Personal Leave','Personal time off',5,false),
  ('Emergency Leave','Emergency situations',3,false);

-- Default attendance policy
INSERT INTO attendance_policies (name, description, working_hours_start, working_hours_end,
  break_duration_minutes, grace_period_minutes, overtime_threshold_hours) VALUES
  ('Default Policy','Standard 9-to-5 working hours with 1-hour break','09:00:00','17:00:00',60,15,8);
