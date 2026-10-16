export const dashboardData = {
  project: {
    name: 'Xây dựng tòa nhà văn phòng A',
    investor: 'Công ty Cổ phần Đầu tư ABC',
    updatedAt: '2026-10-16T09:30:00',
  },
  summary: {
    totalTasks: 50,
    completedTasks: 34,
    inProgressTasks: 12,
    delayedTasks: 4,
    notStartedTasks: 4,
    overallProgress: 68,
  },
  categories: [
    { id: 'foundation', name: 'Móng', progress: 100, status: 'completed', totalTasks: 10 },
    { id: 'structure', name: 'Kết cấu', progress: 80, status: 'on-track', totalTasks: 15 },
    { id: 'finishing', name: 'Hoàn thiện', progress: 60, status: 'delayed', totalTasks: 15 },
    { id: 'mep', name: 'Cơ điện (MEP)', progress: 33, status: 'on-track', totalTasks: 6 },
    { id: 'landscape', name: 'Cảnh quan', progress: 25, status: 'on-track', totalTasks: 4 },
  ],
  upcomingTasks: [
    { id: 'task-1', name: 'Nghiệm thu đổ bê tông sàn tầng 8', assignee: 'Nguyễn Minh Đức', endDate: '2026-10-15', status: 'overdue' },
    { id: 'task-2', name: 'Lắp đặt hệ thống cấp nước tầng 5', assignee: 'Trần Quốc Huy', endDate: '2026-10-17', status: 'in-progress' },
    { id: 'task-3', name: 'Hoàn thiện trần thạch cao tầng 3', assignee: 'Lê Hoàng Nam', endDate: '2026-10-18', status: 'in-progress' },
    { id: 'task-4', name: 'Thi công chống thấm mái', assignee: 'Phạm Văn Long', endDate: '2026-10-20', status: 'not-started' },
    { id: 'task-5', name: 'Lắp đặt cửa kính mặt dựng', assignee: 'Đỗ Thu Hà', endDate: '2026-10-22', status: 'in-progress' },
  ],
  milestones: [
    { id: 'milestone-1', name: 'Hoàn thành thi công móng', date: '2026-09-30', completed: true },
    { id: 'milestone-2', name: 'Hoàn thành kết cấu tầng 5', date: '2026-10-10', completed: true },
    { id: 'milestone-3', name: 'Hoàn thành kết cấu phần thân', date: '2026-11-15', completed: false },
    { id: 'milestone-4', name: 'Nghiệm thu và bàn giao', date: '2027-06-30', completed: false },
  ],
};
