import React from 'react';
import { CalendarOutlined, InfoCircleOutlined } from '@ant-design/icons';
import WorkingCalendarCard from '../components/wbs/WorkingCalendarCard';

const WorkingCalendarPage = () => (
  <div className="page-container working-calendar-page">
    <div className="page-header">
      <div>
        <h1 className="page-title">
          <CalendarOutlined /> Lịch làm việc
        </h1>
        <p className="page-subtitle">
          Theo dõi lịch theo từng tháng, ngày làm việc và các ngày nghỉ
        </p>
      </div>
    </div>

    <WorkingCalendarCard />

    <section className="wbs-main-card working-calendar-help">
      <InfoCircleOutlined />
      <div>
        <strong>Cách lịch được áp dụng</strong>
        <p>
          Hệ thống làm việc từ Thứ 2 đến Thứ 7. Chủ nhật và các ngày lễ trong danh sách
          sẽ không được tính vào thời lượng công việc, đường găng và ngày hoàn thành dự kiến.
        </p>
      </div>
    </section>
  </div>
);

export default WorkingCalendarPage;
