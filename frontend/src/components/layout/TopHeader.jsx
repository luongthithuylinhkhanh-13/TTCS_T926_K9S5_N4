import React, { useCallback, useState } from 'react';
import { 
  MenuFoldOutlined, 
  MenuUnfoldOutlined, 
  SearchOutlined, 
  BellOutlined, 
  DownOutlined 
} from '@ant-design/icons';
import { Breadcrumb, Badge, Avatar, Dropdown, message } from 'antd';
import { getAuthUser, logoutUser } from '../../utils/auth';
import { getRoleDisplayName } from '../../utils/permissions';
import { useLocation, useNavigate } from 'react-router-dom';
import { logout } from '../../services/wbsApi';
import InstallAppButton from '../pwa/InstallAppButton';
import ProfileSettingsModal from './ProfileSettingsModal';
import { useLocale } from '../../utils/LocaleContext';
import SyncQueueStatusBar from '../sync/SyncQueueStatusBar';

const TopHeader = ({ collapsed, setCollapsed, preferences, onPreferencesChange }) => {
  const { t } = useLocale();
  const navigate = useNavigate();
  const location = useLocation();
  const [user, setUser] = useState(getAuthUser);
  const [modalMode, setModalMode] = useState(null);
  const displayName = user?.fullName || user?.email || t('Tài khoản');
  const roleTitle = getRoleDisplayName(user?.roleName);
  const initials = displayName
    .split(/[\s.@_-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(part => part[0].toUpperCase())
    .join('');

  const handleProfileUpdated = useCallback(profile => {
    setUser(current => ({ ...current, ...profile }));
  }, []);

  const getBreadcrumbItems = () => {
    switch (location.pathname) {
      case '/dashboard':
        return [
          { title: t('Trang chủ') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Dashboard')}</span> }
        ];
      case '/projects':
        return [
          { title: t('Trang chủ') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Dự án')}</span> }
        ];
      case '/progress':
        return [
          { title: t('Trang chủ') },
          { title: t('Dự án') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Tiến độ')}</span> }
        ];
      case '/progress/milestones':
        return [
          { title: t('Trang chủ') },
          { title: t('Dự án') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Mốc tiến độ')}</span> }
        ];
      case '/working-calendar':
        return [
          { title: t('Trang chủ') },
          { title: t('Dự án') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Lịch làm việc')}</span> }
        ];
      case '/tasks':
        return [
          { title: t('Trang chủ') },
          { title: t('Dự án') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Công việc')}</span> }
        ];
      case '/task-assignment':
        return [
          { title: t('Trang chủ') },
          { title: t('Dự án') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Giao việc')}</span> }
        ];
      case '/assigned-tasks':
        return [
          { title: t('Trang chủ') },
          { title: t('Dự án') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{user?.roleName?.toUpperCase() === 'WORKER' ? t('Công việc được giao') : t('Công việc đã giao')}</span> }
        ];
      case '/site-diary':
        return [
          { title: t('Trang chủ') },
          { title: t('Dự án') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Nhật ký công trường')}</span> }
        ];
      case '/budget':
        return [
          { title: t('Trang chủ') },
          { title: t('Dự án') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Dự toán công trình')}</span> }
        ];
      case '/acceptance':
        return [
          { title: t('Trang chủ') },
          { title: t('Dự án') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Nghiệm thu khối lượng')}</span> }
        ];
      case '/acceptance-records':
        return [
          { title: t('Trang chủ') },
          { title: t('Dự án') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Phiếu nghiệm thu')}</span> }
        ];
      case '/wbs':
      default:
        return [
          { title: t('Trang chủ') },
          { title: t('Dự án') },
          { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{t('Cơ cấu công việc')}</span> }
        ];
    }
  };

  const handleDropdownClick = async ({ key }) => {
    if (key === 'logout') {
      try {
        await logout();
      } catch (error) {
        message.warning(error.message || 'Không thể thu hồi phiên trên máy chủ');
      }
      logoutUser();
      message.info(t('Đã đăng xuất tài khoản'));
      navigate('/login');
    } else if (key === 'profile' || key === 'settings') {
      setModalMode(key);
    }
  };

  const userMenuItems = [
    { key: 'profile', label: t('Thông tin cá nhân') },
    { key: 'settings', label: t('Cài đặt hệ thống') },
    { type: 'divider' },
    { key: 'logout', label: t('Đăng xuất'), danger: true },
  ];

  return (
    <header className="top-header">
      <div className="header-left">
        <div 
          className="collapse-toggle-btn"
          onClick={() => setCollapsed(!collapsed)}
          title={collapsed ? t('Mở rộng sidebar') : t('Thu gọn sidebar')}
        >
          {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        </div>

        <Breadcrumb items={getBreadcrumbItems()} />
      </div>

      <div className="header-right">
        <InstallAppButton />

        <div 
          className="header-icon-btn" 
          onClick={() => message.info(t('Tìm kiếm nhanh (Demo UI)'))}
        >
          <SearchOutlined />
        </div>

        <Badge count={3} offset={[-2, 4]}>
          <div 
            className="header-icon-btn" 
            onClick={() => message.info(t('Thông báo hệ thống (Demo UI)'))}
          >
            <BellOutlined />
          </div>
        </Badge>

        <SyncQueueStatusBar />

        <div style={{ width: 1, height: 24, backgroundColor: '#E6EAF0' }} />

        <Dropdown menu={{ items: userMenuItems, onClick: handleDropdownClick }} trigger={['click']}>
          <div className="header-user-dropdown" style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
            <Avatar style={{ backgroundColor: '#2563EB', fontWeight: 600 }}>
              {initials || 'U'}
            </Avatar>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', lineHeight: 1.2 }}>
              <span style={{ fontSize: 13, fontWeight: 600, color: '#1F2937' }}>
                {displayName}
              </span>
              <span style={{ fontSize: 11, color: '#2563EB', fontWeight: 500 }}>
                {roleTitle}
              </span>
            </div>
            <DownOutlined style={{ fontSize: 10, color: '#667085', marginLeft: 2 }} />
          </div>
        </Dropdown>
      </div>
      <ProfileSettingsModal
        open={modalMode !== null}
        mode={modalMode}
        preferences={preferences}
        onPreferencesChange={onPreferencesChange}
        onClose={() => setModalMode(null)}
        onProfileUpdated={handleProfileUpdated}
      />
    </header>
  );
};

export default TopHeader;
