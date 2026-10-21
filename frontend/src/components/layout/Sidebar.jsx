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
  WalletOutlined,
  AuditOutlined,
  DollarOutlined,
  SettingOutlined,
  LogoutOutlined,
  CalculatorOutlined,
  UsergroupAddOutlined,
  TeamOutlined,
  SafetyCertificateOutlined,
  HeartOutlined,
  UserSwitchOutlined
} from '@ant-design/icons';
import { message, Tooltip } from 'antd';
import { logoutUser, getAuthUser } from '../../utils/auth';
import { getRoleDisplayName } from '../../utils/permissions';
import { logout } from '../../services/wbsApi';
import { useLocale } from '../../utils/LocaleContext';
import { useProjectContext } from '../../utils/ProjectContext';

const getMenuSections = (roleName) => {
  const role = String(roleName || '').toUpperCase();
  switch (role) {
    case 'ADMIN':
      return [
        {
          section: 'TỔNG QUAN',
          items: [{ label: 'Dashboard', path: '/dashboard', icon: <DashboardOutlined /> }]
        },
        {
          section: 'QUẢN TRỊ',
          items: [
            { label: 'Duyệt thành viên', path: '/admin/approvals', icon: <UsergroupAddOutlined /> },
            { label: 'Quản lý người dùng', path: '/admin/users', icon: <TeamOutlined /> },
            { label: 'Phân quyền', path: '/admin/roles', icon: <SafetyCertificateOutlined /> }
          ]
        },
        {
          section: 'HỆ THỐNG',
          items: [
            { label: 'Dự án', path: '/projects', icon: <ProjectOutlined /> },
            { label: 'Thành viên dự án', path: '/project-members', icon: <TeamOutlined /> },
            { label: 'Sức khỏe hệ thống', path: '/admin/system-health', icon: <HeartOutlined /> }
          ]
        },
        {
          section: 'KẾ HOẠCH',
          items: [
            {
              label: 'Tiến độ',
              path: '/progress',
              icon: <HistoryOutlined />,
              subItems: [{ label: 'Mốc tiến độ', path: '/progress/milestones' }]
            }
          ]
        }
      ];
    case 'PROJECT_MANAGER':
      return [
        {
          section: 'TỔNG QUAN',
          items: [{ label: 'Dashboard', path: '/dashboard', icon: <DashboardOutlined /> }]
        },
        {
          section: 'DỰ ÁN',
          items: [
            { label: 'Dự án', path: '/projects', icon: <ProjectOutlined /> },
            { label: 'Thành viên dự án', path: '/project-members', icon: <TeamOutlined /> },
            { label: 'Cơ cấu công việc', path: '/wbs', icon: <NodeIndexOutlined /> }
          ]
        },
        {
          section: 'KẾ HOẠCH',
          items: [
            { label: 'Dự toán', path: '/budget', icon: <CalculatorOutlined /> },
            {
              label: 'Tiến độ',
              path: '/progress',
              icon: <HistoryOutlined />,
              subItems: [{ label: 'Mốc tiến độ', path: '/progress/milestones' }]
            },
            { label: 'Lịch làm việc', path: '/working-calendar', icon: <CalendarOutlined /> }
          ]
        },
        {
          section: 'THỰC HIỆN',
          items: [
            { label: 'Công việc', path: '/tasks', icon: <CheckSquareOutlined /> },
            { label: 'Nhật ký công trường', path: '/site-diary', icon: <BookOutlined /> }
          ]
        },
        {
          section: 'NGHIỆM THU',
          items: [
            { label: 'Phiếu nghiệm thu', path: '/acceptance-records', icon: <AuditOutlined /> },
            { label: 'Nghiệm thu khối lượng', path: '/acceptance', icon: <AuditOutlined /> }
          ]
        },
        {
          section: 'TÀI CHÍNH',
          items: [
            { label: 'Đề nghị thanh toán', path: '/payment-requests', icon: <WalletOutlined /> },
            { label: 'Chi phí thực tế', path: '/actual-costs', icon: <DollarOutlined /> }
          ]
        }
      ];
    case 'CUSTOMER':
      return [
        {
          section: 'TỔNG QUAN',
          items: [{ label: 'Dashboard', path: '/dashboard', icon: <DashboardOutlined /> }]
        },
        {
          section: 'DỰ ÁN',
          items: [
            { label: 'Dự án', path: '/projects', icon: <ProjectOutlined /> }
          ]
        },
        {
          section: 'THEO DÕI',
          items: [
            {
              label: 'Tiến độ',
              path: '/progress',
              icon: <HistoryOutlined />,
              subItems: [{ label: 'Mốc tiến độ', path: '/progress/milestones' }]
            }
          ]
        },
        {
          section: 'NGHIỆM THU',
          items: [
            { label: 'Phiếu nghiệm thu', path: '/acceptance-records', icon: <AuditOutlined /> },
            { label: 'Nghiệm thu khối lượng', path: '/acceptance', icon: <AuditOutlined /> }
          ]
        },
        {
          section: 'TÀI CHÍNH',
          items: [
            { label: 'Đề nghị thanh toán', path: '/payment-requests', icon: <WalletOutlined /> },
            { label: 'Chi phí thực tế', path: '/actual-costs', icon: <DollarOutlined /> }
          ]
        }
      ];
    case 'SITE_MANAGER':
      return [
        {
          section: 'TỔNG QUAN',
          items: [{ label: 'Dashboard', path: '/dashboard', icon: <DashboardOutlined /> }]
        },
        {
          section: 'DỰ ÁN',
          items: [
            { label: 'Dự án', path: '/projects', icon: <ProjectOutlined /> },
            { label: 'Cơ cấu công việc', path: '/wbs', icon: <NodeIndexOutlined /> }
          ]
        },
        {
          section: 'TIẾN ĐỘ',
          items: [
            {
              label: 'Tiến độ',
              path: '/progress',
              icon: <HistoryOutlined />,
              subItems: [{ label: 'Mốc tiến độ', path: '/progress/milestones' }]
            },
            { label: 'Lịch làm việc', path: '/working-calendar', icon: <CalendarOutlined /> }
          ]
        },
        {
          section: 'CÔNG VIỆC',
          items: [
            { label: 'Công việc', path: '/tasks', icon: <CheckSquareOutlined /> },
            { label: 'Giao việc', path: '/task-assignment', icon: <UserSwitchOutlined /> },
            { label: 'Công việc đã giao', path: '/assigned-tasks', icon: <CheckSquareOutlined /> }
          ]
        },
        {
          section: 'HIỆN TRƯỜNG',
          items: [
            { label: 'Nhật ký công trường', path: '/site-diary', icon: <BookOutlined /> }
          ]
        },
        {
          section: 'NGHIỆM THU',
          items: [
            { label: 'Phiếu nghiệm thu', path: '/acceptance-records', icon: <AuditOutlined /> },
            { label: 'Nghiệm thu khối lượng', path: '/acceptance', icon: <AuditOutlined /> }
          ]
        },
        {
          section: 'THEO DÕI',
          items: [
            { label: 'Chi phí thực tế', path: '/actual-costs', icon: <DollarOutlined /> }
          ]
        }
      ];
    case 'SITE_ENGINEER':
      return [
        {
          section: 'TỔNG QUAN',
          items: [{ label: 'Dashboard', path: '/dashboard', icon: <DashboardOutlined /> }]
        },
        {
          section: 'DỰ ÁN',
          items: [
            { label: 'Dự án của tôi', path: '/projects', icon: <ProjectOutlined /> }
          ]
        },
        {
          section: 'HIỆN TRƯỜNG',
          items: [
            { label: 'Nhật ký công trường', path: '/site-diary', icon: <BookOutlined /> }
          ]
        },
        {
          section: 'NGHIỆM THU',
          items: [
            { label: 'Phiếu nghiệm thu', path: '/acceptance-records', icon: <AuditOutlined /> },
            { label: 'Nghiệm thu khối lượng', path: '/acceptance', icon: <AuditOutlined /> }
          ]
        },
        {
          section: 'THEO DÕI',
          items: [
            { label: 'Tiến độ', path: '/progress', icon: <HistoryOutlined /> }
          ]
        }
      ];
    case 'WORKER':
      return [
        {
          section: 'TỔNG QUAN',
          items: [{ label: 'Dashboard', path: '/dashboard', icon: <DashboardOutlined /> }]
        },
        {
          section: 'CÔNG VIỆC',
          items: [
            { label: 'Công việc được giao', path: '/assigned-tasks', icon: <CheckSquareOutlined /> },
            { label: 'Lịch làm việc', path: '/working-calendar', icon: <CalendarOutlined /> }
          ]
        },
        {
          section: 'HIỆN TRƯỜNG',
          items: [
            { label: 'Nhật ký công trường', path: '/site-diary', icon: <BookOutlined /> },
            { label: 'Cập nhật khối lượng', path: '/acceptance', icon: <AuditOutlined /> }
          ]
        },
        {
          section: 'DỰ ÁN',
          items: [
            { label: 'Dự án của tôi', path: '/projects', icon: <ProjectOutlined /> }
          ]
        }
      ];
    case 'STAFF':
      return [
        {
          section: 'TỔNG QUAN',
          items: [{ label: 'Dashboard', path: '/dashboard', icon: <DashboardOutlined /> }]
        },
        {
          section: 'DỰ ÁN',
          items: [
            { label: 'Dự án liên quan', path: '/projects', icon: <ProjectOutlined /> }
          ]
        },
        {
          section: 'TÀI CHÍNH',
          items: [
            { label: 'Dự toán', path: '/budget', icon: <CalculatorOutlined /> },
            { label: 'Đề nghị thanh toán', path: '/payment-requests', icon: <WalletOutlined /> },
            { label: 'Chi phí thực tế', path: '/actual-costs', icon: <DollarOutlined /> }
          ]
        },
        {
          section: 'NGHIỆM THU',
          items: [
            { label: 'Nghiệm thu khối lượng', path: '/acceptance', icon: <AuditOutlined /> }
          ]
        }
      ];
    default:
      return [
        {
          section: 'TỔNG QUAN',
          items: [{ label: 'Dashboard', path: '/dashboard', icon: <DashboardOutlined /> }]
        },
        {
          section: 'DỰ ÁN',
          items: [
            { label: 'Dự án', path: '/projects', icon: <ProjectOutlined /> }
          ]
        },
        {
          section: 'TIẾN ĐỘ',
          items: [
            { label: 'Tiến độ', path: '/progress', icon: <HistoryOutlined /> }
          ]
        }
      ];
  }
};

