import { getAuthUser } from './auth';

const PROJECT_MANAGER_EMAIL = 'lanc5676@gmail.com';

export const isProjectManager = (user = getAuthUser()) => {
  const roleName = String(user?.roleName || '').toUpperCase();
  return ['ADMIN', 'PROJECT_MANAGER'].includes(roleName)
    || user?.email?.toLowerCase() === PROJECT_MANAGER_EMAIL;
};
