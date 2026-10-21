import React, { useEffect, useState, useCallback } from 'react';
import { Button, Table, Tag, Typography, Spin, Space } from 'antd';
import { DollarOutlined, PlusOutlined, HistoryOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { getActualCostsByWbsItem, formatCurrency, calculateTotalCost } from '../../services/actualCostApi';
import ActualCostModal from './ActualCostModal';

const { Text } = Typography;

const COST_TYPE_TAGS = {
  'VẬT_TƯ': { color: 'blue', label: 'Vật tư' },
  'NHÂN_CÔNG': { color: 'green', label: 'Nhân công' },
  'MÁY_THI_CÔNG': { color: 'orange', label: 'Máy móc' },
  'QUẢN_LÝ': { color: 'purple', label: 'Quản lý' },
  'KHÁC': { color: 'default', label: 'Khác' }
};

const ActualCostSection = ({ projectId, wbsItem }) => {
  const [costs, setCosts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);

  const itemId = wbsItem?.id || wbsItem?.key;

  const fetchCosts = useCallback(async () => {
    if (!projectId || !itemId) return;
    setLoading(true);
    try {
      const data = await getActualCostsByWbsItem(projectId, itemId);
      setCosts(data);
    } catch {
      setCosts([]);
    } finally {
      setLoading(false);
    }
  }, [projectId, itemId]);

  useEffect(() => {
    fetchCosts();
  }, [fetchCosts]);

  const totalActual = calculateTotalCost(costs);
  const estimatedCost = (Number(wbsItem?.totalPrice) || (Number(wbsItem?.quantity) * Number(wbsItem?.unitPrice))) || 0;

  const columns = [
    {
      title: 'Ngày',
      dataIndex: 'incurredDate',
      key: 'incurredDate',
      width: 95,
      render: date => (date ? dayjs(date).format('DD/MM/YYYY') : '--')
    },
    {
      title: 'Khoản chi',
      dataIndex: 'name',
      key: 'name',
      ellipsis: true
    },
    {
      title: 'Loại',
      dataIndex: 'costType',
      key: 'costType',
      width: 90,
      render: type => {
        const conf = COST_TYPE_TAGS[type] || { color: 'default', label: type };
        return <Tag color={conf.color}>{conf.label}</Tag>;
      }
    },
    {
      title: 'Số tiền',
      dataIndex: 'amount',
      key: 'amount',
      align: 'right',
      width: 120,
      render: amt => <span style={{ fontWeight: 600, color: '#DC2626' }}>{formatCurrency(amt)}</span>
    }
  ];

  return (
    <div className="drawer-section" style={{ marginTop: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <div className="drawer-section-title" style={{ margin: 0 }}>
          <DollarOutlined style={{ marginRight: 6, color: '#16A34A' }} />
          CHI PHÍ THỰC TẾ (S-47)
        </div>
        <Button
          type="primary"
          size="small"
          icon={<PlusOutlined />}
          onClick={() => setModalOpen(true)}
          style={{ background: '#16A34A', borderColor: '#16A34A' }}
        >
          Nhập chi phí
        </Button>
      </div>

      {/* Summary Box */}
      <div
        style={{
          background: '#F8FAFC',
          border: '1px solid #E2E8F0',
          borderRadius: 8,
          padding: '10px 14px',
          marginBottom: 12,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}
      >
        <div>
          <div style={{ fontSize: 11, color: '#64748B' }}>Tổng chi phí thực tế đã chi:</div>
          <div style={{ fontSize: 16, fontWeight: 700, color: '#DC2626' }}>
            {formatCurrency(totalActual)}
          </div>
        </div>
        {estimatedCost > 0 && (
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 11, color: '#64748B' }}>Dự toán hạng mục:</div>
            <div style={{ fontSize: 14, fontWeight: 600, color: '#2563EB' }}>
              {formatCurrency(estimatedCost)}
            </div>
          </div>
        )}
      </div>

      {/* History Table */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: 20 }}>
          <Spin size="small" />
        </div>
      ) : costs.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '16px 0', color: '#94A3B8', fontSize: 13 }}>
          <HistoryOutlined style={{ fontSize: 20, marginBottom: 4, display: 'block' }} />
          Chưa có khoản chi phí thực tế nào được ghi nhận cho hạng mục này
        </div>
      ) : (
        <Table
          dataSource={costs}
          columns={columns}
          rowKey="id"
          size="small"
          pagination={costs.length > 5 ? { pageSize: 5 } : false}
        />
      )}

      {/* Modal nhập chi phí */}
      <ActualCostModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        projectId={projectId}
        wbsItems={wbsItem ? [wbsItem] : []}
        defaultWbsItemId={itemId}
        onSuccess={fetchCosts}
      />
    </div>
  );
};

export default ActualCostSection;
