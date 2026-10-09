import React, { useState, useEffect } from 'react';
import {
  Card,
  Form,
  Input,
  DatePicker,
  InputNumber,
  Button,
  Table,
  Tag,
  Row,
  Col,
  Alert,
  Switch,
  notification,
  Select,
  Typography,
  Space
} from 'antd';
import {
  BookOutlined,
  CloudUploadOutlined,
  WifiOutlined,
  DisconnectOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { getProjects } from '../services/wbsApi';
import { submitSiteDiary, getSiteDiaries } from '../services/siteDiaryApi';
import syncQueueService from '../services/syncQueueService';
import autoSyncService from '../services/autoSyncService';
import SyncQueueStatusBar from '../components/sync/SyncQueueStatusBar';

const { Title, Text } = Typography;
const { TextArea } = Input;

const SiteDiaryPage = () => {
  const [form] = Form.useForm();
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [diaries, setDiaries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [simulateOffline, setSimulateOffline] = useState(false);

  // Tải danh sách dự án
  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const list = await getProjects();
        if (Array.isArray(list) && list.length > 0) {
          setProjects(list);
          setSelectedProjectId(list[0].id);
        }
      } catch (err) {
        console.error('Lỗi tải dự án:', err);
      }
    };
    fetchProjects();
  }, []);

  // Tải nhật ký khi chọn dự án
  const loadDiaries = async () => {
    if (!selectedProjectId) return;
    setLoading(true);
    try {
      const data = await getSiteDiaries(selectedProjectId);
      setDiaries(data);
    } catch (err) {
      console.error('Lỗi tải nhật ký:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDiaries();
  }, [selectedProjectId]);

  // Lắng nghe sự kiện đồng bộ để tự động làm mới danh sách
  useEffect(() => {
    const handleSyncChange = () => {
      loadDiaries();
    };
    window.addEventListener('ntdhtcct:sync-status', handleSyncChange);
    window.addEventListener('ntdhtcct:sync-queue-updated', handleSyncChange);
    return () => {
      window.removeEventListener('ntdhtcct:sync-status', handleSyncChange);
      window.removeEventListener('ntdhtcct:sync-queue-updated', handleSyncChange);
    };
  }, [selectedProjectId]);

  // Xử lý gửi biểu mẫu nhật ký
  const onFinish = async values => {
    if (!selectedProjectId) {
      notification.warning({ message: 'Vui lòng chọn một dự án' });
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        diaryDate: values.diaryDate.format('YYYY-MM-DD'),
        weather: values.weather,
        temperature: values.temperature,
        workerCount: values.workerCount,
        equipmentStatus: values.equipmentStatus,
        workSummary: values.workSummary,
        issues: values.issues
      };

      // Nếu đang bật cờ giả lập offline
      if (simulateOffline) {
        const queueItem = syncQueueService.enqueue({
          projectId: selectedProjectId,
          entityType: 'SITE_DIARY',
          action: 'CREATE',
          endpoint: `/api/projects/${selectedProjectId}/site-diaries/sync`,
          method: 'POST',
          payload,
          title: `Nhật ký ${payload.diaryDate} (Mô phỏng ngoại tuyến)`
        });

        notification.info({
          message: 'Đã đưa vào hàng đợi ngoại tuyến (T-70)',
          description: `Đang giả lập mất mạng. Bản ghi được lưu cục bộ. Khi tắt giả lập hoặc có mạng, hệ thống sẽ tự động gửi (T-71).`,
          placement: 'bottomRight'
        });
      } else {
        const result = await submitSiteDiary(selectedProjectId, payload);
        if (result.isOfflineQueued) {
          notification.warning({
            message: 'Đã lưu vào hàng đợi ngoại tuyến (T-70)',
            description: result.message,
            placement: 'bottomRight'
          });
        } else {
          notification.success({
            message: 'Thành công',
            description: 'Đã ghi nhận nhật ký công trường lên máy chủ thành công.',
            placement: 'bottomRight'
          });
        }
      }

      form.resetFields();
      form.setFieldsValue({ diaryDate: dayjs(), weather: 'Nắng', workerCount: 30 });
      loadDiaries();
    } catch (err) {
      notification.error({
        message: 'Lỗi ghi nhật ký',
        description: err.message
      });
    } finally {
      setSubmitting(false);
    }
  };

  const columns = [
    {
      title: 'Ngày ghi',
      dataIndex: 'diaryDate',
      key: 'diaryDate',
      width: 120,
      render: date => (date ? dayjs(date).format('DD/MM/YYYY') : '--')
    },
    {
      title: 'Thời tiết',
      key: 'weather',
      width: 130,
      render: (_, r) => (
        <span>
          {r.weather || 'Bình thường'} {r.temperature ? `(${r.temperature})` : ''}
        </span>
      )
    },
    {
      title: 'Nhân công',
      dataIndex: 'workerCount',
      key: 'workerCount',
      width: 100,
      render: count => (count ? `${count} người` : '--')
    },
    {
      title: 'Nội dung thi công',
      dataIndex: 'workSummary',
      key: 'workSummary',
      render: (text, r) => (
        <div>
          <div style={{ fontWeight: 500 }}>{text}</div>
          {r.equipmentStatus && (
            <div style={{ fontSize: '12px', color: '#8c8c8c' }}>
              Thiết bị: {r.equipmentStatus}
            </div>
          )}
          {r.issues && (
            <div style={{ fontSize: '12px', color: '#ff4d4f' }}>
              Vướng mắc: {r.issues}
            </div>
          )}
        </div>
      )
    },
    {
      title: 'Trạng thái đồng bộ (S-30)',
      key: 'syncStatus',
      width: 200,
      render: (_, r) => {
        if (r._isPendingSync) {
          return (
            <Tag icon={<ClockCircleOutlined />} color="warning">
              Chờ đồng bộ ngoại tuyến (T-70)
            </Tag>
          );
        }
        return (
          <Tag icon={<CheckCircleOutlined />} color="success">
            Đã đồng bộ máy chủ (T-71)
          </Tag>
        );
      }
    }
  ];

  return (
    <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
      {/* Tiêu đề & Thanh trạng thái mạng */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '20px',
          flexWrap: 'wrap',
          gap: '12px'
        }}
      >
        <div>
          <Title level={2} style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BookOutlined style={{ color: '#1890ff' }} /> Nhật ký công trường (E-05 / S-30)
          </Title>
          <Text type="secondary">
            Hàng đợi đồng bộ tự gửi khi có mạng (NTDHTCT-214: T-70 & T-71)
          </Text>
        </div>

        <Space>
          <SyncQueueStatusBar />
        </Space>
      </div>

      {/* Thanh công cụ kiểm thử / Chọn dự án */}
      <Card size="small" style={{ marginBottom: '20px', background: '#fafafa' }}>
        <Row gutter={16} align="middle">
          <Col xs={24} sm={12} md={8}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Text strong>Dự án:</Text>
              <Select
                value={selectedProjectId}
                onChange={setSelectedProjectId}
                style={{ flex: 1 }}
                options={projects.map(p => ({ label: `${p.code} - ${p.name}`, value: p.id }))}
                placeholder="Chọn dự án..."
              />
            </div>
          </Col>

          <Col xs={24} sm={12} md={16} style={{ textAlign: 'right' }}>
            <Space wrap>
              {/* Công tắc giả lập Offline để kiểm thử T-70 và T-71 */}
              <div
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  background: simulateOffline ? '#fff1f0' : '#f0f5ff',
                  padding: '4px 12px',
                  borderRadius: '6px',
                  border: `1px solid ${simulateOffline ? '#ffa39e' : '#adc6ff'}`
                }}
              >
                {simulateOffline ? <DisconnectOutlined style={{ color: '#ff4d4f' }} /> : <WifiOutlined style={{ color: '#1890ff' }} />}
                <Text style={{ fontSize: '13px' }}>
                  Giả lập mất mạng (Offline Test):
                </Text>
                <Switch
                  checked={simulateOffline}
                  onChange={checked => {
                    setSimulateOffline(checked);
                    if (!checked) {
                      // Khi tắt giả lập -> kích hoạt tự động gửi ngay (T-71)
                      autoSyncService.syncQueueNow();
                    }
                  }}
                />
              </div>

              <Button icon={<ReloadOutlined />} onClick={loadDiaries}>
                Làm mới
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Row gutter={24}>
        {/* Form ghi nhật ký */}
        <Col xs={24} lg={10}>
          <Card
            title={
              <Space>
                <CloudUploadOutlined style={{ color: '#1890ff' }} />
                <span>Ghi nhật ký thi công (Hỗ trợ Offline)</span>
              </Space>
            }
            bordered
          >
            <Form
              form={form}
              layout="vertical"
              onFinish={onFinish}
              initialValues={{
                diaryDate: dayjs(),
                weather: 'Nắng',
                temperature: '31°C',
                workerCount: 35
              }}
            >
              <Row gutter={12}>
                <Col span={12}>
                  <Form.Item
                    name="diaryDate"
                    label="Ngày ghi nhật ký"
                    rules={[{ required: true, message: 'Vui lòng chọn ngày' }]}
                  >
                    <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="weather" label="Thời tiết">
                    <Input placeholder="Nắng, mưa, râm mát..." />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={12}>
                <Col span={12}>
                  <Form.Item name="temperature" label="Nhiệt độ">
                    <Input placeholder="Ví dụ: 32°C" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="workerCount" label="Số nhân công">
                    <InputNumber min={0} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item name="equipmentStatus" label="Máy móc & thiết bị">
                <Input placeholder="Ví dụ: 2 máy đào, 1 cẩu tháp vận hành bình thường" />
              </Form.Item>

              <Form.Item
                name="workSummary"
                label="Nội dung thi công trong ngày"
                rules={[{ required: true, message: 'Vui lòng nhập nội dung thi công' }]}
              >
                <TextArea
                  rows={3}
                  placeholder="Ghi nhận các công việc hoàn thành trong ca làm việc..."
                />
              </Form.Item>

              <Form.Item name="issues" label="Vướng mắc / An toàn lao động">
                <TextArea rows={2} placeholder="Sự cố, gián đoạn hoặc ghi chú an toàn (nếu có)..." />
              </Form.Item>

              <Button
                type="primary"
                htmlType="submit"
                block
                size="large"
                loading={submitting}
                icon={<CloudUploadOutlined />}
              >
                Lưu nhật ký (Tự động chuyển hàng đợi nếu Offline)
              </Button>
            </Form>
          </Card>
        </Col>

        {/* Danh sách nhật ký */}
        <Col xs={24} lg={14}>
          <Card
            title={
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>Lịch sử nhật ký & Trạng thái đồng bộ</span>
                <Text type="secondary" style={{ fontSize: '13px' }}>
                  Tổng: {diaries.length} bản ghi
                </Text>
              </div>
            }
            bordered
          >
            <Table
              dataSource={diaries}
              columns={columns}
              rowKey={r => r.clientSyncId || r.id}
              loading={loading}
              pagination={{ pageSize: 6 }}
              size="middle"
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default SiteDiaryPage;
