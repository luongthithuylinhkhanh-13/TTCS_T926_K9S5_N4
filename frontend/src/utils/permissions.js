import { getAuthUser } from './auth.js';

export const ROLE_DISPLAY_NAMES = {
  ADMIN: 'Quản trị hệ thống',
  PROJECT_MANAGER: 'Ban quản lý dự án',
  CUSTOMER: 'Chủ đầu tư',
  SITE_MANAGER: 'Chỉ huy trưởng công trường',
  SITE_ENGINEER: 'Kỹ sư giám sát',
  WORKER: 'Đội trưởng thi công',
  STAFF: 'Kế toán chủ đầu tư',
  VIEWER: 'Người xem'
};

export const getRoleDisplayName = (roleName) => {
  const normalized = String(roleName || '').toUpperCase();
  return ROLE_DISPLAY_NAMES[normalized] || roleName || 'Người dùng';
};

const PROJECT_MANAGER_EMAILS = [
  'lanc5676@gmail.com',
  'test@test.com',
  'linh.work@gmail.com',
  'kien.work@gmail.com',
  'bach.work@gmail.com',
  'chinh.work@gmail.com',
  'lan.work@gmail.com'
];

export const isProjectManager = (user = getAuthUser()) => {
  const roleName = String(user?.roleName || '').toUpperCase();
  const email = String(user?.email || '').trim().toLowerCase();
  return ['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER'].includes(roleName)
    || PROJECT_MANAGER_EMAILS.includes(email);
};

