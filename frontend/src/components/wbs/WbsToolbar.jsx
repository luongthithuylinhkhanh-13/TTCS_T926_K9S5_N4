import React from 'react';
import { Select, Input, Button, Space, Tooltip } from 'antd';
import {
  SearchOutlined,
  FilterOutlined,
  ExpandOutlined,
  CompressOutlined,
  CloseCircleOutlined
} from '@ant-design/icons';

const { Option } = Select;

const WbsToolbar = ({
  projects = [],
  selectedProjectId,
  onSelectProject,
  searchText,
  onSearchChange,
  statusFilter,
  onStatusFilterChange,
  onExpandAll,
  onCollapseAll,
  onClearFilters
}) => {
  const isFiltered =
    !!searchText || (statusFilter && statusFilter !== 'ALL');

  return (
    <div className="wbs-toolbar">
      <div className="toolbar-project-select">
        <Select
          value={selectedProjectId}
          onChange={onSelectProject}
          style={{ width: '100%' }}
          size="middle"
          placeholder="Chọn dự án"
        >
          {projects.map(project => (
            <Option key={project.id} value={project.id}>
              <strong>{project.code}</strong> - {project.name}
            </Option>
          ))}
        </Select>
      </div>

      <div className="toolbar-search">
        <Input
          placeholder="Tìm kiếm theo tên hoặc mã WBS..."
          prefix={<SearchOutlined style={{ color: '#94A3B8' }} />}
          value={searchText}
          onChange={(e) => onSearchChange(e.target.value)}
          allowClear
        />
      </div>

      <div className="toolbar-status-select">
        <Select
          value={statusFilter}
          onChange={onStatusFilterChange}
          style={{ width: '100%' }}
          prefix={<FilterOutlined />}
        >
          <Option value="ALL">Tất cả trạng thái</Option>
          <Option value="not_started">Chưa bắt đầu</Option>
          <Option value="in_progress">Đang thực hiện</Option>
          <Option value="completed">Hoàn thành</Option>
          <Option value="paused">Tạm dừng</Option>
        </Select>
      </div>

      {isFiltered && (
        <Button
          type="dashed"
          icon={<CloseCircleOutlined />}
          onClick={onClearFilters}
          style={{ color: '#DC2626', borderColor: '#FCA5A5' }}
        >
          Xóa bộ lọc
        </Button>
      )}

      <Space style={{ marginLeft: 'auto' }}>
        <Tooltip title="Mở rộng tất cả cây WBS">
          <Button icon={<ExpandOutlined />} onClick={onExpandAll}>
            Mở rộng tất cả
          </Button>
        </Tooltip>

        <Tooltip title="Thu gọn tất cả cây WBS">
          <Button icon={<CompressOutlined />} onClick={onCollapseAll}>
            Thu gọn tất cả
          </Button>
        </Tooltip>
      </Space>
    </div>
  );
};

export default WbsToolbar;