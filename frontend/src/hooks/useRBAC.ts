import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/hooks/useAuth";

export interface Role {
  id: string;
  name: string;
  description?: string;
  is_system_role: boolean;
  created_at?: string;
  updated_at?: string;
}

export interface Permission {
  id: string;
  module: string;
  action: string;
  description?: string;
  created_at?: string;
}

export interface RolePermission {
  id: string;
  role_id: string;
  permission_id: string;
  permission: Permission;
}

export interface UserRoleAssignment {
  id: string;
  user_id: string;
  role_id: string;
  assigned_by?: string;
  assigned_at: string;
  role: Role;
}

export function useRBAC() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [rolePermissions, setRolePermissions] = useState<Record<string, Permission[]>>({});
  const [userPermissions, setUserPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();
  const { user } = useAuth();

  const fetchRoles = async () => {
    try {
      const data = await api("/api/v1/roles");
      setRoles((data || []).map((d: any) => ({
        id: d.id, name: d.name, description: d.description,
        is_system_role: d.isSystemRole,
      })));
    } catch (error) {
      console.error("Error fetching roles:", error);
      toast({ title: "Error", description: "Failed to fetch roles", variant: "destructive" });
    }
  };

  const fetchPermissions = async () => {
    try {
      const data = await api("/api/v1/permissions");
      setPermissions((data || []).map((d: any) => ({
        id: d.id, module: d.module, action: d.action, description: d.description,
      })));
    } catch (error) {
      console.error("Error fetching permissions:", error);
      toast({ title: "Error", description: "Failed to fetch permissions", variant: "destructive" });
    }
  };

  const fetchRolePermissions = async () => {
    try {
      const allRoles = await api("/api/v1/roles");
      const grouped: Record<string, Permission[]> = {};
      await Promise.all(
        (allRoles || []).map(async (role: any) => {
          const perms = await api(`/api/v1/role-permissions/${role.id}`);
          grouped[role.id] = (perms || []).map((p: any) => ({
            id: p.id, module: p.module, action: p.action, description: p.description,
          }));
        })
      );
      setRolePermissions(grouped);
    } catch (error) {
      console.error("Error fetching role permissions:", error);
    }
  };

  const fetchUserPermissions = async (userId?: string) => {
    const target = userId || user?.id;
    if (!target) return;
    try {
      const data = await api("/api/v1/me/permissions");
      setUserPermissions((data || []).map((p: any) => ({
        id: "", module: p.module, action: p.action, description: p.description,
      })));
    } catch (error) {
      console.error("Error fetching user permissions:", error);
    }
  };

  const hasPermission = (module: string, action: string): boolean => {
    return userPermissions.some((p) => p.module === module && p.action === action);
  };

  const createRole = async (roleData: { name: string; description?: string }) => {
    try {
      const data = await api("/api/v1/roles", { method: "POST", body: roleData });
      await fetchRoles();
      toast({ title: "Success", description: "Role created successfully" });
      return data;
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      throw error;
    }
  };

  const updateRole = async (roleId: string, updates: { name?: string; description?: string }) => {
    try {
      await api(`/api/v1/roles/${roleId}`, { method: "PUT", body: updates });
      await fetchRoles();
      toast({ title: "Success", description: "Role updated" });
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      throw error;
    }
  };

  const deleteRole = async (roleId: string) => {
    try {
      await api(`/api/v1/roles/${roleId}`, { method: "DELETE" });
      await fetchRoles();
      toast({ title: "Success", description: "Role deleted" });
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      throw error;
    }
  };

  const assignPermissionToRole = async (roleId: string, permissionId: string) => {
    try {
      await api(`/api/v1/role-permissions/${roleId}`, { method: "POST", body: { permissionId } });
      await fetchRolePermissions();
      toast({ title: "Success", description: "Permission assigned successfully" });
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      throw error;
    }
  };

  const removePermissionFromRole = async (roleId: string, permissionId: string) => {
    try {
      await api(`/api/v1/role-permissions/${roleId}/${permissionId}`, { method: "DELETE" });
      await fetchRolePermissions();
      toast({ title: "Success", description: "Permission removed successfully" });
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      throw error;
    }
  };

  const assignRoleToUser = async (userId: string, roleId: string) => {
    try {
      await api(`/api/v1/user-roles/${userId}`, { method: "POST", body: { roleId } });
      toast({ title: "Success", description: "Role assigned to user successfully" });
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      throw error;
    }
  };

  const removeRoleFromUser = async (userId: string, roleId: string) => {
    try {
      await api(`/api/v1/user-roles/${userId}/${roleId}`, { method: "DELETE" });
      toast({ title: "Success", description: "Role removed from user successfully" });
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      throw error;
    }
  };

  const getUserRoles = async (userId: string): Promise<UserRoleAssignment[]> => {
    try {
      const data = await api(`/api/v1/user-roles/${userId}`);
      return (data || []).map((d: any) => ({
        id: d.id, user_id: d.userId, role_id: d.roleId, assigned_by: undefined,
        assigned_at: d.assignedAt,
        role: { id: d.roleId, name: d.roleName, is_system_role: false, assigned_at: d.assignedAt } as Role,
      }));
    } catch (error) {
      console.error("Error fetching user roles:", error);
      return [];
    }
  };

  const getUsersWithRoles = async () => {
    try {
      const data = await api("/api/v1/users-with-roles");
      return (data || []).map((d: any) => ({
        id: d.id, user_id: d.userId, email: d.email,
        first_name: d.firstName, last_name: d.lastName,
        employee_id: d.employeeId, department: d.department, job_title: d.jobTitle,
        roles: d.roles || [],
      }));
    } catch (error: any) {
      toast({ title: "Error", description: error.message || "Failed to fetch users with roles", variant: "destructive" });
      return [];
    }
  };

  useEffect(() => {
    const initializeData = async () => {
      setLoading(true);
      await Promise.all([fetchRoles(), fetchPermissions(), fetchRolePermissions()]);
      setLoading(false);
    };
    initializeData();
  }, []);

  return {
    roles, permissions, rolePermissions, userPermissions, loading,
    createRole, updateRole, deleteRole,
    assignPermissionToRole, removePermissionFromRole,
    assignRoleToUser, removeRoleFromUser, getUserRoles, getUsersWithRoles,
    hasPermission, fetchUserPermissions,
    fetchRoles, fetchPermissions, fetchRolePermissions,
  };
}
