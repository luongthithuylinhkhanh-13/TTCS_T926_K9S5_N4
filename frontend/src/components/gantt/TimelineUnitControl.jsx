import React from 'react';
import { Segmented } from 'antd';
import { CalendarOutlined, FieldTimeOutlined } from '@ant-design/icons';

/**
 * Control to toggle between 'day' and 'week' timeline units.
 * Uses existing Ant Design Segmented component without adding any external dependencies.
 *
 * @param {Object} props
 * @param {'day'|'week'} props.value Current active unit ('day' or 'week')
 * @param {(unit: 'day'|'week') => void} props.onChange Callback invoked when user toggles unit
 * @param {boolean} [props.disabled=false]
 * @param {'small'|'middle'|'large'} [props.size='middle']
 * @param {string} [props.className='']
 * @param {Object} [props.style={}]
 */
const TimelineUnitControl = ({
  value = 'day',
  onChange,
  disabled = false,
  size = 'middle',
  className = '',
  style = {},
}) => {
  const options = [
    {
      label: 'Ngày',
      value: 'day',
      icon: <CalendarOutlined />,
    },
    {
      label: 'Tuần',
      value: 'week',
      icon: <FieldTimeOutlined />,
    },
  ];

  return (
    <div
      className={`timeline-unit-control-wrapper ${className}`}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 8,
        ...style,
      }}
    >
      <span
        style={{
          fontSize: 13,
          fontWeight: 500,
          color: '#64748b',
          whiteSpace: 'nowrap',
        }}
      >
        Đơn vị hiển thị:
      </span>
      <Segmented
        options={options}
        value={value}
        onChange={onChange}
        disabled={disabled}
        size={size}
        aria-label="Chọn đơn vị trục thời gian (Ngày hoặc Tuần)"
      />
    </div>
  );
};

export default TimelineUnitControl;
