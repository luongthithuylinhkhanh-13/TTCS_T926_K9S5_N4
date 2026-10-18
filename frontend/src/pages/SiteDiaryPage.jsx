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
  Space,
  Tooltip,
  Badge,
  Popconfirm
} from 'antd';
import {
  BookOutlined,
  CloudUploadOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ReloadOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  LockOutlined,
  UnlockOutlined,
  SyncOutlined,
  CloseCircleOutlined,
  CloudSyncOutlined,
  UnorderedListOutlined,
  LoadingOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { getProjects } from '../services/wbsApi';
import {
  submitSiteDiary,
  getSiteDiaries,
  updateSiteDiary,
  deleteSiteDiary,
  lockSiteDiaryDate,
  unlockSiteDiaryDate,
  getProjectDiaryLocks,
  retryQueueItem,
  triggerManualSync
} from '../services/siteDiaryApi';
import { isProjectManager } from '../utils/permissions';
import syncQueueService from '../services/syncQueueService';
import SyncQueueModal from '../components/sync/SyncQueueModal';

const { Title, Text } = Typography;
const { TextArea } = Input;

const SiteDiaryPage = () => {
  const [form] = Form.useForm();
  const [lockForm] = Form.useForm();
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [diaries, setDiaries] = useState([]);
  const [locks, setLocks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [online, setOnline] = useState(() => (
    typeof navigator === 'undefined' || !('onLine' in navigator) || navigator.onLine
  ));
  const [editingDiaryId, setEditingDiaryId] = useState(null);
  const [detailDiary, setDetailDiary] = useState(null);
  const [diaryModalOpen, setDiaryModalOpen] = useState(false);
  const [lockModalOpen, setLockModalOpen] = useState(false);
  const [lockModalMode, setLockModalMode] = useState('LOCK'); // 'LOCK' | 'UNLOCK'
  const [lockActionSubmitting, setLockActionSubmitting] = useState(false);
  const [syncFilter, setSyncFilter] = useState('ALL'); // 'ALL' | 'UNSYNCED' | 'SYNCED'
  const [manualSyncing, setManualSyncing] = useState(false);
  const [queueModalOpen, setQueueModalOpen] = useState(false);

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

  // Tải danh sách khóa sổ theo dự án
  const loadLocks = async () => {
    if (!selectedProjectId) return;
    try {
      const lockList = await getProjectDiaryLocks(selectedProjectId);
      setLocks(lockList);
    } catch (err) {
      console.error('Lỗi tải danh sách khóa sổ:', err);
    }
  };

  // Tải nhật ký khi chọn dự án
  const loadDiaries = async () => {
    if (!selectedProjectId) return;
    setLoading(true);
    try {
      const [data, lockList] = await Promise.all([
        getSiteDiaries(selectedProjectId),
        getProjectDiaryLocks(selectedProjectId)
      ]);
      setDiaries(data);
      setLocks(lockList);
    } catch (err) {
      console.error('Lỗi tải nhật ký:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDiaries();
  }, [selectedProjectId]);

  useEffect(() => {
    const updateOnlineStatus = () => setOnline(navigator.onLine);
    window.addEventListener('online', updateOnlineStatus);
    window.addEventListener('offline', updateOnlineStatus);
    return () => {
      window.removeEventListener('online', updateOnlineStatus);
      window.removeEventListener('offline', updateOnlineStatus);
    };
  }, []);

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

    const dateFormatted = values.diaryDate.format('YYYY-MM-DD');
    if (isDateLocked(dateFormatted)) {
      notification.error({
        message: 'Ngày đã bị khóa sổ',
        description: `Không thể lưu hoặc cập nhật nhật ký cho ngày ${values.diaryDate.format('DD/MM/YYYY')} vì ngày này đã bị khóa sổ.`
      });
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

  const isDateLocked = targetDate => {
    if (!targetDate) return false;
    const dateStr = dayjs(targetDate).format('YYYY-MM-DD');
    const lockRecord = locks.find(l => l.lockDate === dateStr);
    return Boolean(lockRecord?.locked || lockRecord?.status === 'LOCKED');
  };

  const startEditingDiary = diary => {
    if (isDateLocked(diary.diaryDate)) {
      notification.warning({
        message: 'Không thể chỉnh sửa',
        description: `Nhật ký ngày ${dayjs(diary.diaryDate).format('DD/MM/YYYY')} đã bị khóa sổ.`
      });
      return;
    }
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

  const handleDeleteDiary = async diary => {
    if (isDateLocked(diary.diaryDate)) {
      notification.warning({
        message: 'Không thể xóa',
        description: `Nhật ký ngày ${dayjs(diary.diaryDate).format('DD/MM/YYYY')} đã bị khóa sổ.`
      });
      return;
    }

    try {
      if (diary._isPendingSync || diary._isRejectedLocked) {
        syncQueueService.remove(diary.id);
        notification.success({
          message: 'Đã xóa thành công',
          description: 'Đã xóa bản ghi khỏi hàng đợi ngoại tuyến.'
        });
      } else {
        await deleteSiteDiary(selectedProjectId, diary.id);
        notification.success({
          message: 'Đã xóa thành công',
          description: 'Đã xóa nhật ký công trường trên máy chủ.'
        });
      }
      await loadDiaries();
    } catch (err) {
      notification.error({
        message: 'Không thể xóa nhật ký',
        description: err.message || 'Có lỗi xảy ra khi xóa nhật ký.'
      });
    }
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

  const handleManualSyncNow = async () => {
    setManualSyncing(true);
    try {
      await triggerManualSync();
      await loadDiaries();
      notification.success({
        message: 'Hoàn tất đồng bộ',
        description: 'Đã hoàn thành kiểm tra và đồng bộ hàng đợi ngoại tuyến.'
      });
    } catch (err) {
      notification.error({
        message: 'Lỗi đồng bộ',
        description: err.message
      });
    } finally {
      setManualSyncing(false);
    }
  };

  const pendingDiaries = diaries.filter(d => d._isPendingSync);
  const syncedDiaries = diaries.filter(d => !d._isPendingSync);
  const failedDiaries = pendingDiaries.filter(d => d.rawSyncStatus === 'FAILED');
  const pendingCount = pendingDiaries.length;
  const syncedCount = syncedDiaries.length;
  const failedCount = failedDiaries.length;

  const filteredDiaries = diaries.filter(d => {
    if (syncFilter === 'UNSYNCED') return d._isPendingSync;
    if (syncFilter === 'SYNCED') return !d._isPendingSync;
    return true;
  });

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
      title: 'Trạng thái đồng bộ',
      key: 'syncStatus',
      width: 170,
      render: (_, r) => {
        if (r._isRejectedLocked) {
          return (
            <Tooltip title={r._lastError || 'Ngày nhật ký đã bị khóa sổ'}>
              <Tag icon={<LockOutlined />} color="error">
                Từ chối (Đã khóa)
              </Tag>
            </Tooltip>
          );
        }
        if (r._isPendingSync) {
          if (r.rawSyncStatus === 'FAILED') {
            return (
              <Tooltip title={`Lỗi đồng bộ: ${r._lastError || 'Không thể kết nối máy chủ'}. Số lần thử: ${r._retryCount || 1}`}>
                <Tag icon={<CloseCircleOutlined />} color="error">
                  Chưa đồng bộ (Lỗi)
                </Tag>
              </Tooltip>
            );
          }
          if (r.rawSyncStatus === 'SYNCING') {
            return (
              <Tag icon={<SyncOutlined spin />} color="processing">
                Đang đồng bộ...
              </Tag>
            );
          }
          return (
            <Tooltip title={`Bản ghi được lưu trên thiết bị lúc ${dayjs(r.offlineCreatedAt || r.createdAt).format('HH:mm DD/MM/YYYY')}. Đang chờ gửi lên máy chủ.`}>
              <Tag icon={<ClockCircleOutlined />} color="warning">
                Chưa đồng bộ
              </Tag>
            </Tooltip>
          );
        }
        return (
          <Tooltip title="Đã lưu trữ an toàn trên máy chủ">
            <Tag icon={<CheckCircleOutlined />} color="success">
              Đã đồng bộ
            </Tag>
          </Tooltip>
        );
      }
    },
    {
      title: 'Khóa sổ',
      key: 'lockStatus',
      width: 130,
      render: (_, r) => {
        const isLocked = isDateLocked(r.diaryDate);
        if (isLocked) {
          return (
            <Tag icon={<LockOutlined />} color="error">
              Đã khóa sổ
            </Tag>
          );
        }
        return (
          <Tag icon={<UnlockOutlined />} color="default">
            Mở
          </Tag>
        );
      }
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 270,
      render: (_, diary) => {
        const locked = isDateLocked(diary.diaryDate);
        return (
          <Space>
            <Button
              icon={<EyeOutlined />}
              onClick={() => setDetailDiary(diary)}
            >
              Chi tiết
            </Button>
            {diary._isRejectedLocked ? (
              <Button
                type="link"
                size="small"
                onClick={async () => {
                  retryQueueItem(diary.id);
                  notification.info({
                    message: 'Đã đưa vào hàng đợi',
                    description: 'Bản ghi đã được chuyển về trạng thái chờ đồng bộ.'
                  });
                  await loadDiaries();
                }}
              >
                Thử lại
              </Button>
            ) : (
              <Tooltip title={locked ? 'Ngày này đã bị khóa sổ, không thể chỉnh sửa.' : ''}>
                <Button
                  icon={<EditOutlined />}
                  disabled={locked || diary._isPendingSync}
                  onClick={() => startEditingDiary(diary)}
                >
                  Sửa
                </Button>
              </Tooltip>
            )}
            <Popconfirm
              title="Xóa nhật ký?"
              description="Hành động này không thể hoàn tác. Bạn có chắc chắn muốn xóa bản ghi này?"
              onConfirm={() => handleDeleteDiary(diary)}
              okText="Xóa"
              cancelText="Hủy"
              okButtonProps={{ danger: true }}
              disabled={locked}
            >
              <Tooltip title={locked ? 'Ngày này đã bị khóa sổ, không thể xóa.' : ''}>
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  disabled={locked}
                >
                  Xóa
                </Button>
              </Tooltip>
            </Popconfirm>
          </Space>
        );
      }
    }
  ];

  return (
    <div style={{ padding: '24px', width: '100%', maxWidth: '1440px', margin: '0 auto', boxSizing: 'border-box' }}>
      {/* Tiêu đề */}
      {!online && (
        <Alert
          type="warning"
          showIcon
          title="Đang ngoại tuyến"
          description="Bạn vẫn có thể tạo nhật ký. Nội dung sẽ được lưu trên thiết bị và tự đồng bộ khi có mạng."
          style={{ marginBottom: 16 }}
        />
      )}
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
              {isProjectManager() && (
                <>
                  <Button
                    icon={<LockOutlined />}
                    danger
                    onClick={() => {
                      setLockModalMode('LOCK');
                      lockForm.resetFields();
                      lockForm.setFieldsValue({ lockDate: dayjs() });
                      setLockModalOpen(true);
                    }}
                    disabled={!selectedProjectId}
                  >
                    Khóa sổ
                  </Button>
                  <Button
                    icon={<UnlockOutlined />}
                    onClick={() => {
                      setLockModalMode('UNLOCK');
                      lockForm.resetFields();
                      lockForm.setFieldsValue({ lockDate: dayjs() });
                      setLockModalOpen(true);
                    }}
                    disabled={!selectedProjectId}
                  >
                    Mở khóa sổ
                  </Button>
                </>
              )}
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

      {/* T-73 (NTDHTCT-258): Thông báo số lượng mục đang chờ đồng bộ */}
      {pendingCount > 0 && (
        <Alert
          type="warning"
          showIcon
          icon={<ClockCircleOutlined style={{ fontSize: 20, color: '#fa8c16' }} />}
          message={
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
              <span style={{ fontWeight: 600, fontSize: '15px' }}>
                Đang có {pendingCount} mục nhật ký chờ đồng bộ lên máy chủ
              </span>
              {failedCount > 0 && (
                <Tag color="error">({failedCount} mục gặp lỗi cần thử lại)</Tag>
              )}
            </div>
          }
          description={
            <div style={{ marginTop: '6px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
              <span style={{ color: '#595959' }}>
                {online
                  ? 'Dữ liệu được lưu trữ an toàn trên thiết bị và sẽ tự động gửi. Bạn cũng có thể nhấn đồng bộ thủ công ngay lập tức.'
                  : 'Thiết bị đang ngoại tuyến. Dữ liệu sẽ tự động được gửi lên máy chủ khi kết nối mạng trở lại.'}
              </span>
              <Space wrap>
                <Button
                  size="small"
                  icon={<UnorderedListOutlined />}
                  onClick={() => setQueueModalOpen(true)}
                >
                  Xem hàng đợi ({pendingCount})
                </Button>
                {online && (
                  <Button
                    type="primary"
                    size="small"
                    icon={manualSyncing ? <LoadingOutlined /> : <CloudSyncOutlined />}
                    loading={manualSyncing}
                    onClick={handleManualSyncNow}
                    style={{ backgroundColor: '#1890ff' }}
                  >
                    Đồng bộ ngay ({pendingCount} mục)
                  </Button>
                )}
              </Space>
            </div>
          }
          style={{ marginBottom: '20px', border: '1px solid #ffe58f', background: '#fffbe6' }}
        />
      )}

      {/* T-72 & T-73: Thẻ tóm tắt số lượng mục đồng bộ */}
      <Row gutter={[16, 16]} style={{ marginBottom: '20px' }}>
        <Col xs={24} sm={8}>
          <Card
            size="small"
            hoverable
            onClick={() => setSyncFilter('ALL')}
            style={{
              cursor: 'pointer',
              borderLeft: syncFilter === 'ALL' ? '4px solid #1890ff' : '4px solid #d9d9d9',
              background: '#ffffff',
              borderRadius: '6px'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Text type="secondary" style={{ fontSize: '13px' }}>Tổng số nhật ký</Text>
                <div style={{ fontSize: '22px', fontWeight: 700, color: '#262626' }}>{diaries.length}</div>
              </div>
              <BookOutlined style={{ fontSize: '24px', color: '#1890ff' }} />
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={8}>
          <Card
            size="small"
            hoverable
            onClick={() => setSyncFilter('UNSYNCED')}
            style={{
              cursor: 'pointer',
              borderLeft: syncFilter === 'UNSYNCED' ? '4px solid #faad14' : pendingCount > 0 ? '4px solid #faad14' : '4px solid #d9d9d9',
              background: pendingCount > 0 ? '#fffbe6' : '#ffffff',
              borderRadius: '6px'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Text type="secondary" style={{ fontSize: '13px' }}>Chờ đồng bộ (T-73)</Text>
                <div style={{ fontSize: '22px', fontWeight: 700, color: pendingCount > 0 ? '#d48806' : '#595959', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span>{pendingCount} mục</span>
                  {pendingCount > 0 && <Badge status="warning" />}
                </div>
              </div>
              <ClockCircleOutlined style={{ fontSize: '24px', color: '#faad14' }} />
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={8}>
          <Card
            size="small"
            hoverable
            onClick={() => setSyncFilter('SYNCED')}
            style={{
              cursor: 'pointer',
              borderLeft: syncFilter === 'SYNCED' ? '4px solid #52c41a' : '4px solid #d9d9d9',
              background: '#ffffff',
              borderRadius: '6px'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Text type="secondary" style={{ fontSize: '13px' }}>Đã đồng bộ</Text>
                <div style={{ fontSize: '22px', fontWeight: 700, color: '#389e0d' }}>{syncedCount} mục</div>
              </div>
              <CheckCircleOutlined style={{ fontSize: '24px', color: '#52c41a' }} />
            </div>
          </Card>
        </Col>
      </Row>

      <Card
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <Space align="center">
              <span>Lịch sử nhật ký</span>
              {pendingCount > 0 && (
                <Badge
                  count={`${pendingCount} chờ đồng bộ`}
                  style={{ backgroundColor: failedCount > 0 ? '#ff4d4f' : '#faad14' }}
                />
              )}
            </Space>
            <Space wrap align="center">
              <span style={{ fontSize: '13px', color: '#595959' }}>Lọc đồng bộ:</span>
              <Select
                value={syncFilter}
                onChange={setSyncFilter}
                style={{ width: 180 }}
                options={[
                  { label: `Tất cả (${diaries.length})`, value: 'ALL' },
                  { label: `Chưa đồng bộ (${pendingCount})`, value: 'UNSYNCED' },
                  { label: `Đã đồng bộ (${syncedCount})`, value: 'SYNCED' }
                ]}
              />
              <Text type="secondary" style={{ fontSize: '13px' }}>
                Hiển thị: {filteredDiaries.length}/{diaries.length} bản ghi
              </Text>
            </Space>
          </div>
        }
        style={{ width: '100%' }}
      >
        <Table
          dataSource={filteredDiaries}
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
          <Tooltip
            key="edit"
            title={
              detailDiary && isDateLocked(detailDiary.diaryDate)
                ? 'Ngày này đã bị khóa sổ, không thể chỉnh sửa.'
                : ''
            }
          >
            <Button
              type="primary"
              icon={<EditOutlined />}
              disabled={
                Boolean(detailDiary?._isPendingSync) ||
                Boolean(detailDiary?._isRejectedLocked) ||
                (detailDiary && isDateLocked(detailDiary.diaryDate))
              }
              onClick={() => startEditingDiary(detailDiary)}
            >
              Chỉnh sửa
            </Button>
          </Tooltip>
        ]}
      >
        {detailDiary && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            {detailDiary && isDateLocked(detailDiary.diaryDate) && (
              <Alert
                type="error"
                showIcon
                message="Ngày này đã bị khóa sổ. Toàn bộ thao tác chỉnh sửa đều bị khóa."
              />
            )}
            {detailDiary._isRejectedLocked && (
              <Alert
                type="error"
                showIcon
                message={`Bị từ chối đồng bộ: ${detailDiary._lastError || 'Ngày nhật ký đã bị khóa sổ'}`}
              />
            )}
            {/* T-72: Chi tiết trạng thái đồng bộ của nhật ký */}
            <div
              style={{
                padding: '10px 14px',
                borderRadius: '6px',
                background: detailDiary._isRejectedLocked
                  ? '#fff2f0'
                  : detailDiary._isPendingSync
                    ? '#fffbe6'
                    : '#f6ffed',
                border: `1px solid ${
                  detailDiary._isRejectedLocked
                    ? '#ffccc7'
                    : detailDiary._isPendingSync
                      ? '#ffe58f'
                      : '#b7eb8f'
                }`
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                <Text strong>Trạng thái đồng bộ:</Text>
                {detailDiary._isRejectedLocked ? (
                  <Tag icon={<LockOutlined />} color="error">Từ chối (Đã khóa sổ)</Tag>
                ) : detailDiary._isPendingSync ? (
                  detailDiary.rawSyncStatus === 'FAILED' ? (
                    <Tag icon={<CloseCircleOutlined />} color="error">Chưa đồng bộ (Gặp lỗi)</Tag>
                  ) : detailDiary.rawSyncStatus === 'SYNCING' ? (
                    <Tag icon={<SyncOutlined spin />} color="processing">Đang gửi lên máy chủ...</Tag>
                  ) : (
                    <Tag icon={<ClockCircleOutlined />} color="warning">Chưa đồng bộ (Ngoại tuyến)</Tag>
                  )
                ) : (
                  <Tag icon={<CheckCircleOutlined />} color="success">Đã đồng bộ lên máy chủ</Tag>
                )}
              </div>
              {detailDiary._isRejectedLocked ? (
                <div style={{ marginTop: '6px', fontSize: '12px', color: '#cf1322' }}>
                  Bản ghi này chưa thể đồng bộ lên máy chủ vì ngày ghi nhật ký đã bị khóa sổ. Dữ liệu vẫn được giữ an toàn trên thiết bị.
                </div>
              ) : detailDiary._isPendingSync ? (
                <div style={{ marginTop: '6px', fontSize: '12px', color: '#ad6800' }}>
                  Bản ghi này đang được lưu trên thiết bị và chưa được tải lên máy chủ. Hệ thống sẽ tự động đồng bộ khi có kết nối mạng.
                  {detailDiary.offlineCreatedAt && (
                    <div style={{ marginTop: 2 }}>Thời điểm tạo: {dayjs(detailDiary.offlineCreatedAt).format('HH:mm:ss DD/MM/YYYY')}</div>
                  )}
                  {detailDiary._lastError && (
                    <div style={{ color: '#cf1322', marginTop: 2 }}>Chi tiết lỗi: {detailDiary._lastError} (Thử {detailDiary._retryCount} lần)</div>
                  )}
                </div>
              ) : (
                <div style={{ marginTop: '4px', fontSize: '12px', color: '#389e0d' }}>
                  Bản ghi đã được lưu trữ an toàn và xác thực trên máy chủ.
                </div>
              )}
            </div>

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

      {/* Modal Khóa / Mở khóa sổ (T-64) */}
      <Modal
        title={
          <Space>
            {lockModalMode === 'LOCK' ? <LockOutlined style={{ color: '#ff4d4f' }} /> : <UnlockOutlined style={{ color: '#52c41a' }} />}
            <span>{lockModalMode === 'LOCK' ? 'Khóa sổ nhật ký theo ngày' : 'Mở khóa sổ nhật ký theo ngày'}</span>
          </Space>
        }
        open={lockModalOpen}
        onCancel={() => {
          setLockModalOpen(false);
          lockForm.resetFields();
        }}
        footer={null}
        destroyOnClose
      >
        <Form
          form={lockForm}
          layout="vertical"
          onFinish={async values => {
            if (!selectedProjectId) return;
            setLockActionSubmitting(true);
            try {
              const dateStr = values.lockDate.format('YYYY-MM-DD');
              if (lockModalMode === 'LOCK') {
                await lockSiteDiaryDate(selectedProjectId, dateStr, values.reason);
                notification.success({
                  message: 'Khóa sổ thành công',
                  description: `Đã khóa sổ nhật ký ngày ${values.lockDate.format('DD/MM/YYYY')}.`
                });
              } else {
                await unlockSiteDiaryDate(selectedProjectId, dateStr, values.reason);
                notification.success({
                  message: 'Mở khóa sổ thành công',
                  description: `Đã mở khóa sổ nhật ký ngày ${values.lockDate.format('DD/MM/YYYY')}.`
                });
              }
              setLockModalOpen(false);
              lockForm.resetFields();
              await loadDiaries();
            } catch (err) {
              notification.error({
                message: lockModalMode === 'LOCK' ? 'Lỗi khóa sổ' : 'Lỗi mở khóa sổ',
                description: err.response?.data?.message || err.message
              });
            } finally {
              setLockActionSubmitting(false);
            }
          }}
          initialValues={{ lockDate: dayjs() }}
        >
          <Form.Item
            name="lockDate"
            label="Ngày áp dụng"
            rules={[{ required: true, message: 'Vui lòng chọn ngày' }]}
          >
            <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
          </Form.Item>

          <Form.Item
            name="reason"
            label={lockModalMode === 'LOCK' ? 'Lý do khóa sổ' : 'Lý do mở khóa sổ'}
            rules={[{ required: lockModalMode === 'UNLOCK', message: 'Vui lòng nhập lý do' }]}
          >
            <TextArea
              rows={3}
              placeholder={
                lockModalMode === 'LOCK'
                  ? 'Ví dụ: Đã chốt số liệu và kiểm toán khối lượng thi công ngày...'
                  : 'Ví dụ: Cần bổ sung quân số hoặc khắc phục sai lệch báo cáo...'
              }
            />
          </Form.Item>

          <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={() => setLockModalOpen(false)}>Hủy</Button>
            <Button
              type="primary"
              danger={lockModalMode === 'LOCK'}
              htmlType="submit"
              loading={lockActionSubmitting}
              icon={lockModalMode === 'LOCK' ? <LockOutlined /> : <UnlockOutlined />}
            >
              {lockModalMode === 'LOCK' ? 'Xác nhận khóa sổ' : 'Xác nhận mở khóa'}
            </Button>
          </Space>
        </Form>
      </Modal>

      <SyncQueueModal
        open={queueModalOpen}
        onClose={() => setQueueModalOpen(false)}
        onSyncNow={handleManualSyncNow}
        isSyncing={manualSyncing}
      />
    </div>
  );
};

export default SiteDiaryPage;
