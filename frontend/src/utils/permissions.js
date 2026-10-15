import { getAuthUser } from './auth';

const PROJECT_MANAGER_EMAILS = [
  'lanc5676@gmail.com',
  'linh.work@gmail.com',
  'kien.work@gmail.com',
  'bach.work@gmail.com',
  'chinh.work@gmail.com',
  'lan.work@gmail.com'
];

export const isProjectManager = (user = getAuthUser()) => {
  const roleName = String(user?.roleName || '').toUpperCase();
  return ['ADMIN', 'PROJECT_MANAGER'].includes(roleName)
    || PROJECT_MANAGER_EMAILS.includes(String(user?.email || '').toLowerCase());
};
