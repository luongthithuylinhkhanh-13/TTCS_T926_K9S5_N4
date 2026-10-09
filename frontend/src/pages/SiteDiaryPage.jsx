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
  Modal,
  notification,
  Select,
  Typography,
  Space
} from 'antd';
import {
  BookOutlined,
  CloudUploadOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ReloadOutlined,
  EyeOutlined,
  EditOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { getProjects } from '../services/wbsApi';
import { submitSiteDiary, getSiteDiaries, updateSiteDiary } from '../services/siteDiaryApi';

const { Title, Text } = Typography;
const { TextArea } = Input;

const SiteDiaryPage = () => {
  const [form] = Form.useForm();
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [diaries, setDiaries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingDiaryId, setEditingDiaryId] = useState(null);
  const [detailDiary, setDetailDiary] = useState(null);
  const [diaryModalOpen, setDiaryModalOpen] = useState(false);

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
        engineerCount: values.engineerCount,
        workerCount: values.workerCount,
        crewCount: values.crewCount,
        crewDetails: values.crewDetails,
        equipmentStatus: values.equipmentStatus,
        workSummary: values.workSummary,
        issues: values.issues,
        workingConditions: values.workingConditions
      };

      if (editingDiaryId) {
        await updateSiteDiary(selectedProjectId, editingDiaryId, payload);
        notification.success({
          message: 'Đã cập nhật nhật ký',
          description: 'Nội dung nhật ký đã được cập nhật trên máy chủ.',
          placement: 'bottomRight'
        });
        setEditingDiaryId(null);
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
      setDiaryModalOpen(false);
      await loadDiaries();
    } catch (err) {
      notification.error({
        message: 'Lỗi ghi nhật ký',
        description: err.message
      });
    } finally {
      setSubmitting(false);
    }
  };

  const startEditingDiary = diary => {
    if (diary._isPendingSync) {
      notification.info({
        message: 'Nhật ký chưa đồng bộ',
        description: 'Hãy đồng bộ nhật ký lên máy chủ trước khi chỉnh sửa.'
      });
      return;
    }
    setDetailDiary(null);
    setEditingDiaryId(diary.id);
    setDiaryModalOpen(true);
    form.setFieldsValue({
      diaryDate: diary.diaryDate ? dayjs(diary.diaryDate) : null,
      weather: diary.weather,
      temperature: diary.temperature,
      engineerCount: diary.engineerCount,
      workerCount: diary.workerCount,
      crewCount: diary.crewCount,
      crewDetails: diary.crewDetails,
      equipmentStatus: diary.equipmentStatus,
      workSummary: diary.workSummary,
      issues: diary.issues,
      workingConditions: diary.workingConditions
    });
  };

  const cancelEditingDiary = () => {
    setEditingDiaryId(null);
    setDiaryModalOpen(false);
    form.resetFields();
    form.setFieldsValue({ diaryDate: dayjs(), weather: 'Nắng', workerCount: 30 });
  };

  const openNewDiary = () => {
    setEditingDiaryId(null);
    form.resetFields();
    form.setFieldsValue({ diaryDate: dayjs(), weather: 'Nắng', workerCount: 30 });
    setDiaryModalOpen(true);
  };

  const handleProjectChange = projectId => {
    cancelEditingDiary();
    setDetailDiary(null);
    setSelectedProjectId(projectId);
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
      width: 180,
      render: (_, diary) => (
        <div>
          <div>Kỹ sư: {diary.engineerCount ?? '--'}</div>
          <div>Công nhân: {diary.workerCount ?? '--'}</div>
          <div>Tổ đội: {diary.crewCount ?? '--'}</div>
        </div>
      )
    },
    {
      title: 'Nội dung thi công',
      dataIndex: 'workSummary',
      key: 'workSummary',
      width: 420,
      render: (text, r) => (
        <div style={{ minWidth: 0, overflowWrap: 'anywhere' }}>
          <div
            style={{
              fontWeight: 500,
              display: '-webkit-box',
              WebkitBoxOrient: 'vertical',
              WebkitLineClamp: 3,
              overflow: 'hidden'
            }}
          >
            {text}
          </div>
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
      title: 'Đồng bộ',
      key: 'syncStatus',
      width: 200,
      render: (_, r) => {
        if (r._isPendingSync) {
          return (
            <Tag icon={<ClockCircleOutlined />} color="warning">
              Chờ đồng bộ
            </Tag>
          );
        }
        return (
          <Tag icon={<CheckCircleOutlined />} color="success">
            Đã đồng bộ
          </Tag>
        );
      }
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 130,
      render: (_, diary) => (
        <Button
          icon={<EyeOutlined />}
          onClick={() => setDetailDiary(diary)}
        >
          Chi tiết
        </Button>
      )
    }
  ];

  return (
    <div style={{ padding: '24px', width: '100%', maxWidth: '1440px', margin: '0 auto', boxSizing: 'border-box' }}>
      {/* Tiêu đề */}
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
            <BookOutlined style={{ color: '#1890ff' }} /> Nhật ký thi công
          </Title>
          <Text type="secondary">
            Theo dõi công việc thi công hằng ngày theo từng dự án.
          </Text>
        </div>
      </div>

      {/* Chọn dự án và thao tác nhật ký */}
      <Card size="small" style={{ marginBottom: '20px', background: '#fafafa' }}>
        <Row gutter={[16, 12]} align="middle">
          <Col xs={24} md={9} lg={10}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
              <Text strong style={{ flexShrink: 0 }}>Dự án:</Text>
              <Select
                value={selectedProjectId}
                onChange={handleProjectChange}
                style={{ flex: 1, minWidth: 0 }}
                options={projects.map(p => ({ label: `${p.code} - ${p.name}`, value: p.id }))}
                placeholder="Chọn dự án..."
                showSearch
                optionFilterProp="label"
              />
            </div>
          </Col>

          <Col xs={24} md={15} lg={14}>
            <Space wrap style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button icon={<ReloadOutlined />} onClick={loadDiaries}>
                Làm mới
              </Button>
              <Button
                type="primary"
                icon={<CloudUploadOutlined />}
                onClick={openNewDiary}
                disabled={!selectedProjectId}
              >
                Tạo nhật ký
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Card
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <span>Lịch sử nhật ký</span>
            <Text type="secondary" style={{ fontSize: '13px' }}>
              Tổng cộng: {diaries.length} bản ghi
            </Text>
          </div>
        }
        style={{ width: '100%' }}
      >
        <Table
          dataSource={diaries}
          columns={columns}
          rowKey={r => r.clientSyncId || r.id}
          loading={loading}
          pagination={{ pageSize: 6, showSizeChanger: true, pageSizeOptions: [6, 10, 20] }}
          size="middle"
          scroll={{ x: 1180 }}
          tableLayout="fixed"
        />
      </Card>

      <Modal
        title={
          <Space>
            {editingDiaryId ? <EditOutlined /> : <CloudUploadOutlined />}
            <span>{editingDiaryId ? 'Chỉnh sửa nhật ký' : 'Tạo nhật ký thi công'}</span>
          </Space>
        }
        open={diaryModalOpen}
        onCancel={cancelEditingDiary}
        footer={null}
        width={720}
        forceRender
      >
        {editingDiaryId && (
          <Alert
            type="info"
            showIcon
            message="Cần có kết nối mạng để cập nhật nhật ký."
            style={{ marginBottom: 16 }}
          />
        )}
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
            <Col xs={24} sm={8}>
              <Form.Item
                name="diaryDate"
                label="Ngày ghi nhật ký"
                rules={[{ required: true, message: 'Vui lòng chọn ngày' }]}
              >
                <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8}>
              <Form.Item name="weather" label="Thời tiết">
                <Input placeholder="Nắng, mưa, râm mát..." />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8}>
              <Form.Item name="temperature" label="Nhiệt độ">
                <Input placeholder="Ví dụ: 32°C" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={12}>
            <Col xs={24} sm={8}>
              <Form.Item name="engineerCount" label="Số kỹ sư">
                <InputNumber min={0} precision={0} style={{ width: '100%' }} placeholder="0" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8}>
              <Form.Item name="workerCount" label="Số công nhân">
                <InputNumber min={0} precision={0} style={{ width: '100%' }} placeholder="0" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8}>
              <Form.Item name="crewCount" label="Số tổ đội">
                <InputNumber min={0} precision={0} style={{ width: '100%' }} placeholder="0" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="crewDetails" label="Tổ đội và quân số từng tổ">
            <TextArea
              rows={2}
              placeholder={'Mỗi dòng ghi một tổ, ví dụ:\nTổ cốt thép: 8 người\nTổ cốp pha: 6 người'}
            />
          </Form.Item>

          <Form.Item name="equipmentStatus" label="Danh mục máy móc, thiết bị huy động">
            <TextArea
              rows={3}
              placeholder={'Mỗi dòng ghi thiết bị, số lượng và tình trạng, ví dụ:\nMáy đào: 2 chiếc - hoạt động bình thường\nCẩu tháp: 1 chiếc - bảo trì'}
            />
          </Form.Item>

          <Form.Item
            name="workSummary"
            label="Nội dung thi công trong ngày"
            rules={[{ required: true, message: 'Vui lòng nhập nội dung thi công' }]}
          >
            <TextArea rows={3} placeholder="Ghi lại các công việc đã thực hiện trong ngày..." />
          </Form.Item>

          <Form.Item name="workingConditions" label="Điều kiện ảnh hưởng đến thi công">
            <TextArea
              rows={2}
              placeholder="Ví dụ: mưa làm nền trơn, gió lớn, thiếu ánh sáng..."
            />
          </Form.Item>

          <Form.Item name="issues" label="Vướng mắc và an toàn lao động">
            <TextArea rows={2} placeholder="Ghi sự cố, gián đoạn hoặc lưu ý về an toàn (nếu có)..." />
          </Form.Item>

          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={cancelEditingDiary}>Hủy</Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={submitting}
              icon={editingDiaryId ? <EditOutlined /> : <CloudUploadOutlined />}
            >
              {editingDiaryId ? 'Cập nhật nhật ký' : 'Lưu nhật ký'}
            </Button>
          </Space>
        </Form>
      </Modal>

      <Modal
        title="Chi tiết nhật ký công trường"
        open={Boolean(detailDiary)}
        onCancel={() => setDetailDiary(null)}
        footer={[
          <Button key="close" onClick={() => setDetailDiary(null)}>
            Đóng
          </Button>,
          <Button
            key="edit"
            type="primary"
            icon={<EditOutlined />}
            disabled={detailDiary?._isPendingSync}
            onClick={() => startEditingDiary(detailDiary)}
          >
            Chỉnh sửa
          </Button>
        ]}
      >
        {detailDiary && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            {detailDiary._isPendingSync && (
              <Text type="warning">Nhật ký đang chờ đồng bộ, hiện chưa thể chỉnh sửa.</Text>
            )}
            <div><Text strong>Ngày ghi: </Text>{dayjs(detailDiary.diaryDate).format('DD/MM/YYYY')}</div>
            <div><Text strong>Thời tiết: </Text>{detailDiary.weather || 'Chưa ghi nhận'} {detailDiary.temperature ? `(${detailDiary.temperature})` : ''}</div>
            <div><Text strong>Số kỹ sư: </Text>{detailDiary.engineerCount ?? 'Chưa ghi nhận'}</div>
            <div><Text strong>Số công nhân: </Text>{detailDiary.workerCount ?? 'Chưa ghi nhận'}</div>
            <div><Text strong>Số tổ đội: </Text>{detailDiary.crewCount ?? 'Chưa ghi nhận'}</div>
            <div>
              <Text strong>Tổ đội và quân số từng tổ:</Text>
              <div style={{ whiteSpace: 'pre-wrap' }}>{detailDiary.crewDetails || 'Chưa ghi nhận'}</div>
            </div>
            <div>
              <Text strong>Danh mục máy móc, thiết bị:</Text>
              <div style={{ whiteSpace: 'pre-wrap' }}>{detailDiary.equipmentStatus || 'Chưa ghi nhận'}</div>
            </div>
            <div>
              <Text strong>Điều kiện ảnh hưởng đến thi công:</Text>
              <div style={{ whiteSpace: 'pre-wrap' }}>{detailDiary.workingConditions || 'Chưa ghi nhận'}</div>
            </div>
            <div>
              <Text strong>Nội dung thi công:</Text>
              <div style={{ whiteSpace: 'pre-wrap' }}>{detailDiary.workSummary}</div>
            </div>
            <div>
              <Text strong>Vướng mắc / an toàn:</Text>
              <div style={{ whiteSpace: 'pre-wrap' }}>{detailDiary.issues || 'Không có'}</div>
            </div>
          </Space>
        )}
      </Modal>
    </div>
  );
};

export default SiteDiaryPage;
