import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  BuildOutlined,
  DashboardOutlined,
  ProjectOutlined,
  NodeIndexOutlined,
  HistoryOutlined,
  CheckSquareOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { message, Tooltip } from 'antd';
import { logoutUser, getAuthUser } from '../../utils/auth';
import { logout } from '../../services/wbsApi';



const Sidebar = ({ collapsed }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const user = getAuthUser();

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      message.warning(error.message || 'Không thể thu hồi phiên trên máy chủ');
    }
    logoutUser();
    message.info('Đã đăng xuất tài khoản');
    navigate('/login');
  };

  return (
    <aside
      className="app-sidebar"
      style={{ width: collapsed ? 80 : 280, background: 'linear-gradient(180deg, #0a3d62, #3c6382)' }}
    >
      {/* Brand Header */}
      <div className="sidebar-brand">
        <BuildOutlined className="sidebar-logo-icon" />
        {!collapsed && (
          <div className="sidebar-title-box">
            <span className="sidebar-title">CONSTRUCTFLOW</span>
            <span className="sidebar-subtitle">Quản lý thi công</span>
          </div>
        )}
      </div>

      {/* Navigation Menu */}
      <div className="sidebar-menu">
        {!collapsed && <div className="sidebar-section-label">TỔNG QUAN</div>}
        <div
          className={`sidebar-item ${location.pathname === '/dashboard' ? 'active' : ''}`}
          onClick={() => navigate('/dashboard')}
          title={collapsed ? 'Dashboard' : ''}
        >
          <DashboardOutlined />
          {!collapsed && <span>Dashboard</span>}
        </div>

        {!collapsed && <div className="sidebar-section-label" style={{ marginTop: 12 }}>DỰ ÁN</div>}
        <div
          className={`sidebar-item ${location.pathname === '/projects' ? 'active' : ''}`}
          onClick={() => navigate('/projects')}
          title={collapsed ? 'Dự án' : ''}
        >
          <ProjectOutlined />
          {!collapsed && <span>Dự án</span>}
        </div>

        <div
          className={`sidebar-item ${location.pathname === '/wbs' ? 'active' : ''}`}
          onClick={() => navigate('/wbs')}
          title={collapsed ? 'Cơ cấu công việc' : ''}
        >
          <NodeIndexOutlined />
          {!collapsed && <span>Cơ cấu công việc</span>}
        </div>

        <div
          className={`sidebar-item ${location.pathname === '/progress' ? 'active' : ''}`}
          onClick={() => navigate('/progress')}
          title={collapsed ? 'Tiến độ' : ''}
        >
          <HistoryOutlined />
          {!collapsed && <span>Tiến độ</span>}
        </div>

        <div
          className={`sidebar-item ${location.pathname === '/tasks' ? 'active' : ''}`}
          onClick={() => navigate('/tasks')}
          title={collapsed ? 'Công việc' : ''}
        >
          <CheckSquareOutlined />
          {!collapsed && <span>Công việc</span>}
        </div>
      </div>

      {/* Bottom Gradient placeholder */}
      <div className="sidebar-bottom-image">
        <div
          className="sidebar-bottom-image-box"
          style={{
            background: 'linear-gradient(135deg, #3c6382, #0a3d62)',
          }}
        />
      </div>

      {/* Bottom User Section */}
      <div className="sidebar-user">
        <div className="user-info-box">
          <div className="user-avatar-initials">QT</div>
          {!collapsed && (
            <div className="user-details">
              <span className="user-name">{user?.fullName || 'Quản trị viên'}</span>
              <span className="user-role">{user?.username || 'admin'}</span>
            </div>
          )}
        </div>
        <Tooltip title="Đăng xuất">
          <div className="logout-btn" onClick={handleLogout}>
            <LogoutOutlined />
          </div>
        </Tooltip>
      </div>
    </aside>
  );
};

export default Sidebar;
