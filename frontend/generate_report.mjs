/* Slate TimeSync - Technical Project Report generator (uses frontend's local jspdf) */
import { jsPDF } from "jspdf";
import autoTable from "jspdf-autotable";

const doc = new jsPDF({ unit: "mm", format: "a4", compress: true });
const PW = 210, PH = 297, ML = 17, MR = 17, MT = 22, MB = 20;
const CW = PW - ML - MR;
const NAVY=[18,46,86], STEEL=[77,101,133], TEAL=[0,106,113], LIGHT=[233,239,248], LIGHT2=[247,250,253],
      BORDER=[168,182,202], TEXT=[33,39,49], MUTED=[104,113,128], GREEN=[21,110,67], RED=[163,29,45],
      AMBER=[170,116,10], WHITE=[255,255,255], CHIPBG=[225,233,245];
const F = "helvetica";
let y = MT, figN = 0, tabN = 0;
const toc = [];
const cur = () => doc.internal.getCurrentPageInfo().pageNumber;
function newPage(){ doc.addPage(); y = MT; }
function ensure(h){ if (y + h > PH - MB) newPage(); }
function font(style, size, color){ doc.setFont(F, style).setFontSize(size); if (color) doc.setTextColor(...color); }

/* ---------- text primitives ---------- */
function p(str, o = {}){
  const size = o.size ?? 9.3, gap = o.gap ?? 1.7, indent = o.indent ?? 0;
  font(o.style ?? "normal", size, o.color ?? TEXT);
  const lines = doc.splitTextToSize(str, CW - indent);
  ensure(lines.length * size * 0.43 + gap);
  for (const ln of lines){ doc.text(ln, ML + indent, y + size * 0.30); y += size * 0.43; }
  y += gap;
}
function bullets(items, o = {}){
  const size = o.size ?? 9.2, indent = o.indent ?? 0, gap = o.gap ?? 1.1;
  for (const it of items){
    const txt = typeof it === "string" ? it : it.t;
    const rest = typeof it === "string" ? null : it.d;
    font("normal", size, o.bulletColor ?? TEAL);
    ensure(size * 0.43 + gap + 6);
    doc.text(o.bullet ?? "\u2022", ML + indent + 1, y + size * 0.30);
    font("normal", size, TEXT);
    const lines = doc.splitTextToSize(txt, CW - indent - 5);
    let yy = y;
    for (const ln of lines){ doc.text(ln, ML + indent + 4.6, yy + size * 0.30); yy += size * 0.43; }
    if (rest){
      font("normal", size - 0.4, MUTED);
      const rlines = doc.splitTextToSize(rest, CW - indent - 7);
      for (const ln of rlines){ doc.text(ln, ML + indent + 7.4, yy + (size-0.4) * 0.30); yy += (size-0.4) * 0.43; }
    }
    y = yy + gap;
  }
  y += o.after ?? 0.8;
}

/* ---------- headings ---------- */
function h1(num, title){
  doc.addPage(); y = MT + 1;
  toc.push({ n: num, t: title, page: cur(), level: 1 });
  doc.setFillColor(...NAVY);
  doc.roundedRect(ML, y, 10.5, 8.4, 1.4, 1.4, "F");
  font("bold", 10.5, WHITE); doc.text(String(num), ML + 5.25, y + 5.9, { align: "center" });
  font("bold", 14, NAVY);
  if (doc.getTextWidth(title) > CW - 14) font("bold", 12, NAVY);
  doc.text(title, ML + 13.5, y + 5.8);
  y += 11.2;
  doc.setDrawColor(...BORDER); doc.setLineWidth(0.5);
  doc.line(ML, y, PW - MR, y);
  doc.setDrawColor(...TEAL); doc.setLineWidth(1.1); doc.line(ML, y, ML + 26, y);
  y += 5.4;
}
function h2(title){
  ensure(13);
  font("bold", 11.4, NAVY);
  doc.text(title, ML, y + 4);
  y += 6.6;
  doc.setDrawColor(...CHIPBG); doc.setLineWidth(0.6);
  doc.line(ML, y - 1.6, PW - MR, y - 1.6);
  y += 0.6;
}
function h3(title){
  ensure(10);
  font("bold", 9.8, STEEL);
  doc.text(title, ML, y + 3.4);
  y += 5.6;
}
function caption(txt){
  font("italic", 8.1, MUTED);
  const lines = doc.splitTextToSize(txt, CW);
  ensure(lines.length * 3.4 + 2.5);
  for (const ln of lines){ doc.text(ln, ML, y + 2.6); y += 3.4; }
  y += 2.2;
}
function tableCaption(txt){ tabN++; ensure(8); caption(`Table ${tabN}. ${txt}`); }
function figCaption(txt){ figN++; ensure(7); caption(`Figure ${figN}. ${txt}`); }

/* ---------- tables ---------- */
function statusStyle(d){
  const v = String(d.cell.raw ?? "").trim();
  if (/^(PASS|Verified|Implemented|Yes|Complete|Passed|Success|OK|Active|Present)$/i.test(v)){
    d.cell.styles.textColor = GREEN; d.cell.styles.fontStyle = "bold";
  } else if (/^(FAIL|Failed|Not verified|Missing|Not implemented|Critical|High risk)$/i.test(v)){
    d.cell.styles.textColor = RED; d.cell.styles.fontStyle = "bold";
  } else if (/^(Partial|Recommendation|Planned|Medium|Low|Deferred|Skipped|Not executed|Not identified in the current implementation\.?)$/i.test(v)){
    d.cell.styles.textColor = AMBER; d.cell.styles.fontStyle = "bold";
  }
}
function table({ head, body, widths, caption: cap, size = 8.1, aligns, minCellH }){
  if (cap) tableCaption(cap);
  ensure(16);
  const colStyles = {};
  if (widths) widths.forEach((w, i) => colStyles[i] = { cellWidth: w });
  if (aligns) aligns.forEach((a, i) => { colStyles[i] = { ...(colStyles[i] || {}), halign: a }; });
  autoTable(doc, {
    head: [head], body,
    startY: y,
    margin: { left: ML, right: MR, top: MT + 3, bottom: MB },
    theme: "grid",
    styles: { font: F, fontSize: size, cellPadding: 1.5, textColor: TEXT, lineColor: BORDER,
              lineWidth: 0.14, overflow: "linebreak", valign: "middle", minCellHeight: minCellH ?? 5 },
    headStyles: { fillColor: NAVY, textColor: WHITE, fontStyle: "bold", fontSize: size, halign: "left" },
    alternateRowStyles: { fillColor: LIGHT2 },
    columnStyles: colStyles,
    didParseCell: statusStyle
  });
  y = doc.lastAutoTable.finalY + 4.2;
}

/* ---------- diagram primitives ---------- */
function dBox(x, yy, w, h, lines, o = {}){
  doc.setFillColor(...(o.fill ?? LIGHT));
  doc.setDrawColor(...(o.stroke ?? NAVY));
  doc.setLineWidth(0.32);
  doc.roundedRect(x, yy, w, h, 1.5, 1.5, "FD");
  const size = o.size ?? 7.8;
  font(o.bold ? "bold" : "normal", size, o.color ?? TEXT);
  const lh = size * 0.40 + 0.55;
  let ty = yy + h / 2 - ((lines.length - 1) * lh) / 2 + size * 0.32;
  lines.forEach((ln, i) => doc.text(ln, x + w / 2, ty + i * lh, { align: "center", maxWidth: w - 2.2 }));
}
function dArrow(x1, y1, x2, y2, o = {}){
  doc.setDrawColor(...(o.color ?? STEEL)); doc.setLineWidth(o.w ?? 0.38);
  if (o.dash) doc.setLineDashPattern([1.2, 1.2], 0);
  doc.line(x1, y1, x2, y2);
  doc.setLineDashPattern([], 0);
  const ang = Math.atan2(y2 - y1, x2 - x1), hl = 2.1;
  doc.setFillColor(...(o.color ?? STEEL));
  doc.triangle(x2, y2, x2 - hl * Math.cos(ang - 0.42), y2 - hl * Math.sin(ang - 0.42),
               x2 - hl * Math.cos(ang + 0.42), y2 - hl * Math.sin(ang + 0.42), "F");
  if (o.label){
    font(o.lstyle ?? "normal", o.lsize ?? 6.9, o.lcolor ?? MUTED);
    const lx = (x1 + x2) / 2, ly = (y1 + y2) / 2;
    const tw = doc.getTextWidth(o.label);
    doc.setFillColor(...WHITE); doc.rect(lx - tw / 2 - 0.6, ly - 2.4, tw + 1.2, 3.1, "F");
    doc.text(o.label, lx, ly, { align: "center" });
  }
}
function layerLabel(txt, yy){
  const yyv = yy ?? y;
  font("bold", 7.4, TEAL);
  doc.text(txt.toUpperCase(), ML, yyv);
}
function code(str){
  const lines = doc.splitTextToSize(str, CW - 4);
  const h = lines.length * 4 + 3;
  ensure(h + 2.4);
  font("normal", 8.6, [60, 70, 90]);
  doc.setFillColor(...LIGHT2); doc.setDrawColor(...BORDER); doc.setLineWidth(0.25);
  doc.roundedRect(ML, y, CW, h, 1.2, 1.2, "FD");
  lines.forEach((ln, i) => doc.text(ln, ML + 2.4, y + 4 + i * 4));
  y += h + 2.4;
}
function spacer(h){ y += h; }

/* ================= COVER PAGE ================= */
(function cover(){
  doc.setFillColor(...NAVY); doc.rect(0, 0, PW, 92, "F");
  doc.setFillColor(...TEAL); doc.rect(0, 92, PW, 2.2, "F");
  doc.setDrawColor(60, 84, 122); doc.setLineWidth(0.15);
  for (let i = 0; i < 12; i++) for (let j = 0; j < 5; j++)
    doc.circle(150 + i * 5, 12 + j * 5, 0.5, "S");

  font("bold", 9.5, [158, 196, 255]);
  doc.text("TECHNICAL PROJECT REPORT", PW / 2, 26, { align: "center" });
  font("bold", 27, WHITE);
  doc.text("Slate TimeSync", PW / 2, 46, { align: "center" });
  font("normal", 12.5, [196, 214, 240]);
  doc.text("Human Resource Management System (HRMS)", PW / 2, 57, { align: "center" });
  doc.setFontSize(9.8);
  doc.text("Workforce Time, Attendance, Leave and Role-Based Access Platform", PW / 2, 66, { align: "center" });
  doc.text("Spring Boot 3  |  React 18  |  PostgreSQL 16  |  Redis 7  |  Kafka  |  Docker", PW / 2, 76, { align: "center" });

  font("normal", 10, TEXT);
  const rows = [
    ["Project", "Slate TimeSync - HRMS (slate-timesync)"],
    ["Report Title", "Full Project Analysis and Technical Documentation"],
    ["Backend Version", "1.0.0 (hrms-backend, Maven artifact)"],
    ["Document Version", "1.0"],
    ["Report Date", "September 4, 2026"],
    ["Architecture", "Modular Monolith REST API + React SPA + Event-Driven Notifications"],
    ["Prepared By", "Engineering Analysis (automated codebase review)"],
  ];
  let ry = 118;
  doc.setFillColor(...LIGHT2); doc.setDrawColor(...BORDER); doc.setLineWidth(0.25);
  const rw = 150, rx = (PW - rw) / 2, rh = 11;
  rows.forEach((r, i) => {
    doc.setFillColor(...(i % 2 ? WHITE : LIGHT2));
    doc.rect(rx, ry, rw, rh, "FD");
    font("bold", 9, NAVY); doc.text(r[0], rx + 4, ry + 7);
    font("normal", 9, TEXT); doc.text(r[1], rx + 42, ry + 7);
    ry += rh;
  });

  font("bold", 10.5, NAVY);
  doc.text("Document Scope", PW / 2, ry + 16, { align: "center" });
  font("normal", 8.6, MUTED);
  const scope = doc.splitTextToSize(
    "This report is based entirely on the actual source code in this workspace. Every architectural claim, " +
    "configuration value, endpoint and workflow described here was verified by direct inspection of the code. " +
    "Verification results (build, lint, test inventory) reflect real commands executed during report preparation; " +
    "checks that could not be executed in the authoring environment are explicitly marked as such.",
    160);
  doc.text(scope, PW / 2, ry + 23, { align: "center" });
  doc.setFillColor(...NAVY); doc.rect(0, PH - 12, PW, 12, "F");
  font("normal", 8, [190, 205, 228]);
  doc.text("Slate TimeSync  |  Technical Project Report  |  Version 1.0  |  September 2026", PW / 2, PH - 4.5, { align: "center" });
})();

/* ================= TOC PAGE (reserved, filled at end) ================= */
doc.addPage(); // page 2 - TOC placeholder
y = MT; // content continues from page 3

function lead(label, rest, o = {}){
  const size = o.size ?? 9.3;
  font("bold", size, o.color ?? NAVY);
  ensure(size * 0.43 + 1);
  const lw = doc.getTextWidth(label) + 1.2;
  doc.text(label, ML, y + size * 0.30);
  font("normal", size, o.bodyColor ?? TEXT);
  const lines = doc.splitTextToSize(rest, CW - lw);
  let first = true;
  for (const ln of lines){
    if (!first) ensure(size * 0.43);
    doc.text(ln, first ? ML + lw : ML, y + size * 0.30);
    y += size * 0.43; first = false;
  }
  y += o.gap ?? 1.6;
}

/* ================= SECTION 1 ================= */
h1(1, "Executive Summary");
p("Slate TimeSync is a full-stack Human Resource Management System (HRMS) that digitises the daily operational " +
  "core of workforce management: employee records, attendance (clock in/out, breaks, corrections, holidays, policies), " +
  "leave management (types, balances, approvals) and weekly timesheets (entries, submission, approval), wrapped in a " +
  "dynamic role-based access control (RBAC) engine, real-time notifications, and an auditable event pipeline.");
p("The system solves a concrete operational problem: organisations still tracking attendance on spreadsheets and " +
  "approving leave over email have no single, permission-aware source of truth. Slate TimeSync consolidates these " +
  "workflows behind one authenticated web application with clear separation between employee, manager, HR and " +
  "super-admin capabilities.");
lead("Target users. ", "Employees (self-service attendance, timesheets, leave), Managers (team visibility and approvals), " +
  "HR administrators (directory, policies, balances, corrections) and Super Administrators (roles, permissions, audit, system configuration).");
lead("Technical approach. ", "A Java 21 / Spring Boot 3.3 modular monolith exposes a versioned REST API (/api/v1) backed by " +
  "PostgreSQL 16 (Flyway-managed schema of 22 tables), Redis 7 (caching, rate limiting, event idempotency) and Apache Kafka " +
  "(asynchronous notification delivery via a transactional outbox). The frontend is a React 18 + TypeScript single-page " +
  "application built with Vite, styled with Tailwind CSS and shadcn/ui, using TanStack Query for server state and WebSocket " +
  "for live notifications. Docker Compose provisions the entire stack, including Prometheus and Grafana observability.");
lead("Project origin. ", "The frontend was originally generated on the Lovable platform against a Supabase backend. The " +
  "codebase documents a deliberate migration to a self-hosted Spring Boot stack: the Supabase migrations remain under " +
  "supabase/migrations as schema history, the API client comment states it 'replaces the Supabase integration', and " +
  "RbacService explicitly documents that it 'replaces the Supabase RLS policies'.");
