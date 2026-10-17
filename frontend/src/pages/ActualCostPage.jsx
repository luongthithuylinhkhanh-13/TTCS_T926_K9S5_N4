import React, { useEffect, useState, useCallback } from 'react';
import {
  Card,
  Table,
  Button,
  Select,
  DatePicker,
  Tag,
  Row,
  Col,
  Statistic,
  Space,
  Typography,
  message,
  Empty
} from 'antd';
import {
  DollarOutlined,
  PlusOutlined,
  FilterOutlined,
  ReloadOutlined,
  FileDoneOutlined,
  PieChartOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  getActualCosts,
  getActualCostSummary,
  formatCurrency,
  calculateTotalCost
} from '../services/actualCostApi';
import { getProjects, getProjectWbs } from '../services/wbsApi';
import ActualCostModal from '../components/wbs/ActualCostModal';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const COST_TYPES = [
  { value: '', label: 'Tất cả loại chi phí' },
  { value: 'VẬT_TƯ', label: 'Vật tư / Nguyên vật liệu' },
  { value: 'NHÂN_CÔNG', label: 'Nhân công thi công' },
  { value: 'MÁY_THI_CÔNG', label: 'Máy thi công & Thiết bị' },
  { value: 'QUẢN_LÝ', label: 'Chi phí quản lý chung' },
  { value: 'KHÁC', label: 'Chi phí khác' }
];

const COST_TYPE_TAGS = {
  'VẬT_TƯ': { color: 'blue', label: 'Vật tư' },
  'NHÂN_CÔNG': { color: 'green', label: 'Nhân công' },
  'MÁY_THI_CÔNG': { color: 'orange', label: 'Máy móc' },
  'QUẢN_LÝ': { color: 'purple', label: 'Quản lý' },
  'KHÁC': { color: 'default', label: 'Khác' }
};

