import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  InputNumber,
  Button,
  Alert,
  Space,
  Typography,
  Tag,
  notification,
  Spin,
  Descriptions
} from 'antd';
import {
  EnvironmentOutlined,
  SaveOutlined,
  LockOutlined,
  CheckCircleOutlined,
  WarningOutlined
} from '@ant-design/icons';
import {
  getProjectSiteLocation,
  updateProjectSiteLocation,
  validateSiteLocation
} from '../../services/projectLocationApi.js';
import { isProjectManager } from '../../utils/permissions.js';

const { Text } = Typography;

export default function ProjectSiteLocationModal({
  open,
  project,
  onClose,
  onSuccess
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [siteLocation, setSiteLocation] = useState(null);

  const canEdit = isProjectManager();

  useEffect(() => {
    if (!open || !project?.id) {
      setSiteLocation(null);
      form.resetFields();
      return;
    }

    let isMounted = true;
    const fetchLocation = async () => {
      setLoading(true);
      try {
        const data = await getProjectSiteLocation(project.id);
        if (isMounted) {
          setSiteLocation(data);
          form.setFieldsValue({
            latitude: data?.latitude,
            longitude: data?.longitude,
            radiusMeters: data?.radiusMeters
          });
        }
      } catch (err) {
        if (isMounted) {
          notification.error({
            message: 'Không thể tải vị trí công trường',
            description: err.message
          });
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    fetchLocation();

    return () => {
      isMounted = false;
    };
  }, [open, project?.id, form]);

  const handleFinish = async values => {
    const validation = validateSiteLocation(values);
    if (!validation.valid) {
      notification.error({
        message: 'Dữ liệu không hợp lệ',
        description: validation.errors.join('; ')
      });
      return;
    }

    setSaving(true);
    try {
      const updated = await updateProjectSiteLocation(project.id, values);
      setSiteLocation(updated);
      notification.success({
        message: 'Thành công',
        description: 'Đã cập nhật vị trí và bán kính công trường cho dự án'
      });
      if (onSuccess) {
        onSuccess(updated);
      }
      onClose();
    } catch (err) {
      notification.error({
        message: 'Lỗi cập nhật cấu hình',
        description: err.message
      });
    } finally {
      setSaving(false);
    }
  };

  const isConfigured = Boolean(
    siteLocation?.configured &&
    siteLocation?.latitude != null &&
    siteLocation?.longitude != null &&
    siteLocation?.radiusMeters != null
  );

  return (
    <Modal
      title={
        <Space>
          <EnvironmentOutlined style={{ color: '#2563EB' }} />
          <span>Cấu hình vị trí công trường (Geofence)</span>
        </Space>
      }
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="close" onClick={onClose} disabled={saving}>
          Đóng
        </Button>,
        canEdit && (
          <Button
            key="submit"
            type="primary"
            icon={<SaveOutlined />}
            loading={saving}
            disabled={loading}
            onClick={() => form.submit()}
          >
            Lưu cấu hình
          </Button>
        )
      ]}
      destroyOnClose
      width={560}
    >
      <Spin spinning={loading}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Descriptions size="small" bordered column={1}>
            <Descriptions.Item label="Dự án">
              <strong>{project?.name || '---'}</strong> ({project?.code || '---'})
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái định vị">
              {isConfigured ? (
                <Tag color="success" icon={<CheckCircleOutlined />}>
                  Đã cấu hình (Bán kính: {siteLocation.radiusMeters}m)
                </Tag>
              ) : (
                <Tag color="warning" icon={<WarningOutlined />}>
                  Chưa cấu hình vị trí
                </Tag>
              )}
            </Descriptions.Item>
          </Descriptions>

          {isConfigured ? (
            <Alert
              type="info"
              showIcon
              message="Công trường đã được thiết lập phạm vi"
              description={
                <span>
                  Tọa độ tâm: <strong>{siteLocation.latitude?.toFixed(6)}, {siteLocation.longitude?.toFixed(6)}</strong>
                  <br />
                  Bán kính hợp lệ: <strong>{siteLocation.radiusMeters} mét</strong>. Các ảnh hiện trường nằm ngoài phạm vi này sẽ bị đánh dấu nghi vấn.
                </span>
              }
            />
          ) : (
            <Alert
              type="warning"
              showIcon
              message="Chưa cấu hình tọa độ công trường"
              description="Dự án chưa có tọa độ tâm và bán kính hợp lệ. Hãy khai báo để hệ thống tự động phát hiện các ảnh chụp ngoài hiện trường."
            />
          )}

          {!canEdit && (
            <Alert
              type="warning"
              showIcon
              icon={<LockOutlined />}
              message="Quyền chỉ đọc"
              description="Chỉ Quản lý dự án (Project Manager) hoặc Quản trị viên (Admin) mới có quyền chỉnh sửa cấu hình vị trí công trường."
            />
          )}

          <Form
            form={form}
            layout="vertical"
            onFinish={handleFinish}
            disabled={!canEdit || saving}
          >
            <Form.Item
              name="latitude"
              label="Vĩ độ tâm (Latitude)"
              tooltip="Tọa độ vĩ độ tâm công trường trong khoảng từ -90° đến +90°"
              rules={[
                { required: true, message: 'Vui lòng nhập vĩ độ tâm' },
                {
                  validator: (_, val) => {
                    if (val === undefined || val === null || val === '') return Promise.resolve();
                    const n = Number(val);
                    if (isNaN(n) || n < -90 || n > 90) {
                      return Promise.reject(new Error('Vĩ độ phải nằm trong khoảng [-90, 90]'));
                    }
                    return Promise.resolve();
                  }
                }
              ]}
            >
              <InputNumber
                style={{ width: '100%' }}
                step={0.000001}
                precision={6}
                placeholder="VD: 21.028511"
              />
            </Form.Item>

            <Form.Item
              name="longitude"
              label="Kinh độ tâm (Longitude)"
              tooltip="Tọa độ kinh độ tâm công trường trong khoảng từ -180° đến +180°"
              rules={[
                { required: true, message: 'Vui lòng nhập kinh độ tâm' },
                {
                  validator: (_, val) => {
                    if (val === undefined || val === null || val === '') return Promise.resolve();
                    const n = Number(val);
                    if (isNaN(n) || n < -180 || n > 180) {
                      return Promise.reject(new Error('Kinh độ phải nằm trong khoảng [-180, 180]'));
                    }
                    return Promise.resolve();
                  }
                }
              ]}
            >
              <InputNumber
                style={{ width: '100%' }}
                step={0.000001}
                precision={6}
                placeholder="VD: 105.854212"
              />
            </Form.Item>

            <Form.Item
              name="radiusMeters"
              label="Bán kính hợp lệ của công trường (mét)"
              tooltip="Khoảng cách tối đa cho phép từ tâm công trường tính bằng mét (ví dụ 200m)"
              rules={[
                { required: true, message: 'Vui lòng nhập bán kính công trường' },
                {
                  validator: (_, val) => {
                    if (val === undefined || val === null || val === '') return Promise.resolve();
                    const n = Number(val);
                    if (isNaN(n) || n <= 0) {
                      return Promise.reject(new Error('Bán kính phải lớn hơn 0 mét'));
                    }
                    return Promise.resolve();
                  }
                }
              ]}
            >
              <InputNumber
                style={{ width: '100%' }}
                min={1}
                max={100000}
                step={10}
                placeholder="VD: 200"
                addonAfter="mét"
              />
            </Form.Item>
          </Form>
        </div>
      </Spin>
    </Modal>
  );
}
