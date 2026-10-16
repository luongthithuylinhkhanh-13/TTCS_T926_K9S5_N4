import React from 'react';
import {
  ApartmentOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import SummaryCards from '../components/dashboard/SummaryCards';
import OverallProgress from '../components/dashboard/OverallProgress';
import ProgressByCategory from '../components/dashboard/ProgressByCategory';
import UpcomingTasks from '../components/dashboard/UpcomingTasks';
import MilestoneTimeline from '../components/dashboard/MilestoneTimeline';
import ProgressCharts from '../components/dashboard/ProgressCharts';
import { dashboardData } from '../data/dashboardData';
import '../styles/investor-dashboard.css';

const updatedAtFormatter = new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

const InvestorDashboardPage = () => {
  const { project, summary } = dashboardData;

  return (
    <div className="investor-dashboard">
      <header className="investor-page-header">
        <div>
          <span className="investor-eyebrow">TỔNG QUAN DỰ ÁN</span>
          <h1>Trang tổng hợp tiến độ dự án</h1>
          <p>Theo dõi tiến độ và các mốc quan trọng của công trình</p>
        </div>
        <div className="investor-project-meta">
          <div className="investor-project-name">
            <span className="investor-project-icon"><ApartmentOutlined /></span>
            <span><small>Dự án</small><strong>{project.name}</strong></span>
          </div>
          <div className="investor-meta-item"><span>Chủ đầu tư</span><strong>{project.investor}</strong></div>
          <div className="investor-meta-item investor-updated">
            <span><ClockCircleOutlined /> Cập nhật lần cuối</span>
            <strong><CalendarOutlined /> {updatedAtFormatter.format(new Date(project.updatedAt))}</strong>
          </div>
        </div>
      </header>

      <SummaryCards summary={summary} />
      <OverallProgress summary={summary} />

      <div className="investor-content-grid">
        <ProgressByCategory categories={dashboardData.categories} />
        <MilestoneTimeline milestones={dashboardData.milestones} />
      </div>

      <ProgressCharts summary={summary} categories={dashboardData.categories} />
      <UpcomingTasks tasks={dashboardData.upcomingTasks} />
      <p className="investor-data-note">Dữ liệu minh họa — sẽ được đồng bộ từ hệ thống khi API tiến độ sẵn sàng.</p>
    </div>
  );
};

export default InvestorDashboardPage;
