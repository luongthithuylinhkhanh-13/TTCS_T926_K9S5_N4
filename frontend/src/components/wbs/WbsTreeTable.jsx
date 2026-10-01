import React from 'react';
import { Empty } from 'antd';
import WbsTreeRow from './WbsTreeRow';

const WbsTreeTable = ({ 
  nodes, 
  expandedKeys, 
  onToggleExpand, 
  selectedNodeId, 
  onSelectNode,
  onViewDetails,
  onAddChild,
  onEditNode,
  onDeleteNode
}) => {
  if (!nodes || nodes.length === 0) {
    return (
      <div style={{ padding: '40px 0', textAlign: 'center' }}>
        <Empty description="Không tìm thấy công việc phù hợp với bộ lọc" />
      </div>
    );
  }

  // Helper function to recursively render nodes and expanded children
  const renderRows = (items, level = 0) => {
    let rows = [];
    items.forEach(node => {
      const isExpanded = expandedKeys.includes(node.id);
      const isSelected = selectedNodeId === node.id;

      rows.push(
        <WbsTreeRow
          key={node.id}
          node={node}
          level={level}
          isExpanded={isExpanded}
          onToggleExpand={onToggleExpand}
          isSelected={isSelected}
          onSelectNode={onSelectNode}
          onViewDetails={onViewDetails}
          onAddChild={onAddChild}
          onEditNode={onEditNode}
          onDeleteNode={onDeleteNode}
        />
      );

      if (isExpanded && node.children && node.children.length > 0) {
        rows = rows.concat(renderRows(node.children, level + 1));
      }
    });

    return rows;
  };

  return (
    <div className="tree-table-wrapper">
      <table className="tree-table">
        <thead className="tree-table-header">
          <tr>
            <th style={{ width: '40%' }}>CÔNG VIỆC</th>
            <th style={{ width: '18%' }}>NGƯỜI PHỤ TRÁCH</th>
            <th style={{ width: '16%' }}>THỜI GIAN</th>
            <th style={{ width: '12%' }}>TRẠNG THÁI</th>
            <th style={{ width: '10%' }}>TIẾN ĐỘ</th>
            <th style={{ width: '8%', textAlign: 'center' }}>HÌNH ẢNH</th>
            <th style={{ width: '4%', textAlign: 'center' }}>THAO TÁC</th>
          </tr>
        </thead>
        <tbody>
          {renderRows(nodes)}
        </tbody>
      </table>
    </div>
  );
};

export default WbsTreeTable;
