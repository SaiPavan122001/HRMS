import { useEffect, useState } from "react";
import { api } from "@/lib/api";

export interface EmployeePerformance {
  id: string;
  profile_id: string;
  employee_id: string;
  first_name: string;
  last_name: string;
  department: string;
  job_title: string;
  hoursLogged: number;
  targetHours: number;
  attendanceRate: number;
  overtimeHours: number;
  utilizationRate: number;
  performanceScore: number;
}

export interface PerformanceDetail {
  weeklyHours: { week: string; hours: number; target: number }[];
  attendanceTrend: { date: string; status: string }[];
  workloadBreakdown: { category: string; hours: number }[];
  overtimeData: { regular: number; overtime: number; undertime: number };
  efficiency: {
    taskCompletionRate: number;
    timeUtilizationRate: number;
    attendanceImpactScore: number;
    overtimeOutputRatio: number;
    overallEfficiencyScore: number;
    efficiencyTrend: { week: string; score: number }[];
  };
}

const mapEmployee = (d: any): EmployeePerformance => ({
  id: d.profileId,
  profile_id: d.profileId,
  employee_id: d.employeeId,
  first_name: d.firstName,
  last_name: d.lastName,
  department: d.department,
  job_title: d.jobTitle,
  hoursLogged: Number(d.hoursLogged),
  targetHours: d.targetHours,
  attendanceRate: d.attendanceRate,
  overtimeHours: 0,
  utilizationRate: d.utilizationRate,
  performanceScore: d.performanceScore,
});

export const usePerformance = () => {
  const [employees, setEmployees] = useState<EmployeePerformance[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedEmployee, setSelectedEmployee] = useState<string | null>(null);
  const [performanceDetail, setPerformanceDetail] = useState<PerformanceDetail | null>(null);

  const fetchEmployees = async (filters?: { department?: string; dateRange?: { start: Date; end: Date } }) => {
    try {
      setLoading(true);
      const qs = filters?.department ? `?department=${encodeURIComponent(filters.department)}` : "";
      const data = await api(`/api/v1/reports/performance${qs}`);
      setEmployees((data || []).map(mapEmployee));
    } catch (error) {
      console.error("Error fetching employee performance:", error);
    } finally {
      setLoading(false);
    }
  };

  const fetchEmployeeDetail = async (profileId: string, dateRange?: { start: Date; end: Date }) => {
    try {
      setLoading(true);
      const data = await api(`/api/v1/reports/performance/${profileId}/detail`);
      const totalHours = (data.weeklyHours || []).reduce((s: number, w: any) => s + Number(w.hours), 0);
      const regular = Math.min(totalHours, 40 * (data.weeklyHours?.length || 1));
      setPerformanceDetail({
        weeklyHours: (data.weeklyHours || []).map((w: any) => ({
          week: w.week, hours: Number(w.hours), target: Number(w.target),
        })),
        attendanceTrend: (data.attendanceTrend || []).map((a: any) => ({ date: a.date, status: a.status })),
        workloadBreakdown: (data.workloadBreakdown || []).map((w: any) => ({
          category: w.category, hours: Number(w.hours),
        })),
        overtimeData: {
          regular: Math.round(regular * 10) / 10,
          overtime: Math.max(0, Math.round((totalHours - regular) * 10) / 10),
          undertime: 0,
        },
        efficiency: {
          taskCompletionRate: 0,
          timeUtilizationRate: Math.min(100, Math.round((totalHours / 320) * 100)),
          attendanceImpactScore: 0,
          overtimeOutputRatio: 0,
          overallEfficiencyScore: Math.min(100, Math.round((totalHours / 320) * 100)),
          efficiencyTrend: (data.weeklyHours || []).map((w: any) => ({ week: w.week, score: Math.min(100, Math.round((Number(w.hours) / 40) * 100)) })),
        },
      });
    } catch (error) {
      console.error("Error fetching employee detail:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEmployees();
  }, []);

  return { employees, loading, fetchEmployees, selectedEmployee, setSelectedEmployee, performanceDetail, fetchEmployeeDetail };
};


