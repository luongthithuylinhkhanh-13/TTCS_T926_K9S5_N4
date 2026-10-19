import React, { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Tabs,
  Tag,
  Table,
  Tooltip,
  message
} from 'antd';
import {
  CheckOutlined,
  CloseOutlined,
  PlusOutlined,
  ReloadOutlined,
  WalletOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  getPendingPaymentRequests,
  getMyPaymentRequests,
  submitPaymentRequest,
  approvePaymentRequest,
  rejectPaymentRequest
} from '../services/paymentRequestApi';
import { getProjects } from '../services/wbsApi';

const currencyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2
});

const PaymentRequestsPage = () => {
  const [projects, setProjects] = useState([]);
  const [projectId, setProjectId] = useState(null);
  const [requests, setRequests] = useState([]);
  const [myRequests, setMyRequests] = useState([]);
  const [canReview, setCanReview] = useState(false);
  const [canSubmit, setCanSubmit] = useState(false);
  const [activeTab, setActiveTab] = useState('review');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [submitModalOpen, setSubmitModalOpen] = useState(false);
  const [activeRequestId, setActiveRequestId] = useState(null);
  const [rejectingRequest, setRejectingRequest] = useState(null);
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [form] = Form.useForm();

  useEffect(() => {
    getProjects()
      .then(data => {
        const projectList = Array.isArray(data) ? data : [];
        setProjects(projectList);
        setProjectId(projectList[0]?.id || null);
      })
      .catch(loadError => setError(loadError.message || 'Không thể tải danh sách công trình.'))
      .finally(() => setLoading(false));
  }, []);

  const loadRequests = async () => {
    if (!projectId) {
      setRequests([]);
      setMyRequests([]);
      return;
    }

    setLoading(true);
    setError('');
    try {
      const [pendingResult, mineResult] = await Promise.allSettled([
        getPendingPaymentRequests(projectId),
        getMyPaymentRequests(projectId)
      ]);

      if (pendingResult.status === 'fulfilled') {
        setCanReview(true);
        setRequests(Array.isArray(pendingResult.value) ? pendingResult.value : []);
      } else {
        setCanReview(false);
        setRequests([]);
        if (pendingResult.reason.status !== 403) {
          setError(pendingResult.reason.message || 'Không thể tải đề nghị chờ duyệt.');
        }
      }

      if (mineResult.status === 'fulfilled') {
        setCanSubmit(true);
        setMyRequests(Array.isArray(mineResult.value) ? mineResult.value : []);
      } else {
        setCanSubmit(false);
        setMyRequests([]);
        if (mineResult.reason.status !== 403) {
          setError(mineResult.reason.message || 'Không thể tải đề nghị của bạn.');
        }
      }

      setActiveTab(pendingResult.status === 'fulfilled' ? 'review' : 'mine');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (projectId) loadRequests();
  }, [projectId]);

  const handleApprove = async request => {
    setActiveRequestId(request.id);
    try {
      await approvePaymentRequest(projectId, request.id);
      message.success(`Đã duyệt đề nghị ${request.requestCode}`);
      await loadRequests();
    } catch (actionError) {
      message.error(actionError.message || 'Không thể duyệt đề nghị.');
    } finally {
      setActiveRequestId(null);
    }
  };

  const openRejectDialog = request => {
    setRejectingRequest(request);
    setReason('');
  };

  const handleReject = async () => {
    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      message.warning('Vui lòng ghi lý do trả lại đề nghị.');
      return;
    }

    setActiveRequestId(rejectingRequest.id);
    try {
      await rejectPaymentRequest(projectId, rejectingRequest.id, trimmedReason);
      message.success(`Đã trả lại đề nghị ${rejectingRequest.requestCode}`);
      setRejectingRequest(null);
      await loadRequests();
    } catch (actionError) {
      message.error(actionError.message || 'Không thể trả lại đề nghị.');
    } finally {
      setActiveRequestId(null);
    }
  };

  const handleSubmit = async values => {
    setSubmitting(true);
    try {
      await submitPaymentRequest(projectId, {
        purpose: values.purpose.trim(),
        amount: values.amount
      });
      message.success('Đã gửi đề nghị thanh toán để chờ duyệt.');
      setSubmitModalOpen(false);
      form.resetFields();
      await loadRequests();
      setActiveTab('mine');
    } catch (submitError) {
      message.error(submitError.message || 'Không thể gửi đề nghị thanh toán.');
    } finally {
      setSubmitting(false);
    }
  };

  const columns = [
    {
      title: 'Mã đề nghị',
      dataIndex: 'requestCode',
      key: 'requestCode',
      width: 150,
      render: value => <strong>{value}</strong>
    },
    {
      title: 'Nội dung',
      dataIndex: 'purpose',
      key: 'purpose',
      render: value => <span style={{ overflowWrap: 'anywhere' }}>{value}</span>
    },
    {
      title: 'Số tiền',
      dataIndex: 'amount',
      key: 'amount',
      width: 170,
      align: 'right',
      render: value => currencyFormatter.format(value)
    },
    {
      title: 'Người đề nghị',
      dataIndex: 'requesterName',
      key: 'requesterName',
      width: 190,
      render: value => value || 'Không rõ'
    },
    {
      title: 'Ngày gửi',
      dataIndex: 'submittedAt',
      key: 'submittedAt',
      width: 145,
      render: value => value ? dayjs(value).format('DD/MM/YYYY HH:mm') : '--'
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 104,
      align: 'right',
      render: (_, request) => (
        <Space size={4}>
          <Tooltip title="Duyệt đề nghị">
            <Button
              aria-label={`Duyệt ${request.requestCode}`}
              type="text"
              icon={<CheckOutlined />}
              disabled={Boolean(activeRequestId)}
              loading={activeRequestId === request.id}
              onClick={() => Modal.confirm({
                title: 'Duyệt đề nghị thanh toán?',
                content: `${request.requestCode} · ${currencyFormatter.format(request.amount)}`,
                okText: 'Duyệt',
                cancelText: 'Hủy',
                onOk: () => handleApprove(request)
              })}
            />
          </Tooltip>
          <Tooltip title="Trả lại và ghi lý do">
            <Button
              aria-label={`Trả lại ${request.requestCode}`}
              type="text"
              danger
              icon={<CloseOutlined />}
              disabled={Boolean(activeRequestId)}
              onClick={() => openRejectDialog(request)}
            />
          </Tooltip>
        </Space>
      )
    }
  ];

  const myRequestColumns = [
    columns[0],
    columns[1],
    columns[2],
    {
      title: 'Ngày gửi',
      dataIndex: 'submittedAt',
      key: 'submittedAt',
      width: 145,
      render: value => value ? dayjs(value).format('DD/MM/YYYY HH:mm') : '--'
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: status => {
        const statusInfo = {
          PENDING: ['gold', 'Chờ duyệt'],
          APPROVED: ['success', 'Đã duyệt'],
          REJECTED: ['error', 'Đã trả lại']
        }[status] || ['default', status];
        return <Tag color={statusInfo[0]}>{statusInfo[1]}</Tag>;
      }
    },
    {
      title: 'Lý do trả lại',
      dataIndex: 'decisionReason',
      key: 'decisionReason',
      render: value => value || '--'
    }
  ];

  return (
    <div className="page-container payment-requests-page">
      <div className="page-header">
        <div>
          <h1 className="page-title"><WalletOutlined /> Đề nghị thanh toán</h1>
          <p className="page-subtitle">Hàng chờ duyệt của công trình</p>
        </div>
        <Space wrap>
          <Select
            value={projectId}
            onChange={setProjectId}
            loading={loading && projects.length === 0}
            disabled={projects.length === 0}
            placeholder="Chọn công trình"
            style={{ width: 280, maxWidth: '100%' }}
            options={projects.map(project => ({
              value: project.id,
              label: `${project.code} - ${project.name}`
            }))}
          />
          {canSubmit && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setSubmitModalOpen(true)}
              disabled={!projectId}
            >
              Gửi đề nghị
            </Button>
          )}
          <Tooltip title="Làm mới danh sách">
            <Button
              aria-label="Làm mới danh sách"
              icon={<ReloadOutlined />}
              onClick={loadRequests}
              loading={loading}
              disabled={!projectId}
            />
          </Tooltip>
        </Space>
      </div>

      {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
      {projects.length === 0 && !loading ? (
        <Empty description="Chưa có công trình để duyệt đề nghị" />
      ) : !canReview && !canSubmit && !loading ? (
        <Alert type="warning" showIcon message="Bạn không có quyền truy cập đề nghị thanh toán của công trình này." />
      ) : (
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            ...(canReview ? [{
              key: 'review',
              label: `Chờ duyệt (${requests.length})`,
              children: (
                <Table
                  dataSource={requests}
                  columns={columns}
                  rowKey="id"
                  loading={loading}
                  scroll={{ x: 820 }}
                  pagination={{ pageSize: 10, hideOnSinglePage: true }}
                  locale={{ emptyText: <Empty description="Không có đề nghị nào đang chờ duyệt" /> }}
                />
              )
            }] : []),
            ...(canSubmit ? [{
              key: 'mine',
              label: `Đề nghị của tôi (${myRequests.length})`,
              children: (
                <Table
                  dataSource={myRequests}
                  columns={myRequestColumns}
                  rowKey="id"
                  loading={loading}
                  scroll={{ x: 820 }}
                  pagination={{ pageSize: 10, hideOnSinglePage: true }}
                  locale={{ emptyText: <Empty description="Bạn chưa gửi đề nghị thanh toán nào" /> }}
                />
              )
            }] : [])
          ]}
        />
      )}

      <Modal
        title="Gửi đề nghị thanh toán"
        open={submitModalOpen}
        onCancel={() => {
          setSubmitModalOpen(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        okText="Gửi đề nghị"
        cancelText="Hủy"
        confirmLoading={submitting}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            name="purpose"
            label="Nội dung đề nghị"
            rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập nội dung đề nghị.' }, { max: 2000, message: 'Nội dung tối đa 2000 ký tự.' }]}
          >
            <Input.TextArea rows={4} maxLength={2000} showCount placeholder="Ví dụ: Thanh toán đợt vật tư..." />
          </Form.Item>
          <Form.Item
            name="amount"
            label="Số tiền (VND)"
            rules={[{ required: true, message: 'Vui lòng nhập số tiền.' }]}
          >
            <InputNumber min={0.01} precision={2} style={{ width: '100%' }} placeholder="Nhập số tiền" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`Trả lại đề nghị ${rejectingRequest?.requestCode || ''}`}
        open={Boolean(rejectingRequest)}
        onCancel={() => setRejectingRequest(null)}
        onOk={handleReject}
        okText="Gửi lý do"
        cancelText="Hủy"
        okButtonProps={{ danger: true, disabled: !reason.trim() }}
        confirmLoading={Boolean(activeRequestId)}
      >
        <Input.TextArea
          autoFocus
          rows={4}
          maxLength={2000}
          showCount
          placeholder="Lý do trả lại đề nghị"
          value={reason}
          onChange={event => setReason(event.target.value)}
        />
      </Modal>
    </div>
  );
};

export default PaymentRequestsPage;