const ActualCostPage = () => {
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [wbsItems, setWbsItems] = useState([]);

  const [costs, setCosts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);

  // Filters
  const [filterWbsItemId, setFilterWbsItemId] = useState(null);
  const [filterCostType, setFilterCostType] = useState('');
  const [filterDateRange, setFilterDateRange] = useState(null);

  // Load project list
  useEffect(() => {
    const loadProjects = async () => {
      try {
        const projs = await getProjects();
        if (Array.isArray(projs) && projs.length > 0) {
          setProjects(projs);
          const savedId = localStorage.getItem('last_selected_project_id');
          const found = projs.find(p => p.id === savedId);
          setSelectedProjectId(found ? found.id : projs[0].id);
        }
      } catch (err) {
        message.error('Không thể nạp danh sách dự án');
      }
    };
    loadProjects();
  }, []);

  // Load WBS items for current project
  useEffect(() => {
    if (!selectedProjectId) return;
    localStorage.setItem('last_selected_project_id', selectedProjectId);
    const loadWbs = async () => {
      try {
        const res = await getProjectWbs(selectedProjectId);
        const items = res?.items || (Array.isArray(res) ? res : []);
        setWbsItems(items);
      } catch {
        setWbsItems([]);
      }
    };
    loadWbs();
  }, [selectedProjectId]);

  // Load actual costs with filters
  const loadCosts = useCallback(async () => {
    if (!selectedProjectId) return;
    setLoading(true);
    try {
      const params = {};
      if (filterWbsItemId) params.wbsItemId = filterWbsItemId;
      if (filterCostType) params.costType = filterCostType;
      if (filterDateRange && filterDateRange[0] && filterDateRange[1]) {
        params.startDate = filterDateRange[0].format('YYYY-MM-DD');
        params.endDate = filterDateRange[1].format('YYYY-MM-DD');
      }
      const data = await getActualCosts(selectedProjectId, params);
      setCosts(data);
    } catch (err) {
      message.error('Lỗi khi tải lịch sử chi phí thực tế');
      setCosts([]);
    } finally {
      setLoading(false);
    }
  }, [selectedProjectId, filterWbsItemId, filterCostType, filterDateRange]);

  useEffect(() => {
    loadCosts();
  }, [loadCosts]);

  const totalActual = calculateTotalCost(costs);

  const columns = [
    {
      title: 'Ngày phát sinh',
      dataIndex: 'incurredDate',
      key: 'incurredDate',
      width: 120,
      render: date => (date ? dayjs(date).format('DD/MM/YYYY') : '--'),
      sorter: (a, b) => dayjs(a.incurredDate).unix() - dayjs(b.incurredDate).unix()
    },
    {
      title: 'Nội dung khoản chi',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <div>
          <div style={{ fontWeight: 600, color: '#1E293B' }}>{text}</div>
          {record.note && <div style={{ fontSize: 12, color: '#64748B' }}>{record.note}</div>}
        </div>
      )
    },
    {
      title: 'Hạng mục liên kết (T-95)',
      key: 'wbsItem',
      render: (_, record) => {
        if (!record.wbsItemId) {
          return <Tag color="default">Chi phí chung dự án</Tag>;
        }
        return (
          <Space orientation="vertical" size={2}>
            {record.wbsCode && <span className="wbs-badge" style={{ fontSize: 11 }}>{record.wbsCode}</span>}
            <span style={{ fontWeight: 500 }}>{record.wbsItemName || 'Hạng mục'}</span>
          </Space>
        );
      }
    },
    {
      title: 'Phân loại',
      dataIndex: 'costType',
      key: 'costType',
      width: 130,
      render: type => {
        const conf = COST_TYPE_TAGS[type] || { color: 'default', label: type };
        return <Tag color={conf.color}>{conf.label}</Tag>;
      }
    },
    {
      title: 'Số tiền thực chi',
      dataIndex: 'amount',
      key: 'amount',
      align: 'right',
      width: 160,
      render: val => (
        <span style={{ fontWeight: 700, color: '#DC2626', fontSize: 14 }}>
          {formatCurrency(val)}
        </span>
      ),
      sorter: (a, b) => (Number(a.amount) || 0) - (Number(b.amount) || 0)
    },
    {
      title: 'Người nhập',
      dataIndex: 'createdBy',
      key: 'createdBy',
      width: 130,
      render: user => user || 'Hệ thống'
    }
  ];

  return (
    <div style={{ padding: '20px 24px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <Title level={3} style={{ margin: 0, color: '#0F172A' }}>
            <DollarOutlined style={{ color: '#16A34A', marginRight: 8 }} />
            Quản lý & Lịch sử Chi phí Thực tế (S-47)
          </Title>
          <Text type="secondary">
            Ghi nhận chi phí phát sinh thực tế và liên kết với từng hạng mục công việc
          </Text>
        </div>

        <Space>
          <Select
            style={{ width: 260 }}
            placeholder="Chọn dự án..."
            value={selectedProjectId}
            onChange={setSelectedProjectId}
            options={projects.map(p => ({ value: p.id, label: `[${p.code}] ${p.name}` }))}
          />
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setModalOpen(true)}
            style={{ background: '#16A34A', borderColor: '#16A34A' }}
            disabled={!selectedProjectId}
          >
            Nhập chi phí thực tế (T-94)
          </Button>
        </Space>
      </div>

      {/* KPI Stats */}
      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col span={8}>
          <Card bordered={false} style={{ background: '#FEF2F2', border: '1px solid #FECACA', borderRadius: 8 }}>
            <Statistic
              title={<span style={{ color: '#991B1B', fontWeight: 600 }}>TỔNG CHI PHÍ THỰC TẾ LŨY KẾ</span>}
              value={totalActual}
              formatter={val => formatCurrency(val)}
              valueStyle={{ color: '#DC2626', fontWeight: 700 }}
              prefix={<DollarOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card bordered={false} style={{ background: '#EFF6FF', border: '1px solid #BFDBFE', borderRadius: 8 }}>
            <Statistic
              title={<span style={{ color: '#1E40AF', fontWeight: 600 }}>SỐ LƯỢNG KHOẢN CHI GHI NHẬN</span>}
              value={costs.length}
              valueStyle={{ color: '#2563EB', fontWeight: 700 }}
              prefix={<FileDoneOutlined />}
              suffix="khoản"
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card bordered={false} style={{ background: '#F0FDF4', border: '1px solid #BBF7D0', borderRadius: 8 }}>
            <Statistic
              title={<span style={{ color: '#166534', fontWeight: 600 }}>KHOẢN CHI GẦN NHẤT</span>}
              value={costs.length > 0 ? formatCurrency(costs[0]?.amount) : 'Chưa có'}
              valueStyle={{ color: '#16A34A', fontWeight: 700 }}
              prefix={<PieChartOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Filter Bar */}
      <Card
        style={{ marginBottom: 16, borderRadius: 8 }}
        styles={{ body: { padding: '12px 16px' } }}
      >
        <Space wrap>
          <FilterOutlined style={{ color: '#64748B' }} />
          <Text strong>Bộ lọc:</Text>

          <Select
            allowClear
            style={{ width: 280 }}
            placeholder="Lọc theo hạng mục WBS (T-95)..."
            value={filterWbsItemId}
            onChange={setFilterWbsItemId}
            options={wbsItems.map(item => ({
              value: item.id || item.key,
              label: `${item.wbsCode ? `[${item.wbsCode}] ` : ''}${item.name}`
            }))}
          />

          <Select
            style={{ width: 200 }}
            value={filterCostType}
            onChange={setFilterCostType}
            options={COST_TYPES}
          />

          <RangePicker
            format="DD/MM/YYYY"
            value={filterDateRange}
            onChange={setFilterDateRange}
            placeholder={['Từ ngày', 'Đến ngày']}
          />

          <Button icon={<ReloadOutlined />} onClick={loadCosts}>
            Làm mới
          </Button>
        </Space>
      </Card>

      {/* Main Table (T-96) */}
      <Card style={{ borderRadius: 8 }}>
        <Table
          loading={loading}
          dataSource={costs}
          columns={columns}
          rowKey="id"
          pagination={{ pageSize: 10, showTotal: total => `Tổng cộng ${total} khoản chi` }}
          locale={{ emptyText: <Empty description="Chưa có khoản chi phí thực tế nào" /> }}
        />
      </Card>

      {/* Modal nhập chi phí */}
      <ActualCostModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        projectId={selectedProjectId}
        wbsItems={wbsItems}
        onSuccess={loadCosts}
      />
    </div>
  );
};

export default ActualCostPage;
