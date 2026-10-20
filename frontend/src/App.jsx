import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/layout/AppLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ProjectsPage from './pages/ProjectsPage';
import TasksPage from './pages/TasksPage';
import ProgressPage from './pages/ProgressPage';
import WBSPage from './pages/WBSPage';
import WorkingCalendarPage from './pages/WorkingCalendarPage';
import AssignedTasksPage from './pages/AssignedTasksPage';
import TaskAssignmentPage from './pages/TaskAssignmentPage';
import MilestonesPage from './pages/MilestonesPage';
import SiteDiaryPage from './pages/SiteDiaryPage';
import PaymentRequestsPage from './pages/PaymentRequestsPage';
import AcceptancePage from './pages/AcceptancePage';
import AcceptanceRecordsPage from './pages/AcceptanceRecordsPage';
import BudgetPage from './pages/BudgetPage';
import ActualCostPage from './pages/ActualCostPage';
import SystemHealthPage from './pages/SystemHealthPage';
import AdminApprovalsPage from './pages/AdminApprovalsPage';
import AdminUsersPage from './pages/AdminUsersPage';
import AdminRolesPage from './pages/AdminRolesPage';
import { Result, Button } from 'antd';
import { getAuthUser, isAuthenticated } from './utils/auth';

const ProtectedRoute = ({ children }) => {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

const RoleGuard = ({ allowedRoles, children }) => {
  const user = getAuthUser();
  const role = String(user?.roleName || '').toUpperCase();

  if (!allowedRoles || allowedRoles.length === 0 || allowedRoles.includes(role)) {
    return children;
  }

  return (
    <div style={{ padding: '60px 24px', textAlign: 'center' }}>
      <Result
        status="403"
        title="403"
        subTitle="Bạn không có quyền truy cập chức năng này"
        extra={
          <Button type="primary" onClick={() => window.location.href = '/dashboard'}>
            Về trang chủ
          </Button>
        }
      />
    </div>
  );
};

const AdminRoute = ({ children }) => (
  <RoleGuard allowedRoles={['ADMIN']}>{children}</RoleGuard>
);

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="projects" element={<ProjectsPage />} />
        
        {/* Cơ cấu công việc (WBS) */}
        <Route
          path="wbs"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER']}>
              <WBSPage />
            </RoleGuard>
          }
        />
        
        {/* Dự toán công trình */}
        <Route
          path="budget"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'STAFF']}>
              <BudgetPage />
            </RoleGuard>
          }
        />
        
        {/* Tiến độ thi công */}
        <Route
          path="progress"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'CUSTOMER', 'SITE_MANAGER', 'SITE_ENGINEER']}>
              <ProgressPage />
            </RoleGuard>
          }
        />
        <Route
          path="progress/milestones"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'CUSTOMER', 'SITE_MANAGER']}>
              <MilestonesPage />
            </RoleGuard>
          }
        />
        
        {/* Lịch làm việc */}
        <Route
          path="working-calendar"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER', 'WORKER']}>
              <WorkingCalendarPage />
            </RoleGuard>
          }
        />
        
        {/* Quản lý công việc */}
        <Route
          path="tasks"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER']}>
              <TasksPage />
            </RoleGuard>
          }
        />
        
        {/* Giao việc cho đội thi công */}
        <Route
          path="task-assignment"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER']}>
              <TaskAssignmentPage />
            </RoleGuard>
          }
        />
        <Route
          path="assignments"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER']}>
              <TaskAssignmentPage />
            </RoleGuard>
          }
        />
        
        {/* Công việc đã giao / Công việc được giao */}
        <Route
          path="assigned-tasks"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER', 'WORKER']}>
              <AssignedTasksPage />
            </RoleGuard>
          }
        />
        
        {/* Nhật ký công trường */}
        <Route
          path="site-diary"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER', 'SITE_ENGINEER', 'WORKER']}>
              <SiteDiaryPage />
            </RoleGuard>
          }
        />
        
        {/* Phiếu nghiệm thu */}
        <Route
          path="acceptance-records"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'CUSTOMER', 'SITE_MANAGER', 'SITE_ENGINEER']}>
              <AcceptanceRecordsPage />
            </RoleGuard>
          }
        />
        
        {/* Nghiệm thu khối lượng / Cập nhật khối lượng */}
        <Route
          path="acceptance"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'CUSTOMER', 'SITE_MANAGER', 'SITE_ENGINEER', 'WORKER', 'STAFF']}>
              <AcceptancePage />
            </RoleGuard>
          }
        />
        
        {/* Đề nghị thanh toán */}
        <Route
          path="payment-requests"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'CUSTOMER', 'STAFF']}>
              <PaymentRequestsPage />
            </RoleGuard>
          }
        />
        
        {/* Chi phí thực tế */}
        <Route
          path="actual-costs"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'PROJECT_MANAGER', 'CUSTOMER', 'SITE_MANAGER', 'STAFF']}>
              <ActualCostPage />
            </RoleGuard>
          }
        />
        
        {/* Quản trị hệ thống */}
        <Route path="admin/approvals" element={<AdminRoute><AdminApprovalsPage /></AdminRoute>} />
        <Route path="admin/users" element={<AdminRoute><AdminUsersPage /></AdminRoute>} />
        <Route path="admin/roles" element={<AdminRoute><AdminRolesPage /></AdminRoute>} />
        <Route path="admin/system-health" element={<AdminRoute><SystemHealthPage /></AdminRoute>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;