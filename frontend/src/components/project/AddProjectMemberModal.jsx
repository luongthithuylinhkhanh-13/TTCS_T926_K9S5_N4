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
  UserAddOutlined,
  SaveOutlined,
  UserOutlined,
  SafetyCertificateOutlined,
  ProjectOutlined
} from '@ant-design/icons';
import { getAvailableUsers, addProjectMember } from '../../services/projectMemberApi.js';
import { ROLE_DISPLAY_NAMES } from '../../utils/permissions.js';

const { Text } = Typography;

export const PROJECT_ASSIGNABLE_ROLES = [
  {
    value: 'PROJECT_MANAGER',
    label: 'Ban quản lý dự án (Project Manager)',
    description: 'Toàn quyền điều hành toàn diện dự án, cơ cấu công việc (WBS), tiến độ, chi phí và thanh toán'
  },
  {
    value: 'CUSTOMER',
    label: 'Chủ đầu tư',
    description: 'Theo dõi tiến độ, mốc dự án, phê duyệt và quản lý chung của Chủ đầu tư'
  },
  {
    value: 'SITE_MANAGER',
    label: 'Chỉ huy trưởng công trường',
    description: 'Điều hành toàn diện công trường, cơ cấu công việc (WBS), tiến độ và phân công công việc'
  },
  {
    value: 'SITE_ENGINEER',
    label: 'Kỹ sư giám sát',
    description: 'Giám sát kỹ thuật hiện trường, quản lý nhật ký công trường và nghiệm thu công việc'
  },
  {
    value: 'WORKER',
    label: 'Đội trưởng thi công',
    description: 'Tiếp nhận công việc được giao, điều phối tổ đội, cập nhật tiến độ và báo cáo khó khăn'
  },
  {
    value: 'STAFF',
    label: 'Kế toán chủ đầu tư',
    description: 'Quản lý dự toán, thẩm định chi phí thực tế, nghiệm thu khối lượng và đề nghị thanh toán'
  }
];

export default function AddProjectMemberModal({
  open,
  project,
  onClose,
  onSuccess
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [availableUsers, setAvailableUsers] = useState([]);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (!open || !project?.id) {
      setAvailableUsers([]);
      setErrorMessage('');
      form.resetFields();
      return;
    }

    let isMounted = true;
    const fetchUsers = async () => {
      setLoading(true);
      setErrorMessage('');
      try {
        const users = await getAvailableUsers(project.id);
        if (isMounted) {
          setAvailableUsers(Array.isArray(users) ? users : []);
        }
      } catch (err) {
        if (isMounted) {
          setErrorMessage(err.message || 'Không thể tải danh sách tài khoản');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    fetchUsers();

    return () => {
      isMounted = false;
    };
  }, [open, project?.id, form]);

  const handleSubmit = async (values) => {
    setSubmitting(true);
    setErrorMessage('');
    try {
      await addProjectMember(project.id, values.userId, values.roleCode);
      notification.success({
        message: 'Thành công',
        description: 'Đã thêm thành viên và gán vai trò trong dự án thành công!'
      });
      form.resetFields();
      onSuccess?.();
      onClose();
    } catch (err) {
      const msg = err.message || 'Không thể thêm thành viên vào dự án';
      setErrorMessage(msg);
      notification.error({
        message: 'Lỗi',
        description: msg
      });
    } finally {
      setSubmitting(false);
    }
  };

  const selectedUserId = Form.useWatch('userId', form);
  const selectedUserObj = availableUsers.find(u => u.id === selectedUserId);

  return (
    <Modal
      title={
        <Space>
          <UserAddOutlined style={{ color: '#2563EB' }} />
          <span>THÊM THÀNH VIÊN DỰ ÁN</span>
        </Space>
      }
      open={open}
      onCancel={onClose}
      footer={null}
      destroyOnClose
      width={600}
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
          padding: '12px 16px',
          marginBottom: 20
        }}>
          <Space orientation="vertical" size={4} style={{ width: '100%' }}>
            <Text type="secondary" style={{ fontSize: 13 }}>Dự án:</Text>
            <Space>
              <ProjectOutlined style={{ color: '#2563EB' }} />
              <Text strong style={{ fontSize: 15, color: '#0F172A' }}>
                {project ? `${project.code} - ${project.name}` : 'Chưa chọn dự án'}
              </Text>
            </Space>
          </Space>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '32px 0' }}>
            <Spin tip="Đang tải danh sách tài khoản..." />
          </div>
        ) : (
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
            initialValues={{ roleCode: undefined, userId: undefined }}
          >
            <Form.Item
              name="userId"
              label={<span style={{ fontWeight: 600 }}>Tài khoản:</span>}
              rules={[{ required: true, message: 'Vui lòng chọn tài khoản' }]}
              extra="Chỉ hiển thị các tài khoản đã kích hoạt (ACTIVE). Không chọn được tài khoản Quản trị hệ thống."
            >
              <Select
                showSearch
                placeholder="Chọn tài khoản..."
                optionFilterProp="label"
                filterOption={(input, option) =>
                  (option?.filterText || '').toLowerCase().includes(input.toLowerCase())
                }
                options={availableUsers.map(u => ({
                  value: u.id,
                  label: (
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span>
                        <strong>{u.fullName || u.email}</strong> - {u.email}
                      </span>
                      {u.isAlreadyMember && (
                        <Tag color="orange" style={{ marginLeft: 8 }}>Đã thuộc dự án</Tag>
                      )}
                    </div>
                  ),
                  filterText: `${u.fullName || ''} ${u.email}`,
                  disabled: u.isAlreadyMember
                }))}
              />
            </Form.Item>

            {selectedUserObj?.isAlreadyMember && (
              <Alert
                type="warning"
                message="Thành viên đã thuộc dự án"
                description="Tài khoản này đã là thành viên chính thức của dự án. Vui lòng sử dụng chức năng 'Đổi vai trò' trong danh sách để cập nhật vai trò."
                showIcon
                style={{ marginBottom: 16 }}
              />
            )}

            <Form.Item
              name="roleCode"
              label={<span style={{ fontWeight: 600 }}>Vai trò trong dự án:</span>}
              rules={[{ required: true, message: 'Vui lòng chọn vai trò trong dự án' }]}
              extra="Ban quản lý dự án có quyền phân 5 vai trò nghiệp vụ trong dự án (Không thể gán vai trò Quản trị hệ thống)."
            >
              <Select
                placeholder="Chọn vai trò..."
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
                  disabled={selectedUserObj?.isAlreadyMember}
                  style={{ background: '#2563EB', borderColor: '#2563EB' }}
                >
                  Thêm thành viên
                </Button>
              </Space>
            </div>
          </Form>
        )}
      </div>
    </Modal>
  );
}