lead("Implementation status. ", "The functional core is complete and internally consistent: 50+ REST endpoints across 8 " +
  "controllers, transactional business services, JWT authentication with refresh-token rotation, OAuth2 (Google, GitHub), " +
  "an event-driven notification pipeline with email delivery, audit logging, and a pre-provisioned monitoring stack. " +
  "During report preparation the frontend production build succeeded; the backend test suite (10 tests in 3 classes) exists " +
  "but could not be executed in the authoring environment (no Java/Maven/Docker installed), as reported honestly in Sections 20-22.");

/* ================= SECTION 2 ================= */
h1(2, "Project Overview");
h2("2.1 Purpose and Problem Statement");
p("Manual HR operations produce three recurring failures: (1) attendance data that cannot be trusted because it is " +
  "self-reported and unvalidated; (2) leave approvals that lack balance enforcement and an audit trail; (3) timesheet " +
  "approval that depends on email chains with no central status. Slate TimeSync addresses all three with server-enforced " +
  "rules (unique daily attendance, overlap and balance checks on leave, state-machine transitions on timesheets), a " +
  "permission model enforced at every API boundary, and an append-only audit log for administrative actions.");
h2("2.2 Objectives");
bullets([
  "Provide authenticated, role-aware access to all HR workflows with email verification and account protection (lockout, rate limiting).",
  "Automate attendance capture with policy-driven lateness, break tracking, and manager-approved corrections.",
  "Enforce leave governance: configurable types, yearly balances, overlap prevention, and approve/reject decisions with mandatory rejection reasons.",
  "Support weekly timesheet cycles (draft, submit, approve/reject, resubmit) with bulk approval for approvers.",
  "Deliver notifications reliably: in-app (persisted + WebSocket push) and email (SMTP via Kafka, retried, dead-lettered on failure).",
  "Make every privileged action auditable and observable (audit logs, correlation IDs, Prometheus metrics, Grafana dashboards).",
]);
h2("2.3 Scope");
table({
  head: ["In Scope (implemented)", "Out of Scope (not identified in the current implementation)"],
  widths: [88, 88],
  body: [
    ["Authentication: local + Google/GitHub OAuth2, email verification, password reset, refresh-token rotation",
     "Payroll processing and salary structures"],
    ["Attendance: clock in/out, breaks, corrections, holidays, policies, monthly report",
     "Shift/roster scheduling and shift-swap workflows"],
    ["Leave: types, balances, requests, approvals, cancellation",
     "Recruitment, onboarding pipelines, applicant tracking"],
    ["Timesheets: weekly sheets, project/task entries, billable flags, approvals",
     "Expense management and reimbursements"],
    ["Employees: directory, search/pagination, create/update, onboarding/offboarding flag",
     "Performance reviews / goal-setting cycles (reports cover utilisation only)"],
    ["RBAC: 4 seeded roles, 20 permissions, custom roles, assignments, permission caching",
     "Multi-tenancy (single organisation per deployment)"],
    ["Notifications: in-app + WebSocket + transactional email; reminders (Supabase functions)",
     "Mobile native applications; SMS/push channels"],
    ["Observability: Prometheus metrics, Grafana dashboard, correlation IDs, rolling logs",
     "Distributed tracing (OpenTelemetry) - not configured"],
  ],
  size: 7.9
});
h2("2.4 Use Cases (derived from implemented endpoints and pages)");
table({
  head: ["#", "Actor", "Use Case", "Primary Components"],
  widths: [7, 24, 72, 73],
  body: [
    ["1", "Employee", "Sign up, verify email via SMTP link, sign in", "AuthController, EmailService, VerifyEmail page"],
    ["2", "Employee", "Clock in / start break / end break / clock out", "AttendanceController/Service, Attendance page"],
    ["3", "Employee", "Submit weekly timesheet with project entries", "TimesheetController/Service, Timesheets page"],
    ["4", "Employee", "Apply for leave within remaining balance", "LeaveController/Service, Leave page"],
    ["5", "Manager", "Approve/reject team leave and timesheets (incl. bulk)", "Leave/Timesheet decision endpoints"],
    ["6", "Manager/HR", "Approve attendance corrections with amended times", "Attendance corrections endpoints"],
    ["7", "HR", "Manage employee directory, onboard/offboard profiles", "EmployeeController, TeamManagement page"],
    ["8", "HR", "Configure leave balances and attendance policies", "Leave balances endpoint, Attendance policy endpoints"],
    ["9", "Super Admin", "Create roles, grant permissions, assign roles", "RbacController, RoleManagement page"],
    ["10", "Super Admin", "Review audit trail of privileged changes", "AuditController, /api/v1/admin/audit-logs"],
    ["11", "All", "Receive real-time in-app and email notifications", "Kafka consumer, WebSocketHandler, bell UI"],
    ["12", "Employee", "Import calendar events as timesheet entries", "CalendarImport component (file upload, mapping)"],
  ],
  size: 7.9
});
h2("2.5 Expected Outcomes");
bullets([
  "A single permission-aware system of record for attendance, leave and timesheets.",
  "Managers act on pending approvals instead of chasing email threads; employees see decisions in real time.",
  "Every privileged change is attributable (audit_logs, rbac_audit_logs, attendance_audit_logs).",
  "Operations teams get metrics and dashboards out of the box (Prometheus + Grafana provisioning included).",
]);

/* ================= SECTION 3 ================= */
h1(3, "Functional Requirements");
p("Requirements below are reverse-engineered from the implemented controllers, services, database constraints and UI " +
  "pages. 'Verified' means the behaviour is enforced in code (not merely claimed in documentation).");
table({
  head: ["ID", "Requirement", "Enforced By (evidence)", "Status"],
  widths: [12, 78, 68, 18],
  body: [
    ["FR-A1", "Register with email/password; duplicate emails rejected (case-insensitive)", "AuthService.signUp + users.email UNIQUE + existsByEmailIgnoreCase", "Verified"],
    ["FR-A2", "Email verification required before first sign-in", "SignIn throws EMAIL_NOT_VERIFIED; one_time_tokens purpose=EMAIL_VERIFICATION", "Verified"],
    ["FR-A3", "Password reset via one-time emailed token; all sessions revoked on reset", "forgotPassword / resetPassword; revokeAllSessions(userId)", "Verified"],
    ["FR-A4", "JWT access (15 min) + refresh token (7 days) with rotation and revocation", "JwtTokenProvider; refresh_tokens.token_hash; AuthService.refresh", "Verified"],
    ["FR-A5", "Account lockout after 5 failed logins for 15 minutes", "AuthService MAX_FAILED_ATTEMPTS/LOCK_MINUTES; users.locked_until", "Verified"],
    ["FR-A6", "OAuth2 sign-in with Google and GitHub; user auto-provisioned", "SecurityConfig oauth2Login, CustomOAuth2UserService, OAuth2LoginSuccessHandler", "Verified"],
    ["FR-A7", "Rate limiting: 20 req/min on auth, 300 req/min on API (per IP)", "RateLimitFilter (Redis counters, 429 response)", "Verified"],
    ["FR-AT1", "One attendance record per employee per day (clock in once)", "UNIQUE(profile_id, date); checkIn business-rule guard", "Verified"],
    ["FR-AT2", "Late status derived from policy grace period", "AttendanceService.isLate() vs attendance_policies", "Verified"],
    ["FR-AT3", "Break start/end windows recorded; totals computed on checkout", "startBreak/endBreak; computeTotalHours", "Verified"],
    ["FR-AT4", "Employees can request corrections; approvers amend times and status", "POST /corrections; approveCorrectionExtended", "Verified"],
    ["FR-AT5", "Holidays manageable by HR; viewable by year (recurring supported)", "HolidayRepository, /attendance/holidays", "Verified"],
    ["FR-AT6", "Monthly attendance report per employee (counts by status)", "GET /attendance/report/{profileId}", "Verified"],
    ["FR-L1", "Leave request validated: active type, date order, no overlap, balance sufficiency", "LeaveService.createRequest checks (4 guards)", "Verified"],
    ["FR-L2", "Approve/reject with mandatory rejection reason; approval stamps user+time", "LeaveService.decide", "Verified"],
    ["FR-L3", "Only pending requests cancellable", "LeaveService.cancel guard", "Verified"],
    ["FR-L4", "Role-aware request visibility (own / team / all)", "LeaveService.visibleRequests switch on effectiveRole", "Verified"],
    ["FR-L5", "Leave types and balances administrable", "types CRUD; POST /leaves/balances (ensureBalance)", "Verified"],
    ["FR-T1", "Weekly timesheet auto-created (Monday-anchored) as draft", "TimesheetService.createOrGet", "Verified"],
    ["FR-T2", "Entries editable only in draft/rejected state; totals recalculated", "saveEntry guard; recalculate(ts)", "Verified"],
    ["FR-T3", "Submit, approve, reject (reason required); bulk approve", "submit/decide/bulkApprove", "Verified"],
    ["FR-T4", "Billable flag and project name per entry", "TimesheetEntry fields; SaveEntry record", "Verified"],
  ],
  size: 7.6
});
table({
  head: ["ID", "Requirement", "Enforced By (evidence)", "Status"],
  widths: [12, 78, 68, 18],
  body: [
    ["FR-E1", "Employee search with pagination (max 100 page size) + department filter", "EmployeeService.search (ProfileRepository.search)", "Verified"],
    ["FR-E2", "Create employee with unique email and employee ID (auto EMP-xxxxxxxx)", "EmployeeService.create", "Verified"],
    ["FR-E3", "Update deactivation flag implements onboarding/offboarding", "EmployeeService.update (isActive)", "Verified"],
    ["FR-R1", "Dynamic roles + module/action permissions; 4 system roles and 20 permissions seeded", "V2__seed.sql; roles/permissions/role_permissions", "Verified"],
    ["FR-R2", "Every privileged endpoint checks requirePermission(userId, module, action)", "RbacService; controllers", "Verified"],
    ["FR-R3", "Permission lookups cached in Redis (5 min TTL) with invalidation on change", "RbacService.cache/invalidateUserCache", "Verified"],
    ["FR-R4", "Legacy 4-value role system combined with RBAC for effective role", "RbacService.effectiveRole precedence", "Verified"],
    ["FR-N1", "In-app notifications persisted and pushed live over WebSocket", "NotificationService.push; NotificationWebSocketHandler", "Verified"],
    ["FR-N2", "Business events produce emails asynchronously with retry and DLT", "OutboxService -> Kafka -> HrmsEventConsumer; EmailService retry x3", "Verified"],
    ["FR-N3", "Duplicate event consumption suppressed (7-day idempotency window)", "Redis setIfAbsent on hrms:evdone:{eventId}", "Verified"],
    ["FR-P1", "Weekly performance metrics per employee (hours, attendance %, utilisation, score)", "ReportController.performance", "Verified"],
    ["FR-P2", "Per-employee drill-down: 8-week hours, 30-day attendance trend, workload split", "ReportController.detail", "Verified"],
    ["FR-X1", "Audit log of privileged actions, paged, admin-only", "AuditRecorder; AuditController (role_management:view)", "Verified"],
    ["FR-X2", "Correlation ID + user ID attached to every log line", "CorrelationIdFilter; JwtAuthenticationFilter MDC", "Verified"],
  ],
  size: 7.6
});

/* ================= SECTION 4 ================= */
h1(4, "Non-Functional Requirements");
p("The table distinguishes between properties actively supported by the implementation and properties that are " +
  "recommendations only. No unverified claim is made.");
table({
  head: ["Dimension", "Evidence in Implementation", "Assessment"],
  widths: [28, 110, 38],
  body: [
    ["Performance", "Hikari pool (max 10); Redis caches (permissions TTL 300 s); DB indexes on hot paths (attendance profile+date, refresh token expiry, outbox unprocessed); rate limiting protects auth endpoints; Vite production build.", "Partial; no load testing performed"],
    ["Scalability", "Stateless API (JWT, no server sessions) allows horizontal scaling; Kafka decouples notification load; Redis centralises counters/cache.", "Partial; single-node Postgres is a ceiling"],
    ["Security", "BCrypt cost 12; hashed refresh/one-time tokens; lockout; rate limiting; CORS whitelist; RBAC on every privileged endpoint; secrets via environment; include-stacktrace: never.", "Strong baseline; see Section 19"],
    ["Reliability", "Transactional outbox with retry; Kafka ErrorHandlingDeserializer + DLT (hrms.events.dlt); email retry (3 attempts, backoff); Redis cache degrades gracefully to DB; compose healthchecks.", "Supported by design; failover untested"],
    ["Maintainability", "Package-by-feature layout (com.hrms.<module>); Flyway versioned schema; DTO records per controller; central exception model; Swagger annotations.", "Supported"],
    ["Usability", "shadcn/ui component system, dark theme default, toast feedback, role-gated navigation (useRBAC), live notification bell.", "Supported (qualitative)"],
    ["Availability", "Compose healthchecks (postgres/redis/kafka) with depends_on conditions; actuator health endpoint exposed.", "Partial; no HA configuration"],
    ["Accessibility", "Built on Radix UI primitives (keyboard/ARIA behaviour inherited); no dedicated accessibility audit identified.", "Not verified"],
    ["Data Integrity", "PK/FK constraints, UNIQUE(profile_id,date), CHECK constraints on enums (status, provider, role), ddl-auto: validate, optimistic-lock version columns, audit snapshots.", "Supported"],
    ["Observability", "Actuator (health/info/metrics/prometheus); Micrometer counters (hrms.email.sent/failed, hrms.events.processed/failed); Logback rolling logs (14 days) with MDC; Grafana dashboard provisioned.", "Supported"],
  ],
  size: 7.9
});

