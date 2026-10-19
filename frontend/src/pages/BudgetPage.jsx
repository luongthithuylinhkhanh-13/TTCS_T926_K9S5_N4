import React, { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Table,
  Card,
  Row,
  Col,
  Button,
  Input,
  InputNumber,
  Select,
  Tag,
  Modal,
  Form,
  Space,
  Typography,
  Tooltip,
  Progress,
  Divider,
  Alert,
  Spin,
  notification
} from 'antd';
import {
  CalculatorOutlined,
  DollarOutlined,
  CheckCircleOutlined,
  ReloadOutlined,
  EditOutlined,
  DownloadOutlined,
  InfoCircleOutlined,
  FolderOutlined,
  FileTextOutlined,
  SaveOutlined,
  AppstoreOutlined,
  SearchOutlined,
  CheckSquareOutlined
} from '@ant-design/icons';
import { getProjects } from '../services/wbsApi';
import {
  getProjectBudgetTree,
  getBudgetSummary,
  updateBudgetItem,
  batchUpdateBudget
} from '../services/budgetApi';
import {
  validateBudgetItem,
  calculateItemTotalPrice,
  exportBudgetToCsv
} from '../utils/budgetUtils';
import { useLocale } from '../utils/LocaleContext';

const { Title, Text } = Typography;

const COMMON_UNITS = [
  { value: 'm3', label: 'm3 (Mét khối)' },
  { value: 'm2', label: 'm2 (Mét vuông)' },
  { value: 'm', label: 'm (Mét dài)' },
  { value: 'tấn', label: 'tấn' },
  { value: 'kg', label: 'kg' },
  { value: 'cái', label: 'cái' },
  { value: 'bộ', label: 'bộ' },
  { value: 'hố', label: 'hố' },
  { value: 'gói', label: 'gói' },
  { value: 'cột', label: 'cột' },
  { value: 'cọc', label: 'cọc' }
];

