import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/layout/AppLayout';
import LoginPage from './pages/LoginPage';
import ProgressPage from './pages/ProgressPage';
import WBSPage from './pages/WBSPage';
import WorkingCalendarPage from './pages/WorkingCalendarPage';
import AssignedTasksPage from './pages/AssignedTasksPage';
import MilestonesPage from './pages/MilestonesPage';
import { isAuthenticated } from './utils/auth';

const ProtectedRoute = ({ children }) => {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
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
        <Route index element={<Navigate to="/wbs" replace />} />
        <Route path="wbs" element={<WBSPage />} />
        <Route path="progress" element={<ProgressPage />} />
        <Route path="working-calendar" element={<WorkingCalendarPage />} />
        <Route path="assigned-tasks" element={<AssignedTasksPage />} />
        <Route path="progress/milestones" element={<MilestonesPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;