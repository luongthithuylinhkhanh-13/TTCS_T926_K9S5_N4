import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  DatePicker,
  message,
  Typography
} from 'antd';
import { DollarOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { createActualCost, validateActualCost } from '../../services/actualCostApi';

const { Text } = Typography;

const COST_TYPES = [
  { value: 'VẬT_TƯ', label: 'Vật tư / Nguyên vật liệu' },
  { value: 'NHÂN_CÔNG', label: 'Nhân công thi công' },
  { value: 'MÁY_THI_CÔNG', label: 'Máy thi công & Thiết bị' },
  { value: 'QUẢN_LÝ', label: 'Chi phí quản lý chung' },
  { value: 'KHÁC', label: 'Chi phí khác' }
];

const ActualCostModal = ({
  open,
  onClose,
  projectId,
  wbsItems = [],
  defaultWbsItemId = null,
  onSuccess
}) => {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (open) {
      form.resetFields();
      form.setFieldsValue({
        incurredDate: dayjs(),
        costType: 'VẬT_TƯ',
        wbsItemId: defaultWbsItemId || undefined
      });
    }
  }, [open, defaultWbsItemId, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const validation = validateActualCost({
        name: values.name,
        amount: values.amount,
        costType: values.costType,
        incurredDate: values.incurredDate
      });

      if (!validation.valid) {
        message.error(validation.errors[0]);
        return;
      }

      setSubmitting(true);
      const payload = {
        name: values.name.trim(),
        amount: values.amount,
        costType: values.costType,
        incurredDate: values.incurredDate.format('YYYY-MM-DD'),
        wbsItemId: values.wbsItemId || null,
        note: values.note ? values.note.trim() : null
      };

      await createActualCost(projectId, payload);
      message.success('Ghi nhận chi phí thực tế thành công');
      form.resetFields();
      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      if (err.errorFields) return; // Validation error on client form
      const errMsg = err?.response?.data?.message || err.message || 'Không thể lưu chi phí thực tế';
      message.error(errMsg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <DollarOutlined style={{ color: '#16A34A', fontSize: 18 }} />
          <span>Ghi nhận chi phí thực tế (S-47 / T-94 & T-95)</span>
        </div>
      }
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={submitting}
      okText="Lưu chi phí"
      cancelText="Hủy"
      destroyOnClose
      width={560}
    >
      <Form
        form={form}
        layout="vertical"
        style={{ marginTop: 16 }}
        initialValues={{
          costType: 'VẬT_TƯ',
          incurredDate: dayjs()
        }}
      >
        <Form.Item
          name="name"
          label="Tên / Nội dung khoản chi"
          rules={[
            { required: true, message: 'Vui lòng nhập tên khoản chi' },
            { max: 255, message: 'Tên không vượt quá 255 ký tự' }
          ]}
        >
          <Input placeholder="Ví dụ: Mua thép Pomina D20, Thanh toán tổ đội coppha..." />
        </Form.Item>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <Form.Item
            name="amount"
            label="Số tiền thực chi (VNĐ)"
            rules={[
              { required: true, message: 'Vui lòng nhập số tiền chi phí' },
              { type: 'number', min: 1, message: 'Số tiền phải lớn hơn 0' }
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              placeholder="Nhập số tiền..."
              formatter={value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={value => value.replace(/\$\s?|(,*)/g, '')}
            />
          </Form.Item>

          <Form.Item
            name="costType"
            label="Loại chi phí"
            rules={[{ required: true, message: 'Vui lòng chọn loại chi phí' }]}
          >
            <Select options={COST_TYPES} placeholder="Chọn phân loại..." />
          </Form.Item>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <Form.Item
            name="incurredDate"
            label="Ngày phát sinh chi phí"
            rules={[{ required: true, message: 'Vui lòng chọn ngày phát sinh' }]}
          >
            <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" placeholder="Chọn ngày..." />
          </Form.Item>

          <Form.Item
            name="wbsItemId"
            label="Hạng mục tương ứng (T-95)"
            tooltip="Liên kết khoản chi này với một hạng mục công việc cụ thể"
          >
            <Select
              allowClear
              placeholder="Chọn hạng mục công việc..."
              options={wbsItems.map(item => ({
                value: item.id || item.key,
                label: `${item.wbsCode ? `[${item.wbsCode}] ` : ''}${item.name}`
              }))}
            />
          </Form.Item>
        </div>

        <Form.Item name="note" label="Ghi chú / Diễn giải thêm">
          <Input.TextArea rows={3} placeholder="Thông tin hóa đơn, chứng từ, người nhận..." />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ActualCostModal;
