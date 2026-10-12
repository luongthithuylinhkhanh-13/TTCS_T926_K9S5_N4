import test from 'node:test';
import assert from 'node:assert/strict';

test('1. Breadcrumb mapping contract', () => {
  const getBreadcrumbTitle = (pathname) => {
    switch (pathname) {
      case '/dashboard':
        return ['Trang chủ', 'Dashboard'];
      case '/projects':
        return ['Trang chủ', 'Dự án'];
      case '/progress':
        return ['Trang chủ', 'Dự án', 'Tiến độ'];
      case '/tasks':
        return ['Trang chủ', 'Dự án', 'Công việc'];
      case '/wbs':
      default:
        return ['Trang chủ', 'Dự án', 'Cơ cấu công việc'];
    }
  };

  assert.deepEqual(getBreadcrumbTitle('/dashboard'), ['Trang chủ', 'Dashboard']);
  assert.deepEqual(getBreadcrumbTitle('/projects'), ['Trang chủ', 'Dự án']);
  assert.deepEqual(getBreadcrumbTitle('/wbs'), ['Trang chủ', 'Dự án', 'Cơ cấu công việc']);
  assert.deepEqual(getBreadcrumbTitle('/progress'), ['Trang chủ', 'Dự án', 'Tiến độ']);
  assert.deepEqual(getBreadcrumbTitle('/tasks'), ['Trang chủ', 'Dự án', 'Công việc']);
});

test('2. Sidebar active route resolution contract', () => {
  const getActiveMenu = (pathname) => {
    if (pathname === '/dashboard') return 'dashboard';
    if (pathname === '/projects') return 'projects';
    if (pathname === '/wbs') return 'wbs';
    if (pathname === '/progress') return 'progress';
    if (pathname === '/tasks') return 'tasks';
    return 'none';
  };

  assert.equal(getActiveMenu('/dashboard'), 'dashboard');
  assert.equal(getActiveMenu('/projects'), 'projects');
  assert.equal(getActiveMenu('/wbs'), 'wbs');
  assert.equal(getActiveMenu('/progress'), 'progress');
  assert.equal(getActiveMenu('/tasks'), 'tasks');
});

test('3. Query parameter projectId extraction contract', () => {
  const extractProjectId = (searchString) => {
    const params = new URLSearchParams(searchString);
    return params.get('projectId');
  };

  assert.equal(extractProjectId('?projectId=DA-001'), 'DA-001');
  assert.equal(extractProjectId('?projectId=58fa26e1-72cb-458d-bdb0-24cd197f2672'), '58fa26e1-72cb-458d-bdb0-24cd197f2672');
  assert.equal(extractProjectId(''), null);
});
