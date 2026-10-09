const AUTH_KEY = 'constructflow_auth';

/**
 * Lưu thông tin người dùng đã đăng nhập.
 *
 * @param {object} user - Đối tượng người dùng (KHÔNG chứa password).
 * @param {boolean} remember - Nếu true lưu vào localStorage, ngược lại sessionStorage.
 */
export const setAuthUser = (user, remember = false) => {
  try {
    const data = JSON.stringify(user);
    if (remember) {
      localStorage.setItem(AUTH_KEY, data);
    } else {
      sessionStorage.setItem(AUTH_KEY, data);
    }
  } catch (error) {
    console.error('Error saving auth state:', error);
  }
};

export const updateAuthUser = updates => {
  const current = getAuthUser();
  if (!current) return;

  const remember = Boolean(localStorage.getItem(AUTH_KEY));
  setAuthUser({ ...current, ...updates }, remember);
};

/**
 * Lấy thông tin người dùng đã đăng nhập.
 * Kiểm tra cả localStorage và sessionStorage.
 *
 * @returns {object|null}
 */
export const getAuthUser = () => {
  try {
    const data =
      localStorage.getItem(AUTH_KEY) ||
      sessionStorage.getItem(AUTH_KEY);
    return data ? JSON.parse(data) : null;
  } catch (error) {
    console.error('Error reading auth state:', error);
    return null;
  }
};

/**
 * Xóa trạng thái đăng nhập ở cả hai storage.
 */
export const logoutUser = () => {
  try {
    localStorage.removeItem(AUTH_KEY);
    sessionStorage.removeItem(AUTH_KEY);
  } catch (error) {
    console.error('Error clearing auth state:', error);
  }
};

/**
 * Kiểm tra người dùng đã đăng nhập hay chưa.
 *
 * @returns {boolean}
 */
export const isAuthenticated = () => {
  return !!getAuthUser();
};