/* ================= SECTION 5 ================= */
h1(5, "Features and Functionalities");
h2("5.1 Implemented Features");
table({
  head: ["Feature", "What It Does / How It Works", "Key Components", "Status"],
  widths: [30, 84, 44, 18],
  body: [
    ["Authentication suite", "Signup sends a real verification email (via outbox + Kafka). Sign-in validates credentials, verification state, lockout and status, then issues an access JWT (15 min) and a rotating refresh token stored as SHA-256 hash. Logout revokes; forgot/reset/change-password flows included.", "AuthService, AuthController, JwtTokenProvider, one_time_tokens, refresh_tokens", "Implemented"],
    ["OAuth2 social login", "Spring Security oauth2Login for Google and GitHub; CustomOAuth2UserService provisions the local user; success handler issues the same JWT pair.", "SecurityConfig, CustomOAuth2UserService, OAuth2LoginSuccessHandler", "Implemented"],
    ["Rate limiting", "Redis sliding counters per IP: 20/min on /api/v1/auth/*, 300/min elsewhere; 429 with JSON error; auth endpoints protected against brute force in combination with lockout.", "RateLimitFilter", "Implemented"],
    ["Attendance tracking", "Daily clock in/out, break windows, total hours, policy-driven 'late' status, notes; one record per person per day enforced by DB constraint.", "AttendanceController/Service, attendance table", "Implemented"],
    ["Attendance corrections", "Employee submits correction with reason; approver view lists pending; approval can amend check-in/out times, recompute hours, set status; audit snapshot written; event emitted.", "corrections endpoints, attendance_audit_logs, ATTENDANCE_CORRECTED event", "Implemented"],
    ["Holidays & policies", "Year-filtered holiday calendar (recurring support); attendance policy CRUD with working hours, grace period, break and overtime threshold; 'late' logic reads the active policy.", "holidays, attendance_policies", "Implemented"],
    ["Monthly report", "GET /attendance/report/{profileId} aggregates present/late/absent/half-day counts for a year-month.", "AttendanceController.monthlyReport", "Implemented"],
    ["Leave management", "Type catalogue (4 seeded types), yearly balances per type, requests with overlap + balance guards, approve/reject with mandatory reason, cancel while pending, role-aware listing.", "LeaveController/Service, leave_* tables", "Implemented"],
    ["Timesheets", "Monday-anchored weekly sheet auto-created as draft; per-entry project/task/hours/billable; totals recalculated on every change; draft/rejected editable only; submit/approve/reject + bulk approve; team visibility by role.", "TimesheetController/Service, timesheets, timesheet_entries", "Implemented"],
    ["Calendar import (timesheets)", "Upload a calendar export file, map events to projects, select events, then bulk-create timesheet entries for the current week.", "components/timesheets/CalendarImport.tsx", "Implemented"],
    ["Employee directory", "Paged search (search + department filters), create with unique employee ID (auto-generated EMP- prefix), update, active-flag onboarding/offboarding; /employees/me resolves own profile.", "EmployeeController/Service", "Implemented"],
    ["RBAC engine", "4 seeded roles x 20 module:action permissions; custom roles; role-permission grants; user assignments; effective role merges legacy user_roles with assignments; Redis-cached permissions (5 min TTL) invalidated on change.", "RbacController/Service, roles, permissions, user_role_assignments", "Implemented"],
    ["Performance reports", "Weekly per-employee scorecard (hours vs 40h target, attendance rate, utilisation, composite score) plus 8-week hours trend, 30-day attendance trend and workload breakdown.", "ReportController", "Implemented"],
    ["Notification pipeline", "Transactional outbox -> Kafka -> consumer creates in-app notifications (persisted + WebSocket push) and emails (SendGrid SMTP, 3 retries). Redis idempotency prevents double-processing; failures land in DLT.", "OutboxService, HrmsEventConsumer, EmailService, NotificationService", "Implemented"],
    ["WebSocket live updates", "Client connects to /ws/notifications with JWT in query; handler pushes per-user JSON payloads; client auto-reconnects every 5 s.", "WebSocketConfig, NotificationWebSocketHandler, api.connectNotifications", "Implemented"],
    ["Audit logging", "Append-only audit_logs with old/new value snapshots for employees, roles, role assignments; separate rbac_audit_logs and attendance_audit_logs; admin-only paged API.", "AuditRecorder, AuditController", "Implemented"],
    ["Observability", "Actuator + Micrometer Prometheus endpoint; hrms.* business counters; CorrelationIdFilter propagates X-Correlation-Id; Logback pattern includes correlationId and userId; Grafana dashboard provisioned.", "application.yml, logback-spring.xml, docker/grafana", "Implemented"],
    ["PDF/XLSX exports (client)", "Frontend bundles jspdf + jspdf-autotable and xlsx for client-side report/export generation in HR screens.", "package.json dependencies", "Implemented"],
  ],
  size: 7.5
});
h2("5.2 Partially Implemented / Legacy");
table({
  head: ["Item", "Observation", "Status"],
  widths: [34, 122, 20],
  body: [
    ["Supabase edge functions", "Five Deno functions (reminder/notification emails) remain under supabase/functions; they are not part of the Spring Boot runtime and would only run on Supabase infrastructure. Kept as legacy/history.", "Legacy"],
    ["AttendanceReminderJob", "Backend scheduled job exists for attendance reminders; its runtime behaviour was reviewed statically only (not executed in this environment).", "Implemented (static review)"],
    ["Team attendance view", "GET /attendance/team returns all records in range for non-employees (role string check), without manager-scope filtering.", "Partial"],
  ],
  size: 7.9
});

/* ================= SECTION 6 ================= */
h1(6, "Technology Stack");
table({
  head: ["Technology", "Version", "Purpose", "Where Used"],
  widths: [40, 18, 66, 52],
  body: [
    ["Java", "21", "Backend language (virtual-thread era LTS)", "backend/pom.xml (java.version)"],
    ["Spring Boot", "3.3.5", "Application framework (modular monolith)", "backend"],
    ["Spring Security + OAuth2 Client", "6.x (Boot-managed)", "JWT stateless auth, BCrypt, Google/GitHub login", "SecurityConfig, auth package"],
    ["Spring Data JPA / Hibernate", "Boot-managed", "ORM, repositories, ddl-auto validate", "All services"],
    ["PostgreSQL", "16-alpine", "Primary relational database", "docker-compose, Flyway V1/V2"],
    ["Flyway", "10.x (Boot-managed)", "Versioned migrations + seed data", "V1__init.sql, V2__seed.sql"],
    ["Redis", "7-alpine", "Cache (permissions), rate limiting, event idempotency", "RedisConfig, RateLimitFilter, RbacService, HrmsEventConsumer"],
    ["Apache Kafka (bitnami)", "3.7", "Async event bus for notifications", "KafkaConfig, OutboxService, HrmsEventConsumer"],
    ["JJWT", "0.12.6", "JWT creation/parsing (HMAC-SHA)", "JwtTokenProvider"],
    ["Spring Kafka", "Boot-managed", "Producer/consumer, JsonSerializer, DLT", "KafkaConfig, KafkaErrorConfig"],
    ["Spring WebSocket", "Boot-managed", "Real-time notification push", "WebSocketConfig, NotificationWebSocketHandler"],
    ["Spring Mail (Jakarta Mail)", "Boot-managed", "SMTP email (SendGrid relay)", "EmailService, EmailTemplates"],
    ["Flyway + Testcontainers + H2", "test scope", "Integration testing against real Postgres", "backend tests"],
    ["springdoc-openapi", "2.6.0", "Swagger UI / OpenAPI docs", "OpenApiConfig, /swagger-ui.html"],
    ["Micrometer + Prometheus registry", "Boot-managed", "Metrics export for Prometheus", "Actuator config"],
    ["Lombok", "Boot-managed", "Boilerplate reduction", "Entities"],
    ["Logback", "Boot-managed", "Structured logging, MDC, rolling files", "logback-spring.xml"],
    ["React", "18.3", "UI library", "frontend/src"],
    ["TypeScript", "5.8", "Type-safe frontend", "All frontend sources"],
    ["Vite", "5.4", "Dev server + production bundler (SWC)", "vite.config.ts"],
    ["Tailwind CSS + tailwindcss-animate", "3.4", "Utility-first styling", "tailwind.config.ts"],
    ["shadcn/ui + Radix primitives", "various", "Accessible component kit (48 UI components)", "src/components/ui"],
    ["TanStack Query", "5.83", "Server-state cache/mutations", "App QueryClientProvider, hooks"],
    ["react-router-dom", "6.30", "Client routing (14 routes)", "App.tsx"],
    ["react-hook-form + zod + @hookform/resolvers", "7.6 / 3.25", "Form handling and schema validation", "Forms in HR screens"],
    ["recharts", "2.15", "Dashboard charts (performance)", "Performance page"],
    ["date-fns", "3.6", "Date handling", "Timesheet/calendar components"],
    ["jspdf + jspdf-autotable + xlsx", "3.0 / 5.0 / 0.18", "Client-side PDF and Excel export", "HR report screens"],
    ["lucide-react", "0.462", "Icon set", "UI"],
    ["Docker / Docker Compose", "-", "Containerised build + full local stack", "backend/Dockerfile, frontend/Dockerfile, docker/docker-compose.yml"],
    ["Nginx", "1.27-alpine", "Static SPA hosting + reverse proxy (/api, /ws)", "frontend/nginx.conf"],
    ["Prometheus / Grafana", "latest", "Metrics scraping + dashboards", "docker/prometheus, docker/grafana"],
    ["Deno (legacy)", "std 0.190", "Supabase edge functions (reminder emails)", "supabase/functions"],
    ["ESLint + typescript-eslint", "9.32 / 8.38", "Static analysis (frontend)", "eslint.config.js"],
  ],
  size: 7.6
});

/* ================= SECTION 7 ================= */
h1(7, "System Architecture");
p("Slate TimeSync is a modular monolith: one Spring Boot deployment owns all business modules, chosen deliberately to " +
  "keep transactional integrity (single database, single transaction manager) while retaining module boundaries that " +
  "could later be extracted into services. Asynchronous work (emails, notifications) is decoupled through Kafka using " +
  "the transactional outbox pattern, so a Kafka or SMTP outage can never roll back a business transaction.");
(function arch(){
  const x = ML, W = CW;
  layerLabel("Client layer", y); y += 3.4;
  dBox(x, y, W, 11, ["React 18 SPA  -  TypeScript + Vite 5 + Tailwind CSS + shadcn/ui  -  TanStack Query state  -  WebSocket client"], { bold: false });
  let top = y; y += 11;
  dArrow(x + W / 2, y, x + W / 2, y + 8, { label: "HTTPS - REST /api/v1  +  WebSocket /ws/notifications (JWT)" });
  y += 8;
  layerLabel("Edge / hosting", y); y += 3.4;
  dBox(x, y, W / 2 - 3, 13, ["Nginx 1.27  (container :8080)", "Static SPA + reverse proxy /api,/ws -> backend:8081"], { size: 7.4 });
  dBox(x + W / 2 + 3, y, W / 2 - 3, 13, ["Vite Dev Server (:8080/5173, dev only)", "proxy /api,/oauth2,/login,/ws -> localhost:8081"], { size: 7.4, fill: LIGHT2 });
  y += 13;
  dArrow(x + W * 0.25, y, x + W * 0.4, y + 7); dArrow(x + W * 0.75, y, x + W * 0.6, y + 7);
  y += 7;
  layerLabel("Application layer - Spring Boot 3.3 (container :8081)"); y += 3.4;
  doc.setFillColor(...LIGHT); doc.setDrawColor(...NAVY); doc.setLineWidth(0.35);
  doc.roundedRect(x, y, W, 30, 1.5, 1.5, "FD");
  font("bold", 7.6, NAVY); doc.text("Filter chain ->  CORS  >  RateLimitFilter (Redis)  >  JwtAuthenticationFilter  >  Controller", x + 3, y + 5.4);
  const bw = (W - 8) / 3;
  dBox(x + 2, y + 7.4, bw, 19, ["REST Controllers", "Auth - Attendance - Leave", "Timesheets - Employees - RBAC", "Reports - Notifications - Audit"], { fill: WHITE, size: 7.2 });
  dBox(x + 4 + bw, y + 7.4, bw, 19, ["Services (business rules)", "RBAC requirePermission checks", "Transactional + outbox events"], { fill: WHITE, size: 7.4 });
  dBox(x + 6 + 2 * bw, y + 7.4, bw, 19, ["Spring Data JPA repositories", "Flyway schema (22 tables)", "JPA + Hibernate (validate)"], { fill: WHITE, size: 7.4 });
  y += 30;
  dArrow(x + W * 0.2, y, x + W * 0.2, y + 7); dArrow(x + W * 0.42, y, x + W * 0.42, y + 7);
  dArrow(x + W * 0.64, y, x + W * 0.64, y + 7); dArrow(x + W * 0.86, y, x + W * 0.86, y + 7);
  y += 7;
  layerLabel("Data / infrastructure layer"); y += 3.4;
  const iw = (W - 9) / 4;
  dBox(x, y, iw, 14, ["PostgreSQL 16", ":5432", "Flyway, Hikari pool"], { fill: WHITE });
  dBox(x + iw + 3, y, iw, 14, ["Redis 7", ":6379", "cache - rate limit - idempotency"], { fill: WHITE, size: 7.3 });
  dBox(x + 2 * (iw + 3), y, iw, 14, ["Apache Kafka 3.7", ":9092", "hrms.events + .dlt"], { fill: WHITE });
  dBox(x + 3 * (iw + 3), y, iw, 14, ["SMTP (SendGrid)", ":587", "async email, 3 retries"], { fill: WHITE });
  y += 14;
  dArrow(x + iw * 0.5 + 1.5, y, x + iw * 0.5 + 1.5, y + 7, { dash: true, color: MUTED });
  dArrow(x + 2.5 * iw + 5.5, y, x + 2.5 * iw + 5.5, y + 7, { dash: true, color: MUTED });
  y += 7;
  layerLabel("External / observability"); y += 3.4;
  dBox(x, y, W / 2 - 3, 13, ["External identity providers", "Google OAuth2 + GitHub OAuth2 (server-side flow)"], { fill: LIGHT2, size: 7.4 });
  dBox(x + W / 2 + 3, y, W / 2 - 3, 13, ["Prometheus :9090  ->  Grafana :3001", "Actuator /actuator/prometheus scrape + HRMS dashboard"], { fill: WHITE, stroke: TEAL, size: 7.3 });
  y += 13;
  figCaption("Slate TimeSync system architecture - request path (solid) and monitoring (dashed).");
})();
p("Flow of a typical request: the SPA calls same-origin /api/v1 paths. In production Nginx serves the built bundle and " +
  "proxies /api and /ws to the backend container; in development the Vite dev server performs the same proxying. The " +
  "backend applies CORS, Redis-backed rate limiting and JWT validation before any controller executes. Services enforce " +
  "RBAC permissions, mutate PostgreSQL through JPA, and append outbox events inside the same transaction. A scheduled " +
  "dispatcher publishes outbox events to Kafka; the notification consumer fans out to in-app messages (WebSocket) and email.");

