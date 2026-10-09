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
import { useLocation, useNavigate } from 'react-router-dom';
import { logout } from '../../services/wbsApi';
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
  const initials = displayName
    .split(/[\s.@_-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(part => part[0].toUpperCase())
    .join('');
  const currentSection = location.pathname === '/progress'
    ? t('Tiến độ')
    : location.pathname === '/progress/milestones'
      ? t('Mốc tiến độ')
    : location.pathname === '/working-calendar'
      ? t('Lịch làm việc')
      : location.pathname === '/assigned-tasks'
        ? t('Công việc')
      : t('Cơ cấu công việc');

  const handleProfileUpdated = useCallback(profile => {
    setUser(current => ({ ...current, ...profile }));
  }, []);

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

        <Breadcrumb
          items={[
            { title: t('Trang chủ') },
            { title: t('Dự án') },
            { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{currentSection}</span> },
          ]}
        />
      </div>

      <div className="header-right">
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
          <div className="header-user-dropdown">
            <Avatar style={{ backgroundColor: '#2563EB', fontWeight: 600 }}>
              {initials || 'U'}
            </Avatar>
            <span style={{ fontSize: 13, fontWeight: 600, color: '#1F2937' }}>
                {displayName}
            </span>
            <DownOutlined style={{ fontSize: 10, color: '#667085' }} />
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
