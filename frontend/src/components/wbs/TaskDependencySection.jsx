import React, { useEffect, useState, useMemo } from 'react';
import {
  Table,
  Button,
  Select,
  InputNumber,
  Space,
  Tag,
  Popconfirm,
  message,
  Typography,
  Card,
  Empty
} from 'antd';
import {
  LinkOutlined,
  PlusOutlined,
  DeleteOutlined,
  ArrowRightOutlined
} from '@ant-design/icons';
import {
  getTaskDependencies,
  addTaskDependency,
  deleteTaskDependency
} from '../../services/taskDependencyApi';

const { Text } = Typography;

const DEPENDENCY_TYPE_LABELS = {
  FS: { label: 'FS (Kết thúc - Bắt đầu)', color: 'blue' },
  SS: { label: 'SS (Bắt đầu - Bắt đầu)', color: 'green' },
  FF: { label: 'FF (Kết thúc - Kết thúc)', color: 'purple' },
  SF: { label: 'SF (Bắt đầu - Kết thúc)', color: 'orange' }
};

const TaskDependencySection = ({ taskId, allTasks = [] }) => {
  const [dependencies, setDependencies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [adding, setAdding] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Form state
  const [selectedPredecessor, setSelectedPredecessor] = useState(null);
  const [selectedType, setSelectedType] = useState('FS');
  const [lagDays, setLagDays] = useState(0);

  // Map taskId -> task info để tra cứu tên nhanh
  const taskMap = useMemo(() => {
    const map = new Map();
    allTasks.forEach((t) => {
      const id = t.id || t.key;
      if (id) {
        map.set(id, t);
      }
    });
    return map;
  }, [allTasks]);

  // Danh sách công việc có thể chọn làm predecessor (loại trừ chính nó và những cái đã liên kết)
  const availablePredecessors = useMemo(() => {
    const existingPredIds = new Set(dependencies.map((d) => d.predecessorId));
    return allTasks.filter((t) => {
      const id = t.id || t.key;
      return id !== taskId && !existingPredIds.has(id);
    });
  }, [allTasks, taskId, dependencies]);

  const loadDependencies = async () => {
    if (!taskId) return;
    setLoading(true);
    try {
      const data = await getTaskDependencies(taskId);
      setDependencies(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Lỗi tải quan hệ phụ thuộc:', err);
      message.error(err.message || 'Không thể tải danh sách quan hệ phụ thuộc');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDependencies();
    setAdding(false);
    setSelectedPredecessor(null);
    setSelectedType('FS');
    setLagDays(0);
  }, [taskId]);

  const handleAddDependency = async () => {
    if (!selectedPredecessor) {
      message.warning('Vui lòng chọn công việc tiên quyết');
      return;
    }

    setSubmitting(true);
    try {
      await addTaskDependency(taskId, {
        predecessorId: selectedPredecessor,
        dependencyType: selectedType,
        lagDays: lagDays || 0
      });

      message.success('Thêm quan hệ phụ thuộc thành công');
      setAdding(false);
      setSelectedPredecessor(null);
      setSelectedType('FS');
      setLagDays(0);
      loadDependencies();
    } catch (err) {
      // Bắt lỗi chu trình tuần hoàn từ backend
      message.error(err.message || 'Thêm quan hệ phụ thuộc thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (depId) => {
    try {
      await deleteTaskDependency(depId);
      message.success('Đã xóa quan hệ phụ thuộc');
      loadDependencies();
    } catch (err) {
      message.error(err.message || 'Xóa quan hệ thất bại');
    }
  };

  const formatLag = (days) => {
    if (!days || days === 0) return <Text type="secondary">0 ngày</Text>;
    if (days > 0) return <Text style={{ color: '#fa8c16' }}>+{days} ngày (Trễ)</Text>;
    return <Text style={{ color: '#52c41a' }}>{days} ngày (Gối đầu)</Text>;
  };

  const columns = [
    {
      title: 'Công việc tiên quyết (Predecessor)',
      key: 'predecessor',
      render: (_, record) => {
        const pred = taskMap.get(record.predecessorId);
        return (
          <Space>
            <ArrowRightOutlined style={{ orientation: 'right', color: '#1677ff' }} />
            <Text strong>{pred?.name || pred?.title || record.predecessorId}</Text>
            {pred?.code && <Tag>{pred.code}</Tag>}
          </Space>
        );
      }
    },
    {
      title: 'Loại',
      dataIndex: 'dependencyType',
      key: 'dependencyType',
      width: 140,
      render: (type) => {
        const conf = DEPENDENCY_TYPE_LABELS[type] || { label: type, color: 'default' };
        return <Tag color={conf.color}>{conf.label}</Tag>;
      }
    },
    {
      title: 'Độ trễ (Lag)',
      dataIndex: 'lagDays',
      key: 'lagDays',
      width: 130,
      render: (days) => formatLag(days)
    },
    {
      title: '',
      key: 'action',
      width: 60,
      render: (_, record) => (
        <Popconfirm
          title="Xóa quan hệ này?"
          description="Công việc này sẽ không còn phụ thuộc vào công việc tiên quyết nữa."
          onConfirm={() => handleDelete(record.id)}
          okText="Xóa"
          cancelText="Hủy"
        >
          <Button type="text" danger size="small" icon={<DeleteOutlined />} />
        </Popconfirm>
      )
    }
  ];

  return (
    <div style={{ marginTop: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <Space>
          <LinkOutlined style={{ color: '#1677ff', fontSize: 16 }} />
          <Text strong style={{ fontSize: 15 }}>Quan hệ phụ thuộc (Dependencies)</Text>
          <Tag color="geekblue">{dependencies.length}</Tag>
        </Space>
        {!adding && (
          <Button
            type="dashed"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => setAdding(true)}
          >
            Thêm liên kết
          </Button>
        )}
      </div>

      {adding && (
        <Card
          size="small"
          style={{ marginBottom: 16, background: '#fafafa', borderColor: '#d9d9d9' }}
          title={<Text style={{ fontSize: 13 }}>Thiết lập liên kết phụ thuộc mới</Text>}
        >
          <Space direction="vertical" orientation="left" style={{ width: '100%' }} size="middle">
            <div>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                Công việc trước:
              </Text>
              <Select
                showSearch
                style={{ width: '100%' }}
                placeholder="Chọn công việc tiên quyết..."
                value={selectedPredecessor}
                onChange={setSelectedPredecessor}
                optionFilterProp="children"
                filterOption={(input, option) =>
                  (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                }
                options={availablePredecessors.map((t) => ({
                  value: t.id || t.key,
                  label: `${t.code ? `[${t.code}] ` : ''}${t.name || t.title}`
                }))}
              />
            </div>

            <Space wrap>
              <div>
                <Text orientation="left" type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                  Loại liên kết:
                </Text>
                <Select
                  value={selectedType}
                  onChange={setSelectedType}
                  style={{ width: 180 }}
                  options={[
                    { value: 'FS', label: 'FS (Kết thúc - Bắt đầu)' },
                    { value: 'SS', label: 'SS (Bắt đầu - Bắt đầu)' },
                    { value: 'FF', label: 'FF (Kết thúc - Kết thúc)' },
                    { value: 'SF', label: 'SF (Bắt đầu - Kết thúc)' }
                  ]}
                />
              </div>

              <div>
                <Text orientation="left" type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                  Độ trễ / gối đầu (ngày):
                </Text>
                <InputNumber
                  value={lagDays}
                  onChange={setLagDays}
                  style={{ width: 140 }}
                  placeholder="0"
                />
              </div>
            </Space>

            <div style={{ textAlign: 'right', marginTop: 8 }}>
              <Space>
                <Button size="small" onClick={() => setAdding(false)}>
                  Hủy
                </Button>
                <Button
                  type="primary"
                  size="small"
                  loading={submitting}
                  onClick={handleAddDependency}
                >
                  Lưu liên kết
                </Button>
              </Space>
            </div>
          </Space>
        </Card>
      )}

      <Table
        dataSource={dependencies}
        columns={columns}
        rowKey="id"
        size="small"
        pagination={false}
        loading={loading}
        locale={{
          emptyText: (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="Chưa có quan hệ phụ thuộc nào"
            />
          )
        }}
      />
    </div>
  );
};

export default TaskDependencySection;