/* ================= SECTION 8 ================= */
h1(8, "Detailed Component Architecture");
table({
  head: ["Component", "Responsibility", "Processing", "Outputs / Interactions"],
  widths: [32, 46, 62, 36],
  body: [
    ["AuthController / AuthService", "Sign up, sign in, refresh (rotation), logout, forgot/reset/change password, verify/resend email, /me", "Uniqueness check, BCrypt encode/verify, lockout counters, verification gate, token hashing (SHA-256), session revocation", "JWT pair; outbox EMAIL_VERIFICATION / PASSWORD_RESET events; users / refresh_tokens / one_time_tokens"],
    ["JwtTokenProvider", "Issue/parse HMAC-signed access tokens", "Claims: sub, email, roles, type=access, 15-min expiry", "Signed JWT; parsed claims for the filter"],
    ["JwtAuthenticationFilter", "Authenticate every request from Bearer header", "Signature + expiry check, build UserPrincipal, set SecurityContext and MDC userId", "Authenticated request or anonymous (401 downstream)"],
    ["RateLimitFilter", "Per-IP sliding window limits", "Redis INCR with 60 s expiry; auth 20/min, API 300/min", "X-RateLimit-Limit header; 429 JSON on breach; skipped for /ws and /actuator"],
    ["AttendanceController/Service", "Daily attendance lifecycle, corrections, holidays, policies, monthly report", "Duplicate-day guard, grace-period lateness, total-hours computation, audit snapshots, correction approval", "attendance rows, attendance_audit_logs, ATTENDANCE_CORRECTED event"],
    ["LeaveController/Service", "Leave types, balances, requests, decisions", "4 validation guards; state transitions; visibility by role", "leave_requests rows, LEAVE_* events, leave_balances"],
    ["TimesheetController/Service", "Weekly timesheet lifecycle", "Monday anchoring, ownership check, state machine, totals recalculation, bulk approve", "timesheets / timesheet_entries, TIMESHEET_* events"],
    ["EmployeeController/Service", "Directory CRUD + paged search", "Duplicate email/ID guards, EMP-id generation, audit record", "profiles rows, EMPLOYEE_* audit logs"],
    ["RbacController/Service", "Roles, permissions, grants, assignments, effective role", "Uniqueness checks, cache invalidation, audit", "roles / permissions tables, Redis perm cache, ROLE_* audit logs"],
    ["ReportController", "Performance metrics and drill-down", "Aggregation over timesheet sums + attendance counts", "JSON metric maps (scorecard, trends, workload)"],
    ["OutboxService", "Transactional outbox", "append() inside caller txn; scheduled dispatcher publishes up to 100 pending; retries++ on failure; 7-day purge", "Kafka hrms.events; outbox_events table"],
    ["HrmsEventConsumer", "Event -> notifications fan-out", "Redis idempotency (7 d), per-type switch, Micrometer counters", "notifications rows + WebSocket push + EmailService send; failures -> DLT"],
    ["EmailService", "SMTP delivery", "3 attempts with linear backoff; MAIL_ENABLED=false logs instead; Micrometer counters", "Email delivered or permanent-failure log"],
    ["NotificationService + WebSocket", "In-app notifications", "Persist + JSON push to the user's sessions; read / read-all transitions", "notifications table, WebSocket frames"],
    ["AuditRecorder / AuditController", "Privileged-change trail", "Append-only insert with old/new snapshots; paged admin query gated by permission", "audit_logs rows; /api/v1/admin/audit-logs"],
    ["RbacService", "Authorization core", "Effective-role precedence; permission load from assignments; Redis cache (5 min) with DB fallback", "hasPermission / requirePermission; 403 via AccessDeniedException"],
    ["GlobalExceptionHandler / ApiException", "Uniform error contract", "Map ApiException / AccessDenied / validation errors to HTTP status + {code, message} JSON; no stack traces", "ErrorResponse JSON"],
    ["Frontend api client (lib/api.ts)", "All HTTP access", "Attach Bearer; on 401 single-flight refresh then one retry; normalise ApiError; 204 passthrough", "Parsed JSON or ApiError; tokens in localStorage"],
    ["useAuth / useRBAC hooks", "Session + permission state", "GET /auth/me bootstrap; signIn/signUp/signOut wrappers; role gates", "AuthState consumed by pages"],
  ],
  size: 7.3
});

/* ================= SECTION 9 ================= */
h1(9, "Frontend Architecture");
p("The frontend is a React 18 + TypeScript SPA scaffolded on Vite with the shadcn/ui ecosystem. Server state lives in " +
  "TanStack Query; ephemeral UI state lives in hooks; there is no global Redux-style store. All HTTP access is funnelled " +
  "through one typed client (src/lib/api.ts) that owns token storage, transparent refresh-token rotation and error normalisation.");
(function fe(){
  const x = ML, W = CW;
  layerLabel("Pages (14 routes, react-router-dom)"); y += 3.4;
  const pw = (W - 21) / 7, ph = 9.5;
  const pages = ["Index", "Login", "Dashboard", "Attendance", "Timesheets", "Leave", "Performance",
                 "RoleMgmt", "TeamMgmt", "ForgotPw", "ResetPw", "VerifyEmail", "OAuthCallback", "NotFound"];
  pages.forEach((pg, i) => {
    const cx = x + (i % 7) * (pw + 3.5), cy = y + Math.floor(i / 7) * (ph + 2.6);
    dBox(cx, cy, pw, ph, [pg], { size: 6.9, fill: i < 7 ? LIGHT : LIGHT2 });
  });
  y += 2 * (ph + 2.6) + 2;
  dArrow(x + W / 2, y, x + W / 2, y + 6, { label: "compose + data-fetch" });
  y += 6;
  dBox(x, y, W / 2 - 3, 13, ["Feature hooks (8)", "useAuth - useRBAC - useAttendance", "useLeave - useTimesheets - useNotifications", "usePerformance - use-toast"], { size: 7.1 });
  dBox(x + W / 2 + 3, y, W / 2 - 3, 13, ["UI layer", "shadcn/ui components (48)", "feature components: attendance, leave,", "timesheets, rbac, team, performance"], { size: 7.1, fill: WHITE });
  y += 12;
  dArrow(x + W / 2, y, x + W / 2, y + 6, { label: "single API surface" });
  y += 6;
  dBox(x, y, W, 12, ["lib/api.ts  -  fetch client: Bearer header, auto refresh (single-flight), ApiError normalisation, WebSocket connector", "tokens in localStorage (hrms_access_token / hrms_refresh_token)  -  base URL: same-origin (vite proxy / nginx)"], { size: 7.3 });
  y += 12;
  dArrow(x + W / 2, y, x + W / 2, y + 6);
  y += 6;
  dBox(x, y, W, 9, ["Spring Boot API  /api/v1 (REST)   +   /ws/notifications (WebSocket, JWT in query param)"], { size: 7.6, stroke: TEAL });
  y += 9;
  figCaption("Frontend architecture - pages compose feature hooks, which call the central API client.");
})();
h2("9.1 Routing, State and Communication");
bullets([
  { t: "Routing (App.tsx): ", d: "14 routes under BrowserRouter with a catch-all NotFound. Pages are thin wrappers composing feature components (e.g. Login renders LoginPage). No route-level code splitting is configured - all pages ship in one bundle." },
  { t: "Server state: ", d: "TanStack Query QueryClientProvider wraps the app; feature hooks (useAttendance, useLeave, useTimesheets, useNotifications, usePerformance) encapsulate queries/mutations against /api/v1." },
  { t: "Session state: ", d: "useAuth loads /auth/me on mount, stores user/profile/role, exposes signIn/signUp/signOut and OAuth redirect helpers; a 401 clears local tokens." },
  { t: "Authorization UI: ", d: "useRBAC reads the permission list returned by /auth/me to gate navigation and actions client-side; the server remains the enforcement point." },
  { t: "Real-time: ", d: "connectNotifications opens /ws/notifications?token=JWT, retries every 5 s on close, and feeds the notification bell." },
  { t: "Forms & validation: ", d: "react-hook-form + zod resolvers are available; timesheet/leave screens validate inputs before submission; server-side validation stays authoritative." },
  { t: "Error handling: ", d: "ApiError (status + code + message) is thrown for non-2xx responses; toasts surface messages; auto-refresh retry happens at most once per request." },
  { t: "Theming: ", d: "ThemeProvider defaults to dark; a theme-toggle component exists; design tokens flow through Tailwind configuration." },
]);

/* ================= SECTION 10 ================= */
h1(10, "Backend Architecture");
h2("10.1 Structure and Layering");
p("The backend is a single Maven module (com.hrms) organised package-by-feature. Each business module is self-contained: " +
  "controller (HTTP + DTO records), service (business rules + transactions), entities and Spring Data repositories. " +
  "Cross-cutting concerns live in config/, security/, common/ and events/.");
table({
  head: ["Layer", "Implementation", "Notes"],
  widths: [32, 88, 56],
  body: [
    ["Web / controllers", "@RestController classes with @RequestMapping(\"/api/v1/...\")", "DTOs declared as Java records inside controllers; Swagger @Operation annotations"],
    ["Security filters", "RateLimitFilter -> JwtAuthenticationFilter (both before UsernamePasswordAuthenticationFilter)", "Stateless sessions; CSRF disabled (bearer model); CORS from AppProperties"],
    ["Services", "@Service + @Transactional business logic; RBAC via rbac.requirePermission", "Business failures thrown as ApiException (400/403/404/409/rules)"],
    ["Persistence", "Spring Data JPA repositories; Hibernate ddl-auto: validate; Flyway owns schema", "Open-in-view disabled; UTC timezone enforced"],
    ["Events", "OutboxService.append (MANDATORY propagation) + scheduled dispatcher + @KafkaListener consumer", "Topic hrms.events; DLT hrms.events.dlt; JSON serde"],
    ["Async / scheduling", "Outbox dispatch poller, purge job, AttendanceReminderJob (@Scheduled)", "Email and reminder work never blocks API threads"],
    ["Real-time", "WebSocketConfig registering NotificationWebSocketHandler at /ws/notifications", "JWT passed as query parameter; per-user push"],
    ["Configuration", "AppProperties @ConfigurationProperties; RedisConfig; KafkaConfig + KafkaErrorConfig; OpenApiConfig; CorrelationIdFilter", "All secrets from environment variables (Appendix)"],
    ["Error handling", "GlobalExceptionHandler -> ErrorResponse {code, message}; stack traces disabled", "Machine-readable codes (EMAIL_NOT_VERIFIED, RATE_LIMIT_EXCEEDED, ...)"],
    ["Observability", "Actuator (health/info/metrics/prometheus); Micrometer counters; Logback MDC (correlationId, userId)", "Rolling daily log files, 14-day retention, logs/hrms.log"],
  ],
  size: 7.7
});
h2("10.2 Business Logic Highlights");
bullets([
  { t: "Attendance: ", d: "checkIn rejects a second record for the same day (a DB unique constraint backs the guard); status becomes 'late' when LocalTime.now() exceeds the active policy start plus grace minutes; checkout computes total hours." },
  { t: "Leave: ", d: "createRequest validates active type, date ordering, overlapping pending/approved requests (repository existsOverlapping) and remaining balance before persisting; rejection requires a reason." },
  { t: "Timesheets: ", d: "entries are only editable on draft/rejected sheets owned by the caller; every entry change recalculates the sheet total; decisions emit events for notification." },
  { t: "RBAC: ", d: "effectiveRole merges the legacy user_roles enum with dynamic assignments; permissions load from role_permissions, are cached in Redis for 5 minutes, and are invalidated on any role/assignment change." },
  { t: "Notifications: ", d: "every notification side-effect flows through the outbox -> Kafka -> consumer path with Redis deduplication on event IDs and a dead-letter topic for poison messages." },
]);

/* ================= SECTION 11 ================= */
h1(11, "API Documentation");
p("Base URL: /api/v1 (backend port 8081; proxied same-origin in production). 'Auth' column: Public = reachable without " +
  "a token; JWT = valid access token required; Perm = additionally gated by an RBAC module:action permission. Interactive " +
  "docs are served at /swagger-ui.html (springdoc).");
table({
  head: ["Method", "Endpoint", "Purpose", "Auth", "Errors"],
  widths: [17, 52, 63, 22, 22],
  body: [
    ["POST", "/api/v1/auth/signup", "Create account, send verification email", "Public", "409 duplicate"],
    ["POST", "/api/v1/auth/login", "Email/password sign-in -> token pair", "Public + rate limit", "401, 403 (locked/unverified)"],
    ["POST", "/api/v1/auth/refresh", "Rotate refresh token -> new pair", "Public", "401 invalid/expired"],
    ["POST", "/api/v1/auth/logout", "Revoke refresh token", "Public", "400"],
    ["POST", "/api/v1/auth/forgot-password", "Email a reset link (no user enumeration)", "Public", "200 always"],
    ["POST", "/api/v1/auth/reset-password", "Set new password with one-time token", "Public", "400 invalid/expired"],
    ["POST", "/api/v1/auth/change-password", "Change password while signed in", "JWT", "400 wrong current"],
    ["POST", "/api/v1/auth/verify-email", "Verify email with token param", "Public", "400 invalid/expired"],
    ["POST", "/api/v1/auth/resend-verification", "Re-send verification email", "Public", "200 (idempotent)"],
    ["GET", "/api/v1/auth/me", "Current user, profile, role, permissions", "JWT", "404"],
    ["GET", "/api/v1/attendance/today", "My attendance for today", "JWT", "404 profile"],
    ["GET", "/api/v1/attendance/me?start&end", "My attendance history (range)", "JWT", "400 parse"],
    ["POST", "/api/v1/attendance/check-in", "Clock in (notes optional)", "JWT", "409 already checked in"],
    ["POST", "/api/v1/attendance/check-out", "Clock out, compute total hours", "JWT", "400/409 state"],
    ["POST", "/api/v1/attendance/break/start", "Start break window", "JWT", "400/409 state"],
    ["POST", "/api/v1/attendance/break/end", "End break window", "JWT", "400/409 state"],
    ["POST", "/api/v1/attendance/corrections", "Request attendance correction", "JWT", "400/404"],
    ["GET", "/api/v1/attendance/corrections", "Pending corrections (approver)", "Perm attendance:approve", "403"],
    ["POST", "/api/v1/attendance/corrections/{id}/approve", "Approve/amend correction", "Perm attendance:approve", "403/404"],
    ["POST", "/api/v1/attendance/corrections/{id}/reject", "Reject correction", "Perm attendance:approve", "403/404"],
    ["GET", "/api/v1/attendance/report/{profileId}?year&month", "Monthly attendance report", "JWT", "404"],
    ["GET/POST", "/api/v1/attendance/holidays", "List (by year) / create holiday", "JWT / Perm attendance:edit", "403"],
    ["PUT/DELETE", "/api/v1/attendance/holidays/{id}", "Update / delete holiday", "Perm attendance:edit", "403/404"],
    ["GET", "/api/v1/attendance/policies", "Active attendance policies", "JWT", "-"],
    ["POST/PUT/DELETE", "/api/v1/attendance/policies(/{id})", "Policy administration", "Perm attendance:edit", "403/404"],
    ["GET", "/api/v1/attendance/team?start&end", "Team attendance (non-employees)", "JWT (role check)", "403"],
  ],
  size: 7.2
});

table({
  head: ["Method", "Endpoint", "Purpose", "Auth", "Errors"],
  widths: [17, 52, 63, 22, 22],
  body: [
    ["GET", "/api/v1/employees?search&department&page&size", "Paged employee search", "JWT", "-"],
    ["GET", "/api/v1/employees/active | /me | /{id}", "Active list / own profile / by id", "JWT", "404"],
    ["POST", "/api/v1/employees", "Create employee", "Perm role_management:assign", "409 duplicate"],
    ["PATCH", "/api/v1/employees/{id}", "Update / onboard / offboard", "Perm role_management:edit", "404"],
    ["GET", "/api/v1/leaves/types | /balances?year", "Leave catalogue and my balances", "JWT", "-"],
    ["GET", "/api/v1/leaves/requests", "Role-aware leave list", "JWT", "-"],
    ["POST", "/api/v1/leaves/requests", "Submit leave request", "Perm leave_management:create", "400/409 balance/overlap"],
    ["POST", "/api/v1/leaves/requests/{id}/decision", "Approve / reject (reason if rejected)", "Perm leave_management:approve", "400/403"],
    ["DELETE", "/api/v1/leaves/requests/{id}", "Cancel pending request", "JWT (owner)", "404/409"],
    ["POST/PUT/DELETE", "/api/v1/leaves/types(/{id})", "Leave type administration", "Perm role_management:create/edit/delete", "403/409"],
    ["POST", "/api/v1/leaves/balances", "Ensure/update a leave balance", "JWT (HR/admin)", "404"],
    ["GET", "/api/v1/timesheets", "Role-aware timesheet list", "JWT", "-"],
    ["POST", "/api/v1/timesheets/current", "Get-or-create week sheet", "JWT", "-"],
    ["POST/DELETE", "/api/v1/timesheets/{id}/entries(/{entryId})", "Add/update/remove entry", "JWT (owner, draft/rejected)", "403/404/409"],
    ["POST", "/api/v1/timesheets/{id}/submit", "Submit for approval", "JWT (owner)", "409 state"],
    ["POST", "/api/v1/timesheets/{id}/decision", "Approve / reject with reason", "Perm timesheets:approve", "400/403/409"],
    ["POST", "/api/v1/timesheets/bulk-approve", "Bulk approve submitted sheets", "Perm timesheets:approve", "403"],
    ["GET", "/api/v1/reports/performance?department", "Weekly performance scorecards", "Perm attendance:view", "403"],
    ["GET", "/api/v1/reports/performance/{profileId}/detail", "Trends + workload drill-down", "Perm attendance:view", "403/404"],
    ["GET", "/api/v1/roles | /permissions", "List roles / permissions", "JWT", "-"],
    ["POST/PUT/DELETE", "/api/v1/roles(/{id})", "Role administration", "Perm role_management:create/edit/delete", "403/409/404"],
    ["GET/POST/DELETE", "/api/v1/role-permissions(/{roleId}/{permissionId})", "View / grant / revoke role permissions", "Perm role_management:edit", "403/404"],
    ["GET/POST/DELETE", "/api/v1/user-roles/{userId}(/{roleId})", "Assign / list / remove user roles", "Perm role_management:assign", "403/404/409"],
    ["GET", "/api/v1/users-with-roles", "All users with assignments", "Perm role_management:view", "403"],
    ["GET", "/api/v1/me/permissions", "My effective permissions", "JWT", "-"],
    ["GET", "/api/v1/notifications | /unread-count", "List (top 50) + unread count", "JWT", "-"],
    ["POST", "/api/v1/notifications/{id}/read | /read-all", "Mark read / mark all read", "JWT", "-"],
    ["GET", "/api/v1/admin/audit-logs?page&size", "Paged audit trail", "Perm role_management:view", "403"],
    ["GET", "/actuator/health | /actuator/prometheus", "Health + metrics scrape", "Public (intentional)", "-"],
    ["WS", "/ws/notifications?token=JWT", "Live notification push", "JWT (query param)", "-"],
  ],
  size: 7.2,
  caption: "Consolidated REST surface (56 method/route groups across 8 controllers plus WebSocket). OAuth2 login endpoints (/oauth2/authorization/{google|github} and /login/oauth2/code/*) are provided by Spring Security."
});

