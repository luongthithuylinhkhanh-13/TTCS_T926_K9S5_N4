import { getAuthUser } from './auth.js';

const PROJECT_MANAGER_EMAILS = [
  'lanc5676@gmail.com',
  'test@test.com',
];

export const isProjectManager = (user = getAuthUser()) => {
  const roleName = String(user?.roleName || '').toUpperCase();
  const email = String(user?.email || '').trim().toLowerCase();
  return ['ADMIN', 'PROJECT_MANAGER'].includes(roleName)
    || PROJECT_MANAGER_EMAILS.includes(email);
};