const BudgetPage = () => {
  const { t } = useLocale();
  const [searchParams, setSearchParams] = useSearchParams();

  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [budgetTree, setBudgetTree] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // Filters & search
  const [searchText, setSearchText] = useState('');
  const [filterType, setFilterType] = useState('ALL'); // ALL, LEAF, PARENT, UNBUDGETED
  const [expandedKeys, setExpandedKeys] = useState([]);

  // Modal editing
  const [editingItem, setEditingItem] = useState(null);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [form] = Form.useForm();

  // Quick edit mode (Batch edit)
  const [isQuickEditMode, setIsQuickEditMode] = useState(false);
  const [quickEdits, setQuickEdits] = useState({}); // { [itemId]: { quantity, unit, unitPrice } }

  // Load project list
  useEffect(() => {
    const loadProjectsList = async () => {
      try {
        const list = await getProjects();
        setProjects(list || []);

        const requestedId = searchParams.get('projectId');
        if (requestedId && list.some(p => p.id === requestedId)) {
          setSelectedProjectId(requestedId);
        } else if (list.length > 0) {
          setSelectedProjectId(list[0].id);
        }
      } catch (err) {
        notification.error({
          message: 'Lỗi tải dự án',
          description: err.message || 'Không thể lấy danh sách dự án.'
        });
      }
    };
    loadProjectsList();
  }, []);

  // Sync selected project with URL search param
  useEffect(() => {
    if (selectedProjectId) {
      setSearchParams({ projectId: selectedProjectId });
      loadBudgetData(selectedProjectId);
    }
  }, [selectedProjectId]);

  const loadBudgetData = async (projectId) => {
    if (!projectId) return;
    setLoading(true);
    try {
      const [treeData, summaryData] = await Promise.all([
        getProjectBudgetTree(projectId),
        getBudgetSummary(projectId)
      ]);
      setBudgetTree(treeData || []);
      setSummary(summaryData || null);

      // Auto expand root keys
      const rootKeys = (treeData || []).map(item => item.id);
      setExpandedKeys(rootKeys);
      setQuickEdits({});
    } catch (err) {
      notification.error({
        message: 'Lỗi nạp dự toán',
        description: err.message || 'Không thể tải cấu trúc dự toán dự án.'
      });
    } finally {
      setLoading(false);
    }
  };

  // Open modal for a leaf item
  const handleOpenEditModal = (item) => {
    if (!item.isLeaf) {
      notification.warning({
        message: 'Không thể nhập cho hạng mục cha',
        description: 'Hạng mục cha tự động cộng dồn từ các hạng mục con.'
      });
      return;
    }
    setEditingItem(item);
    form.setFieldsValue({
      quantity: item.quantity ?? null,
      unit: item.unit ?? undefined,
      unitPrice: item.unitPrice ?? null
    });
    setIsModalVisible(true);
  };

  // Submit modal form
  const handleModalSave = async () => {
    try {
      const values = await form.validateFields();
      const validation = validateBudgetItem(values.quantity, values.unit, values.unitPrice);
      if (!validation.valid) {
        notification.error({ message: 'Dữ liệu không hợp lệ', description: validation.error });
        return;
      }

      setSaving(true);
      await updateBudgetItem(selectedProjectId, editingItem.id, values);
      notification.success({
        message: 'Lưu dự toán thành công',
        description: `Đã cập nhật dự toán cho hạng mục "${editingItem.name}".`
      });
      setIsModalVisible(false);
      await loadBudgetData(selectedProjectId);
    } catch (err) {
      if (err.errorFields) return; // Antd validation
      notification.error({
        message: 'Không thể lưu dự toán',
        description: err.response?.data?.message || err.message || 'Có lỗi xảy ra khi cập nhật.'
      });
    } finally {
      setSaving(false);
    }
  };

  // Quick edit value changes
  const handleQuickEditChange = (itemId, field, val) => {
    setQuickEdits(prev => {
      const current = prev[itemId] || {};
      return {
        ...prev,
        [itemId]: {
          ...current,
          [field]: val
        }
      };
    });
  };

  // Batch save quick edits
  const handleSaveQuickEdits = async () => {
    const editEntries = Object.entries(quickEdits);
    if (editEntries.length === 0) {
      notification.info({ message: 'Không có thay đổi nào cần lưu.' });
      return;
    }

    // Validate all items
    for (const [id, vals] of editEntries) {
      const validation = validateBudgetItem(vals.quantity, vals.unit, vals.unitPrice);
      if (!validation.valid) {
        notification.error({
          message: 'Dữ liệu không hợp lệ',
          description: `Mục ID ${id}: ${validation.error}`
        });
        return;
      }
    }

    setSaving(true);
    try {
      const itemsToUpdate = editEntries.map(([id, vals]) => ({
        itemId: id,
        quantity: vals.quantity,
        unit: vals.unit,
        unitPrice: vals.unitPrice
      }));

      await batchUpdateBudget(selectedProjectId, itemsToUpdate);
      notification.success({
        message: 'Lưu toàn bộ dự toán thành công',
        description: `Đã cập nhật đồng loạt ${itemsToUpdate.length} hạng mục.`
      });
      setIsQuickEditMode(false);
      await loadBudgetData(selectedProjectId);
    } catch (err) {
      notification.error({
        message: 'Không thể lưu dự toán hàng loạt',
        description: err.response?.data?.message || err.message || 'Có lỗi xảy ra.'
      });
    } finally {
      setSaving(false);
    }
  };

  // Export CSV
  const handleExportCsv = () => {
    const currentProject = projects.find(p => p.id === selectedProjectId);
    const projectName = currentProject ? currentProject.name : 'Du_An';
    exportBudgetToCsv(budgetTree, projectName);
    notification.success({
      message: 'Xuất file thành công',
      description: 'Bảng dự toán chi tiết đã được xuất ra định dạng CSV.'
    });
  };

  // Expand / collapse all keys
  const handleToggleExpandAll = () => {
    const collectAllKeys = (items) => {
      let keys = [];
      items.forEach(node => {
        if (node.children && node.children.length > 0) {
          keys.push(node.id);
          keys = keys.concat(collectAllKeys(node.children));
        }
      });
      return keys;
    };

    if (expandedKeys.length > 0) {
      setExpandedKeys([]);
    } else {
      setExpandedKeys(collectAllKeys(budgetTree));
    }
  };

  // Filter tree data by search text and filter type
  const filteredTreeData = useMemo(() => {
    const query = searchText.trim().toLowerCase();

    const filterNode = (node) => {
      const matchesQuery = !query ||
        (node.wbsCode && node.wbsCode.toLowerCase().includes(query)) ||
        (node.name && node.name.toLowerCase().includes(query));

      let matchesType = true;
      if (filterType === 'LEAF') matchesType = node.isLeaf;
      else if (filterType === 'PARENT') matchesType = !node.isLeaf;
      else if (filterType === 'UNBUDGETED') matchesType = node.isLeaf && (!node.totalPrice || node.totalPrice === 0);

      let filteredChildren = [];
      if (node.children && node.children.length > 0) {
        filteredChildren = node.children.map(filterNode).filter(Boolean);
      }

      if ((matchesQuery && matchesType) || filteredChildren.length > 0) {
        return {
          ...node,
          children: filteredChildren.length > 0 ? filteredChildren : undefined
        };
      }
      return null;
    };

    if (!searchText && filterType === 'ALL') return budgetTree;
    return budgetTree.map(filterNode).filter(Boolean);
  }, [budgetTree, searchText, filterType]);

  // Current selected project info
  const currentProject = useMemo(() => {
    return projects.find(p => p.id === selectedProjectId) || null;
  }, [projects, selectedProjectId]);

  // Table columns definition
  const columns = [
    {
      title: 'Mã WBS',
      dataIndex: 'wbsCode',
      key: 'wbsCode',
      width: 140,
      render: (code) => (
        <span style={{ fontWeight: 600, fontFamily: 'monospace', color: '#0A3D62' }}>
          {code}
        </span>
      )
    },
    {
      title: 'Hạng mục công việc',
      dataIndex: 'name',
      key: 'name',
      render: (name, record) => {
        const isParent = !record.isLeaf;
        return (
          <Space>
            {isParent ? (
              <FolderOutlined style={{ color: '#0284C7', fontSize: 16 }} />
            ) : (
              <FileTextOutlined style={{ color: '#10B981', fontSize: 15 }} />
            )}
            <span style={{ fontWeight: isParent ? 700 : 500, color: isParent ? '#0F172A' : '#334155' }}>
              {name}
            </span>
            {isParent && (
              <Tag color="cyan" style={{ fontSize: 11, marginLeft: 6 }}>
                {record.childCount || (record.children ? record.children.length : 0)} mục con
              </Tag>
            )}
          </Space>
        );
      }
    },
    {
      title: 'Phân loại',
      key: 'itemType',
      width: 150,
      render: (_, record) => {
        if (!record.isLeaf) {
          return (
            <Tag color="blue" icon={<AppstoreOutlined />}>
              Cộng dồn tự động
            </Tag>
          );
        }
        return (
          <Tag color="green" icon={<CheckSquareOutlined />}>
            Hạng mục lá
          </Tag>
        );
      }
    },
    {
      title: 'Khối lượng',
      key: 'quantity',
      width: 130,
      render: (_, record) => {
        if (!record.isLeaf) {
          return <span style={{ color: '#94A3B8' }}>—</span>;
        }

        if (isQuickEditMode) {
          const currentVal = quickEdits[record.id]?.quantity ?? record.quantity;
          return (
            <InputNumber
              size="small"
              min={0}
              step={0.1}
              value={currentVal}
              onChange={val => handleQuickEditChange(record.id, 'quantity', val)}
              style={{ width: '100%' }}
              placeholder="0"
            />
          );
        }

        return (
          <span style={{ fontWeight: 600 }}>
            {record.quantity != null ? record.quantity.toLocaleString('vi-VN') : '—'}
          </span>
        );
      }
    },
    {
      title: 'ĐVT',
      key: 'unit',
      width: 100,
      render: (_, record) => {
        if (!record.isLeaf) {
          return <span style={{ color: '#94A3B8' }}>—</span>;
        }

        if (isQuickEditMode) {
          const currentVal = quickEdits[record.id]?.unit ?? record.unit;
          return (
            <Input
              size="small"
              value={currentVal}
              onChange={e => handleQuickEditChange(record.id, 'unit', e.target.value)}
              placeholder="ĐVT"
              maxLength={50}
            />
          );
        }

        return record.unit ? <Tag color="default">{record.unit}</Tag> : <span style={{ color: '#94A3B8' }}>—</span>;
      }
    },
    {
      title: 'Đơn giá (VNĐ)',
      key: 'unitPrice',
      width: 150,
      render: (_, record) => {
        if (!record.isLeaf) {
          return <span style={{ color: '#94A3B8' }}>—</span>;
        }

        if (isQuickEditMode) {
          const currentVal = quickEdits[record.id]?.unitPrice ?? record.unitPrice;
          return (
            <InputNumber
              size="small"
              min={0}
              value={currentVal}
              formatter={v => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={v => v.replace(/\$\s?|(,*)/g, '')}
              onChange={val => handleQuickEditChange(record.id, 'unitPrice', val)}
              style={{ width: '100%' }}
              placeholder="0"
            />
          );
        }

        return (
          <span style={{ color: '#334155' }}>
            {record.unitPrice != null ? `${record.unitPrice.toLocaleString('vi-VN')} đ` : '—'}
          </span>
        );
      }
    },
    {
      title: 'Thành tiền dự toán',
      key: 'totalPrice',
      width: 180,
      align: 'right',
      render: (_, record) => {
        const isParent = !record.isLeaf;
        let amount = record.totalPrice ?? 0;

        // If in quick edit mode for leaf item, calculate preview live
        if (isQuickEditMode && record.isLeaf && quickEdits[record.id]) {
          const q = quickEdits[record.id].quantity ?? record.quantity;
          const p = quickEdits[record.id].unitPrice ?? record.unitPrice;
          amount = calculateItemTotalPrice(q, p);
        }

        if (isParent) {
          return (
            <Tooltip title="Tổng tiền cộng dồn tự động từ tất cả các hạng mục con bên dưới">
              <span style={{ fontWeight: 800, color: '#0369A1', fontSize: 14 }}>
                {amount.toLocaleString('vi-VN')} đ
              </span>
            </Tooltip>
          );
        }

        return (
          <span
            style={{
              fontWeight: 700,
              color: amount > 0 ? '#047857' : '#94A3B8',
              fontSize: 13
            }}
          >
            {amount > 0 ? `${amount.toLocaleString('vi-VN')} đ` : '0 đ'}
          </span>
        );
      }
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 130,
      align: 'center',
      render: (_, record) => {
        if (!record.isLeaf) {
          return (
            <Tooltip title="Hạng mục cha tự động cộng dồn từ các hạng mục con, không thể nhập trực tiếp.">
              <Button size="small" disabled icon={<InfoCircleOutlined />}>
                Cộng dồn
              </Button>
            </Tooltip>
          );
        }

        return (
          <Button
            size="small"
            type="primary"
            icon={<EditOutlined />}
            onClick={() => handleOpenEditModal(record)}
            style={{ background: '#0F766E', borderColor: '#0F766E' }}
          >
            Nhập dự toán
          </Button>
        );
      }
    }
  ];

  return (
    <div style={{ padding: '24px', background: '#F8FAFC', minHeight: '100vh' }}>
      {/* Top Header Card */}
      <Card
        style={{
          borderRadius: 12,
          marginBottom: 20,
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
          background: 'linear-gradient(135deg, #0A3D62 0%, #1E4E79 100%)',
          color: '#FFFFFF'
        }}
        bodyStyle={{ padding: '20px 24px' }}
      >
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col xs={24} md={14}>
            <Space align="center" size={12}>
              <div
                style={{
                  background: 'rgba(255, 255, 255, 0.15)',
                  padding: 10,
                  borderRadius: 10,
                  display: 'flex',
                  alignItems: 'center'
                }}
              >
                <CalculatorOutlined style={{ fontSize: 28, color: '#FCD34D' }} />
              </div>
              <div>
                <h2 style={{ margin: 0, color: '#FFFFFF', fontWeight: 700, fontSize: 22 }}>
                  Dự toán công trình (S-46)
                </h2>
                <p style={{ margin: '4px 0 0', color: '#E2E8F0', fontSize: 13 }}>
                  Khai báo dự toán chi tiết theo hạng mục lá, tự động cộng dồn lên các cấp hạng mục cha và toàn dự án.
                </p>
              </div>
            </Space>
          </Col>

          <Col xs={24} md={10} style={{ textAlign: 'right' }}>
            <Space wrap>
              <Select
                value={selectedProjectId}
                onChange={setSelectedProjectId}
                style={{ minWidth: 260 }}
                placeholder="Chọn dự án điều hành"
                options={projects.map(p => ({
                  value: p.id,
                  label: `${p.code} - ${p.name}`
                }))}
              />
              <Button
                icon={<ReloadOutlined />}
                onClick={() => loadBudgetData(selectedProjectId)}
                loading={loading}
              >
                Làm mới
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Hero Summary Cards (T-91) */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card
            bordered={false}
            style={{
              borderRadius: 12,
              background: '#FFFFFF',
              boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
              borderTop: '4px solid #F59E0B'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <Text type="secondary" style={{ fontSize: 13, fontWeight: 500 }}>
                  TỔNG DỰ TOÁN CÔNG TRÌNH
                </Text>
                <div style={{ fontSize: 22, fontWeight: 800, color: '#B45309', marginTop: 6 }}>
                  {(summary?.totalBudget ?? 0).toLocaleString('vi-VN')} VNĐ
                </div>
              </div>
              <div style={{ background: '#FEF3C7', padding: 8, borderRadius: 8 }}>
                <DollarOutlined style={{ fontSize: 22, color: '#D97706' }} />
              </div>
            </div>
            <div style={{ marginTop: 12, fontSize: 12, color: '#64748B' }}>
              Tự động cộng dồn từ toàn bộ cây công việc
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card
            bordered={false}
            style={{
              borderRadius: 12,
              background: '#FFFFFF',
              boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
              borderTop: '4px solid #10B981'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <Text type="secondary" style={{ fontSize: 13, fontWeight: 500 }}>
                  HẠNG MỤC ĐÃ CÓ DỰ TOÁN
                </Text>
                <div style={{ fontSize: 22, fontWeight: 800, color: '#047857', marginTop: 6 }}>
                  {summary?.budgetedLeafItems ?? 0} / {summary?.totalLeafItems ?? 0}
                </div>
              </div>
              <div style={{ background: '#D1FAE5', padding: 8, borderRadius: 8 }}>
                <CheckCircleOutlined style={{ fontSize: 22, color: '#059669' }} />
              </div>
            </div>
            <Progress
              percent={summary?.completionRate ?? 0}
              strokeColor="#10B981"
              size="small"
              style={{ marginTop: 12 }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card
            bordered={false}
            style={{
              borderRadius: 12,
              background: '#FFFFFF',
              boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
              borderTop: '4px solid #3B82F6'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <Text type="secondary" style={{ fontSize: 13, fontWeight: 500 }}>
                  NHÓM HẠNG MỤC CỘNG DỒN
                </Text>
                <div style={{ fontSize: 22, fontWeight: 800, color: '#1D4ED8', marginTop: 6 }}>
                  {summary?.totalParentItems ?? 0} nhóm
                </div>
              </div>
              <div style={{ background: '#DBEAFE', padding: 8, borderRadius: 8 }}>
                <AppstoreOutlined style={{ fontSize: 22, color: '#2563EB' }} />
              </div>
            </div>
            <div style={{ marginTop: 12, fontSize: 12, color: '#64748B' }}>
              Không thể nhập trực tiếp, tự tổng hợp từ nhánh con
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card
            bordered={false}
            style={{
              borderRadius: 12,
              background: '#FFFFFF',
              boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
              borderTop: '4px solid #8B5CF6'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <Text type="secondary" style={{ fontSize: 13, fontWeight: 500 }}>
                  TỔNG SỐ HẠNG MỤC WBS
                </Text>
                <div style={{ fontSize: 22, fontWeight: 800, color: '#6D28D9', marginTop: 6 }}>
                  {summary?.totalItems ?? 0} hạng mục
                </div>
              </div>
              <div style={{ background: '#EDE9FE', padding: 8, borderRadius: 8 }}>
                <CalculatorOutlined style={{ fontSize: 22, color: '#7C3AED' }} />
              </div>
            </div>
            <div style={{ marginTop: 12, fontSize: 12, color: '#64748B' }}>
              Tỷ lệ hoàn thành lập dự toán: <b>{summary?.completionRate ?? 0}%</b>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Main Budget Tree Table Card */}
      <Card
        style={{
          borderRadius: 12,
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
        }}
        bodyStyle={{ padding: '20px' }}
      >
        {/* Table Toolbar */}
        <Row justify="space-between" align="middle" style={{ marginBottom: 16 }} gutter={[12, 12]}>
          <Col xs={24} md={12}>
            <Space wrap>
              <Input
                placeholder="Tìm theo mã WBS hoặc tên công việc..."
                prefix={<SearchOutlined style={{ color: '#94A3B8' }} />}
                value={searchText}
                onChange={e => setSearchText(e.target.value)}
                allowClear
                style={{ width: 280 }}
              />
              <Select
                value={filterType}
                onChange={setFilterType}
                style={{ width: 200 }}
                options={[
                  { value: 'ALL', label: 'Tất cả hạng mục' },
                  { value: 'LEAF', label: 'Chỉ hạng mục lá (Nhập dự toán)' },
                  { value: 'PARENT', label: 'Chỉ hạng mục cha (Cộng dồn)' },
                  { value: 'UNBUDGETED', label: 'Chưa có dự toán' }
                ]}
              />
            </Space>
          </Col>

          <Col xs={24} md={12} style={{ textAlign: 'right' }}>
            <Space wrap>
              <Button onClick={handleToggleExpandAll}>
                {expandedKeys.length > 0 ? 'Thu gọn tất cả' : 'Mở rộng tất cả'}
              </Button>

              {isQuickEditMode ? (
                <>
                  <Button
                    type="primary"
                    icon={<SaveOutlined />}
                    onClick={handleSaveQuickEdits}
                    loading={saving}
                    style={{ background: '#059669', borderColor: '#059669' }}
                  >
                    Lưu toàn bộ thay đổi ({Object.keys(quickEdits).length})
                  </Button>
                  <Button onClick={() => { setIsQuickEditMode(false); setQuickEdits({}); }}>
                    Thoát chế độ nhanh
                  </Button>
                </>
              ) : (
                <Button
                  icon={<EditOutlined />}
                  onClick={() => setIsQuickEditMode(true)}
                >
                  Nhập nhanh trên bảng
                </Button>
              )}

              <Button
                icon={<DownloadOutlined />}
                onClick={handleExportCsv}
                disabled={budgetTree.length === 0}
              >
                Xuất CSV
              </Button>
            </Space>
          </Col>
        </Row>

        {isQuickEditMode && (
          <Alert
            message="Chế độ nhập nhanh trên bảng đang bật"
            description="Bạn có thể nhập trực tiếp khối lượng, đơn vị tính và đơn giá vào các ô của hạng mục lá. Nhấn 'Lưu toàn bộ thay đổi' để cập nhật lên máy chủ và tính toán lại giá trị cộng dồn."
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
            closable
          />
        )}

        {/* Tree Table */}
        <Table
          columns={columns}
          dataSource={filteredTreeData}
          rowKey="id"
          loading={loading}
          expandable={{
            expandedRowKeys: expandedKeys,
            onExpandedRowsChange: setExpandedKeys
          }}
          pagination={false}
          scroll={{ x: 1100 }}
          bordered
          rowClassName={(record) => (!record.isLeaf ? 'parent-budget-row' : 'leaf-budget-row')}
        />
      </Card>

      {/* Modal nhập dự toán cho hạng mục lá (T-92) */}
      <Modal
        title={
          <Space>
            <CalculatorOutlined style={{ color: '#0F766E' }} />
            <span>Khai báo dự toán: {editingItem?.name}</span>
          </Space>
        }
        open={isModalVisible}
        onCancel={() => setIsModalVisible(false)}
        onOk={handleModalSave}
        confirmLoading={saving}
        okText="Lưu dự toán"
        cancelText="Hủy"
        destroyOnClose
        width={560}
      >
        <div style={{ background: '#F1F5F9', padding: '12px 16px', borderRadius: 8, marginBottom: 20 }}>
          <div style={{ fontSize: 13, color: '#64748B' }}>Mã WBS: <b>{editingItem?.wbsCode}</b></div>
          <div style={{ fontSize: 14, fontWeight: 600, color: '#1E293B', marginTop: 4 }}>
            {editingItem?.name}
          </div>
          <Tag color="green" style={{ marginTop: 8 }}>
            Hạng mục lá (Cho phép nhập dự toán)
          </Tag>
        </div>

        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="Khối lượng dự toán"
                name="quantity"
                rules={[
                  {
                    type: 'number',
                    min: 0,
                    message: 'Khối lượng phải lớn hơn hoặc bằng 0'
                  }
                ]}
              >
                <InputNumber
                  min={0}
                  step={0.1}
                  precision={2}
                  style={{ width: '100%' }}
                  placeholder="Ví dụ: 150.5"
                />
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item
                label="Đơn vị tính (ĐVT)"
                name="unit"
                rules={[
                  { max: 50, message: 'Đơn vị tính không quá 50 ký tự' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      const qty = getFieldValue('quantity');
                      if (qty != null && qty > 0 && (!value || !value.trim())) {
                        return Promise.reject(new Error('Vui lòng nhập đơn vị tính khi đã có khối lượng'));
                      }
                      return Promise.resolve();
                    }
                  })
                ]}
              >
                <Select
                  showSearch
                  allowClear
                  placeholder="Chọn hoặc nhập ĐVT"
                  options={COMMON_UNITS}
                  dropdownRender={(menu) => (
                    <>
                      {menu}
                      <Divider style={{ margin: '8px 0' }} />
                      <div style={{ padding: '0 8px 4px', fontSize: 12, color: '#94A3B8' }}>
                        Hoặc nhập ĐVT tùy chọn
                      </div>
                    </>
                  )}
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="Đơn giá dự toán (VNĐ)"
            name="unitPrice"
            rules={[
              {
                type: 'number',
                min: 0,
                message: 'Đơn giá phải lớn hơn hoặc bằng 0'
              }
            ]}
          >
            <InputNumber
              min={0}
              style={{ width: '100%' }}
              formatter={v => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={v => v.replace(/\$\s?|(,*)/g, '')}
              placeholder="Ví dụ: 1,250,000"
            />
          </Form.Item>

          {/* Real-time calculated live preview */}
          <Form.Item
            noStyle
            shouldUpdate={(prev, cur) =>
              prev.quantity !== cur.quantity || prev.unitPrice !== cur.unitPrice
            }
          >
            {({ getFieldValue }) => {
              const qty = Number(getFieldValue('quantity') || 0);
              const price = Number(getFieldValue('unitPrice') || 0);
              const total = calculateItemTotalPrice(qty, price);

              return (
                <div
                  style={{
                    background: '#ECFDF5',
                    border: '1px solid #A7F3D0',
                    borderRadius: 8,
                    padding: '12px 16px',
                    marginTop: 8
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: 13, color: '#065F46', fontWeight: 600 }}>
                      Thành tiền dự toán ước tính:
                    </span>
                    <span style={{ fontSize: 17, fontWeight: 800, color: '#047857' }}>
                      {total.toLocaleString('vi-VN')} VNĐ
                    </span>
                  </div>
                  <div style={{ fontSize: 12, color: '#047857', marginTop: 4 }}>
                    Công thức: {qty.toLocaleString('vi-VN')} x {price.toLocaleString('vi-VN')} đ = {total.toLocaleString('vi-VN')} đ
                  </div>
                </div>
              );
            }}
          </Form.Item>
        </Form>
      </Modal>

      <style>{`
        .parent-budget-row {
          background-color: #F8FAFC;
        }
        .parent-budget-row:hover > td {
          background-color: #F1F5F9 !important;
        }
        .leaf-budget-row:hover > td {
          background-color: #F0FDF4 !important;
        }
      `}</style>
    </div>
  );
};

export default BudgetPage;
