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

export const isProjectManager = (userOrRole = getAuthUser(), projectId = null) => {
  if (!userOrRole && typeof window !== 'undefined') {
    userOrRole = getAuthUser();
  }

  // If passed directly as role string
  if (typeof userOrRole === 'string') {
    const normalized = userOrRole.trim().toUpperCase();
    return ['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER'].includes(normalized);
  }

  const user = userOrRole;
  if (!user) return false;

  const globalRole = String(user.roleName || user.role || '').toUpperCase();
  const email = String(user.email || '').trim().toLowerCase();

  // ADMIN is a system-wide role
  if (globalRole === 'ADMIN' || PROJECT_MANAGER_EMAILS.includes(email)) {
    return true;
  }

  // Determine active/target projectId
  let targetProjectId = projectId;
  if (!targetProjectId && typeof window !== 'undefined') {
    try {
      targetProjectId = new URLSearchParams(window.location.search).get('projectId')
        || localStorage.getItem('constructflow_active_project_id');
    } catch (e) {
      // ignore
    }
  }

  // Check projectMemberships for the target project
  const memberships = Array.isArray(user.projectMemberships) ? user.projectMemberships : [];
  if (targetProjectId && memberships.length > 0) {
    const member = memberships.find(
      m => m.projectId === targetProjectId && String(m.status || '').toUpperCase() === 'ACTIVE'
    );
    if (member) {
      const projRole = String(member.roleName || member.role || '').toUpperCase();
      return ['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER'].includes(projRole);
    }
    return false;
  }

  // Check effectiveRole or currentProjectRole attached to user object
  const effRole = String(user.effectiveRole || user.currentProjectRole || '').toUpperCase();
  if (effRole) {
    return ['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER'].includes(effRole);
  }

  // If user only has 1 active project membership, check that
  if (memberships.length === 1 && String(memberships[0].status || '').toUpperCase() === 'ACTIVE') {
    const singleRole = String(memberships[0].roleName || memberships[0].role || '').toUpperCase();
    return ['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER'].includes(singleRole);
  }

  // Fallback to global role
  return ['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER'].includes(globalRole);
};

export const canAssignTask = (user = getAuthUser(), projectId = null) => {
  return isProjectManager(user, projectId);
};