/* ================= SECTION 12 ================= */
h1(12, "Database Architecture");
p("PostgreSQL 16 with a Flyway-managed schema (V1__init.sql creates 22 tables, V2__seed.sql seeds 20 permissions, 4 system " +
  "roles with permission grants, 4 leave types and a default attendance policy). Hibernate runs with ddl-auto: validate, " +
  "so any drift between entities and migrations fails startup. The migration header notes the schema was converted from " +
  "the original Supabase migrations, which remain under supabase/migrations.");
(function er(){
  const x = ML, W = CW;
  const bw = (W - 15) / 4, bh = 12.5, gx = 5, gy = 8;
  const E = [
    ["users", "PK id - email UQ", "provider, status, lockout"],
    ["profiles", "PK id - FK user_id UQ", "manager_id (self FK)"],
    ["user_roles", "legacy role enum", "UQ(user_id, role)"],
    ["roles", "PK id - name UQ", "is_system_role"],
    ["permissions", "UQ(module, action)", "20 seeded"],
    ["role_permissions", "UQ(role, perm)", "grants"],
    ["user_role_assignments", "UQ(user_id, role)", "assigned_by/at"],
    ["refresh_tokens", "token_hash idx", "revoked, expires_at"],
    ["attendance", "UQ(profile_id, date)", "status CHECK, hours"],
    ["leave_requests", "FK profile, type", "status, approved_by"],
    ["timesheets", "UQ(profile, week_start)", "status"],
    ["timesheet_entries", "FK timesheet_id", "hours, project"],
    ["leave_balances", "FK profile+type+year", "total/used/remaining"],
    ["notifications", "FK user_id idx", "type, status"],
    ["outbox_events", "processed_at idx", "retries, payload"],
    ["audit_logs", "entity idx, created_at", "old/new JSONB"],
  ];
  const pos = [];
  E.forEach((e, i) => {
    const cx = x + (i % 4) * (bw + gx), cy = y + Math.floor(i / 4) * (bh + gy);
    dBox(cx, cy, bw, bh, [e[0], e[1], e[2]], { size: 6.6, fill: WHITE });
    pos.push([cx, cy]);
  });
  y += 4 * (bh + gy) + 1;
  dArrow(pos[1][0] + bw / 2, pos[1][1], pos[0][0] + bw / 2, pos[0][1] + bh, { label: "1:1" });
  dArrow(pos[8][0] + bw / 2, pos[8][1], pos[1][0] + bw / 2, pos[1][1] + bh, { label: "1:N" });
  dArrow(pos[10][0] + bw / 2, pos[10][1], pos[1][0] + bw, pos[1][1] + bh, { label: "1:N" });
  dArrow(pos[11][0] + bw / 2, pos[11][1], pos[10][0] + bw / 2, pos[10][1] + bh, { label: "1:N" });
  dArrow(pos[3][0] + bw, pos[3][1] + bh / 2, pos[4][0], pos[4][1] + bh / 2, { label: "M:N", dash: true });
  dArrow(pos[7][0] + bw / 2, pos[7][1], pos[0][0] + bw, pos[0][1] + bh, { label: "1:N", dash: true });
  figCaption("Simplified entity-relationship view (16 of 22 tables; attendance_policies, holidays, leave_types, one_time_tokens and the three audit tables are detailed below).");
})();
h2("12.1 Table Inventory (V1__init.sql)");
table({
  head: ["Table", "Purpose", "Key Columns / Constraints"],
  widths: [34, 56, 86],
  body: [
    ["users", "Identity (replaces Supabase auth.users)", "id PK, email UQ, password (bcrypt), provider CHECK(LOCAL/GOOGLE/GITHUB), status CHECK, failed_login_attempts, locked_until"],
    ["profiles", "Employee record 1:1 with user", "user_id UQ FK->users CASCADE, employee_id UQ, manager_id self-FK, department, hire_date, is_active"],
    ["user_roles", "Legacy quick roles (mirrors Supabase)", "role CHECK(SUPER_ADMIN/HR/MANAGER/EMPLOYEE), UQ(user_id, role)"],
    ["roles / permissions", "Dynamic RBAC catalogue", "roles.name UQ; permissions UQ(module, action); 4 roles + 20 permissions seeded"],
    ["role_permissions / user_role_assignments", "Grants and assignments", "UQ(role_id, permission_id); UQ(user_id, role_id), assigned_by, assigned_at"],
    ["audit_logs (+ rbac, attendance variants)", "Append-only trails", "JSONB old/new values; indexes on created_at and entity"],
    ["attendance", "Daily attendance", "UQ(profile_id, date), status CHECK(present/absent/late/half_day), total_hours DECIMAL(4,2), corrected_by FK"],
    ["attendance_policies / holidays", "Policy configuration", "09:00-17:00 defaults, grace 15 min, overtime threshold; holidays.date idx, is_recurring, year"],
    ["leave_types / leave_balances / leave_requests", "Leave domain", "max_days_per_year, carry_forward; balances UQ per profile+type+year; requests status CHECK, approved_by / rejection_reason"],
    ["timesheets / timesheet_entries", "Weekly timesheets", "UQ(profile_id, week_start_date); entries FK timesheet_id, hours, project_name, is_billable"],
    ["refresh_tokens / one_time_tokens", "Token storage", "SHA-256 token_hash idx, expires_at idx, revoked/revoked_at; purpose, used_at"],
    ["notifications", "In-app + email records", "user_id idx, (user_id, status) idx, type, status, read_at"],
    ["outbox_events", "Transactional outbox", "processed_at idx, retries, payload JSONB"],
  ],
  size: 7.4
});
h2("12.2 Integrity, Indexing and Security Policies");
bullets([
  { t: "Constraints: ", d: "CHECK constraints encode enums (attendance.status, users.provider/status, user_roles.role); UNIQUE keys prevent duplicate emails, duplicate daily attendance and duplicate role grants/assignments; optimistic-lock version columns on hot entities." },
  { t: "Indexes: ", d: "idx_attendance_profile_date, idx_attendance_date, idx_leave_balances_profile, idx_leave_requests_(profile,status), idx_timesheet_entries_(timesheet,date), idx_refresh_tokens_(user_id,expires_at), idx_outbox_unprocessed, audit created_at/entity indexes." },
  { t: "Security policy shift: ", d: "the original Supabase row-level security was replaced by application-layer RBAC (RbacService.requirePermission at every privileged endpoint). No RLS policies exist in the Flyway schema." },
  { t: "Migration discipline: ", d: "ddl-auto: validate guarantees schema/entity alignment at startup; migrations are versioned (V1 schema, V2 seed)." },
]);

/* ================= SECTION 13 ================= */
h1(13, "Authentication and Authorization");
(function authFlow(){
  const x = ML, W = CW;
  const bw = (W - 20) / 5, bh = 13;
  layerLabel("Authentication flow"); y += 3.4;
  dBox(x, y, bw, bh, ["User", "login form / OAuth2"], { fill: WHITE });
  dBox(x + (bw + 5), y, bw, bh, ["POST /auth/login", "or /oauth2/authorization/*"], { fill: WHITE, size: 7.2 });
  dBox(x + 2 * (bw + 5), y, bw, bh, ["AuthService", "bcrypt, lockout, verified check"], { size: 7.0 });
  dBox(x + 3 * (bw + 5), y, bw, bh, ["Token pair", "access 15 min + refresh 7 d (hashed)"], { size: 6.9 });
  dBox(x + 4 * (bw + 5), y, bw, bh, ["SPA", "stores tokens, calls /auth/me"], { fill: WHITE, size: 7.2 });
  for (let i = 0; i < 4; i++) dArrow(x + (i + 1) * (bw + 5) - 5, y + bh / 2, x + (i + 1) * (bw + 5), y + bh / 2);
  y += bh + 2;
  const bw2 = (W - 15) / 4;
  dBox(x, y, bw2, 12, ["Every request", "Authorization: Bearer <access>"], { fill: LIGHT2, size: 7.2 });
  dBox(x + (bw2 + 5), y, bw2, 12, ["JwtAuthenticationFilter", "verify signature/expiry"], { fill: WHITE, size: 7.2 });
  dBox(x + 2 * (bw2 + 5), y, bw2, 12, ["SecurityContext", "UserPrincipal + ROLE_* authorities"], { size: 7.0 });
  dBox(x + 3 * (bw2 + 5), y, bw2, 12, ["RBAC check", "requirePermission(module, action)"], { fill: WHITE, stroke: TEAL, size: 7.1 });
  for (let i = 0; i < 3; i++) dArrow(x + (i + 1) * (bw2 + 5) - 5, y + 6, x + (i + 1) * (bw2 + 5), y + 6);
  y += 12;
  figCaption("Authentication (top) and per-request authorization (bottom) flows.");
})();
h2("13.1 Mechanisms in Detail");
table({
  head: ["Aspect", "Implementation", "Evidence"],
  widths: [28, 90, 58],
  body: [
    ["Registration", "Email+password with case-insensitive uniqueness; creates user + profile; assigns default Employee role in both role systems; emits verification email event.", "AuthService.signUp; RbacService.assignDefaultRole"],
    ["Password handling", "BCrypt with strength 12; Supabase bcrypt hashes are portable into users.password per setup guide.", "SecurityConfig.passwordEncoder"],
    ["Email verification", "32-byte SecureRandom token, SHA-256 hashed at rest, purpose + expiry; sign-in rejected with EMAIL_NOT_VERIFIED until verified.", "one_time_tokens; AuthService.signIn"],
    ["Session model", "Stateless JWT; no server sessions (SessionCreationPolicy.STATELESS).", "SecurityConfig"],
    ["Refresh rotation", "Refresh token stored as SHA-256 hash; refresh exchanges old for new (old revoked); logout and password reset revoke all sessions.", "AuthService.refresh / revokeAllSessions"],
    ["Account protection", "5 failed attempts -> 15-minute lock (users.locked_until); Redis rate limit 20/min per IP on auth endpoints.", "AuthService constants; RateLimitFilter"],
    ["OAuth2", "Google (openid, profile, email) and GitHub (read:user, user:email) with server-side callbacks; custom user service provisions locals; success handler issues JWT pair.", "SecurityConfig; application.yml oauth2 block"],
    ["Authorization", "Effective role from legacy enum or dynamic assignments (super_admin > hr > manager > employee); module:action permissions checked per endpoint; 403 JSON via accessDeniedHandler.", "RbacService; SecurityConfig handlers"],
    ["Frontend protection", "Routes rendered only with a valid session (useAuth); role-based navigation via useRBAC; 401 clears tokens and returns to login.", "useAuth.ts, useRBAC.ts"],
    ["Logout", "POST /auth/logout revokes the refresh token; client clears localStorage.", "AuthService.logout; useAuth.signOut"],
  ],
  size: 7.5
});

/* ================= SECTION 14 ================= */
h1(14, "Complete Application Workflows");
h2("14.1 Leave Request Workflow");
(function wf(){
  const x = ML, W = CW;
  const bw = (W - 25) / 6, bh = 13;
  const steps = [
    ["Employee", "submits leave", "(dates, type, reason)"],
    ["LeaveService", "validates: type active,", "no overlap, balance"],
    ["POST /leaves/requests", "persist pending", "request"],
    ["Outbox event", "LEAVE_SUBMITTED", "same transaction"],
    ["Kafka consumer", "in-app + email", "to approver(s)"],
    ["Manager decision", "approve / reject", "(reason required)"],
  ];
  steps.forEach((s, i) => {
    dBox(x + i * (bw + 5), y, bw, bh, s, { size: 6.9, fill: i === 2 ? WHITE : LIGHT });
    if (i < 5) dArrow(x + (i + 1) * (bw + 5) - 5, y + bh / 2, x + (i + 1) * (bw + 5), y + bh / 2);
  });
  y += bh;
  figCaption("Leave request workflow - server-validated request, event-driven notifications, decision loop.");
})();
h2("14.2 Timesheet Workflow (state machine)");
(function wf2(){
  const x = ML, W = CW;
  const bw = (W - 30) / 7, bh = 12;
  const st = [["Draft", "(auto-created Monday)"], ["Add/Edit entries", "project, hours, billable"], ["Submitted", "(recalculated)"], ["Approved", "(stamps approver)"], ["Rejected", "(reason saved)"], ["Re-edit", "(back to editable)"], ["Bulk approve", "(approvers, many at once)"]];
  st.forEach((s, i) => {
    dBox(x + i * (bw + 5), y, bw, bh, [s[0], s[1]], { size: 6.7, fill: i === 3 ? WHITE : (i === 4 ? LIGHT2 : LIGHT) });
    if (i < 6) dArrow(x + (i + 1) * (bw + 5) - 5, y + bh / 2, x + (i + 1) * (bw + 5), y + bh / 2);
  });
  y += bh;
  dArrow(x + 4 * (bw + 5) + bw / 2, y + 0.5, x + 5 * (bw + 5) + bw / 2, y + 0.5, { dash: true, label: "" });
  figCaption("Timesheet lifecycle - only draft/rejected sheets accept entries; every change recalculates totals.");
})();
h2("14.3 Attendance Daily Cycle");
bullets([
  { t: "Clock in -> ", d: "guard against duplicate day record; status = late if now > policy start + grace; audit row written." },
  { t: "Break start/end -> ", d: "windows recorded on the same daily record." },
  { t: "Clock out -> ", d: "total hours computed (checkout minus check-in minus break); record becomes final for the day." },
  { t: "Correction -> ", d: "employee requests with reason; approver amends times/status; audit snapshot + ATTENDANCE_CORRECTED event notify the employee." },
]);
h2("14.4 Signup-to-Working-Account Flow");
bullets([
  "Sign up -> verification email (async via outbox/Kafka) -> click token link (VerifyEmail page) -> account verified.",
  "Sign in -> tokens stored -> /auth/me returns profile, effective role, permissions -> role-aware dashboard.",
  "First administrator is promoted manually via SQL (documented in README-SETUP.md): UPDATE user_roles SET role = 'SUPER_ADMIN' ... plus an RBAC assignment row.",
]);

