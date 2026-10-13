import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  Empty,
  Form,
  Input,
  Modal,
  notification,
  Select,
  Space,
  Table,
  Tag,
  Timeline,
  Typography
} from 'antd';
import {
  AuditOutlined,
  CheckOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  RollbackOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { useLocale } from '../utils/LocaleContext';
import { getProjects } from '../services/wbsApi';
import {
  approveAcceptanceRecord,
  createAcceptanceRecord,
  getAcceptanceRecord,
  getAcceptanceRecordHistory,
  getAcceptanceRecords,
  returnAcceptanceRecord,
  updateAcceptanceRecord
} from '../services/acceptanceRecordApi';

const { Title, Text } = Typography;
const { TextArea } = Input;

const statusLabels = {
  PENDING_APPROVAL: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  RETURNED: 'Trả lại'
};

const actionLabels = {
  CREATE: 'Tạo phiếu',
  EDIT: 'Sửa phiếu',
  APPROVE: 'Duyệt',
  RETURN: 'Trả lại'
};

const AcceptanceRecordsPage = () => {
  const { t } = useLocale();
  const [form] = Form.useForm();
  const [projects, setProjects] = useState([]);
  const [projectId, setProjectId] = useState(null);
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [formSubmitting, setFormSubmitting] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [history, setHistory] = useState([]);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [workflowAction, setWorkflowAction] = useState(null);
  const [workflowSubmitting, setWorkflowSubmitting] = useState(false);
  const [returnComment, setReturnComment] = useState('');

  useEffect(() => {
    const loadProjects = async () => {
      try {
        const list = await getProjects();
        setProjects(Array.isArray(list) ? list : []);
        setProjectId(list?.[0]?.id || null);
      } catch (error) {
        notification.error({ message: t('Không thể tải danh sách dự án'), description: error.message });
      }
    };
    loadProjects();
  }, [t]);

  const loadRecords = async () => {
    if (!projectId) {
      setRecords([]);
      return;
    }
    setLoading(true);
    try {
      const data = await getAcceptanceRecords(projectId);
      setRecords(Array.isArray(data) ? data : []);
    } catch (error) {
      notification.error({ message: t('Không thể tải phiếu nghiệm thu'), description: error.message });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRecords();
  }, [projectId]);

  const openDetail = async record => {
    setDetail(record);
    setHistory([]);
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const [recordData, historyData] = await Promise.all([
        getAcceptanceRecord(projectId, record.id),
        getAcceptanceRecordHistory(projectId, record.id)
      ]);
      setDetail(recordData);
      setHistory(Array.isArray(historyData) ? historyData : []);
    } catch (error) {
      notification.error({ message: t('Không thể tải chi tiết phiếu'), description: error.message });
    } finally {
      setDetailLoading(false);
    }
  };

  const openCreateForm = () => {
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ inspectionDate: dayjs() });
    setFormOpen(true);
  };

  const openEditForm = () => {
    if (!detail) return;
    setEditingId(detail.id);
    form.setFieldsValue({
      title: detail.title,
      inspectionDate: dayjs(detail.inspectionDate),
      description: detail.description
    });
    setDetailOpen(false);
    setFormOpen(true);
  };

  const submitForm = async values => {
    if (!projectId) return;
    const payload = {
      title: values.title,
      inspectionDate: values.inspectionDate.format('YYYY-MM-DD'),
      description: values.description
    };
    setFormSubmitting(true);
    try {
      if (editingId) {
        await updateAcceptanceRecord(projectId, editingId, payload);
        notification.success({ message: t('Đã cập nhật phiếu nghiệm thu') });
      } else {
        await createAcceptanceRecord(projectId, payload);
        notification.success({ message: t('Đã tạo phiếu nghiệm thu') });
      }
      setFormOpen(false);
      await loadRecords();
    } catch (error) {
      notification.error({ message: t('Không thể lưu phiếu nghiệm thu'), description: error.message });
    } finally {
      setFormSubmitting(false);
    }
  };

  const submitWorkflow = async () => {
    if (!projectId || !detail || !workflowAction) return;
    setWorkflowSubmitting(true);
    try {
      if (workflowAction === 'APPROVE') {
        await approveAcceptanceRecord(projectId, detail.id);
        notification.success({ message: t('Đã duyệt phiếu nghiệm thu') });
      } else {
        await returnAcceptanceRecord(projectId, detail.id, returnComment);
        notification.success({ message: t('Đã trả lại phiếu nghiệm thu') });
      }
      setWorkflowAction(null);
      setReturnComment('');
      await loadRecords();
      await openDetail({ ...detail });
    } catch (error) {
      notification.error({ message: t('Không thể cập nhật trạng thái phiếu'), description: error.message });
    } finally {
      setWorkflowSubmitting(false);
    }
  };

  const columns = [
    {
      title: t('Tên phiếu'),
      dataIndex: 'title',
      key: 'title',
      render: (title, record) => (
        <Button type="link" onClick={() => openDetail(record)} style={{ paddingInline: 0 }}>
          {title}
        </Button>
      )
    },
    {
      title: t('Ngày nghiệm thu'),
      dataIndex: 'inspectionDate',
      key: 'inspectionDate',
      width: 150,
      render: value => value ? dayjs(value).format('DD/MM/YYYY') : '—'
    },
    {
      title: t('Trạng thái'),
      dataIndex: 'status',
      key: 'status',
      width: 150,
      render: status => (
        <Tag color={status === 'APPROVED' ? 'green' : status === 'RETURNED' ? 'red' : 'gold'}>
          {t(statusLabels[status] || status)}
        </Tag>
      )
    },
    {
      title: t('Cập nhật lần cuối'),
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 180,
      render: value => value ? dayjs(value).format('DD/MM/YYYY HH:mm') : '—'
    },
    {
      title: t('Thao tác'),
      key: 'actions',
      width: 100,
      render: (_, record) => (
        <Button icon={<EyeOutlined />} onClick={() => openDetail(record)}>
          {t('Chi tiết')}
        </Button>
      )
    }
  ];

  return (
    <div className="acceptance-records-page">
      <Card>
        <Space direction="vertical" size="large" style={{ display: 'flex' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
            <div>
              <Title level={3} style={{ margin: 0 }}>{t('Phiếu nghiệm thu')}</Title>
              <Text type="secondary">{t('Theo dõi phiếu, trạng thái duyệt và lịch sử thao tác')}</Text>
            </div>
            <Space>
              <Select
                aria-label={t('Chọn dự án')}
                placeholder={t('Chọn dự án')}
                value={projectId}
                onChange={setProjectId}
                options={projects.map(project => ({ value: project.id, label: project.name }))}
                style={{ minWidth: 220 }}
              />
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreateForm} disabled={!projectId}>
                {t('Tạo phiếu')}
              </Button>
            </Space>
          </div>
          <Table
            rowKey="id"
            columns={columns}
            dataSource={records}
            loading={loading}
            locale={{ emptyText: <Empty description={t('Chưa có phiếu nghiệm thu')} /> }}
            scroll={{ x: 760 }}
          />
        </Space>
      </Card>

      <Modal
        title={editingId ? t('Chỉnh sửa phiếu nghiệm thu') : t('Tạo phiếu nghiệm thu')}
        open={formOpen}
        onCancel={() => setFormOpen(false)}
        onOk={() => form.submit()}
        confirmLoading={formSubmitting}
        okText={t('Lưu')}
        cancelText={t('Hủy')}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={submitForm}>
          <Form.Item
            name="title"
            label={t('Tên phiếu')}
            rules={[{ required: true, whitespace: true, message: t('Vui lòng nhập tên phiếu') }, { max: 255, message: t('Tên phiếu tối đa 255 ký tự') }]}
          >
            <Input maxLength={255} />
          </Form.Item>
          <Form.Item
            name="inspectionDate"
            label={t('Ngày nghiệm thu')}
            rules={[{ required: true, message: t('Vui lòng chọn ngày nghiệm thu') }]}
          >
            <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="description"
            label={t('Nội dung nghiệm thu')}
            rules={[{ required: true, whitespace: true, message: t('Vui lòng nhập nội dung nghiệm thu') }]}
          >
            <TextArea rows={4} maxLength={10000} showCount />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={<Space><AuditOutlined />{t('Chi tiết phiếu nghiệm thu')}</Space>}
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={[
          detail && detail.status !== 'APPROVED' && (
            <Button key="edit" icon={<EditOutlined />} onClick={openEditForm}>
              {t('Chỉnh sửa')}
            </Button>
          ),
          detail?.status === 'PENDING_APPROVAL' && (
            <Button key="return" danger icon={<RollbackOutlined />} onClick={() => setWorkflowAction('RETURN')}>
              {t('Trả lại')}
            </Button>
          ),
          detail?.status === 'PENDING_APPROVAL' && (
            <Button key="approve" type="primary" icon={<CheckOutlined />} onClick={() => setWorkflowAction('APPROVE')}>
              {t('Duyệt')}
            </Button>
          ),
          <Button key="close" onClick={() => setDetailOpen(false)}>{t('Đóng')}</Button>
        ]}
        width={720}
      >
        {detail && (
          <Space direction="vertical" size="large" style={{ display: 'flex' }}>
            <Descriptions bordered column={1} size="small" loading={detailLoading}>
              <Descriptions.Item label={t('Tên phiếu')}>{detail.title}</Descriptions.Item>
              <Descriptions.Item label={t('Ngày nghiệm thu')}>
                {detail.inspectionDate ? dayjs(detail.inspectionDate).format('DD/MM/YYYY') : '—'}
              </Descriptions.Item>
              <Descriptions.Item label={t('Trạng thái')}>
                <Tag color={detail.status === 'APPROVED' ? 'green' : detail.status === 'RETURNED' ? 'red' : 'gold'}>
                  {t(statusLabels[detail.status] || detail.status)}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label={t('Nội dung nghiệm thu')}>{detail.description}</Descriptions.Item>
            </Descriptions>
            <div>
              <Title level={5}><Space><AuditOutlined />{t('Lịch sử thao tác')}</Space></Title>
              {history.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={t('Chưa có lịch sử thao tác')} />
              ) : (
                <Timeline
                  items={history.map(item => ({
                    color: item.action === 'APPROVE' ? 'green' : item.action === 'RETURN' ? 'red' : 'blue',
                    children: (
                      <Space direction="vertical" size={0}>
                        <Text strong>{item.actorName}</Text>
                        <Text>{t(actionLabels[item.action] || item.action)}</Text>
                        {item.comment && <Text type="secondary">{item.comment}</Text>}
                        <Text type="secondary">{dayjs(item.performedAt).format('DD/MM/YYYY HH:mm:ss')}</Text>
                      </Space>
                    )
                  }))}
                />
              )}
            </div>
          </Space>
        )}
      </Modal>

      <Modal
        title={workflowAction === 'APPROVE' ? t('Duyệt phiếu nghiệm thu') : t('Trả lại phiếu nghiệm thu')}
        open={Boolean(workflowAction)}
        onCancel={() => setWorkflowAction(null)}
        onOk={submitWorkflow}
        confirmLoading={workflowSubmitting}
        okText={workflowAction === 'APPROVE' ? t('Duyệt') : t('Trả lại')}
        cancelText={t('Hủy')}
      >
        {workflowAction === 'APPROVE'
          ? t('Bạn có chắc chắn muốn duyệt phiếu nghiệm thu này?')
          : (
            <Form layout="vertical">
              <Form.Item label={t('Lý do trả lại')}>
                <TextArea
                  value={returnComment}
                  onChange={event => setReturnComment(event.target.value)}
                  maxLength={2000}
                  showCount
                  rows={3}
                />
              </Form.Item>
            </Form>
          )}
      </Modal>
    </div>
  );
};

export default AcceptanceRecordsPage;
