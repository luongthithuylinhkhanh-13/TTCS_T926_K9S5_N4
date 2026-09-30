import React, { useEffect } from 'react';
import { Modal, Form, Input, Select, DatePicker, Slider, InputNumber, Row, Col, message } from 'antd';
import dayjs from 'dayjs';
import { USERS } from '../../data/users';

const { Option } = Select;
const { TextArea } = Input;

const WorkFormModal = ({
  visible,
  onCancel,
  onSubmit,
  initialValues,
  parentOptions = [],
  taskOptions = [],
  isEdit = false
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (visible) {
      if (initialValues) {
        form.setFieldsValue({
          wbsCode: initialValues.wbsCode || '',
          name: initialValues.name || '',
          duration: initialValues.duration ?? undefined,
          parentId: initialValues.parentId || null,
          assigneeId: initialValues.assignee?.id || undefined,
          status: initialValues.status || 'not_started',
          startDate: initialValues.startDate ? dayjs(initialValues.startDate) : null,
          endDate: initialValues.endDate ? dayjs(initialValues.endDate) : null,
          predecessorIds: initialValues.predecessorIds || [],
          progress: initialValues.progress || 0,
          description: initialValues.description || ''
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          status: 'not_started',
          duration: undefined,
          progress: 0,
          predecessorIds: []
        });
      }
    }
  }, [visible, initialValues, form]);

  const handleFinish = (values) => {
    // Validate start and end dates
    if (values.startDate && values.endDate) {
      if (values.endDate.isBefore(values.startDate, 'day')) {
        message.error('Ngày kết thúc phải sau hoặc cùng ngày với ngày bắt đầu');
        return;
      }
    }

    const assigneeObj = USERS.find(u => u.id === values.assigneeId) || null;

    const formattedData = {
      ...initialValues,
      wbsCode: values.wbsCode,
      name: values.name,
      duration: values.duration,
      type: isEdit ? initialValues.type : 'task',
      parentId: values.parentId || null,
      assignee: assigneeObj,
      status: values.status,
      progress: values.progress,
      startDate: values.startDate ? values.startDate.format('YYYY-MM-DD') : '',
      endDate: values.endDate ? values.endDate.format('YYYY-MM-DD') : '',
      description: values.description || ''
    };

    onSubmit(formattedData);
  };

  return (
    <Modal
      title={isEdit ? "Chỉnh sửa công việc" : "Thêm công việc mới"}
      open={visible}
      onCancel={onCancel}
      onOk={() => form.submit()}
      width={680}
      okText={isEdit ? "Lưu thay đổi" : "Thêm công việc"}
      cancelText="Hủy"
      destroyOnClose
    >
      <div style={{ marginBottom: 16, color: '#667085', fontSize: 13 }}>
        {isEdit 
          ? "Cập nhật thông tin chi tiết và tiến độ cho công việc WBS"
          : "Tạo công việc trong cơ cấu phân rã WBS của dự án"}
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        requiredMark={false}
      >
        {/* ROW 1: WBS Code & Name */}
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              label="Mã WBS"
              name="wbsCode"
              rules={[{ required: true, message: 'Vui lòng nhập mã WBS' }]}
            >
              <Input 
                placeholder="Ví dụ: 1.2.5" 
                disabled={isEdit}
                style={{ fontFamily: 'monospace', fontWeight: 600 }}
              />
            </Form.Item>
          </Col>

          <Col span={16}>
            <Form.Item
              label="Tên công việc"
              name="name"
              rules={[{ required: true, message: 'Vui lòng nhập tên công việc' }]}
            >
              <Input placeholder="Nhập tên công việc" />
            </Form.Item>
          </Col>
        </Row>

        {(!isEdit || initialValues?.type === 'task') && (
          <Form.Item
            label="Thời lượng thực hiện (ngày)"
            name="duration"
            rules={[
              { required: true, type: 'number', min: 1, message: 'Thời lượng phải lớn hơn 0' }
            ]}
          >
            <InputNumber min={1} precision={0} changeOnBlur={false} style={{ width: '100%' }} placeholder="Nhập số ngày" />
          </Form.Item>
        )}

        {/* ROW 2: Parent Task */}
        <Form.Item
          label="Công việc / Hạng mục cha"
          name="parentId"
        >
          <Select 
            placeholder="Chọn hạng mục cha (Để trống nếu là Hạng mục chính)"
            allowClear
            disabled={isEdit}
          >
            {parentOptions.map(p => (
              <Option key={p.id} value={p.id}>
                <strong>{p.wbsCode}</strong> - {p.name}
              </Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item noStyle shouldUpdate={(previous, current) => previous.parentId !== current.parentId}>
          {({ getFieldValue }) => {
            const isTask = isEdit
              ? initialValues?.type === 'task'
              : Boolean(getFieldValue('parentId'));

            return isTask ? (
              <Form.Item
                label="Công việc tiền nhiệm"
                name="predecessorIds"
                extra="Các công việc phải hoàn thành trước công việc này (quan hệ kết thúc-bắt đầu)."
              >
                <Select
                  mode="multiple"
                  allowClear
                  options={taskOptions
                    .filter(task => task.id !== initialValues?.id)
                    .map(task => ({
                      value: task.id,
                      label: `${task.wbsCode} - ${task.name}`
                    }))}
                  placeholder="Chọn công việc tiền nhiệm"
                  optionFilterProp="label"
                />
              </Form.Item>
            ) : null;
          }}
        </Form.Item>

        {/* ROW 3: Assignee & Status */}
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label="Người phụ trách"
              name="assigneeId"
              rules={[{ required: true, message: 'Vui lòng chọn người phụ trách' }]}
            >
              <Select placeholder="Chọn người phụ trách">
                {USERS.map(user => (
                  <Option key={user.id} value={user.id}>
                    [{user.initials}] {user.name}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>

          <Col span={12}>
            <Form.Item
              label="Trạng thái"
              name="status"
              rules={[{ required: true, message: 'Vui lòng chọn trạng thái' }]}
            >
              <Select placeholder="Chọn trạng thái">
                <Option value="not_started">Chưa bắt đầu</Option>
                <Option value="in_progress">Đang thực hiện</Option>
                <Option value="completed">Hoàn thành</Option>
                <Option value="paused">Tạm dừng</Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>

        {/* ROW 4: Start & End Dates */}
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label="Ngày bắt đầu"
              name="startDate"
              rules={[{ required: true, message: 'Vui lòng chọn ngày bắt đầu' }]}
            >
              <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} placeholder="Chọn ngày" />
            </Form.Item>
          </Col>

          <Col span={12}>
            <Form.Item
              label="Ngày kết thúc"
              name="endDate"
              rules={[{ required: true, message: 'Vui lòng chọn ngày kết thúc' }]}
            >
              <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} placeholder="Chọn ngày" />
            </Form.Item>
          </Col>
        </Row>

        {/* ROW 5: Progress */}
        <Form.Item label="Tiến độ hoàn thành (%)">
          <Row gutter={16} align="middle">
            <Col span={18}>
              <Form.Item name="progress" noStyle>
                <Slider min={0} max={100} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="progress" noStyle>
                <InputNumber min={0} max={100} formatter={value => `${value}%`} parser={value => value.replace('%', '')} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
        </Form.Item>

        {/* ROW 6: Description */}
        <Form.Item label="Mô tả công việc" name="description">
          <TextArea rows={3} placeholder="Nhập chi tiết mô tả công việc, quy chuẩn thi công..." />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default WorkFormModal;
