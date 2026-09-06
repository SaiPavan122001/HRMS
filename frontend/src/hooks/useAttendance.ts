import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";
import { useAuth } from "@/hooks/useAuth";
import { connectNotifications } from "@/lib/api";

export interface AttendanceRecord {
  id: string;
  profile_id: string;
  date: string;
  check_in_time: string | null;
  check_out_time: string | null;
  break_start_time: string | null;
  break_end_time: string | null;
  total_hours: number | null;
  status: string;
  notes: string | null;
  is_corrected: boolean;
  correction_reason: string | null;
  corrected_by: string | null;
  created_at?: string;
  updated_at?: string;
}

export interface AttendancePolicy {
  id: string;
  name: string;
  description: string;
  working_hours_start: string;
  working_hours_end: string;
  break_duration_minutes: number;
  grace_period_minutes: number;
  overtime_threshold_hours: number;
  is_active: boolean;
}

const mapRecord = (d: any): AttendanceRecord => ({
  id: d.id,
  profile_id: d.profileId,
  date: d.date,
  check_in_time: d.checkInTime,
  check_out_time: d.checkOutTime,
  break_start_time: d.breakStartTime,
  break_end_time: d.breakEndTime,
  total_hours: d.totalHours === null ? null : Number(d.totalHours),
  status: d.status,
  notes: d.notes,
  is_corrected: d.isCorrected,
  correction_reason: d.correctionReason,
  corrected_by: null,
});

const mapPolicy = (d: any): AttendancePolicy => ({
  id: d.id,
  name: d.name,
  description: d.description || "",
  working_hours_start: d.workingHoursStart,
  working_hours_end: d.workingHoursEnd,
  break_duration_minutes: d.breakDurationMinutes,
  grace_period_minutes: d.gracePeriodMinutes,
  overtime_threshold_hours: Number(d.overtimeThresholdHours),
  is_active: d.isActive,
});

export function useAttendance() {
  const [todaysAttendance, setTodaysAttendance] = useState<AttendanceRecord | null>(null);
  const [attendanceRecords, setAttendanceRecords] = useState<AttendanceRecord[]>([]);
  const [policies, setPolicies] = useState<AttendancePolicy[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();
  const { profile } = useAuth();

  const loadTodaysAttendance = async () => {
    if (!profile?.id) return;
    try {
      const d = await api("/api/v1/attendance/today");
      setTodaysAttendance(d ? mapRecord(d) : null);
    } catch (error) {
      console.error("Error loading today attendance:", error);
    }
  };

  const loadAttendanceRecords = async (startDate: string, endDate: string) => {
    if (!profile?.id) return;
    try {
      setLoading(true);
      const data = await api(`/api/v1/attendance/me?start=${startDate}&end=${endDate}`);
      setAttendanceRecords((data || []).map(mapRecord));
    } catch (error) {
      console.error("Error loading attendance records:", error);
      toast({ title: "Error", description: "Failed to load attendance records", variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  const loadPolicies = async () => {
    try {
      const data = await api("/api/v1/attendance/policies");
      setPolicies((data || []).map(mapPolicy));
    } catch (error) {
      console.error("Error loading policies:", error);
    }
  };

  const checkIn = async (notes?: string) => {
    try {
      const d = await api("/api/v1/attendance/check-in", { method: "POST", body: { notes } });
      const record = mapRecord(d);
      setTodaysAttendance(record);
      toast({ title: "Checked In", description: `Successfully checked in at ${new Date(record.check_in_time!).toLocaleTimeString()}` });
      return record;
    } catch (error: any) {
      toast({ title: "Check-in Failed", description: error.message, variant: "destructive" });
      throw error;
    }
  };

  const checkOut = async (notes?: string) => {
    if (!todaysAttendance) return;
    try {
      const d = await api("/api/v1/attendance/check-out", { method: "POST", body: { notes } });
      const record = mapRecord(d);
      setTodaysAttendance(record);
      toast({
        title: "Checked Out",
        description: `Successfully checked out. Total hours: ${Number(record.total_hours || 0).toFixed(2)}`,
      });
      return record;
    } catch (error: any) {
      toast({ title: "Check-out Failed", description: error.message, variant: "destructive" });
      throw error;
    }
  };

  const startBreak = async () => {
    if (!todaysAttendance) return;
    try {
      const d = await api("/api/v1/attendance/break/start", { method: "POST" });
      const record = mapRecord(d);
      setTodaysAttendance(record);
      toast({ title: "Break Started", description: `Break started at ${new Date(record.break_start_time!).toLocaleTimeString()}` });
      return record;
    } catch (error: any) {
      toast({ title: "Error", description: error.message || "Failed to start break.", variant: "destructive" });
      throw error;
    }
  };

  const endBreak = async () => {
    if (!todaysAttendance) return;
    try {
      const d = await api("/api/v1/attendance/break/end", { method: "POST" });
      const record = mapRecord(d);
      setTodaysAttendance(record);
      toast({ title: "Break Ended", description: `Break ended at ${new Date(record.break_end_time!).toLocaleTimeString()}` });
      return record;
    } catch (error: any) {
      toast({ title: "Error", description: error.message || "Failed to end break.", variant: "destructive" });
      throw error;
    }
  };

  // Real-time updates for own attendance arrive via WebSocket notifications
  useEffect(() => {
    if (!profile?.id) return;
    const disconnect = connectNotifications((n: any) => {
      if (n?.referenceType === "attendance") loadTodaysAttendance();
    });
    return disconnect;
  }, [profile?.id]);

  useEffect(() => {
    if (profile?.id) {
      loadTodaysAttendance();
      loadPolicies();
      const today = new Date();
      const startOfWeek = new Date(today.setDate(today.getDate() - today.getDay()));
      const endOfWeek = new Date(today.setDate(today.getDate() - today.getDay() + 6));
      loadAttendanceRecords(
        startOfWeek.toISOString().split("T")[0],
        endOfWeek.toISOString().split("T")[0]
      );
    }
  }, [profile?.id]);

  return {
    todaysAttendance,
    attendanceRecords,
    policies,
    loading,
    checkIn,
    checkOut,
    startBreak,
    endBreak,
    loadAttendanceRecords,
    loadTodaysAttendance,
  };
}
