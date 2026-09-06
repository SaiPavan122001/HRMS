import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";
import { format, endOfWeek } from "date-fns";

export interface TimesheetEntry {
  id?: string;
  date: string;
  hours: number;
  project_name?: string;
  task_description?: string;
  is_billable: boolean;
  timesheet_id?: string;
}

export interface Timesheet {
  id: string;
  profile_id: string;
  week_start_date: string;
  week_end_date: string;
  total_hours: number;
  status: string;
  submitted_at?: string | null;
  approved_by?: string | null;
  approved_at?: string | null;
  rejection_reason?: string | null;
  entries?: TimesheetEntry[];
}

const mapSheet = (d: any): Timesheet => ({
  id: d.id, profile_id: d.profileId, week_start_date: d.weekStartDate, week_end_date: d.weekEndDate,
  total_hours: Number(d.totalHours || 0), status: d.status, submitted_at: d.submittedAt,
  approved_by: d.approvedBy, approved_at: d.approvedAt, rejection_reason: d.rejectionReason,
  entries: (d.entries || []).map((e: any) => ({
    id: e.id, date: e.date, hours: Number(e.hours), project_name: e.projectName,
    task_description: e.taskDescription, is_billable: e.isBillable, timesheet_id: e.timesheetId,
  })),
});

export const useTimesheets = () => {
  const [timesheets, setTimesheets] = useState<Timesheet[]>([]);
  const [currentTimesheet, setCurrentTimesheet] = useState<Timesheet | null>(null);
  const [entries, setEntries] = useState<TimesheetEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  const fetchTimesheets = async () => {
    setLoading(true);
    try {
      const data = await api("/api/v1/timesheets");
      setTimesheets((data || []).map(mapSheet));
    } catch (error) {
      console.error("Error fetching timesheets:", error);
      toast({ title: "Error", description: "Failed to fetch timesheets", variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  const createOrGetTimesheet = async (weekStartDate: Date) => {
    const startDate = format(weekStartDate, "yyyy-MM-dd");
    try {
      const data = await api("/api/v1/timesheets/current", {
        method: "POST",
        body: { weekStartDate: startDate },
      });
      const sheet = mapSheet(data);
      setCurrentTimesheet(sheet);
      setEntries(sheet.entries || []);
      return sheet;
    } catch (error) {
      console.error("Error fetching timesheet:", error);
      toast({ title: "Error", description: "Failed to load timesheet", variant: "destructive" });
      throw error;
    }
  };

const saveEntry = async (entry: TimesheetEntry) => {
  const timesheetId = currentTimesheet?.id;
  if (!timesheetId) {
    toast({ title: "Error", description: "Open a timesheet first", variant: "destructive" });
    throw new Error("No current timesheet");
  }
    try {
      await api(`/api/v1/timesheets/${timesheetId}/entries`, {
        method: "POST",
        body: {
          id: entry.id,
          date: entry.date,
          hours: entry.hours,
          projectName: entry.project_name,
          taskDescription: entry.task_description,
          isBillable: entry.is_billable,
        },
      });
      const sheet = await createOrGetTimesheet(new Date(currentTimesheet?.week_start_date || entry.date));
      toast({ title: "Success", description: "Entry saved" });
      return sheet.entries?.find((e) => e.date === entry.date);
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      throw error;
    }
  };

const deleteEntry = async (entryId: string) => {
  const timesheetId = currentTimesheet?.id;
  if (!timesheetId) return;
    try {
      await api(`/api/v1/timesheets/${timesheetId}/entries/${entryId}`, { method: "DELETE" });
      toast({ title: "Success", description: "Entry deleted" });
      if (currentTimesheet) await createOrGetTimesheet(new Date(currentTimesheet.week_start_date));
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
      throw error;
    }
  };

  const submitTimesheet = async (timesheetId: string) => {
    try {
      const data = await api(`/api/v1/timesheets/${timesheetId}/submit`, { method: "POST" });
      toast({ title: "Success", description: "Timesheet submitted for approval" });
      setCurrentTimesheet(mapSheet(data));
      await fetchTimesheets();
      return mapSheet(data);
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
    }
  };

  const approveTimesheet = async (timesheetId: string) => {
    try {
      await api(`/api/v1/timesheets/${timesheetId}/decision`, { method: "POST", body: { approved: true } });
      toast({ title: "Success", description: "Timesheet approved" });
      await fetchTimesheets();
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
    }
  };

  const rejectTimesheet = async (timesheetId: string, reason: string) => {
    try {
      await api(`/api/v1/timesheets/${timesheetId}/decision`, { method: "POST", body: { approved: false, reason } });
      toast({ title: "Success", description: "Timesheet rejected" });
      await fetchTimesheets();
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
    }
  };

  const bulkApproveTimesheets = async (timesheetIds: string[]) => {
    try {
      const res = await api("/api/v1/timesheets/bulk-approve", { method: "POST", body: { ids: timesheetIds } });
      toast({ title: "Success", description: `${res.approved} timesheets approved` });
      await fetchTimesheets();
    } catch (error: any) {
      toast({ title: "Error", description: error.message, variant: "destructive" });
    }
  };

  useEffect(() => {
    fetchTimesheets();
  }, []);

  return {
    timesheets, currentTimesheet, entries, loading,
    fetchTimesheets, createOrGetTimesheet, saveEntry, deleteEntry,
    submitTimesheet, approveTimesheet, rejectTimesheet, bulkApproveTimesheets,
    setCurrentTimesheet, setEntries,
  };
};