/* ================= SECTION 15 ================= */
h1(15, "Complete Data Flow");
(function dfd(){
  const x = ML, W = CW;
  const bw = (W - 20) / 5, bh = 14;
  layerLabel("Synchronous write path"); y += 3.4;
  dBox(x, y, bw, bh, ["Client", "JSON request + Bearer JWT"], { fill: WHITE });
  dBox(x + (bw + 5), y, bw, bh, ["Validation", "@Valid DTOs; business guards"], { fill: WHITE });
  dBox(x + 2 * (bw + 5), y, bw, bh, ["Service", "RBAC check + rules + audit"], { fill: WHITE });
  dBox(x + 3 * (bw + 5), y, bw, bh, ["JPA / PostgreSQL", "entities + Flyway schema"], { fill: WHITE });
  dBox(x + 3 * (bw + 5), y + bh + 4, bw, 10, ["HTTP JSON response", "or ErrorResponse {code,message}"], { size: 7.1, fill: LIGHT2, stroke: TEAL });
  dArrow(x + (bw + 5) - 5, y + bh / 2, x + (bw + 5), y + bh / 2);
  dArrow(x + 2 * (bw + 5) - 5, y + bh / 2, x + 2 * (bw + 5), y + bh / 2);
  dArrow(x + 3 * (bw + 5) - 5, y + bh / 2, x + 3 * (bw + 5), y + bh / 2);
  dArrow(x + 4 * (bw + 5) - 5, y + bh / 2, x + 4 * (bw + 5), y + bh / 2);
  dArrow(x + 3.5 * bw + 15, y + bh, x + 3.5 * bw + 15, y + bh + 4, { label: "200/4xx" });
  y += bh + 4;
  const bw2 = (W - 25) / 6;
  layerLabel("Asynchronous notification path"); y += 3.4;
  const steps = [["Service (same txn)", "outbox_events row"], ["Dispatcher (@Scheduled)", "publish up to 100"], ["Kafka hrms.events", "key = aggregateId"], ["HrmsEventConsumer", "Redis dedup 7 d"], ["In-app + WebSocket", "persisted + push"], ["EmailService", "SMTP x3 retries"]];
  steps.forEach((s, i) => {
    dBox(x + i * (bw2 + 5), y, bw2, 14, s, { size: 6.8, fill: i === 0 ? LIGHT2 : LIGHT });
    if (i < 5) dArrow(x + (i + 1) * (bw2 + 5) - 5, y + 7, x + (i + 1) * (bw2 + 5), y + 7);
  });
  y += bh;
  p("Error paths: consumer failures increment hrms.events.failed and route to hrms.events.dlt after retries; email failures retry 3x then log permanently; outbox dispatch failures increment retries and are retried on the next poll; Redis unavailability degrades (permission lookups fall back to the database; rate limiting skips when Redis errors).", { size: 8.8 });
  figCaption("Data flow - synchronous request path (top) and asynchronous event pipeline (bottom).");
})();

/* ================= SECTION 16 ================= */
h1(16, "Project Folder Structure");
p("Root: c:\\Users\\Mukesh\\Desktop\\HRMS\\slate-timesync", { size: 9 });
code(
`slate-timesync/
  README.md                 Lovable boilerplate (outdated - see README-SETUP.md)
  README-SETUP.md           Accurate setup/verification guide for this stack
  .env.example              All environment variables (names + safe examples)
  .gitignore                node_modules, dist, .env, target, logs...
  backend/                  Spring Boot 3.3 modular monolith (Java 21, Maven)
    Dockerfile              multi-stage: Maven build -> JRE 21 alpine runtime
    src/main/java/com/hrms/
      HrmsApplication.java  entry point (@SpringBootApplication)
      auth/                 AuthController, AuthService, OAuth2 handlers, DTOs, repos
      security/             JwtTokenProvider, JwtAuthenticationFilter, RateLimitFilter,
                            SecurityUtils, UserPrincipal
      config/               SecurityConfig, AppProperties, RedisConfig, KafkaConfig,
                            KafkaErrorConfig, OpenApiConfig, CorrelationIdFilter
      employee/             EmployeeController/Service, Profile, ProfileRepository
      attendance/           controller, service, entities (attendance, policies,
                            holidays, audit), repositories, AttendanceReminderJob
      leave/                LeaveController/Service, LeaveType/Balance/Request, repos
      timesheet/            TimesheetController/Service, Timesheet/Entry, repos
      reports/              ReportController (performance metrics)
      role/                 RbacController/Service, RoleEntity, PermissionEntity, grants
      notification/         NotificationController/Service, WebSocket handler + config
      email/                EmailService (SMTP + retry), EmailTemplates
      events/               OutboxService/Event/Repository, HrmsEvent,
                            HrmsEventConsumer, OutboxDispatcher
      audit/                AuditController, AuditLog, AuditRecorder, AuditRepository
      common/exception/     ApiException, ErrorResponse, GlobalExceptionHandler
    src/main/resources/
      application.yml       datasource, redis, kafka, mail, oauth2, hrms.* props
      logback-spring.xml    console + rolling file (14 days), MDC pattern
      db/migration/         V1__init.sql (22 tables), V2__seed.sql (seed data)
    src/test/java/com/hrms/ AuthServiceTest, JwtTokenProviderTest, FlywayMigrationTest
  frontend/                 React 18 + TypeScript + Vite SPA
    Dockerfile, nginx.conf  nginx:1.27 hosting + /api,/ws reverse proxy
    vite.config.ts          proxies /api,/oauth2,/login,/ws to :8081 in dev
    src/
      App.tsx               providers + 14 routes
      lib/api.ts            fetch client: tokens, refresh rotation, ApiError, WebSocket
      hooks/                useAuth, useRBAC, useAttendance, useLeave, useTimesheets,
                            useNotifications, usePerformance, use-toast
      pages/                Dashboard, Attendance, Timesheets, Leave, RoleManagement,
                            TeamManagement, Performance, Login, Index, password flows...
      components/           feature components + ui/ (48 shadcn components)
  docker/
    docker-compose.yml      postgres, redis, kafka, kafka-ui, backend, frontend,
                            prometheus, grafana
    prometheus/prometheus.yml, grafana/ (provisioning + hrms.json dashboard)
  supabase/                 legacy (pre-migration) schema history + 5 Deno edge functions
    migrations/             original 8 SQL migrations (schema history)
    functions/              check-* / send-* email reminder functions`);

/* ================= SECTION 17 ================= */
h1(17, "Module and Implementation Details");
table({
  head: ["Module", "Responsibility", "Main Logic", "Depends On"],
  widths: [24, 40, 68, 44],
  body: [
    ["auth", "Account lifecycle and token issuance", "signUp/signIn/refresh/logout/verify/reset flows; lockout; OAuth provisioning; me() aggregation", "security, role (RBAC), events (outbox), employee (profile)"],
    ["security", "Request authentication + throttling", "JWT verify per request; Redis rate counters; client-IP resolution (X-Forwarded-For first hop)", "config (AppProperties), Redis"],
    ["attendance", "Daily attendance + corrections + policy admin", "grace-period lateness, total-hours computation, snapshot auditing, correction approval", "events, employee (profile)"],
    ["leave", "Leave governance", "4 validation guards, decisions, balance admin, role-aware visibility", "role, events"],
    ["timesheet", "Weekly timesheet cycles", "Monday anchoring, entry CRUD with state guard, recalculation, bulk approve", "role, events"],
    ["employee", "Directory management", "paged search, EMP-id generation, onboarding/offboarding, audit", "audit, role"],
    ["role (RBAC)", "Dynamic permissions", "effective-role precedence, Redis permission cache (5 min), admin operations with cache invalidation", "employee, Redis"],
    ["events", "Reliable async notifications", "transactional outbox + Kafka publish/consume + DLT + metrics", "email, notification, employee, leave, attendance"],
    ["notification", "In-app channel", "persist + WebSocket push + read state", "WebSocket handler"],
    ["email", "SMTP delivery", "3 attempts linear backoff; disabled-mode logging; Micrometer counters", "mail config, AppProperties"],
    ["audit", "Privileged-change trail", "append-only recorder + admin paged query", "role (permission gate)"],
    ["reports", "Performance analytics", "weekly scorecard + drill-down aggregations", "employee, attendance, timesheet, role"],
    ["common.exception", "Uniform error contract", "ApiException factories (badRequest, notFound, duplicate, forbidden, businessRule) -> handler", "Spring web"],
    ["config", "Cross-cutting wiring", "CORS allow-list, Redis, Kafka (JSON + DLT), OpenAPI, correlation filter", "AppProperties"],
  ],
  size: 7.4
});

/* ================= SECTION 18 ================= */
h1(18, "Validation and Error Handling");
h2("18.1 Backend");
bullets([
  { t: "Bean Validation: ", d: "@Valid on request bodies (AuthController, LeaveController, TimesheetController) with jakarta constraints (@NotNull, @NotBlank) on DTO records; violations handled by GlobalExceptionHandler." },
  { t: "Business validation: ", d: "explicit guards in services - duplicate email/employee ID, already-checked-in/out, overlapping leave, insufficient balance, invalid state transitions (timesheet status), missing rejection reasons." },
  { t: "Database validation: ", d: "CHECK + UNIQUE + FK constraints as the final backstop; Hibernate validate prevents schema drift at boot." },
  { t: "Error contract: ", d: "ErrorResponse {code, message} JSON; ApiException factories map intent to status (400, 403, 404, 409, business-rule failures); AccessDenied -> 403; JSON 401 entry point; include-stacktrace: never." },
  { t: "External service failures: ", d: "email retries 3x with 1s/2s/3s backoff then permanent-failure log + metric; Kafka consumer uses ErrorHandlingDeserializer and a DLT; outbox poller retries with a retry counter; Redis failures degrade gracefully (cache miss / rate-limit skip) with warnings." },
  { t: "Logging and correlation: ", d: "CorrelationIdFilter propagates X-Correlation-Id; JwtAuthenticationFilter adds userId to MDC; every line carries timestamp, level, app, correlationId, user; rolling file logs with 14-day history." },
]);
h2("18.2 Frontend");
bullets([
  "Single fetch wrapper converts all non-2xx responses into ApiError(status, code, message); 204 passthrough.",
  "On 401 with a stored refresh token, exactly one refresh attempt (single-flight promise) then a single retry.",
  "UI feedback via toast notifications on mutation success/failure; destructive variant for errors.",
  "WebSocket client auto-reconnects every 5 s and silently ignores malformed frames.",
]);

/* ================= SECTION 19 ================= */
h1(19, "Security Analysis");
h2("19.1 Implemented Security");
table({
  head: ["Control", "Implementation", "Assessment"],
  widths: [30, 92, 54],
  body: [
    ["Password storage", "BCrypt strength 12", "Strong"],
    ["Token storage", "Refresh + one-time tokens stored as SHA-256 hashes; refresh rotation; revocation on logout/reset", "Strong"],
    ["Brute-force defence", "5-attempt lockout (15 min) + Redis rate limiting (20/min auth, 300/min API)", "Strong"],
    ["Authorization", "RBAC module:action checks on every privileged endpoint; 403 JSON handler", "Strong"],
    ["Secrets management", "All credentials via environment variables; .env gitignored; .env.example has placeholders only", "Good"],
    ["CORS", "Explicit allow-list from CORS_ALLOWED_ORIGINS; credentials enabled", "Good"],
    ["Error exposure", "include-stacktrace: never; JSON 401/403 handlers; no user enumeration on forgot-password", "Good"],
    ["Injection", "JPA parameter binding throughout; no string-concatenated SQL identified", "Good"],
    ["XSS", "React escapes output by default; no dangerouslySetInnerHTML usage identified in reviewed sources", "Baseline good"],
    ["Auditability", "Append-only audit tables with actor + old/new snapshots; admin-only access", "Strong"],
    ["CSRF", "Disabled; justified by stateless bearer-token model (no cookies)", "Acceptable"],
    ["Mass assignment", "DTO records with explicit fields instead of direct entity binding", "Good"],
  ],
  size: 7.6
});
h2("19.2 Recommended Security Improvements");
table({
  head: ["Finding", "Risk", "Recommendation"],
  widths: [80, 20, 76],
  body: [
    ["JWT secret has a committed fallback in application.yml (change-me-in-production-...). If JWT_SECRET is unset in production, tokens are signed with a publicly-known key.", "High", "Fail startup when JWT_SECRET is missing or equals the default; add a startup assertion."],
    ["RateLimitFilter and AuthController trust the first X-Forwarded-For value, which a client can spoof; limits can be bypassed.", "High", "Trust proxy headers only from known proxies (ForwardedHeaderFilter / trusted-proxy config)."],
    ["JWT passed as a WebSocket query parameter may appear in access logs.", "Medium", "Use Sec-WebSocket-Protocol or a short-lived single-use ticket."],
    ["Tokens stored in localStorage are readable by any injected script.", "Medium", "Consider httpOnly Secure cookies for the refresh token; keep short-lived access in memory."],
    ["No security response headers (HSTS, CSP, X-Content-Type-Options, frame-ancestors) in nginx or Spring.", "Medium", "Add a headers block in nginx.conf and/or Spring Security headers DSL."],
    ["Swagger UI, /api-docs and /actuator/prometheus are public.", "Medium", "Restrict by network or authentication in production."],
    ["No dependency vulnerability scanning (OWASP dependency-check / npm audit in CI) - not identified in the current implementation.", "Medium", "Add dependency scanning to CI."],
    ["No JWT key-rotation mechanism; changing the secret invalidates all sessions (undocumented).", "Low", "Document rotation; consider kid-based rotation."],
    ["Rate limiter is per-IP only; authenticated abuse across many IPs is unthrottled.", "Low", "Add a per-user limit for authenticated traffic."],
  ],
  size: 7.5
});
p("Secret exposure check: the repository contains only placeholder secrets (.env.example, compose defaults). No real API " +
  "keys, passwords or tokens were found in the inspected files, and none are reproduced in this report.", { size: 8.9 });

