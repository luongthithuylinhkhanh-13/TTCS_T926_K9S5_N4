import React from 'react';
import { useNavigate } from 'react-router-dom';
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



const Sidebar = ({ collapsed }) => {
  const navigate = useNavigate();
  const user = getAuthUser();

  const handleLogout = () => {
    logoutUser();
    message.info('Đã đăng xuất tài khoản');
    navigate('/login');
  };

  const handleNonWbsClick = (menuName) => {
    message.info(`Mục "${menuName}" là chức năng ngoài phạm vi T-05/T-06`);
  };

  return (
    <aside
      className="app-sidebar"
      style={{ width: collapsed ? 80 : 250, background: 'linear-gradient(180deg, #0a3d62, #3c6382)' }}
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
          className="sidebar-item"
          onClick={() => handleNonWbsClick('Dashboard')}
          title={collapsed ? 'Dashboard' : ''}
        >
          <DashboardOutlined />
          {!collapsed && <span>Dashboard</span>}
        </div>

        {!collapsed && <div className="sidebar-section-label" style={{ marginTop: 12 }}>DỰ ÁN</div>}
        <div
          className="sidebar-item"
          onClick={() => handleNonWbsClick('Dự án')}
          title={collapsed ? 'Dự án' : ''}
        >
          <ProjectOutlined />
          {!collapsed && <span>Dự án</span>}
        </div>

        {/* ACTIVE ITEM */}
        <div className="sidebar-item active" title={collapsed ? 'Cơ cấu công việc' : ''}>
          <NodeIndexOutlined />
          {!collapsed && <span>Cơ cấu công việc</span>}
        </div>

        <div
          className="sidebar-item"
          onClick={() => handleNonWbsClick('Tiến độ')}
          title={collapsed ? 'Tiến độ' : ''}
        >
          <HistoryOutlined />
          {!collapsed && <span>Tiến độ</span>}
        </div>

        <div
          className="sidebar-item"
          onClick={() => handleNonWbsClick('Công việc')}
          title={collapsed ? 'Công việc' : ''}
        >
          <CheckSquareOutlined />
          {!collapsed && <span>Công việc</span>}
        </div>
      </div>

      {/* Bottom Gradient placeholder */}
      <div
        className="sidebar-bottom-image"
        style={{
          flexGrow: 1,
          display: 'flex',
          alignItems: 'flex-end',
          padding: '12px',
        }}
      >
        <div
          style={{
            width: '100%',
            height: '80px',
            background: 'linear-gradient(135deg, #3c6382, #0a3d62)',
            borderRadius: '4px',
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
