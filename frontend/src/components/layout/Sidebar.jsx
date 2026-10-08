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
import { useLocale } from '../../utils/LocaleContext';



const Sidebar = ({ collapsed }) => {
  const { t } = useLocale();
  const navigate = useNavigate();
  const location = useLocation();
  const user = getAuthUser();
  const displayName = user?.fullName || user?.email || t('Tài khoản');
  const initials = displayName
    .split(/[\s.@_-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(part => part[0].toUpperCase())
    .join('');

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      message.warning(error.message || 'Không thể thu hồi phiên trên máy chủ');
    }
    logoutUser();
    message.info(t('Đã đăng xuất tài khoản'));
    navigate('/login');
  };

  const handleNonWbsClick = (menuName) => {
    message.info(`${t('Mục')} "${menuName}" ${t('là chức năng ngoài phạm vi T-05/T-06')}`);
  };

  return (
    <aside
      className={`app-sidebar${collapsed ? ' collapsed' : ''}`}
      style={{ background: 'linear-gradient(180deg, #0a3d62, #3c6382)' }}
    >
      {/* Brand Header */}
      <div className="sidebar-brand">
        <BuildOutlined className="sidebar-logo-icon" />
        {!collapsed && (
          <div className="sidebar-title-box">
            <span className="sidebar-title">CONSTRUCTFLOW</span>
            <span className="sidebar-subtitle">{t('Quản lý thi công')}</span>
          </div>
        )}
      </div>

      {/* Navigation Menu */}
      <div className="sidebar-menu">
        {!collapsed && <div className="sidebar-section-label">{t('TỔNG QUAN')}</div>}
        <div
          className="sidebar-item"
          onClick={() => handleNonWbsClick(t('Dashboard'))}
          title={collapsed ? t('Dashboard') : ''}
        >
          <DashboardOutlined />
          {!collapsed && <span>{t('Dashboard')}</span>}
        </div>

        {!collapsed && <div className="sidebar-section-label" style={{ marginTop: 12 }}>{t('DỰ ÁN')}</div>}
        <div
          className="sidebar-item"
          onClick={() => handleNonWbsClick(t('Dự án'))}
          title={collapsed ? t('Dự án') : ''}
        >
          <ProjectOutlined />
          {!collapsed && <span>{t('Dự án')}</span>}
        </div>

        {/* ACTIVE ITEM */}
        <div
          className={`sidebar-item ${location.pathname === '/' ? 'active' : ''}`}
          onClick={() => navigate('/')}
          title={collapsed ? t('Cơ cấu công việc') : ''}
        >
          <NodeIndexOutlined />
          {!collapsed && <span>{t('Cơ cấu công việc')}</span>}
        </div>

        <div
          className={`sidebar-item ${location.pathname === '/progress' ? 'active' : ''}`}
          onClick={() => navigate('/progress')}
          title={collapsed ? t('Tiến độ') : ''}
        >
          <HistoryOutlined />
          {!collapsed && <span>{t('Tiến độ')}</span>}
        </div>

        <div
          className="sidebar-item"
          onClick={() => handleNonWbsClick(t('Công việc'))}
          title={collapsed ? t('Công việc') : ''}
        >
          <CheckSquareOutlined />
          {!collapsed && <span>{t('Công việc')}</span>}
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
          <div className="user-avatar-initials">{initials || 'U'}</div>
          {!collapsed && (
            <div className="user-details">
              <span className="user-name">{displayName}</span>
              <span className="user-role">{user?.email || ''}</span>
            </div>
          )}
        </div>
        <Tooltip title={t('Đăng xuất')}>
          <div className="logout-btn" onClick={handleLogout}>
            <LogoutOutlined />
          </div>
        </Tooltip>
      </div>
    </aside>
  );
};

export default Sidebar;