/* ================= SECTION 20 ================= */
h1(20, "Testing");
h2("20.1 Test Inventory (actual files in backend/src/test)");
table({
  head: ["Test Class", "Type", "Tests", "What It Covers"],
  widths: [38, 30, 16, 90],
  body: [
    ["AuthServiceTest", "Unit (JUnit 5 + Mockito)", "7", "Duplicate signup rejection; signup creates user/profile + verification event; wrong password increments failure counter; lockout after 5 failures; unverified email rejected (EMAIL_NOT_VERIFIED); forgot-password does not enumerate users; refresh rotation revokes the old token"],
    ["JwtTokenProviderTest", "Unit", "2", "Token create/parse round-trip with roles; tampered and malformed tokens rejected"],
    ["FlywayMigrationTest", "Integration (@SpringBootTest + Testcontainers PostgreSQL 16)", "1", "Flyway migrations run against a real database; seed applied; effectiveRole falls back to employee; fresh database empty. Auto-skips without Docker (disabledWithoutDocker = true)"],
  ],
  size: 7.7
});
h2("20.2 Test Execution in the Authoring Environment");
p("The authoring environment has Node.js 24 / npm 11 installed but not Java 21, Maven, or Docker. Consequently mvn test " +
  "could not be executed, and the Testcontainers-based integration test would additionally require Docker. The 10 backend " +
  "tests are documented from source inspection; their pass/fail status is NOT claimed. This is an environment limitation, " +
  "not a project defect - the test tooling is correctly wired (spring-boot-starter-test, spring-security-test, " +
  "testcontainers junit-jupiter/postgresql, spring-boot-testcontainers, H2 for fast contexts).");
h2("20.3 Frontend Testing");
p("No test runner, test files or test scripts are configured in frontend/package.json (scripts: dev, build, build:dev, " +
  "lint, preview only). E2E or component tests are not identified in the current implementation. Frontend verification " +
  "therefore relied on production build and lint, reported in Section 21.");

/* ================= SECTION 21 ================= */
h1(21, "Verification and Quality Assurance");
p("The following checks were actually executed during report preparation on the real codebase. Anything not executable " +
  "in the authoring environment is explicitly marked.");
table({
  head: ["Verification", "Command / Method", "Result", "Status"],
  widths: [38, 56, 48, 34],
  body: [
    ["Frontend production build", "npm run build (vite v5.4.19)", "3822 modules transformed; built in 8.63 s; warning: main chunk 1,921 kB (>500 kB)", "PASS"],
    ["Frontend static analysis", "npm run lint (eslint 9)", "128 problems (100 errors, 28 warnings), e.g. require() import in tailwind.config.ts", "FAIL"],
    ["Backend unit/integration tests", "mvn test", "Not executable - Java 21/Maven absent in authoring environment", "Not executed"],
    ["Backend compile", "mvn package", "Not executable - Java 21/Maven absent", "Not executed"],
    ["Full stack startup", "docker compose -f docker/docker-compose.yml up", "Not executable - Docker absent", "Not executed"],
    ["Schema review", "Manual inspection of V1__init.sql + V2__seed.sql", "22 tables, FKs, CHECK/UNIQUE constraints, seed roles/permissions verified", "PASS"],
    ["Configuration consistency", "Cross-check .env.example vs application.yml vs compose", "All variable names align (DATABASE_*, JWT_*, MAIL_*, OAuth, CORS)", "PASS"],
    ["API surface review", "Mapping annotation scan of 8 controllers", "56 method/route groups documented (Section 11)", "PASS"],
    ["Secrets scan", "Inspection of committed files (.env.example, compose, configs)", "Placeholders only; no real credentials found", "PASS"],
    ["Dependency inventory", "package.json + pom.xml review", "Documented in Section 6 / Appendix", "PASS"],
  ],
  size: 7.5
});

/* ================= SECTION 22 ================= */
h1(22, "Performance Analysis");
p("No load testing or runtime benchmarking was possible in the authoring environment (no runtime), so measured numbers " +
  "are limited to what was actually observed; the rest is inspection-based analysis.");
table({
  head: ["Aspect", "Observation / Evidence", "Basis"],
  widths: [38, 94, 44],
  body: [
    ["Frontend build performance", "Production build: 3822 modules, 8.63 s total, 70 kB CSS, 1,921 kB main JS (570 kB gzip)", "Measured (npm run build)"],
    ["Bundle size risk", "Main JS chunk exceeds 500 kB; Vite recommends manualChunks / dynamic import(). No route-level code splitting configured.", "Measured + Vite warning"],
    ["Database access", "Hikari pool max 10; hot queries covered by composite indexes (attendance profile+date, timesheet entries by sheet/date, refresh tokens by expiry).", "Code inspection"],
    ["Cache behaviour", "Permissions cached 5 min per user in Redis; generic Spring cache TTL 300 s; RBAC lookups avoid a 3-table join per request.", "Code inspection"],
    ["Potential hot spots", "Team/hr listing paths use findAll().limit(200) style reads; HrmsEventConsumer emails all top-level profiles per TIMESHEET_SUBMITTED event; performance report aggregates per-profile in a loop.", "Code inspection"],
    ["Rate-limit overhead", "One Redis INCR (+EXPIRE on first hit) per API request; cheap but adds a Redis round-trip per call.", "Code inspection"],
    ["Async offloading", "Email and notification fan-out are fully off the request thread (outbox + Kafka), keeping write latency dominated by the DB transaction only.", "Code inspection"],
  ],
  size: 7.7
});

/* ================= SECTION 23 ================= */
h1(23, "Scalability");
table({
  head: ["Dimension", "Behaviour as Load Grows", "Bottlenecks / Remedies"],
  widths: [30, 76, 70],
  body: [
    ["Concurrent users", "Stateless API (JWT) allows adding backend replicas behind any load balancer; no sticky sessions needed for REST.", "WebSocket connections are per-instance; sticky routing or a Redis/Kafka-backed WS broker needed at scale."],
    ["API traffic", "Rate limiting shared via Redis works across replicas.", "Monolith scaling is coarse; modules are cleanly bounded for future extraction."],
    ["Data volume", "Indexes cover primary query paths; pagination on employees/audit.", "findAll().limit(200) paths (leave/timesheet listing for HR) will degrade; replace with paged queries. Outbox purge (7 days) exists; audit tables lack retention."],
    ["Event throughput", "Kafka partitions by aggregateId; consumer group scales horizontally.", "Single consumer group with default concurrency; raise concurrency per replica as volume grows."],
    ["Notification fan-out", "Per-event emails batched per consumer record; idempotent via Redis.", "Timesheet-submitted fan-out to all top-level profiles is O(approvers); target direct managers instead."],
    ["Database", "Single PostgreSQL node.", "Read replicas / managed Postgres when write load grows; pool (10) should scale with replicas."],
  ],
  size: 7.7
});

/* ================= SECTION 24 ================= */
h1(24, "Deployment and DevOps");
(function dep(){
  const x = ML, W = CW;
  const bw = (W - 35) / 8, bh = 13;
  layerLabel("docker compose - 8 services (docker/docker-compose.yml)"); y += 3.6;
  const names = [["postgres", "16-alpine", ":5432"], ["redis", "7-alpine", ":6379"], ["kafka", "bitnami 3.7", ":9092"], ["kafka-ui", "-", ":8085"],
                 ["backend", "Spring Boot", ":8081"], ["frontend", "nginx", ":8080->80"], ["prometheus", "-", ":9090"], ["grafana", "-", ":3001"]];
  names.forEach((n, i) => {
    dBox(x + i * (bw + 5), y, bw, bh, [n[0], n[1], n[2]], { size: 6.6, fill: i === 4 ? WHITE : LIGHT, stroke: i === 4 ? TEAL : NAVY });
  });
  y += bh;
  figCaption("Deployment topology - one command (docker compose -f docker/docker-compose.yml up -d --build) starts the full stack with healthchecks and depends_on ordering.");
})();
bullets([
  { t: "Build images: ", d: "backend multi-stage (maven:3.9-temurin-21 build -> eclipse-temurin:21-jre-alpine, MaxRAMPercentage=75); frontend multi-stage (node:20-alpine build -> nginx:1.27-alpine)." },
  { t: "Runtime wiring: ", d: "nginx proxies /api and /ws to backend:8081; backend depends_on postgres/redis (healthy) and kafka (started); kafka-ui at :8085 for topic inspection." },
  { t: "Observability deployment: ", d: "Prometheus scrapes /actuator/prometheus (docker/prometheus/prometheus.yml); Grafana auto-provisions the datasource and a pre-built HRMS dashboard (docker/grafana/dashboards/hrms.json)." },
  { t: "Environments: ", d: "single compose profile for local/self-hosted deployment; environment variables injected from .env (Appendix). Kubernetes or CI/CD manifests are not identified in the current implementation." },
  { t: "Legacy channel: ", d: "supabase/functions document the former Supabase deployment path (Deno edge functions), retained for history." },
]);

/* ================= SECTION 25 ================= */
h1(25, "Installation and Setup");
p("Verified against README-SETUP.md, .env.example and the Dockerfiles. Prerequisites: Java 21, Maven 3.9+ (or Docker), " +
  "Node 20/npm, Docker Desktop (for Postgres, Redis, Kafka, Prometheus, Grafana).");
h3("Option A - full stack in Docker");
code(`cp .env.example .env      # fill DATABASE_*, JWT_SECRET, MAIL_*, OAuth ids
docker compose -f docker/docker-compose.yml up -d --build
# Frontend   http://localhost:8080     Backend API  http://localhost:8081/api/v1
# Swagger    http://localhost:8081/swagger-ui.html
# Prometheus http://localhost:9090     Grafana      http://localhost:3001 (admin)
# Kafka UI   http://localhost:8085`);
h3("Option B - local development");
code(`docker compose -f docker/docker-compose.yml up -d postgres redis kafka
cd backend  && mvn spring-boot:run          # needs JDK 21
cd frontend && npm install && npm run dev   # vite proxies /api -> :8081`);
h3("First administrator (from README-SETUP.md)");
code(`UPDATE user_roles SET role = 'SUPER_ADMIN'
WHERE user_id = (SELECT user_id FROM profiles WHERE email = 'you@company.com');
INSERT INTO user_role_assignments (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'you@company.com' AND r.name = 'Super Admin';`);
h3("OAuth, email and production build");
bullets([
  "Google Cloud Console OAuth web client; redirect URI http://localhost:8081/login/oauth2/code/google.",
  "GitHub OAuth App callback http://localhost:8081/login/oauth2/code/github.",
  "SendGrid SMTP relay: host smtp.sendgrid.net, user 'apikey', password = SG key; MAIL_FROM must be a verified sender; set MAIL_ENABLED=false to log emails instead of sending during development.",
  "Production build: mvn -DskipTests package (backend jar) and npm run build (frontend dist served by nginx).",
]);

/* ================= SECTION 26 ================= */
h1(26, "Problems Found and Improvements");
p("No historical fix log exists in the repository, so every item below is a potential issue identified during this " +
  "analysis (Problem -> Cause -> Recommended Solution). None is claimed as already fixed.");
table({
  head: ["Problem", "Cause", "Recommended Solution"],
  widths: [62, 50, 64],
  body: [
    ["Outdated root README (Lovable boilerplate) contradicts the actual architecture.", "Project migrated from Lovable/Supabase to Spring Boot; README not rewritten.", "Replace README.md with README-SETUP.md content plus an architecture summary."],
    ["Default JWT secret fallback could silently secure production.", "application.yml provides a default expression for JWT_SECRET.", "Refuse to start when the secret is missing or equals the documented default."],
    ["ESLint gate fails (100 errors / 28 warnings).", "Generated/adapted code never passed a lint gate; e.g. require() import in tailwind.config.ts.", "Fix auto-fixable issues, tune rules, add lint to CI."],
    ["1.9 MB single JS chunk slows first load.", "No route-level code splitting; heavy libs (xlsx, jspdf, html2canvas, recharts) eagerly bundled.", "React.lazy per route and manualChunks for vendor libraries."],
    ["HR/leave/timesheet listing paths can load unbounded data.", "visibleRequests/visibleSheets use findAll().limit(200); team attendance lacks pagination.", "Keyset/page queries with filters and total counts."],
    ["Timesheet bulk approve swallows exceptions silently.", "bulkApprove catches and ignores per-id exceptions, returning only a count.", "Collect per-item results/errors and report them in the response."],
    ["Approver resolution heuristic can notify the wrong people.", "HrmsEventConsumer.isApprover treats top-level profiles (manager == null) as approvers.", "Resolve approvers from profile.manager or role assignments."],
    ["Dual role systems increase cognitive load.", "Legacy user_roles kept alongside dynamic RBAC for Supabase parity.", "Migrate fully to RBAC assignments; keep the legacy column read-only for one release, then drop."],
    ["Leave cancellation deletes the row (no trail).", "cancel() deletes the entity rather than marking it cancelled.", "Soft-delete with a CANCELLED status (consistent with the other state machines)."],
    ["Change-password does not revoke other sessions.", "changePassword only re-encodes the password.", "Call revokeAllSessions after a successful change."],
    ["Audit tables have no retention policy.", "Only outbox rows are purged (7 days).", "Scheduled archival/partitioning for audit tables."],
    ["frontend/.env committed (harmless: VITE_API_BASE_URL only) and .tsbuildinfo artifacts present.", "Generator defaults.", "Add .env and *.tsbuildinfo to .gitignore."],
  ],
  size: 7.4
});

/* ================= SECTION 27 ================= */
h1(27, "Current Strengths");
bullets([
  { t: "Sound architectural choices: ", d: "transactional outbox, Redis idempotency, Kafka DLT, stateless JWT with rotation - patterns usually seen in production systems, applied consistently." },
  { t: "Clean code organisation: ", d: "package-by-feature with controller/service/repository separation; DTO records colocated with controllers; central exception model; Swagger documented endpoints." },
  { t: "Security depth: ", d: "hashed refresh tokens, rotation, lockout, rate limiting, permission checks at every boundary, no committed secrets." },
  { t: "Operational readiness: ", d: "one-command Docker environment, Prometheus/Grafana pre-provisioned, correlation-ID logging, health endpoints, rolling logs." },
  { t: "Schema discipline: ", d: "Flyway migrations with constraints as backstops and ddl-auto validate to catch drift." },
  { t: "Modern frontend engineering: ", d: "TypeScript throughout, TanStack Query, central API client with refresh single-flight, 48-component accessible UI kit." },
  { t: "Documentation trail: ", d: "README-SETUP.md is accurate and includes a 10-point manual verification checklist matching the actual features." },
]);

/* ================= SECTION 28 ================= */
h1(28, "Known Limitations");
table({
  head: ["Severity", "Limitation"],
  widths: [24, 152],
  body: [
    ["High", "No CI/CD pipeline and no automated lint/test gates; correctness currently relies on manual runs."],
    ["High", "JWT secret fallback permits a misconfigured production deployment (Section 19)."],
    ["High", "Frontend test coverage is zero; backend coverage limited to auth/token/migration areas (10 tests)."],
    ["Medium", "Some HR listing endpoints are unbounded; scale risk with data growth (Section 23)."],
    ["Medium", "Approver fan-out heuristic may email the wrong set of approvers."],
    ["Medium", "WebSocket auth uses a query-parameter token; tokens live in localStorage."],
    ["Medium", "Legacy dual role system maintained in parallel with RBAC."],
    ["Medium", "No code-splitting; 1.9 MB initial bundle."],
    ["Low", "README.md is outdated boilerplate; no LICENSE file; no CHANGELOG."],
    ["Low", "Supabase edge functions and migrations are dead weight for the current stack (kept as history)."],
  ],
  size: 7.7
});

