import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/hooks/useAuth";

export interface LeaveType {
  id: string;
  name: string;
  description?: string;
  max_days_per_year?: number;
  is_carry_forward: boolean;
  is_active: boolean;
  created_at?: string;
  updated_at?: string;
}

export interface LeaveBalance {
  id: string;
  profile_id: string;
  leave_type_id: string;
  year: number;
  total_days: number;
  used_days: number;
  remaining_days: number;
  leave_types?: { id: string; name: string; description?: string };
}

export interface LeaveRequest {
  id: string;
  profile_id: string;
  leave_type_id: string;
  start_date: string;
  end_date: string;
  days_requested: number;
  reason?: string;
  status: string;
  approved_by?: string;
  approved_at?: string;
  rejection_reason?: string;
  created_at: string;
  leave_types?: { name: string };
  profiles?: { first_name: string; last_name: string; employee_id: string };
  approved_by_profile?: { first_name: string; last_name: string };
}

export interface Holiday {
  id: string;
  name: string;
  date: string;
  year?: number;
  is_recurring: boolean;
  created_at?: string;
  updated_at?: string;
}

const mapType = (d: any): LeaveType => ({
  id: d.id, name: d.name, description: d.description,
  max_days_per_year: d.maxDaysPerYear, is_carry_forward: d.isCarryForward,
  is_active: d.isActive, created_at: "", updated_at: "",
});

const mapBalance = (d: any): LeaveBalance => ({
  id: d.id, profile_id: d.profileId, leave_type_id: d.leaveTypeId, year: d.year,
  total_days: Number(d.totalDays), used_days: Number(d.usedDays), remaining_days: Number(d.remainingDays),
});

const mapRequest = (d: any): LeaveRequest => ({
  id: d.id, profile_id: d.profileId, leave_type_id: d.leaveTypeId,
  start_date: d.startDate, end_date: d.endDate, days_requested: Number(d.daysRequested),
  reason: d.reason, status: d.status, approved_by: d.approvedBy, approved_at: d.approvedAt,
  rejection_reason: d.rejectionReason, created_at: d.createdAt,
});

const mapHoliday = (d: any): Holiday => ({
  id: d.id, name: d.name, date: d.date, year: d.year, is_recurring: d.isRecurring,
});

