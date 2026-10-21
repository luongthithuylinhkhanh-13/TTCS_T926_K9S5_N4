import React, { useEffect, useState } from 'react';
import { Button, Col, Empty, Form, Input, InputNumber, List, Modal, Row, Select, Spin, Tag, message } from 'antd';
import { ClockCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { createCategoryTask, getCategoryTasks } from '../../services/categoryTaskApi';
import { useLocale } from '../../utils/LocaleContext';

const CategoryTaskSection = ({ projectId, category }) => {
  const { t } = useLocale();
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
        if (active) message.error(error.message || t('Không thể tải công việc của hạng mục'));
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
      message.success(t('Đã thêm công việc vào hạng mục'));
    } catch (error) {
      if (error?.errorFields) return;
      message.error(error.message || t('Không thể tạo công việc'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="drawer-section">
      <div className="drawer-section-title category-task-heading">
        <span>{t('CÔNG VIỆC TRONG HẠNG MỤC')} ({tasks.length})</span>
        <Button
          size="small"
          icon={<PlusOutlined />}
          onClick={() => setModalOpen(true)}
        >
          {t('Thêm task')}
        </Button>
      </div>

      <Spin spinning={loading}>
        {tasks.length ? (
          <List
            size="small"
            dataSource={tasks}
            renderItem={task => (
              <List.Item className="category-task-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <span style={{ fontWeight: 600 }}>{task.name}</span>
                  {(task.quantity != null || task.unitPrice != null) && (
                    <span style={{ fontSize: 12, color: '#64748B' }}>
                      {task.quantity != null && (
                        <span>KL: <strong>{task.quantity} {task.unit || ''}</strong></span>
                      )}
                      {task.unitPrice != null && (
                        <span> · {Number(task.unitPrice).toLocaleString('vi-VN')} đ/đv</span>
                      )}
                      {task.totalPrice != null && (
                        <span style={{ color: '#047857', fontWeight: 600 }}> = {Number(task.totalPrice).toLocaleString('vi-VN')} đ</span>
                      )}
                    </span>
                  )}
                </div>
                <Tag icon={<ClockCircleOutlined />} color="blue">
                  {task.duration} {t('ngày')}
                </Tag>
              </List.Item>
            )}
          />
        ) : (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={t('Chưa có công việc trong hạng mục này')}
          />
        )}
      </Spin>

      <Modal
        title={`${t('Thêm công việc')} · ${category.name}`}
        open={modalOpen}
        onCancel={() => {
          if (!saving) {
            setModalOpen(false);
            form.resetFields();
          }
        }}
        onOk={handleCreate}
        confirmLoading={saving}
        okText={t('Tạo công việc')}
        cancelText={t('Hủy')}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            label={t('Tên công việc')}
            name="name"
            rules={[
              { required: true, whitespace: true, message: t('Nhập tên công việc') },
              { max: 255, message: t('Tên công việc tối đa 255 ký tự') }
            ]}
          >
            <Input maxLength={255} placeholder={t('Ví dụ: Đào đất móng')} />
          </Form.Item>
          <Form.Item
            label={t('Thời lượng (ngày)')}
            name="duration"
            rules={[{ required: true, message: t('Nhập thời lượng') }]}
          >
            <InputNumber min={1} precision={0} style={{ width: '100%' }} />
          </Form.Item>

          <Row gutter={12}>
            <Col span={8}>
              <Form.Item
                label={t('Khối lượng')}
                name="quantity"
                rules={[{ type: 'number', min: 0, message: t('Khối lượng >= 0') }]}
              >
                <InputNumber min={0} step={0.1} precision={2} style={{ width: '100%' }} placeholder={t('Ví dụ: 100')} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label={t('Đơn vị tính')}
                name="unit"
                rules={[
                  { max: 50, message: t('Tối đa 50 ký tự') },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      const qty = getFieldValue('quantity');
                      if (qty != null && qty > 0 && (!value || !value.trim())) {
                        return Promise.reject(new Error(t('Nhập ĐVT')));
                      }
                      return Promise.resolve();
                    }
                  })
                ]}
              >
                <Input placeholder="m3, m2, tấn..." />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label={t('Đơn giá (VNĐ)')}
                name="unitPrice"
                rules={[{ type: 'number', min: 0, message: t('Đơn giá >= 0') }]}
              >
                <InputNumber
                  min={0}
                  style={{ width: '100%' }}
                  formatter={value => value == null ? '' : `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                  parser={value => value?.replace(/\$\s?|(,*)/g, '') ?? ''}
                  placeholder="250,000"
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item noStyle shouldUpdate={(prev, cur) => prev.quantity !== cur.quantity || prev.unitPrice !== cur.unitPrice}>
            {({ getFieldValue }) => {
              const qty = Number(getFieldValue('quantity') || 0);
              const price = Number(getFieldValue('unitPrice') || 0);
              const total = qty > 0 && price > 0 ? qty * price : 0;
              return total > 0 ? (
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 6, color: '#047857', fontSize: 13, fontWeight: 600, marginTop: -4 }}>
                  <span>{t('Thành tiền ước tính:')}</span>
                  <span>{total.toLocaleString('vi-VN')} VNĐ</span>
                </div>
              ) : null;
            }}
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default CategoryTaskSection;