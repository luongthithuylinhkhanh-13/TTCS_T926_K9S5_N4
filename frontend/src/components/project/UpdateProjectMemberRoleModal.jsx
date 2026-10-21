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
  Alert
} from 'antd';
import {
  EditOutlined,
  SaveOutlined,
  UserOutlined,
  ProjectOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons';
import { updateProjectMemberRole } from '../../services/projectMemberApi.js';
import { PROJECT_ASSIGNABLE_ROLES } from './AddProjectMemberModal.jsx';
import { ROLE_DISPLAY_NAMES } from '../../utils/permissions.js';

const { Text } = Typography;

export default function UpdateProjectMemberRoleModal({
  open,
  project,
  member,
  onClose,
  onSuccess
}) {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (!open || !member) {
      setErrorMessage('');
      form.resetFields();
      return;
    }

    form.setFieldsValue({
      newRoleName: member.roleCode || member.roleName
    });
  }, [open, member, form]);

  const handleSubmit = async (values) => {
    if (!project?.id || !member?.userId) return;

    setSubmitting(true);
    setErrorMessage('');
    try {
      await updateProjectMemberRole(project.id, member.userId, values.newRoleName);
      notification.success({
        message: 'Thành công',
        description: `Đã đổi vai trò thành viên sang "${ROLE_DISPLAY_NAMES[values.newRoleName] || values.newRoleName}" thành công!`
      });
      onSuccess?.();
      onClose();
    } catch (err) {
      const msg = err.message || 'Không thể cập nhật vai trò thành viên';
      setErrorMessage(msg);
      notification.error({
        message: 'Lỗi',
        description: msg
      });
    } finally {
      setSubmitting(false);
    }
  };

  const currentRoleName = member?.roleCode || member?.roleName;
  const currentRoleLabel = ROLE_DISPLAY_NAMES[currentRoleName] || currentRoleName || 'Chưa xác định';

  return (
    <Modal
      title={
        <Space>
          <EditOutlined style={{ color: '#2563EB' }} />
          <span>ĐỔI VAI TRÒ THÀNH VIÊN TRONG DỰ ÁN</span>
        </Space>
      }
      open={open}
      onCancel={onClose}
      footer={null}
      destroyOnClose
      width={560}
    >
      <div style={{ marginTop: 16 }}>
        {errorMessage && (
          <Alert
            type="error"
            message={errorMessage}
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        <div style={{
          background: '#F8FAFC',
          border: '1px solid #E2E8F0',
          borderRadius: 8,
          padding: '14px 16px',
          marginBottom: 20
        }}>
          <Space direction="vertical" size={10} style={{ width: '100%' }}>
            <div>
              <Text orientation="horizontal" type="secondary" style={{ fontSize: 13, marginRight: 8 }}>Dự án:</Text>
              <Text strong style={{ color: '#0F172A' }}>
                <ProjectOutlined style={{ color: '#2563EB', marginRight: 6 }} />
                {project ? `${project.code} - ${project.name}` : ''}
              </Text>
            </div>

            <div>
              <Text type="secondary" style={{ fontSize: 13, marginRight: 8 }}>Thành viên:</Text>
              <Text strong style={{ color: '#0F172A' }}>
                <UserOutlined style={{ color: '#0284C7', marginRight: 6 }} />
                {member?.fullName} ({member?.email})
              </Text>
            </div>

            <div>
              <Text type="secondary" style={{ fontSize: 13, marginRight: 8 }}>Vai trò hiện tại:</Text>
              <Tag color="blue" style={{ fontSize: 13, padding: '2px 10px' }}>
                {currentRoleLabel}
              </Tag>
            </div>
          </Space>
        </div>

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            name="newRoleName"
            label={<span style={{ fontWeight: 600 }}>Vai trò mới trong dự án:</span>}
            rules={[{ required: true, message: 'Vui lòng chọn vai trò mới' }]}
            extra="Vai trò mới chỉ áp dụng trong phạm vi dự án này, không ảnh hưởng đến các dự án khác hay vai trò hệ thống."
          >
            <Select
              placeholder="Chọn vai trò mới..."
              options={PROJECT_ASSIGNABLE_ROLES.map(r => ({
                value: r.value,
                label: (
                  <div>
                    <div style={{ fontWeight: 600 }}>{r.label}</div>
                    <div style={{ fontSize: 12, color: '#64748B' }}>{r.description}</div>
                  </div>
                )
              }))}
            />
          </Form.Item>

          <div style={{ textAlign: 'right', marginTop: 24, borderTop: '1px solid #E2E8F0', paddingTop: 16 }}>
            <Space>
              <Button onClick={onClose} disabled={submitting}>
                Hủy
              </Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={submitting}
                icon={<SaveOutlined />}
                style={{ background: '#2563EB', borderColor: '#2563EB' }}
              >
                Lưu thay đổi
              </Button>
            </Space>
          </div>
        </Form>
      </div>
    </Modal>
  );
}
