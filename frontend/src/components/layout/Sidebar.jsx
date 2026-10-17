import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  BuildOutlined,
  DashboardOutlined,
  ProjectOutlined,
  NodeIndexOutlined,
  HistoryOutlined,
  CheckSquareOutlined,
  CalendarOutlined,
  BookOutlined,
  AuditOutlined,
  DollarOutlined,
  SettingOutlined,
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
  const [progressExpanded, setProgressExpanded] = useState(
    () => location.pathname.startsWith('/progress')
  );
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
          className={`sidebar-item ${location.pathname === '/dashboard' ? 'active' : ''}`}
          onClick={() => navigate('/dashboard')}
          title={collapsed ? t('Dashboard') : ''}
        >
          <DashboardOutlined />
          {!collapsed && <span>{t('Dashboard')}</span>}
        </div>

        {!collapsed && <div className="sidebar-section-label" style={{ marginTop: 12 }}>{t('DỰ ÁN')}</div>}
        <div
          className={`sidebar-item ${location.pathname === '/projects' ? 'active' : ''}`}
          onClick={() => navigate('/projects')}
          title={collapsed ? t('Dự án') : ''}
        >
          <ProjectOutlined />
          {!collapsed && <span>{t('Dự án')}</span>}
        </div>

        <div
          className={`sidebar-item ${location.pathname === '/wbs' ? 'active' : ''}`}
          onClick={() => navigate('/wbs')}
          title={collapsed ? t('Cơ cấu công việc') : ''}
        >
          <NodeIndexOutlined />
          {!collapsed && <span>{t('Cơ cấu công việc')}</span>}
        </div>

        <div
          className={`sidebar-item ${location.pathname === '/progress' ? 'active' : ''}`}
          onClick={() => {
            setProgressExpanded(true);
            navigate('/progress');
          }}
          title={collapsed ? t('Tiến độ') : ''}
        >
          <HistoryOutlined />
          {!collapsed && <span>{t('Tiến độ')}</span>}
        </div>
        {!collapsed && progressExpanded && (
          <div
            className={`sidebar-subitem ${location.pathname === '/progress/milestones' ? 'active' : ''}`}
            onClick={() => navigate('/progress/milestones')}
          >
            <span className="sidebar-subitem-dot" />
            <span>{t('Mốc tiến độ')}</span>
          </div>
        )}

        <div
          className={`sidebar-item ${location.pathname === '/working-calendar' ? 'active' : ''}`}
          onClick={() => navigate('/working-calendar')}
          title={collapsed ? t('Lịch làm việc') : ''}
        >
          <CalendarOutlined />
          {!collapsed && <span>{t('Lịch làm việc')}</span>}
        </div>

        <div
          className={`sidebar-item ${location.pathname === '/tasks' ? 'active' : ''}`}
          onClick={() => navigate('/tasks')}
          title={collapsed ? t('Công việc') : ''}
        >
          <CheckSquareOutlined />
          {!collapsed && <span>{t('Công việc')}</span>}
        </div>

        <div
          className={`sidebar-item ${location.pathname === '/assigned-tasks' ? 'active' : ''}`}
          onClick={() => navigate('/assigned-tasks')}
          title={collapsed ? t('Công việc đã giao') : ''}
        >
          <CheckSquareOutlined />
          {!collapsed && <span>{t('Công việc đã giao')}</span>}
        </div>

        <div
          className={`sidebar-item ${location.pathname === '/site-diary' ? 'active' : ''}`}
          onClick={() => navigate('/site-diary')}
          title={collapsed ? t('Nhật ký công trường') : ''}
        >
          <BookOutlined />
          {!collapsed && <span>{t('Nhật ký công trường')}</span>}
        </div>
        <div
          className={`sidebar-item ${location.pathname.startsWith('/acceptance-records') ? 'active' : ''}`}
          onClick={() => navigate('/acceptance-records')}
          title={collapsed ? t('Phiếu nghiệm thu') : ''}
        >
          <AuditOutlined />
          {!collapsed && <span>{t('Phiếu nghiệm thu')}</span>}
        </div>

        <div
          className={`sidebar-item ${location.pathname === '/acceptance' ? 'active' : ''}`}
          onClick={() => navigate('/acceptance')}
          title={collapsed ? t('Nghiệm thu khối lượng') : ''}
        >
          <AuditOutlined />
          {!collapsed && <span>{t('Nghiệm thu khối lượng')}</span>}
        </div>

        <div
          className={`sidebar-item ${location.pathname === '/actual-costs' ? 'active' : ''}`}
          onClick={() => navigate('/actual-costs')}
          title={collapsed ? t('Chi phí thực tế') : ''}
        >
          <DollarOutlined />
          {!collapsed && <span>{t('Chi phí thực tế')}</span>}
        </div>
        {user?.roleName?.toUpperCase() === 'ADMIN' && (
          <div
            className={`sidebar-item ${location.pathname === '/admin/system-health' ? 'active' : ''}`}
            onClick={() => navigate('/admin/system-health')}
            title={collapsed ? 'Sức khỏe hệ thống' : ''}
          >
            <SettingOutlined />
            {!collapsed && <span>Sức khỏe hệ thống</span>}
          </div>
        )}
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
