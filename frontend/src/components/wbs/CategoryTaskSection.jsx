import React, { useEffect, useState } from 'react';
import { Button, Empty, Form, Input, InputNumber, List, Modal, Spin, Tag, message } from 'antd';
import { ClockCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { createCategoryTask, getCategoryTasks } from '../../services/categoryTaskApi';

const CategoryTaskSection = ({ projectId, category }) => {
  const [form] = Form.useForm();
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;

    if (!projectId || !category?.id || category.type === 'task') {
      setTasks([]);
      return () => {
        active = false;
      };
    }

    setLoading(true);
    getCategoryTasks(projectId, category.id)
      .then(data => {
        if (active) setTasks(Array.isArray(data) ? data : []);
      })
      .catch(error => {
        if (active) message.error(error.message || 'Không thể tải công việc của hạng mục');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [projectId, category?.id, category?.type]);

  if (!category || category.type === 'task') return null;

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      const task = await createCategoryTask(projectId, category.id, values);
      setTasks(current => [...current, task]);
      setModalOpen(false);
      form.resetFields();
      message.success('Đã thêm công việc vào hạng mục');
    } catch (error) {
      if (error?.errorFields) return;
      message.error(error.message || 'Không thể tạo công việc');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="drawer-section">
      <div className="drawer-section-title category-task-heading">
        <span>CÔNG VIỆC TRONG HẠNG MỤC ({tasks.length})</span>
        <Button
          size="small"
          icon={<PlusOutlined />}
          onClick={() => setModalOpen(true)}
        >
          Thêm task
        </Button>
      </div>

      <Spin spinning={loading}>
        {tasks.length ? (
          <List
            size="small"
            dataSource={tasks}
            renderItem={task => (
              <List.Item className="category-task-item">
                <span>{task.name}</span>
                <Tag icon={<ClockCircleOutlined />} color="blue">
                  {task.duration} ngày
                </Tag>
              </List.Item>
            )}
          />
        ) : (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Chưa có công việc trong hạng mục này"
          />
        )}
      </Spin>

      <Modal
        title={`Thêm công việc · ${category.name}`}
        open={modalOpen}
        onCancel={() => {
          if (!saving) {
            setModalOpen(false);
            form.resetFields();
          }
        }}
        onOk={handleCreate}
        confirmLoading={saving}
        okText="Tạo công việc"
        cancelText="Hủy"
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            label="Tên công việc"
            name="name"
            rules={[
              { required: true, whitespace: true, message: 'Nhập tên công việc' },
              { max: 255, message: 'Tên công việc tối đa 255 ký tự' }
            ]}
          >
            <Input maxLength={255} placeholder="Ví dụ: Đào đất móng" />
          </Form.Item>
          <Form.Item
            label="Thời lượng (ngày)"
            name="duration"
            rules={[{ required: true, message: 'Nhập thời lượng' }]}
          >
            <InputNumber min={1} precision={0} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default CategoryTaskSection;