export function useLeave() {
  const [leaveTypes, setLeaveTypes] = useState<LeaveType[]>([]);
  const [leaveBalances, setLeaveBalances] = useState<LeaveBalance[]>([]);
  const [leaveRequests, setLeaveRequests] = useState<LeaveRequest[]>([]);
  const [holidays, setHolidays] = useState<Holiday[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();
  const { profile, userRole } = useAuth();

  useEffect(() => {
    if (profile) fetchLeaveData();
  }, [profile]);

  const fetchLeaveData = async () => {
    try {
      setLoading(true);
      await Promise.all([fetchLeaveTypes(), fetchLeaveBalances(), fetchLeaveRequests(), fetchHolidays()]);
    } catch (error) {
      console.error("Error fetching leave data:", error);
      toast({ title: "Error", description: "Failed to fetch leave data", variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  const fetchLeaveTypes = async () => {
    try {
      const data = await api("/api/v1/leaves/types");
      setLeaveTypes((data || []).map(mapType));
    } catch (e) { console.error("Error fetching leave types:", e); }
  };

  const fetchLeaveBalances = async () => {
    if (!profile?.id) return;
    try {
      const data = await api("/api/v1/leaves/balances");
      const types = await api("/api/v1/leaves/types");
      setLeaveBalances((data || []).map((d: any) => ({
        ...mapBalance(d),
        leave_types: types.find((t: any) => t.id === d.leaveTypeId)
          ? { id: d.leaveTypeId, name: types.find((t: any) => t.id === d.leaveTypeId).name }
          : undefined,
      })));
    } catch (e) { console.error("Error fetching leave balances:", e); }
  };

  const fetchLeaveRequests = async () => {
    if (!profile?.id) return;
    try {
      const data = await api("/api/v1/leaves/requests");
      const types = await api("/api/v1/leaves/types");
      setLeaveRequests((data || []).map((d: any) => ({
        ...mapRequest(d),
        leave_types: types.find((t: any) => t.id === d.leaveTypeId)
          ? { name: types.find((t: any) => t.id === d.leaveTypeId).name }
          : undefined,
      })));
    } catch (e) { console.error("Error fetching leave requests:", e); }
  };

  const fetchHolidays = async () => {
    try {
      const data = await api("/api/v1/attendance/holidays");
      setHolidays((data || []).map(mapHoliday));
    } catch (e) { console.error("Error fetching holidays:", e); }
  };

  const createLeaveRequest = async (requestData: {
    leave_type_id: string;
    start_date: string;
    end_date: string;
    days_requested: number;
    reason?: string;
  }) => {
    try {
      const data = await api("/api/v1/leaves/requests", {
        method: "POST",
        body: {
          leaveTypeId: requestData.leave_type_id,
          startDate: requestData.start_date,
          endDate: requestData.end_date,
          daysRequested: requestData.days_requested,
          reason: requestData.reason,
        },
      });
      toast({ title: "Success", description: "Leave request submitted successfully" });
      fetchLeaveRequests();
      return { data: mapRequest(data) };
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      return { error };
    }
  };

  const updateLeaveRequest = async (id: string, updates: { status?: string; rejection_reason?: string }) => {
    try {
      const data = await api(`/api/v1/leaves/requests/${id}/decision`, {
        method: "POST",
        body: { approved: updates.status === "approved", reason: updates.rejection_reason },
      });
      toast({ title: "Success", description: `Leave request ${updates.status} successfully` });
      fetchLeaveRequests();
      fetchLeaveBalances();
      return { data: mapRequest(data) };
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      return { error };
    }
  };

  const createLeaveType = async (leaveTypeData: { name: string; description?: string; max_days_per_year?: number; is_carry_forward: boolean }) => {
    try {
      const data = await api("/api/v1/leaves/types", {
        method: "POST",
        body: { name: leaveTypeData.name, description: leaveTypeData.description,
          maxDaysPerYear: leaveTypeData.max_days_per_year, isCarryForward: leaveTypeData.is_carry_forward },
      });
      toast({ title: "Success", description: "Leave type created successfully" });
      fetchLeaveTypes();
      return { data: mapType(data) };
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      return { error };
    }
  };

  const updateLeaveType = async (id: string, updates: Partial<LeaveType>) => {
    try {
      await api(`/api/v1/leaves/types/${id}`, {
        method: "PUT",
        body: { name: updates.name, description: updates.description,
          maxDaysPerYear: updates.max_days_per_year, isCarryForward: updates.is_carry_forward,
          isActive: updates.is_active },
      });
      toast({ title: "Success", description: "Leave type updated successfully" });
      fetchLeaveTypes();
      return { data: null };
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      return { error };
    }
  };

  const deleteLeaveType = async (id: string) => {
    try {
      await api(`/api/v1/leaves/types/${id}`, { method: "DELETE" });
      toast({ title: "Success", description: "Leave type deleted successfully" });
      fetchLeaveTypes();
      return { error: null };
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      return { error };
    }
  };

  const createHoliday = async (holidayData: { name: string; date: string; year?: number; is_recurring: boolean }) => {
    try {
      const data = await api("/api/v1/attendance/holidays", {
        method: "POST",
        body: { name: holidayData.name, date: holidayData.date, year: holidayData.year, isRecurring: holidayData.is_recurring },
      });
      toast({ title: "Success", description: "Holiday created successfully" });
      fetchHolidays();
      return { data: mapHoliday(data) };
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      return { error };
    }
  };

  const updateHoliday = async (id: string, updates: Partial<Holiday>) => {
    // recreate with new values (backend exposes create/delete)
    try {
      await api(`/api/v1/attendance/holidays/${id}`, { method: "DELETE" });
      if (updates.name && updates.date) {
        await api("/api/v1/attendance/holidays", {
          method: "POST",
          body: { name: updates.name, date: updates.date, year: updates.year, isRecurring: !!updates.is_recurring },
        });
      }
      toast({ title: "Success", description: "Holiday updated successfully" });
      fetchHolidays();
      return { data: null };
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      return { error };
    }
  };

  const deleteHoliday = async (id: string) => {
    try {
      await api(`/api/v1/attendance/holidays/${id}`, { method: "DELETE" });
      toast({ title: "Success", description: "Holiday deleted successfully" });
      fetchHolidays();
      return { error: null };
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      return { error };
    }
  };

  const calculateWorkingDays = (startDate: string, endDate: string, isHalfDay = false): number => {
    const start = new Date(startDate);
    const end = new Date(endDate);
    let workingDays = 0;
    for (let date = new Date(start); date <= end; date.setDate(date.getDate() + 1)) {
      if (date.getDay() !== 0 && date.getDay() !== 6) {
        const dateStr = date.toISOString().split("T")[0];
        const isHoliday = holidays.some((holiday) =>
          holiday.date === dateStr || (holiday.is_recurring && holiday.date.slice(5) === dateStr.slice(5))
        );
        if (!isHoliday) workingDays += isHalfDay ? 0.5 : 1;
      }
    }
    return workingDays;
  };

  return {
    leaveTypes, leaveBalances, leaveRequests, holidays, loading,
    createLeaveRequest, calculateWorkingDays,
    updateLeaveRequest,
    createLeaveType, updateLeaveType, deleteLeaveType,
    createHoliday, updateHoliday, deleteHoliday,
    fetchLeaveData, fetchLeaveRequests, fetchLeaveBalances,
  };
}
