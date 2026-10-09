import React, { useMemo, useState } from 'react';
import {
  CalendarOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LeftOutlined,
  RightOutlined
} from '@ant-design/icons';
import { Button, DatePicker, Input, Modal, message } from 'antd';
import dayjs from 'dayjs';
import { getAuthUser } from '../../utils/auth';
import { isProjectManager } from '../../utils/permissions';

const defaultHolidayMap = {
  '2026-01-01': 'Tết Dương lịch',
  '2026-02-14': 'Tết Nguyên đán',
  '2026-02-15': 'Tết Nguyên đán',
  '2026-02-16': 'Tết Nguyên đán',
  '2026-02-17': 'Tết Nguyên đán',
  '2026-02-18': 'Tết Nguyên đán',
  '2026-02-19': 'Tết Nguyên đán',
  '2026-02-20': 'Tết Nguyên đán',
  '2026-02-21': 'Tết Nguyên đán',
  '2026-04-30': 'Ngày Giải phóng miền Nam',
  '2026-05-01': 'Quốc tế Lao động',
  '2026-09-02': 'Quốc khánh'
};

const weekDays = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
const pad = value => String(value).padStart(2, '0');
const dateKey = date => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

const getCalendarDays = month => {
  const firstDay = new Date(month.getFullYear(), month.getMonth(), 1);
  const start = new Date(firstDay);
  start.setDate(firstDay.getDate() - firstDay.getDay());
  return Array.from({ length: 42 }, (_, index) => {
    const day = new Date(start);
    day.setDate(start.getDate() + index);
    return day;
  });
};

const WorkingCalendarCard = () => {
  const authUser = getAuthUser();
  const canEdit = isProjectManager(authUser);
  const [holidayMap, setHolidayMap] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('constructflow_working_holidays')) || defaultHolidayMap;
    } catch {
      return defaultHolidayMap;
    }
  });
  const [month, setMonth] = useState(() => {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth(), 1);
  });
  const [editorOpen, setEditorOpen] = useState(false);
  const [holidayDate, setHolidayDate] = useState(null);
  const [holidayName, setHolidayName] = useState('');
  const days = useMemo(() => getCalendarDays(month), [month]);
  const monthLabel = month.toLocaleDateString('vi-VN', { month: 'long', year: 'numeric' });

  const moveMonth = offset => {
    setMonth(current => new Date(current.getFullYear(), current.getMonth() + offset, 1));
  };

  const saveHoliday = () => {
    if (!holidayDate || !holidayName.trim()) {
      message.warning('Vui lòng chọn ngày và nhập tên ngày nghỉ.');
      return;
    }
    const next = { ...holidayMap, [holidayDate.format('YYYY-MM-DD')]: holidayName.trim() };
    setHolidayMap(next);
    localStorage.setItem('constructflow_working_holidays', JSON.stringify(next));
    setEditorOpen(false);
    setHolidayDate(null);
    setHolidayName('');
    message.success('Đã thêm ngày nghỉ vào lịch làm việc.');
  };

  const removeHoliday = date => {
    const next = { ...holidayMap };
    delete next[date];
    setHolidayMap(next);
    localStorage.setItem('constructflow_working_holidays', JSON.stringify(next));
    message.success('Đã xóa ngày nghỉ khỏi lịch.');
  };

  return (
    <section className="wbs-main-card working-calendar-card" aria-label="Lịch làm việc">
      <div className="working-calendar-heading">
        <div className="wbs-card-title-box">
          <span className="wbs-card-title"><CalendarOutlined /> Lịch làm việc</span>
          <span className="wbs-card-subtitle">S-17 · Lịch 6 ngày/tuần</span>
        </div>
        <div className="working-calendar-actions">
          {canEdit && (
            <Button type="primary" onClick={() => setEditorOpen(true)}>Chỉnh sửa lịch</Button>
          )}
          <button type="button" className="calendar-nav-button" onClick={() => moveMonth(-1)} aria-label="Tháng trước">
            <LeftOutlined />
          </button>
          <strong>{monthLabel}</strong>
          <button type="button" className="calendar-nav-button" onClick={() => moveMonth(1)} aria-label="Tháng sau">
            <RightOutlined />
          </button>
        </div>
      </div>

      <div className="working-calendar-grid">
        {weekDays.map((day, index) => (
          <div className={`working-calendar-weekday ${index === 0 ? 'is-sunday' : ''}`} key={day}>{day}</div>
        ))}
        {days.map(day => {
          const key = dateKey(day);
          const holiday = holidayMap[key];
          const isSunday = day.getDay() === 0;
          const isOutsideMonth = day.getMonth() !== month.getMonth();
          return (
            <div
              className={`working-calendar-day${isSunday ? ' is-sunday' : ''}${holiday ? ' is-holiday' : ''}${isOutsideMonth ? ' is-outside' : ''}`}
              key={key}
            >
              <span className="working-calendar-date">{day.getDate()}</span>
              {holiday && (
                <span className="working-calendar-event">
                  {holiday}
                  {canEdit && (
                    <button type="button" className="calendar-remove-button" onClick={() => removeHoliday(key)}>
                      Xóa
                    </button>
                  )}
                </span>
              )}
              {!holiday && isSunday && <span className="working-calendar-event">Nghỉ</span>}
              {!holiday && !isSunday && <span className="working-calendar-event is-working"><CheckCircleOutlined /> Làm việc</span>}
            </div>
          );
        })}
      </div>

      <div className="working-calendar-legend">
        <span><CheckCircleOutlined /> Ngày làm việc</span>
        <span className="is-off"><CloseCircleOutlined /> Chủ nhật</span>
        <span className="is-holiday">Ngày lễ</span>
      </div>
      {canEdit && (
        <Modal
          title="Thêm ngày nghỉ"
          open={editorOpen}
          onCancel={() => setEditorOpen(false)}
          onOk={saveHoliday}
          okText="Lưu ngày nghỉ"
          cancelText="Hủy"
        >
          <div className="working-calendar-form">
            <DatePicker
              value={holidayDate}
              onChange={setHolidayDate}
              format="DD/MM/YYYY"
              placeholder="Chọn ngày nghỉ"
              style={{ width: '100%' }}
            />
            <Input
              value={holidayName}
              onChange={event => setHolidayName(event.target.value)}
              placeholder="Ví dụ: Nghỉ bù công trường"
            />
          </div>
        </Modal>
      )}
    </section>
  );
};

export default WorkingCalendarCard;
