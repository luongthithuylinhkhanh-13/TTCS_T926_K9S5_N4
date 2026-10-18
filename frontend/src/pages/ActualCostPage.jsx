import React, { useEffect, useState, useCallback } from 'react';
import {
  Card,
  Table,
  Button,
  Input,
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
import { getProjectBudgetTree } from '../services/budgetApi';
import { calculateActualCostVariance } from '../services/actualCostVariance';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const COST_TYPES = [
  { value: '', label: 'Táº¥t cáº£ loáº¡i chi phÃ­' },
  { value: 'Váº¬T_TÆ¯', label: 'Váº­t tÆ° / NguyÃªn váº­t liá»‡u' },
  { value: 'NHÃ‚N_CÃ”NG', label: 'NhÃ¢n cÃ´ng thi cÃ´ng' },
  { value: 'MÃY_THI_CÃ”NG', label: 'MÃ¡y thi cÃ´ng & Thiáº¿t bá»‹' },
  { value: 'QUáº¢N_LÃ', label: 'Chi phÃ­ quáº£n lÃ½ chung' },
  { value: 'KHÃC', label: 'Chi phÃ­ khÃ¡c' }
];

const COST_TYPE_TAGS = {
  'Váº¬T_TÆ¯': { color: 'blue', label: 'Váº­t tÆ°' },
  'NHÃ‚N_CÃ”NG': { color: 'green', label: 'NhÃ¢n cÃ´ng' },
  'MÃY_THI_CÃ”NG': { color: 'orange', label: 'MÃ¡y mÃ³c' },
  'QUáº¢N_LÃ': { color: 'purple', label: 'Quáº£n lÃ½' },
  'KHÃC': { color: 'default', label: 'KhÃ¡c' }
};

const ActualCostPage = () => {
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [wbsItems, setWbsItems] = useState([]);

  const [costs, setCosts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);

  const [budgetTree, setBudgetTree] = useState([]);
  const [allActualCosts, setAllActualCosts] = useState([]);
  const [varianceLoading, setVarianceLoading] = useState(false);
  // T-100: Filters and sorting for comparison table
  const [varianceSearch, setVarianceSearch] = useState('');
  const [varianceStatusFilter, setVarianceStatusFilter] = useState('ALL');
  const [varianceSort, setVarianceSort] = useState('name');
  const [varianceSortOrder, setVarianceSortOrder] = useState('ascend');

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
        message.error('KhÃ´ng thá»ƒ náº¡p danh sÃ¡ch dá»± Ã¡n');
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

  useEffect(() => {
   if (!selectedProjectId) {
    setBudgetTree([]);
    setAllActualCosts([]);
    setVarianceLoading(false);
    return;
  }

  let cancelled = false;

  const loadVarianceData = async () => {
    setVarianceLoading(true);

    try {
      const [treeData, actualData] = await Promise.all([
        getProjectBudgetTree(selectedProjectId),
        getActualCosts(selectedProjectId),
      ]);

      if (!cancelled) {
        setBudgetTree(Array.isArray(treeData) ? treeData : []);
        setAllActualCosts(Array.isArray(actualData) ? actualData : []);
      }
    } catch (error) {
      if (!cancelled) {
        message.error('KhÃ´ng thá»ƒ táº£i dá»¯ liá»‡u so sÃ¡nh dá»± toÃ¡n vÃ  thá»±c táº¿');
      }
    } finally {
      if (!cancelled) {
        setVarianceLoading(false);
      }
    }
  };

  loadVarianceData();

  return () => {
    cancelled = true;
  };
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
      message.error('Lá»—i khi táº£i lá»‹ch sá»­ chi phÃ­ thá»±c táº¿');
      setCosts([]);
    } finally {
      setLoading(false);
    }
  }, [selectedProjectId, filterWbsItemId, filterCostType, filterDateRange]);

  useEffect(() => {
    loadCosts();
  }, [loadCosts]);

  const totalActual = calculateTotalCost(costs);
  const variance = calculateActualCostVariance(budgetTree, allActualCosts);
  const varianceRows = [];

const flattenVarianceItems = (items, depth = 0) => {
  (Array.isArray(items) ? items : []).forEach((item) => {
    varianceRows.push({
      ...item,
      depth,
      key: item.id,
    });

    flattenVarianceItems(item.children, depth + 1);
  });
};


flattenVarianceItems(variance.items);

// T-100: Search, filter and sort comparison rows
const filteredVarianceRows = varianceRows
  .filter((item) => {
    const keyword = varianceSearch.trim().toLowerCase();

    const matchesSearch =
      !keyword ||
      String(item.name || '').toLowerCase().includes(keyword) ||
      String(item.code || '').toLowerCase().includes(keyword);

    const matchesStatus =
      varianceStatusFilter === 'ALL' ||
      item.status === varianceStatusFilter;

    return matchesSearch && matchesStatus;
  })
  .sort((a, b) => {
    let result = 0;

    switch (varianceSort) {
      case 'budget':
        result = a.budget - b.budget;
        break;
      case 'actual':
        result = a.actual - b.actual;
        break;
      case 'difference':
        result = a.difference - b.difference;
        break;
      case 'percentage':
        result = (a.percentage ?? 0) - (b.percentage ?? 0);
        break;
      case 'name':
      default:
        result = String(a.name || '').localeCompare(
          String(b.name || ''),
          'vi',
        );
        break;
    }

    return varianceSortOrder === 'descend' ? -result : result;
  });

const exportVarianceCsv = () => {
  const headers = [
    'Hang muc WBS',
    'Du toan',
    'Thuc te',
    'Chenh lech',
    'Chenh lech (%)',
    'Trang thai',
  ];

  const statusLabels = {
    OVER_BUDGET: 'Vuot du toan',
    UNDER_BUDGET: 'Tiet kiem',
    ON_BUDGET: 'Dung du toan',
  };

  const escapeCsv = (value) => {
    const text = String(value ?? '');
    return `"${text.replace(/"/g, '""')}"`;
  };

  const rows = filteredVarianceRows.map((item) => [
    item.name || '',
    item.budget,
    item.actual,
    item.difference,
    item.percentage == null ? '' : item.percentage,
    statusLabels[item.status] || 'Chua xac dinh',
  ]);

  const csvContent = [
    headers,
    ...rows,
  ]
    .map((row) => row.map(escapeCsv).join(','))
    .join('\r\n');

  // BOM helps Excel recognize UTF-8
  const blob = new Blob(
    ['\uFEFF', csvContent],
    { type: 'text/csv;charset=utf-8;' },
  );

  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'so-sanh-du-toan-thuc-te.csv';
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
 };


 const varianceColumns = [

  {
    title: 'Háº¡ng má»¥c WBS',
    dataIndex: 'name',
    key: 'name',
    render: (name, record) => (
      <span style={{ paddingLeft: record.depth * 20 }}>
        {name || 'ChÆ°a Ä‘áº·t tÃªn'}
      </span>
    ),
  },
  {
    title: 'Dá»± toÃ¡n',
    dataIndex: 'budget',
    key: 'budget',
    align: 'right',
    render: value => formatCurrency(value),
  },
  {
    title: 'Thá»±c táº¿',
    dataIndex: 'actual',
    key: 'actual',
    align: 'right',
    render: value => formatCurrency(value),
  },
  {
    title: 'ChÃªnh lá»‡ch',
    dataIndex: 'difference',
    key: 'difference',
    align: 'right',
    render: value => (
      <span style={{
        color: value > 0 ? '#DC2626' : value < 0 ? '#16A34A' : '#64748B',
        fontWeight: 600,
      }}>
        {value > 0 ? '+' : ''}{formatCurrency(value)}
      </span>
    ),
  },
  {
    title: 'ChÃªnh lá»‡ch (%)',
    dataIndex: 'percentage',
    key: 'percentage',
    align: 'right',
    render: value => value == null ? 'â€”' : `${value.toFixed(2)}%`,
  },
  {
    title: 'Tráº¡ng thÃ¡i',
    dataIndex: 'status',
    key: 'status',
    render: status => {
      const statusConfig = {
        OVER_BUDGET: { color: 'red', label: 'VÆ°á»£t dá»± toÃ¡n' },
        UNDER_BUDGET: { color: 'green', label: 'Tiáº¿t kiá»‡m' },
        ON_BUDGET: { color: 'blue', label: 'ÄÃºng dá»± toÃ¡n' },
      };
      const config = statusConfig[status] || {
        color: 'default',
        label: 'ChÆ°a xÃ¡c Ä‘á»‹nh',
      };

      return <Tag color={config.color}>{config.label}</Tag>;
    },
  },
];

  const columns = [
    {
      title: 'NgÃ y phÃ¡t sinh',
      dataIndex: 'incurredDate',
      key: 'incurredDate',
      width: 120,
      render: date => (date ? dayjs(date).format('DD/MM/YYYY') : '--'),
      sorter: (a, b) => dayjs(a.incurredDate).unix() - dayjs(b.incurredDate).unix()
    },
    {
      title: 'Ná»™i dung khoáº£n chi',
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
      title: 'Háº¡ng má»¥c liÃªn káº¿t (T-95)',
      key: 'wbsItem',
      render: (_, record) => {
        if (!record.wbsItemId) {
          return <Tag color="default">Chi phÃ­ chung dá»± Ã¡n</Tag>;
        }
        return (
          <Space orientation="vertical" size={2}>
            {record.wbsCode && <span className="wbs-badge" style={{ fontSize: 11 }}>{record.wbsCode}</span>}
            <span style={{ fontWeight: 500 }}>{record.wbsItemName || 'Háº¡ng má»¥c'}</span>
          </Space>
        );
      }
    },
    {
      title: 'PhÃ¢n loáº¡i',
      dataIndex: 'costType',
      key: 'costType',
      width: 130,
      render: type => {
        const conf = COST_TYPE_TAGS[type] || { color: 'default', label: type };
        return <Tag color={conf.color}>{conf.label}</Tag>;
      }
    },
    {
      title: 'Sá»‘ tiá»n thá»±c chi',
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
      title: 'NgÆ°á»i nháº­p',
      dataIndex: 'createdBy',
      key: 'createdBy',
      width: 130,
      render: user => user || 'Há»‡ thá»‘ng'
    }
  ];

  return (
    <div style={{ padding: '20px 24px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <Title level={3} style={{ margin: 0, color: '#0F172A' }}>
            <DollarOutlined style={{ color: '#16A34A', marginRight: 8 }} />
            Quáº£n lÃ½ & Lá»‹ch sá»­ Chi phÃ­ Thá»±c táº¿ (S-47)
          </Title>
          <Text type="secondary">
            Ghi nháº­n chi phÃ­ phÃ¡t sinh thá»±c táº¿ vÃ  liÃªn káº¿t vá»›i tá»«ng háº¡ng má»¥c cÃ´ng viá»‡c
          </Text>
        </div>

        <Space>
          <Select
            style={{ width: 260 }}
            placeholder="Chá»n dá»± Ã¡n..."
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
            Nháº­p chi phÃ­ thá»±c táº¿ (T-94)
          </Button>
        </Space>
      </div>

      {/* KPI Stats */}
      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col span={8}>
          <Card bordered={false} style={{ background: '#FEF2F2', border: '1px solid #FECACA', borderRadius: 8 }}>
            <Statistic
              title={<span style={{ color: '#991B1B', fontWeight: 600 }}>Tá»”NG CHI PHÃ THá»°C Táº¾ LÅ¨Y Káº¾</span>}
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
              title={<span style={{ color: '#1E40AF', fontWeight: 600 }}>Sá» LÆ¯á»¢NG KHOáº¢N CHI GHI NHáº¬N</span>}
              value={costs.length}
              valueStyle={{ color: '#2563EB', fontWeight: 700 }}
              prefix={<FileDoneOutlined />}
              suffix="khoáº£n"
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card bordered={false} style={{ background: '#F0FDF4', border: '1px solid #BBF7D0', borderRadius: 8 }}>
            <Statistic
              title={<span style={{ color: '#166534', fontWeight: 600 }}>KHOáº¢N CHI Gáº¦N NHáº¤T</span>}
              value={costs.length > 0 ? formatCurrency(costs[0]?.amount) : 'ChÆ°a cÃ³'}
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
          <Text strong>Bá»™ lá»c:</Text>

          <Select
            allowClear
            style={{ width: 280 }}
            placeholder="Lá»c theo háº¡ng má»¥c WBS (T-95)..."
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
            placeholder={['Tá»« ngÃ y', 'Äáº¿n ngÃ y']}
          />

          <Button icon={<ReloadOutlined />} onClick={loadCosts}>
            LÃ m má»›i
          </Button>
        </Space>
      </Card>

            {/* T-99: So sÃ¡nh dá»± toÃ¡n vÃ  chi phÃ­ thá»±c táº¿ */}
      <Card
        title="So sÃ¡nh dá»± toÃ¡n vÃ  thá»±c táº¿ (T-99)"
        style={{ marginBottom: 20, borderRadius: 8 }}
      >
        <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
          <Col xs={24} md={8}>
            <Statistic
              title="Tá»•ng dá»± toÃ¡n"
              value={variance.budget}
              formatter={value => formatCurrency(value)}
            />
          </Col>

          <Col xs={24} md={8}>
            <Statistic
              title="Tá»•ng chi phÃ­ thá»±c táº¿"
              value={variance.actual}
              formatter={value => formatCurrency(value)}
            />
          </Col>

          <Col xs={24} md={8}>
            <Statistic
              title="ChÃªnh lá»‡ch"
              value={variance.difference}
              formatter={value =>
                `${Number(value) > 0 ? '+' : ''}${formatCurrency(value)}`
              }
              valueStyle={{
                color: variance.difference > 0
                  ? '#DC2626'
                  : variance.difference < 0
                    ? '#16A34A'
                    : '#64748B',
              }}
            />
          </Col>
        </Row>

        {/* T-100: Comparison table controls */}

        {/* Hàng hiển thị số lượng và xuất CSV */}
        <Row
          gutter={[12, 12]}
          align="middle"
          justify="space-between"
          style={{ marginBottom: 16 }}
        >
          <Col xs={24} md={16}>
            <Typography.Text type="secondary">
              Hiển thị {filteredVarianceRows.length} hạng mục
            </Typography.Text>
          </Col>

          <Col xs={24} md={8} style={{ textAlign: 'right' }}>
            <Button
              onClick={exportVarianceCsv}
              disabled={filteredVarianceRows.length === 0}
            >
              Xuất CSV
            </Button>
          </Col>
        </Row>

        {/* Hàng tìm kiếm, lọc và sắp xếp */}
        <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
          <Col xs={24} md={8}>
            <Input
              allowClear
              placeholder="Tìm theo tên hoặc mã hạng mục WBS"
              value={varianceSearch}
              onChange={(e) => setVarianceSearch(e.target.value)}
            />
          </Col>

          <Col xs={24} md={8}>
            <Select
              style={{ width: '100%' }}
              value={varianceStatusFilter}
              onChange={setVarianceStatusFilter}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: 'OVER_BUDGET', label: 'Vượt dự toán' },
                { value: 'UNDER_BUDGET', label: 'Tiết kiệm' },
                { value: 'ON_BUDGET', label: 'Đúng dự toán' },
              ]}
            />
          </Col>

          <Col xs={24} md={5}>
            <Select
              style={{ width: '100%' }}
              value={varianceSort}
              onChange={setVarianceSort}
              options={[
                { value: 'name', label: 'Tên hạng mục' },
                { value: 'budget', label: 'Số tiền dự toán' },
                { value: 'actual', label: 'Số tiền thực tế' },
                { value: 'difference', label: 'Số tiền chênh lệch' },
                { value: 'percentage', label: 'Chênh lệch (%)' },
              ]}
            />
          </Col>

          <Col xs={24} md={3}>
            <Select
              style={{ width: '100%' }}
              value={varianceSortOrder}
              onChange={setVarianceSortOrder}
              options={[
                { value: 'ascend', label: 'Tăng dần' },
                { value: 'descend', label: 'Giảm dần' },
              ]}
            />
          </Col>
        </Row>


        <Table
          rowKey="key"
          columns={varianceColumns}
          dataSource={filteredVarianceRows}
          loading={varianceLoading}
          pagination={false}
          size="middle"
          locale={{
            emptyText: 'Chưa có dữ liệu dự toán để so sánh',
          }}
          scroll={{ x: 900 }}
        />
      </Card>
      {/* Main Table (T-96) */}
      <Card style={{ borderRadius: 8 }}>
        <Table
          loading={loading}
          dataSource={costs}
          columns={columns}
          rowKey="id"
          pagination={{ pageSize: 10, showTotal: total => `Tá»•ng cá»™ng ${total} khoáº£n chi` }}
          locale={{ emptyText: <Empty description="ChÆ°a cÃ³ khoáº£n chi phÃ­ thá»±c táº¿ nÃ o" /> }}
        />
      </Card>

      {/* Modal nháº­p chi phÃ­ */}
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
