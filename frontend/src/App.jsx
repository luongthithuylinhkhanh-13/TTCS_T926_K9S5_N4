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
import MilestonesPage from './pages/MilestonesPage';
import SiteDiaryPage from './pages/SiteDiaryPage';
import AcceptancePage from './pages/AcceptancePage';
import AcceptanceRecordsPage from './pages/AcceptanceRecordsPage';
import SystemHealthPage from './pages/SystemHealthPage';
import { getAuthUser, isAuthenticated } from './utils/auth';

const ProtectedRoute = ({ children }) => {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

const AdminRoute = ({ children }) => {
  if (getAuthUser()?.roleName?.toUpperCase() !== 'ADMIN') {
    return <Navigate to="/dashboard" replace />;
  }
  return children;
};

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
        <Route path="wbs" element={<WBSPage />} />
        <Route path="progress" element={<ProgressPage />} />
        <Route path="tasks" element={<TasksPage />} />
        <Route path="working-calendar" element={<WorkingCalendarPage />} />
        <Route path="assigned-tasks" element={<AssignedTasksPage />} />
        <Route path="progress/milestones" element={<MilestonesPage />} />
        <Route path="site-diary" element={<SiteDiaryPage />} />
        <Route path="acceptance" element={<AcceptancePage />} />
        <Route path="acceptance-records" element={<AcceptanceRecordsPage />} />
        <Route path="admin/system-health" element={<AdminRoute><SystemHealthPage /></AdminRoute>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;