import React from 'react';
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

const TopHeader = ({ collapsed, setCollapsed }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const user = getAuthUser();
  const currentSection = location.pathname === '/progress' ? 'Tiến độ' : 'Cơ cấu công việc';

  const handleDropdownClick = async ({ key }) => {
    if (key === 'logout') {
      try {
        await logout();
      } catch (error) {
        message.warning(error.message || 'Không thể thu hồi phiên trên máy chủ');
      }
      logoutUser();
      message.info('Đã đăng xuất tài khoản');
      navigate('/login');
    } else {
      message.info('Chức năng demo');
    }
  };

  const userMenuItems = [
    { key: 'profile', label: 'Thông tin cá nhân' },
    { key: 'settings', label: 'Cài đặt hệ thống' },
    { type: 'divider' },
    { key: 'logout', label: 'Đăng xuất', danger: true },
  ];

  return (
    <header className="top-header">
      <div className="header-left">
        <div 
          className="collapse-toggle-btn"
          onClick={() => setCollapsed(!collapsed)}
          title={collapsed ? "Mở rộng sidebar" : "Thu gọn sidebar"}
        >
          {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        </div>

        <Breadcrumb
          items={[
            { title: 'Trang chủ' },
            { title: 'Dự án' },
            { title: <span style={{ color: '#2563EB', fontWeight: 600 }}>{currentSection}</span> },
          ]}
        />
      </div>

      <div className="header-right">
        <div 
          className="header-icon-btn" 
          onClick={() => message.info('Tìm kiếm nhanh (Demo UI)')}
        >
          <SearchOutlined />
        </div>

        <Badge count={3} offset={[-2, 4]}>
          <div 
            className="header-icon-btn" 
            onClick={() => message.info('Thông báo hệ thống (Demo UI)')}
          >
            <BellOutlined />
          </div>
        </Badge>

        <div style={{ width: 1, height: 24, backgroundColor: '#E6EAF0' }} />

        <Dropdown menu={{ items: userMenuItems, onClick: handleDropdownClick }} trigger={['click']}>
          <div className="header-user-dropdown">
            <Avatar style={{ backgroundColor: '#2563EB', fontWeight: 600 }}>QT</Avatar>
            <span style={{ fontSize: 13, fontWeight: 600, color: '#1F2937' }}>
              {user?.fullName || 'Quản trị viên'}
            </span>
            <DownOutlined style={{ fontSize: 10, color: '#667085' }} />
          </div>
        </Dropdown>
      </div>
    </header>
  );
};

export default TopHeader;