/* ================= SECTION 29 ================= */
h1(29, "Recommended Improvements");
h3("High priority");
bullets([
  { t: "Add CI/CD (GitHub Actions): ", d: "mvn verify + dockerised Testcontainers tests, npm lint/build, dependency scanning. Why: the project has good tests but no automated gate; regressions are currently invisible." },
  { t: "Remove the JWT secret fallback: ", d: "fail fast on missing/default secret. Why: prevents an silently insecure production deployment." },
  { t: "Fix the lint baseline: ", d: "clear the 100 errors. Why: restores a meaningful static-analysis gate." },
  { t: "Harden proxy trust for rate limiting: ", d: "only honour X-Forwarded-For from known proxies. Why: the current limiter is bypassable." },
  { t: "Add frontend tests: ", d: "Vitest + React Testing Library for api.ts refresh logic, useAuth and one approval workflow; consider a Playwright smoke test. Why: auth and money-adjacent flows deserve regression cover." },
], { size: 9 });
h3("Medium priority");
bullets([
  "Paginate/filter all HR listing endpoints; add manager-scoped team attendance filtering.",
  "Introduce route-level code splitting and vendor chunking.",
  "Move approver resolution to explicit manager/role relationships.",
  "Adopt httpOnly cookie refresh tokens or at minimum move WebSocket auth off the query string.",
  "Add security headers (HSTS/CSP/X-Content-Type-Options) at nginx.",
  "Document JWT/secret rotation and add audit-table retention.",
], { size: 9 });
h3("Low priority");
bullets([
  "Replace README.md; add LICENSE, CHANGELOG and PR templates.",
  "Remove/retire Supabase artifacts once the migration is signed off.",
  "Add OpenTelemetry tracing; expose actuator/log correlation in Grafana panels.",
  "Add per-user rate limits and request body size limits for bulk endpoints.",
], { size: 9 });

/* ================= SECTION 30 ================= */
h1(30, "Future Enhancements");
bullets([
  { t: "Shift scheduling: ", d: "rosters, shift swaps and policy evaluation per shift - the policy table and grace-period logic extend naturally." },
  { t: "Payroll export: ", d: "approved timesheets + attendance already provide the inputs for period payroll extracts (client-side PDF/XLSX tooling exists)." },
  { t: "Native calendar integrations: ", d: "extend the CalendarImport concept to Google/Outlook sync for automatic timesheet drafts." },
  { t: "SSO expansion: ", d: "SAML/Okta/Azure AD via Spring Security, building on the existing OAuth2 configuration." },
  { t: "Two-factor authentication: ", d: "TOTP second factor reusing the one_time_tokens mechanism." },
  { t: "Notification preferences: ", d: "per-user channel and digest settings; email templates already centralised in EmailTemplates." },
  { t: "Document storage: ", d: "employee document management (object storage + metadata table)." },
  { t: "Advanced analytics: ", d: "trend dashboards over audit/attendance history; anomaly hints (chronic lateness, unused balances)." },
  { t: "Mobile/PWA client: ", d: "the API is fully token-based; a PWA shell would cover clock-in/out on shared devices." },
], { size: 9 });

/* ================= SECTION 31 ================= */
h1(31, "Maintenance and Support");
table({
  head: ["Area", "Guidance"],
  widths: [40, 136],
  body: [
    ["Environment variables", "Everything is configured via .env (names in Appendix). Rotate JWT_SECRET periodically; note that rotation invalidates all sessions."],
    ["Database maintenance", "pgdata is a named compose volume - back it up with pg_dump; Flyway migrations must stay additive; monitor audit table growth (no retention yet)."],
    ["Kafka / outbox", "hrms.events.dlt should be monitored; processed outbox rows are auto-purged after 7 days; kafka-ui (:8085) aids inspection."],
    ["Redis", "Used for rate limiting, permission cache (5 min TTL) and event dedup (7 d TTL); a flush clears caches safely (limits reset)."],
    ["Logs", "logs/hrms.log rolls daily, 14-day retention; correlationId ties a request across lines; email failures and DLT deliveries are logged at WARN/ERROR."],
    ["Email provider", "MAIL_ENABLED=false switches to log-only mode; monitor hrms.email.failed metric for delivery issues."],
    ["Upgrades", "Bump Spring Boot/Boot-managed deps via pom parent; frontend via npm; run browserslist update (data was 15 months stale at build time)."],
    ["Troubleshooting quick table", "429 responses -> rate limit reached; 401 loops -> clear localStorage and re-login (refresh may be revoked); emails missing -> MAIL_ENABLED/MAIL_FROM config; Kafka consumer idle -> check broker :9092 and topic hrms.events; schema error at boot -> entity/migration drift (ddl-auto validate)."],
  ],
  size: 7.7
});

/* ================= SECTION 32 ================= */
h1(32, "GitHub / Repository Readiness");
table({
  head: ["Item", "State", "Assessment"],
  widths: [40, 88, 48],
  body: [
    [".gitignore", "Covers node_modules, dist, .env, backend/target, backend/logs, IDE files", "Good"],
    [".env.example", "Complete variable list with safe placeholders", "Good"],
    ["README.md", "Lovable boilerplate; does not describe this stack", "Poor (use README-SETUP.md)"],
    ["README-SETUP.md", "Accurate, actionable, includes verification checklist", "Strong"],
    ["Dependency files", "package-lock.json committed (frontend); pom.xml pins parent/versions", "Good (reproducible)"],
    ["Docker artifacts", "Dockerfiles + compose + monitoring provisioning committed", "Strong"],
    ["Tests", "Backend tests present; frontend none; no CI wiring", "Partial"],
    ["Secrets", "No real credentials committed; compose defaults are dev-only placeholders", "Good"],
    ["Generated files in repo", "frontend/.env (benign), *.tsbuildinfo, node_modules present locally (ignored)", "Minor cleanup"],
    ["Documentation of legacy", "Supabase artifacts kept with explanatory comments", "Acceptable"],
  ],
  size: 7.7
});
p("Overall readiness: Medium-High. With a rewritten README, a LICENSE, and a CI workflow running lint + tests, the " +
  "repository would be well above average in reproducibility and onboarding clarity for a project of this size.", { size: 8.9 });

/* ================= SECTION 33 ================= */
h1(33, "Resume / Portfolio Value");
table({
  head: ["Skill", "Evidence in This Project"],
  widths: [50, 126],
  body: [
    ["Full-stack development", "End-to-end feature delivery: schema, API, business rules, UI (e.g. the timesheet lifecycle spans V1 schema -> TimesheetService -> Timesheets page)."],
    ["Java 21 / Spring Boot 3", "Modern Boot 3.3 idioms: records, validation, @ConfigurationProperties, security filter chains, scheduling."],
    ["REST API design", "56 route groups, versioned paths, DTO-per-controller, OpenAPI/Swagger documentation."],
    ["Relational database design", "22-table schema with FKs, CHECK/UNIQUE constraints, composite indexes, Flyway migrations + seed."],
    ["Authentication & authorization", "JWT with rotation, OAuth2 (Google/GitHub), BCrypt, lockout, dynamic RBAC engine."],
    ["Event-driven architecture", "Transactional outbox, Kafka pub/sub, DLT handling, Redis idempotency."],
    ["Caching & rate limiting", "Redis-backed permission cache with invalidation and sliding-window throttles."],
    ["Real-time systems", "WebSocket notification push with reconnection strategy."],
    ["React / TypeScript UI engineering", "Vite SPA, TanStack Query, hooks architecture, shadcn/ui design system, dark theme."],
    ["Containerization & DevOps", "Multi-stage Dockerfiles, 8-service compose with healthchecks, Prometheus/Grafana provisioning."],
    ["Observability", "Micrometer business metrics, correlation-ID logging, structured log patterns."],
    ["Testing", "Unit (Mockito) + integration (Testcontainers PostgreSQL) suites wired into Maven."],
    ["Security engineering", "Token hashing, no user enumeration, error hygiene, CORS allow-listing, secrets hygiene."],
  ],
  size: 7.6
});

/* ================= SECTION 34 ================= */
h1(34, "Final Technical Summary");
table({
  head: ["Dimension", "Summary"],
  widths: [40, 136],
  body: [
    ["Architecture", "Modular monolith (Spring Boot 3.3, Java 21) + React 18 SPA; event-driven notifications via transactional outbox + Kafka; Redis for cache/rate-limit/dedup; PostgreSQL 16 with Flyway."],
    ["Technology", "Java 21, Spring Boot 3.3.5, JJWT 0.12.6, Redis 7, Kafka 3.7, React 18.3, TypeScript 5.8, Vite 5.4, Tailwind 3.4, shadcn/ui, Docker, Nginx, Prometheus, Grafana."],
    ["Features", "Auth (local + OAuth2), attendance with corrections/policies/holidays, leave governance, weekly timesheets with bulk approvals, employee directory, RBAC, performance reports, in-app + email notifications, audit, exports."],
    ["API surface", "56 REST route groups across 8 controllers + WebSocket channel; Swagger UI included."],
    ["Data flow", "Validated request -> transactional service (RBAC + rules + audit) -> PostgreSQL; side-effects via outbox -> Kafka -> in-app/WS + email with retries and DLT."],
    ["Security", "Strong baseline: BCrypt 12, hashed rotating refresh tokens, lockout, rate limits, endpoint-level RBAC, no committed secrets. Hardening items identified in Section 19."],
    ["Testing", "10 backend tests (unit + Testcontainers integration) wired into Maven; no frontend tests; suite not executable in the authoring environment."],
    ["Verification (this report)", "Frontend build PASS (8.63 s, chunk warning); lint FAIL (128 problems); schema/config/API/secret reviews PASS; backend and stack runtime checks not executable in this environment - honestly marked."],
    ["Performance", "Indexes, Redis caching and async offloading in place; flagged hot spots: unbounded HR listings, approver fan-out, 1.9 MB bundle."],
    ["Deployment", "One-command 8-service Docker Compose stack with healthchecks and pre-provisioned monitoring; no CI/CD or Kubernetes yet."],
    ["Current status", "Functionally complete core; production-readiness gated by CI, hardening items and data-volume fixes prioritised in Sections 26/29."],
  ],
  size: 7.6
});

/* ================= SECTION 35 ================= */
h1(35, "Appendix");
h2("A. Environment Variables (names only - values never appear in this report)");
p("DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET, JWT_ACCESS_EXPIRATION, JWT_REFRESH_EXPIRATION, " +
  "REDIS_HOST, REDIS_PORT, KAFKA_BOOTSTRAP_SERVERS, MAIL_ENABLED, MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, " +
  "MAIL_FROM, MAIL_FROM_NAME, GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GOOGLE_REDIRECT_URI, GITHUB_CLIENT_ID, " +
  "GITHUB_CLIENT_SECRET, GITHUB_REDIRECT_URI, FRONTEND_URL, CORS_ALLOWED_ORIGINS, GRAFANA_PASSWORD (compose), " +
  "VITE_API_BASE_URL (frontend).", { size: 8.8 });
h2("B. Seeded Permission Matrix (V2__seed.sql)");
table({
  head: ["Module", "Actions", "Super Admin", "HR Admin", "Manager", "Employee"],
  widths: [36, 44, 24, 28, 24, 20],
  body: [
    ["attendance", "view, create, edit, delete, approve", "All", "view/create/edit/approve", "view/edit", "view/create"],
    ["timesheets", "view, create, edit, delete, approve", "All", "view/create/edit/approve", "view/approve", "view/create/edit"],
    ["leave_management", "view, create, edit, delete, approve", "All", "view/create/edit/approve", "view/approve", "view/create/edit"],
    ["role_management", "view, create, edit, delete, assign", "All", "-", "-", "-"],
  ],
  size: 7.7
});
h2("C. Seeded Reference Data");
bullets([
  "Roles: Super Admin, HR Admin, Manager, Employee (is_system_role = true).",
  "Leave types: Annual (25 d, carry-forward), Sick (15 d), Personal (5 d), Emergency (3 d).",
  "Attendance policy: 'Default Policy' 09:00-17:00, 60-min break, 15-min grace, 8 h overtime threshold.",
], { size: 8.9 });
h2("D. Glossary");
table({
  head: ["Term", "Definition"],
  widths: [44, 132],
  body: [
    ["Outbox pattern", "Persisting events in the same DB transaction as business data, then publishing asynchronously - no lost or phantom events."],
    ["DLT (dead-letter topic)", "Kafka topic receiving messages that repeatedly fail processing, for later inspection."],
    ["Refresh token rotation", "Each refresh issues a new refresh token and revokes the previous one, limiting replay of stolen tokens."],
    ["RBAC", "Role-based access control: users -> roles -> permissions (module:action) checked by services."],
    ["Effective role", "Merged role derived from the legacy user_roles enum and dynamic RBAC assignments."],
    ["MDC", "Mapped Diagnostic Context - per-request log fields (correlationId, userId) in Logback."],
    ["Hikari", "JDBC connection pool used by Spring Boot (max 10 connections here)."],
    ["Testcontainers", "JUnit library running throwaway Docker containers (real PostgreSQL) for integration tests."],
  ],
  size: 7.7
});
h2("E. Verification Information");
bullets([
  "Report generated: September 4, 2026, from the workspace copy of slate-timesync.",
  "Executed: npm run build (PASS), npm run lint (FAIL, 128 problems), environment checks (node v24.19.0, npm 11.17.0; Java/Maven/Docker absent).",
  "Static reviews: schema (V1/V2), configuration consistency, API mapping scan (81 mapping annotations / 56 route groups), secrets scan, dependency inventory.",
  "Not executed (marked throughout): mvn test / mvn package, docker compose startup, runtime API smoke tests.",
], { size: 8.9 });

/* ================= TOC fill + page chrome + save ================= */
(function tocPage(){
  doc.setPage(2);
  doc.setFillColor(...NAVY); doc.rect(0, 0, PW, 32, "F");
  font("bold", 18, WHITE); doc.text("Table of Contents", PW / 2, 20, { align: "center" });
  let ty = 44;
  toc.forEach((e) => {
    if (ty > PH - 22) return;
    font("bold", 9.6, NAVY);
    doc.text(String(e.n), ML + 2, ty);
    doc.text(e.t, ML + 12, ty);
    font("normal", 9.2, MUTED);
    doc.text(String(e.page), PW - MR, ty, { align: "right" });
    doc.setDrawColor(...CHIPBG); doc.setLineWidth(0.2);
    doc.line(ML + 12, ty + 1.6, PW - MR - 14, ty + 1.6);
    ty += 5.35;
  });
  font("italic", 8, MUTED);
  doc.text("Slate TimeSync - Technical Project Report  |  Version 1.0  |  September 2026", ML + 2, PH - 12);
})();

(function chrome(){
  const n = doc.getNumberOfPages();
  for (let i = 2; i <= n; i++){
    doc.setPage(i);
    doc.setDrawColor(...CHIPBG); doc.setLineWidth(0.3);
    doc.line(ML, 14, PW - MR, 14);
    font("normal", 7.2, MUTED);
    doc.text("Slate TimeSync | HRMS Technical Report", ML, 10.2);
    doc.text("slate-timesync", PW - MR, 10.2, { align: "right" });
    doc.setDrawColor(...BORDER); doc.setLineWidth(0.2);
    doc.line(ML, PH - 13, PW - MR, PH - 13);
    font("normal", 7.4, MUTED);
    doc.text("Version 1.0  |  September 2026", ML, PH - 9);
    font("bold", 8, NAVY);
    doc.text(`Page ${i} of ${n}`, PW - MR, PH - 9, { align: "right" });
  }
})();

const OUT = "C:/Users/Mukesh/Desktop/HRMS/Slate_TimeSync_Technical_Project_Report.pdf";
await doc.save(OUT);
console.log("Saved:", OUT, "pages:", doc.getNumberOfPages(), "sections:", toc.length, "figures:", figN, "tables:", tabN);





