const Sidebar = ({ collapsed }) => {
  const { t } = useLocale();
  const navigate = useNavigate();
  const location = useLocation();
  const [progressExpanded, setProgressExpanded] = useState(
    () => location.pathname.startsWith('/progress')
  );
  const { effectiveRole } = useProjectContext();
  const user = getAuthUser();
  const displayName = user?.fullName || user?.email || t('Tài khoản');
  const currentRole = effectiveRole || user?.roleName;
  const roleTitle = getRoleDisplayName(currentRole);
  const initials = displayName
    .split(/[\s.@_-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(part => part[0].toUpperCase())
    .join('');

  const menuSections = getMenuSections(currentRole);

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

  const isItemActive = (itemPath) => {
    if (itemPath === '/dashboard') {
      return location.pathname === '/dashboard';
    }
    if (itemPath === '/progress') {
      return location.pathname === '/progress';
    }
    return location.pathname.startsWith(itemPath);
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
        {menuSections.map((sec, secIdx) => (
          <React.Fragment key={sec.section}>
            {!collapsed && (
              <div
                className="sidebar-section-label"
                style={{ marginTop: secIdx === 0 ? 0 : 12 }}
              >
                {t(sec.section)}
              </div>
            )}
            {sec.items.map(item => {
              const active = isItemActive(item.path);
              return (
                <React.Fragment key={item.path}>
                  <div
                    className={`sidebar-item ${active ? 'active' : ''}`}
                    onClick={() => {
                      if (item.subItems) {
                        setProgressExpanded(true);
                      }
                      navigate(item.path);
                    }}
                    title={collapsed ? t(item.label) : ''}
                  >
                    {item.icon}
                    {!collapsed && <span>{t(item.label)}</span>}
                  </div>
                  {!collapsed && item.subItems && progressExpanded && (
                    item.subItems.map(sub => (
                      <div
                        key={sub.path}
                        className={`sidebar-subitem ${location.pathname === sub.path ? 'active' : ''}`}
                        onClick={() => navigate(sub.path)}
                      >
                        <span className="sidebar-subitem-dot" />
                        <span>{t(sub.label)}</span>
                      </div>
                    ))
                  )}
                </React.Fragment>
              );
            })}
          </React.Fragment>
        ))}
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
              <span className="user-role">{roleTitle}</span>
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
