import React from 'react';
import { Empty } from 'antd';
import WbsTreeRow from './WbsTreeRow';
import { useLocale } from '../../utils/LocaleContext';

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
  const { t } = useLocale();
  if (!nodes || nodes.length === 0) {
    return (
      <div style={{ padding: '40px 0', textAlign: 'center' }}>
        <Empty description={t('Không tìm thấy công việc phù hợp với bộ lọc')} />
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
            <th style={{ width: '36%' }}>{t('CÔNG VIỆC')}</th>
            <th>{t('NGƯỜI PHỤ TRÁCH')}</th>
            <th>{t('NGÀY BẮT ĐẦU KẾ HOẠCH')}</th>
            <th>{t('NGÀY KẾT THÚC KẾ HOẠCH')}</th>
            <th>{t('NGÀY BẮT ĐẦU THỰC TẾ')}</th>
            <th>{t('NGÀY KẾT THÚC THỰC TẾ')}</th>
            <th>{t('TRẠNG THÁI')}</th>
            <th style={{ width: '12%' }}>{t('TIẾN ĐỘ THỰC TẾ')}</th>
            <th style={{ width: 60, textAlign: 'center' }}>{t('THAO TÁC')}</th>
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
