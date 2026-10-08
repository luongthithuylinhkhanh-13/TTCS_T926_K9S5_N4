import React, { useEffect, useState } from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
  message
} from 'antd';
import {
  AlertOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  FieldTimeOutlined,
  PlusOutlined,
  ReloadOutlined,
  RightOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { createMilestone, getMilestoneWarnings } from '../../services/milestoneApi';
import { getProjectWbs } from '../../services/wbsApi';

const MilestoneAlertSection = ({ projectId }) => {
  const [warnings, setWarnings] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [selectedChain, setSelectedChain] = useState(null);
  const [form] = Form.useForm();

  const loadData = async () => {
    if (!projectId) return;
    setLoading(true);
    try {
      const [warningData, wbsData] = await Promise.all([
        getMilestoneWarnings(projectId),
        getProjectWbs(projectId).catch(() => [])
      ]);
      setWarnings(Array.isArray(warningData) ? warningData : []);

      // Lọc các hạng mục (non-task hoặc item có type phase/category/subphase)
      const catList = (Array.isArray(wbsData) ? wbsData : []).filter(
        item => item.type !== 'task'
      );
      setCategories(catList);
    } catch (error) {
      message.error(error.message || 'Không thể tải cảnh báo mốc tiến độ');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [projectId]);

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      await createMilestone(projectId, {
        categoryId: values.categoryId,
        name: values.name.trim(),
        targetDate: values.targetDate.format('YYYY-MM-DD'),
        description: values.description
      });
      message.success('Đã tạo mốc tiến độ thành công');
      setModalOpen(false);
      form.resetFields();
      loadData();
    } catch (error) {
      if (error?.errorFields) return;
      message.error(error.message || 'Không thể tạo mốc tiến độ');
    } finally {
      setSubmitting(false);
    }
  };

  const overrunCount = warnings.filter(w => w.isOverrun).length;

  const columns = [
    {
      title: 'Mốc tiến độ (Milestone)',
      key: 'milestoneName',
      render: (_, record) => (
        <div>
          <div style={{ fontWeight: 600, fontSize: '14px' }}>
            {record.milestoneName}
          </div>
          <div style={{ fontSize: '12px', color: '#666' }}>
            Hạng mục: <Tag color="blue">{record.categoryWbsCode} - {record.categoryName}</Tag>
          </div>
        </div>
      )
    },
    {
      title: 'Ngày bắt buộc',
      dataIndex: 'targetDate',
      key: 'targetDate',
      width: 140,
      render: date => (
        <span style={{ fontWeight: 500 }}>
          {date ? dayjs(date).format('DD/MM/YYYY') : '--'}
        </span>
      )
    },
    {
      title: 'Việc cuối hạng mục',
      key: 'lastTask',
      width: 220,
      render: (_, record) => {
        if (!record.lastTaskId) {
          return <span style={{ color: '#999', fontStyle: 'italic' }}>Chưa có task</span>;
        }
        return (
          <div>
            <div style={{ fontWeight: 500 }}>
              {record.lastTaskWbsCode} · {record.lastTaskName}
            </div>
            <div style={{ fontSize: '12px', color: '#888' }}>
              Kết sớm: {record.lastTaskEarlyFinishDate ? dayjs(record.lastTaskEarlyFinishDate).format('DD/MM/YYYY') : `Ngày thứ ${record.lastTaskEfDays}`}
            </div>
          </div>
        );
      }
    },
    {
      title: 'Trạng thái & Số ngày vượt',
      key: 'status',
      width: 180,
      render: (_, record) => {
        if (!record.lastTaskId) {
          return <Tag color="default">Chưa có công việc</Tag>;
        }
        if (record.isOverrun) {
          return (
            <Tag color="error" icon={<AlertOutlined />}>
              VƯỢT {record.overrunDays} NGÀY
            </Tag>
          );
        }
        return (
          <Tag color="success" icon={<CheckCircleOutlined />}>
            ĐÚNG TIẾN ĐỘ
          </Tag>
        );
      }
    },
    {
      title: 'Chuỗi việc gây chậm',
      key: 'delayChain',
      width: 200,
      render: (_, record) => {
        const chain = record.delayChain || [];
        if (chain.length === 0) {
          return <span style={{ color: '#999' }}>--</span>;
        }
        return (
          <Button
            size="small"
            type="link"
            icon={<FieldTimeOutlined />}
            onClick={() => setSelectedChain({ milestone: record, chain })}
          >
            Xem chuỗi ({chain.length} việc)
          </Button>
        );
      }
    }
  ];

  return (
    <Card
      title={
        <Space>
          <ClockCircleOutlined style={{ color: overrunCount > 0 ? '#ff4d4f' : '#1890ff' }} />
          <span>MỐC TIẾN ĐỘ & CẢNH BÁO VƯỢT HẠN</span>
          {overrunCount > 0 ? (
            <Badge count={`Chậm ${overrunCount} mốc`} style={{ backgroundColor: '#ff4d4f' }} />
          ) : (
            <Badge count="An toàn" style={{ backgroundColor: '#52c41a' }} />
          )}
        </Space>
      }
      extra={
        <Space>
          <Button size="small" icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
            Làm mới
          </Button>
          <Button
            size="small"
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setModalOpen(true)}
          >
            Thêm mốc
          </Button>
        </Space>
      }
      style={{ marginTop: 24, borderRadius: 8, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}
    >
      {overrunCount > 0 && (
        <Alert
          message={
            <span>
              <strong>Cảnh báo chậm trễ tiến độ:</strong> Có <strong>{overrunCount}</strong> mốc tiến độ bị công việc cuối hạng mục vượt quá ngày bắt buộc!
            </span>
          }
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Table
        dataSource={warnings}
        columns={columns}
        rowKey="milestoneId"
        loading={loading}
        pagination={false}
        size="middle"
        locale={{ emptyText: 'Chưa có mốc tiến độ nào cho dự án này' }}
      />

      {/* Modal chi tiết chuỗi việc gây chậm (T-45) */}
      <Modal
        title={
          <span>
            <AlertOutlined style={{ color: '#ff4d4f', marginRight: 8 }} />
            Chuỗi công việc chi phối & gây chậm: {selectedChain?.milestone?.milestoneName}
          </span>
        }
        open={!!selectedChain}
        onCancel={() => setSelectedChain(null)}
        footer={[
          <Button key="close" onClick={() => setSelectedChain(null)}>
            Đóng
          </Button>
        ]}
        width={680}
      >
        {selectedChain && (
          <div>
            <div style={{ marginBottom: 16, background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
              <div><strong>Hạng mục:</strong> {selectedChain.milestone.categoryName}</div>
              <div><strong>Hạn bắt buộc (Target):</strong> {selectedChain.milestone.targetDate}</div>
              <div><strong>Số ngày vượt mốc:</strong> <Tag color="error">{selectedChain.milestone.overrunDays} ngày</Tag></div>
            </div>

            <div style={{ fontWeight: 600, marginBottom: 12 }}>
              Đường lan truyền phụ thuộc trực tiếp dẫn đến mốc bị trễ:
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {selectedChain.chain.map((task, index) => (
                <div
                  key={task.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '8px 12px',
                    border: '1px solid #d9d9d9',
                    borderRadius: 6,
                    background: task.critical ? '#fff2f0' : '#fafafa'
                  }}
                >
                  <Space>
                    <Tag color={task.critical ? 'red' : 'blue'}>#{index + 1}</Tag>
                    <span style={{ fontWeight: 500 }}>{task.wbsCode} - {task.name}</span>
                    {task.critical && <Tag color="red">Găng</Tag>}
                  </Space>
                  <Space>
                    <span>Thời lượng: <strong>{task.duration}d</strong></span>
                    <span>ES: {task.es}</span>
                    <RightOutlined style={{ fontSize: 10, color: '#aaa' }} />
                    <span>EF: <strong>{task.ef}</strong></span>
                  </Space>
                </div>
              ))}
            </div>
          </div>
        )}
      </Modal>

      {/* Modal thêm mốc tiến độ (T-43) */}
      <Modal
        title="Thêm mốc tiến độ mới (Milestone)"
        open={modalOpen}
        onOk={handleCreate}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        okText="Lưu mốc"
        cancelText="Hủy"
      >
        <Form form={form} layout="vertical" initialValues={{ targetDate: dayjs().add(1, 'month') }}>
          <Form.Item
            name="categoryId"
            label="Gắn với hạng mục"
            rules={[{ required: true, message: 'Vui lòng chọn hạng mục' }]}
          >
            <Select placeholder="Chọn hạng mục thi công">
              {categories.map(cat => (
                <Select.Option key={cat.id} value={cat.id}>
                  {cat.wbsCode} - {cat.name}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="name"
            label="Tên mốc tiến độ"
            rules={[{ required: true, message: 'Vui lòng nhập tên mốc' }]}
          >
            <Input placeholder="Ví dụ: Hoàn thành phần móng, Cất nóc tầng 10..." />
          </Form.Item>

          <Form.Item
            name="targetDate"
            label="Ngày bắt buộc hoàn thành (Deadline)"
            rules={[{ required: true, message: 'Vui lòng chọn ngày bắt buộc' }]}
          >
            <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
          </Form.Item>

          <Form.Item name="description" label="Ghi chú / Mô tả chi tiết">
            <Input.TextArea rows={3} placeholder="Điều kiện nghiệm thu hoặc ghi chú cho mốc..." />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default MilestoneAlertSection;
