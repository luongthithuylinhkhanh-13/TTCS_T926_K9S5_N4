import React from 'react';
import { Tag, Progress, Dropdown, Button, Image } from 'antd';
import {
  RightOutlined,
  DownOutlined,
  BankOutlined,
  FolderOpenOutlined,
  FileTextOutlined,
  MoreOutlined,
  EyeOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';

const WbsTreeRow = ({
  node,
  level = 0,
  isExpanded,
  onToggleExpand,
  isSelected,
  onSelectNode,
  onViewDetails,
  onAddChild,
  onEditNode,
  onDeleteNode
}) => {
  const hasChildren = node.children && node.children.length > 0;

  // Status mapping helper
  const renderStatusTag = (status) => {
    switch (status) {
      case 'completed':
        return <Tag color="success">Hoàn thành</Tag>;
      case 'in_progress':
        return <Tag color="processing">Đang thực hiện</Tag>;
      case 'paused':
        return <Tag color="warning">Tạm dừng</Tag>;
      case 'not_started':
      default:
        return <Tag color="default">Chưa bắt đầu</Tag>;
    }
  };

  // Icon mapping helper
  const renderNodeIcon = () => {
    if (node.type === 'project') return <BankOutlined className="node-icon project" />;
    if (node.type === 'phase') return <FolderOpenOutlined className="node-icon phase" />;
    return <FileTextOutlined className="node-icon task" />;
  };

  // Node Row CSS Classes
  const getRowClass = () => {
    let classes = 'tree-row';
    if (node.type === 'project') classes += ' node-project';
    else if (node.type === 'phase') classes += ' node-phase';
    if (isSelected) classes += ' selected';
    return classes;
  };

  // Action Menu items
  const menuItems = [
    {
      key: 'view',
      icon: <EyeOutlined />,
      label: 'Xem chi tiết',
      onClick: () => onViewDetails(node)
    },
    {
      key: 'add_child',
      icon: <PlusOutlined />,
      label: 'Thêm công việc con',
      onClick: () => onAddChild(node)
    },
    {
      key: 'edit',
      icon: <EditOutlined />,
      label: 'Chỉnh sửa',
      onClick: () => onEditNode(node)
    },
    {
      type: 'divider'
    },
    {
      key: 'delete',
      icon: <DeleteOutlined />,
      label: 'Xóa',
      danger: true,
      onClick: () => onDeleteNode(node)
    }
  ];

  const handleRowClick = () => {
    onSelectNode(node);
  };

  return (
    <tr className={getRowClass()} onClick={handleRowClick}>
      {/* CÔNG VIỆC COLUMN (With Indentation & Chevron) */}
      <td className="tree-cell">
        <div
          className="task-cell-content"
          style={{ paddingLeft: level * 24 }}
        >
          {hasChildren ? (
            <span
              className="expand-toggle"
              onClick={(e) => {
                e.stopPropagation();
                onToggleExpand(node.id);
              }}
            >
              {isExpanded ? <DownOutlined style={{ fontSize: 11 }} /> : <RightOutlined style={{ fontSize: 11 }} />}
            </span>
          ) : (
            <span style={{ width: 20 }} />
          )}

          {renderNodeIcon()}

          <span className="wbs-code-tag">{node.wbsCode}</span>

          <span className="task-name-text">
            {node.name}
          </span>
        </div>
      </td>

      {/* NGƯỜI PHỤ TRÁCH COLUMN */}
      <td className="tree-cell">
        {node.assignee ? (
          <div className="assignee-box">
            <div className="assignee-avatar">
              {node.assignee.initials || 'NV'}
            </div>
            <span className="assignee-name">{node.assignee.name}</span>
          </div>
        ) : (
          <span style={{ color: '#94A3B8', fontSize: 13, italic: 'true' }}>
            {node.type === 'task' ? 'Chưa phân công' : '--'}
          </span>
        )}
      </td>

      {/* THỜI GIAN COLUMN */}
      <td className="tree-cell">
        {node.startDate && node.endDate ? (
          <span style={{ fontSize: 13, color: '#475569' }}>
            {dayjs(node.startDate).format('DD/MM/YY')} - {dayjs(node.endDate).format('DD/MM/YY')}
          </span>
        ) : (
          <span style={{ color: '#94A3B8' }}>--</span>
        )}
      </td>

      {/* TRẠNG THÁI COLUMN */}
      <td className="tree-cell">
        {renderStatusTag(node.status)}
      </td>

      {/* TIẾN ĐỘ COLUMN */}
      <td className="tree-cell" style={{ width: 140 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Progress
            percent={node.progress || 0}
            size="small"
            showInfo={false}
            strokeColor={node.progress === 100 ? '#16A34A' : '#2563EB'}
          />
          <span style={{ fontSize: 12, fontWeight: 600, minWidth: 32, color: '#334155' }}>
            {node.progress || 0}%
          </span>
        </div>
      </td>

      {/* HÌNH ẢNH COLUMN */}
      <td className="tree-cell" style={{ width: 60, textAlign: 'center' }}>
        {node.images && node.images.length > 0 ? (
          <div style={{ position: 'relative', display: 'inline-block' }}>
            <Image
              src={
                typeof node.images[0] === 'string'
                  ? `/images/construction/${node.images[0]}`
                  : node.images[0]?.url
              }
              width={44}
              height={34}
              style={{ objectFit: 'cover', borderRadius: 4 }}
              preview={false}
            />
            {node.images.length > 1 && (
              <span style={{
                position: 'absolute',
                top: 0,
                right: 0,
                backgroundColor: 'rgba(0,0,0,0.6)',
                color: '#FFF',
                fontSize: 10,
                padding: '2px 4px',
                borderRadius: '0 4px 0 4px'
              }}>
                +{node.images.length - 1}
              </span>
            )}
          </div>
        ) : (
          <Image
            src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8z8AARwMD/UUAKQAAAABJRU5ErkJggg=="
            width={44}
            height={34}
            style={{ objectFit: 'cover', borderRadius: 4 }}
            preview={false}
          />
        )}
      </td>

      {/* THAO TÁC COLUMN */}
      <td className="tree-cell" style={{ textAlign: 'center', width: 60 }}>
        <div onClick={(e) => e.stopPropagation()}>
          <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
            <Button type="text" icon={<MoreOutlined />} size="small" />
          </Dropdown>
        </div>
      </td>
    </tr>
  );
};

export default WbsTreeRow;
