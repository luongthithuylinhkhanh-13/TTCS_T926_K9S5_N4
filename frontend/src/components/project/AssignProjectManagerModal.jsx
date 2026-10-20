import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Select,
  Button,
  Space,
  Typography,
  Tag,
  notification,
  Spin,
  Alert
} from 'antd';
import {
  UserOutlined,
  SaveOutlined,
  CheckCircleOutlined,
  TeamOutlined,
  CrownOutlined
} from '@ant-design/icons';
import { getAdminUsers, getProjectManager, assignProjectManager } from '../../services/adminApi.js';

const { Text } = Typography;

export default function AssignProjectManagerModal({
  open,
  project,
  onClose,
  onSuccess
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [currentManagers, setCurrentManagers] = useState([]);
  const [activeUsers, setActiveUsers] = useState([]);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (!open || !project?.id) {
      setCurrentManagers([]);
      setActiveUsers([]);
      setErrorMessage('');
      form.resetFields();
      return;
    }

    let isMounted = true;
    const loadData = async () => {
      setLoading(true);
      setErrorMessage('');
      try {
        const [managersRes, usersRes] = await Promise.all([
          getProjectManager(project.id),
          getAdminUsers({ status: 'ACTIVE' })
        ]);

        if (isMounted) {
          const managers = Array.isArray(managersRes) ? managersRes : [];
          setCurrentManagers(managers);

          const users = Array.isArray(usersRes) ? usersRes : [];
          // Chỉ lấy tài khoản ACTIVE và loại trừ tài khoản Quản trị viên (admin@admin.com / role ADMIN)
          const validCandidates = users.filter(
            u =>
              u.status === 'ACTIVE' &&
              u.email?.toLowerCase() !== 'admin@admin.com' &&
              u.roleName?.toUpperCase() !== 'ADMIN'
          );
          setActiveUsers(validCandidates);

          // Nếu đã có 1 manager hiện tại, có thể đặt default value
          if (managers.length === 1 && managers[0]?.userId) {
            form.setFieldsValue({ userId: managers[0].userId });
          }
        }
      } catch (err) {
        if (isMounted) {
          setErrorMessage(
            err.response?.data?.message || err.message || 'Không thể tải thông tin dự án'
          );
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    loadData();

    return () => {
      isMounted = false;
    };
  }, [open, project?.id, form]);

  const handleSubmit = async (values) => {
    if (!project?.id || !values.userId) return;

    setSubmitting(true);
    setErrorMessage('');
    try {
      await assignProjectManager(project.id, values.userId);
      notification.success({
        message: 'Thành công',
        description: `Đã phân Ban quản lý dự án cho ${project.code} - ${project.name}`
      });
      onSuccess?.();
      onClose();
    } catch (err) {
      const msg =
        err.response?.data?.message || err.message || 'Không thể phân Ban quản lý dự án';
      setErrorMessage(msg);
      notification.error({
        message: 'Thất bại',
        description: msg
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={
        <Space>
          <CrownOutlined style={{ color: '#F59E0B' }} />
          <span>Phân Ban quản lý dự án</span>
        </Space>
      }
      open={open}
      onCancel={onClose}
      footer={null}
      destroyOnClose
      width={560}
    >
      <Spin spinning={loading}>
        <div style={{ marginTop: 12, marginBottom: 20 }}>
          <div
            style={{
              padding: '12px 16px',
              backgroundColor: '#F8FAFC',
              borderRadius: 8,
              border: '1px solid #E2E8F0',
              marginBottom: 16
            }}
          >
            <div style={{ marginBottom: 8 }}>
              <Text type="secondary" style={{ fontSize: 13 }}>
                Dự án:
              </Text>
              <div>
                <Text strong style={{ fontSize: 15, color: '#1E293B' }}>
                  {project?.code} - {project?.name}
                </Text>
              </div>
            </div>

            <div>
              <Text type="secondary" style={{ fontSize: 13 }}>
                Ban quản lý hiện tại:
              </Text>
              <div style={{ marginTop: 4 }}>
                {currentManagers.length > 0 ? (
                  currentManagers.map(m => (
                    <Tag
                      key={m.userId || m.id}
                      color="blue"
                      icon={<CheckCircleOutlined />}
                      style={{ fontSize: 13, padding: '4px 8px', marginBottom: 4 }}
                    >
                      <strong>{m.fullName || m.email}</strong> ({m.email})
                    </Tag>
                  ))
                ) : (
                  <Text type="secondary" italic>
                    Chưa phân Ban quản lý
                  </Text>
                )}
              </div>
            </div>
          </div>

          {errorMessage && (
            <Alert
              type="error"
              showIcon
              message={errorMessage}
              style={{ marginBottom: 16 }}
            />
          )}

          <Form
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
            autoComplete="off"
          >
            <Form.Item
              name="userId"
              label={
                <span style={{ fontWeight: 600 }}>
                  Chọn tài khoản Ban quản lý dự án:
                </span>
              }
              rules={[
                {
                  required: true,
                  message: 'Vui lòng chọn một tài khoản để gán Ban quản lý dự án'
                }
              ]}
              extra="Tài khoản được chọn sẽ có quyền điều hành WBS, tiến độ và phân chia nhân sự trong dự án này."
            >
              <Select
                placeholder="-- Chọn tài khoản thành viên ACTIVE --"
                showSearch
                optionFilterProp="label"
                options={activeUsers.map(u => ({
                  value: u.id,
                  label: `${u.fullName || u.email} - ${u.email}`
                }))}
                style={{ width: '100%' }}
              />
            </Form.Item>

            <div
              style={{
                display: 'flex',
                justifyContent: 'flex-end',
                gap: 12,
                marginTop: 24
              }}
            >
              <Button onClick={onClose} disabled={submitting}>
                Hủy
              </Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={submitting}
                icon={<SaveOutlined />}
                style={{ backgroundColor: '#2563EB' }}
              >
                Xác nhận
              </Button>
            </div>
          </Form>
        </div>
      </Spin>
    </Modal>
  );
}
