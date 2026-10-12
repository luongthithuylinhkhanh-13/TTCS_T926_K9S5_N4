import React, { useEffect } from 'react';
import { Alert, Modal, Form, Input, Select, DatePicker, Slider, InputNumber, Row, Col, message } from 'antd';
import dayjs from 'dayjs';
import { USERS } from '../../data/users';
import { useLocale } from '../../utils/LocaleContext';

const { Option } = Select;
const { TextArea } = Input;

const WorkFormModal = ({
  visible,
  onCancel,
  onSubmit,
  initialValues,
  parentOptions = [],
  taskOptions = [],
  isEdit = false,
  confirmLoading = false,
  editConflict = false
}) => {
  const { t } = useLocale();
  const [form] = Form.useForm();
  const actualStartDate = Form.useWatch('actualStartDate', form);
  const actualEndDate = Form.useWatch('actualEndDate', form);
  const progressPercent = Form.useWatch('progressPercent', form);
  const actualDatesInvalid = Boolean(
    actualStartDate &&
    actualEndDate &&
    actualEndDate.isBefore(actualStartDate, 'day')
  );
  const progressInvalid = progressPercent == null || progressPercent < 0 || progressPercent > 100;

  useEffect(() => {
    if (visible) {
      if (initialValues) {
        form.setFieldsValue({
          wbsCode: initialValues.wbsCode || '',
          name: initialValues.name || '',
          duration: initialValues.duration ?? (
            initialValues.startDate && initialValues.endDate
              ? dayjs(initialValues.endDate).diff(dayjs(initialValues.startDate), 'day') + 1
              : undefined
          ),
          parentId: initialValues.parentId || null,
          assigneeId: initialValues.assignee?.id || undefined,
          status: initialValues.status || 'not_started',
          startDate: initialValues.startDate ? dayjs(initialValues.startDate) : null,
          endDate: initialValues.endDate ? dayjs(initialValues.endDate) : null,
          actualStartDate: initialValues.actualStartDate ? dayjs(initialValues.actualStartDate) : null,
          actualEndDate: initialValues.actualEndDate ? dayjs(initialValues.actualEndDate) : null,
          predecessorIds: initialValues.predecessorIds || [],
          progressPercent: initialValues.progressPercent ?? initialValues.progress ?? 0,
          description: initialValues.description || ''
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          status: 'not_started',
          duration: undefined,
          progressPercent: 0,
          predecessorIds: []
        });
      }
    }
  }, [visible, initialValues, form]);

  const handleFinish = (values) => {
    // Validate start and end dates
    if (values.startDate && values.endDate) {
      if (values.endDate.isBefore(values.startDate, 'day')) {
        message.error(t('Ngày kết thúc phải sau hoặc cùng ngày với ngày bắt đầu'));
        return;
      }
    }
    if (values.actualStartDate && values.actualEndDate
      && values.actualEndDate.isBefore(values.actualStartDate, 'day')) {
      message.error(t('Ngày kết thúc thực tế phải sau hoặc cùng ngày với ngày bắt đầu thực tế'));
      return;
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
      progress: values.progressPercent,
      progressPercent: values.progressPercent,
      startDate: values.startDate ? values.startDate.format('YYYY-MM-DD') : '',
      endDate: values.endDate ? values.endDate.format('YYYY-MM-DD') : '',
      actualStartDate: values.actualStartDate ? values.actualStartDate.format('YYYY-MM-DD') : null,
      actualEndDate: values.actualEndDate ? values.actualEndDate.format('YYYY-MM-DD') : null,
      description: values.description || ''
    };

    onSubmit(formattedData);
  };

  return (
    <Modal
      title={isEdit ? t('Chỉnh sửa công việc') : t('Thêm công việc mới')}
      open={visible}
      onCancel={onCancel}
      onOk={() => form.submit()}
      width={680}
      okText={isEdit ? t('Lưu thay đổi') : t('Thêm công việc')}
      cancelText={t('Hủy')}
      destroyOnClose
      confirmLoading={confirmLoading}
      okButtonProps={{ disabled: actualDatesInvalid || progressInvalid || confirmLoading || editConflict }}
    >
      {editConflict && (
        <Alert
          type="warning"
          showIcon
          message={t('Công việc đã được người khác cập nhật. Hãy đóng biểu mẫu và mở lại để xem dữ liệu mới nhất. Các thay đổi chưa lưu chưa được áp dụng.')}
          style={{ marginBottom: 16 }}
        />
      )}
      <div style={{ marginBottom: 16, color: '#667085', fontSize: 13 }}>
        {isEdit 
          ? t('Cập nhật thông tin chi tiết và tiến độ cho công việc WBS')
          : t('Tạo công việc trong cơ cấu phân rã WBS của dự án')}
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        requiredMark={false}
      >
        {actualDatesInvalid && (
          <Alert
            type="error"
            showIcon
            message={t('Ngày kết thúc thực tế phải sau hoặc cùng ngày với ngày bắt đầu thực tế')}
            style={{ marginBottom: 16 }}
          />
        )}

        {/* ROW 1: WBS Code & Name */}
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              label={t('Mã WBS')}
              name="wbsCode"
              rules={[{ required: true, message: t('Vui lòng nhập mã WBS') }]}
            >
              <Input 
                placeholder={t('Ví dụ: 1.2.5')}
                disabled={isEdit}
                style={{ fontFamily: 'monospace', fontWeight: 600 }}
              />
            </Form.Item>
          </Col>

          <Col span={16}>
            <Form.Item
              label={t('Tên công việc')}
              name="name"
              rules={[{ required: true, message: t('Vui lòng nhập tên công việc') }]}
            >
              <Input placeholder={t('Nhập tên công việc')} />
            </Form.Item>
          </Col>
        </Row>

        {(!isEdit || initialValues?.type === 'task') && (
          <Form.Item
            label={t('Thời lượng thực hiện (ngày)')}
            name="duration"
            rules={[
              { required: true, type: 'number', min: 1, message: t('Thời lượng phải lớn hơn 0') }
            ]}
          >
            <InputNumber min={1} precision={0} changeOnBlur={false} style={{ width: '100%' }} placeholder={t('Nhập số ngày')} />
          </Form.Item>
        )}

        {/* ROW 2: Parent Task */}
        <Form.Item
          label={t('Công việc / Hạng mục cha')}
          name="parentId"
        >
          <Select 
            placeholder={t('Chọn hạng mục cha (Để trống nếu là Hạng mục chính)')}
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
                label={t('Công việc tiền nhiệm')}
                name="predecessorIds"
                extra={t('Các công việc phải hoàn thành trước công việc này (quan hệ kết thúc-bắt đầu).')}
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
                  placeholder={t('Chọn công việc tiền nhiệm')}
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
              label={t('Người phụ trách')}
              name="assigneeId"
              rules={[{ required: true, message: t('Vui lòng chọn người phụ trách') }]}
            >
              <Select placeholder={t('Chọn người phụ trách')}>
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
              label={t('Trạng thái')}
              name="status"
              rules={[{ required: true, message: t('Vui lòng chọn trạng thái') }]}
            >
              <Select placeholder={t('Chọn trạng thái')}>
                <Option value="not_started">{t('Chưa bắt đầu')}</Option>
                <Option value="in_progress">{t('Đang thực hiện')}</Option>
                <Option value="completed">{t('Hoàn thành')}</Option>
                <Option value="paused">{t('Tạm dừng')}</Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>

        {/* ROW 4: Start & End Dates */}
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label={t('Ngày bắt đầu')}
              name="startDate"
              rules={[{ required: true, message: t('Vui lòng chọn ngày bắt đầu') }]}
            >
              <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} placeholder={t('Chọn ngày')} />
            </Form.Item>
          </Col>

          <Col span={12}>
            <Form.Item
              label={t('Ngày kết thúc')}
              name="endDate"
              rules={[{ required: true, message: t('Vui lòng chọn ngày kết thúc') }]}
            >
              <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} placeholder={t('Chọn ngày')} />
            </Form.Item>
          </Col>
        </Row>

        <div className="actual-progress-form-section">
          <div className="actual-progress-form-heading">{t('TIẾN ĐỘ THỰC TẾ')}</div>
          <Row gutter={16} align="middle">
            <Col span={18}>
              <Slider
                min={0}
                max={100}
                value={progressPercent ?? 0}
                onChange={value => form.setFieldValue('progressPercent', value)}
              />
            </Col>
            <Col span={6}>
              <Form.Item
                name="progressPercent"
                rules={[
                  { required: true, type: 'number', message: t('Vui lòng nhập phần trăm hoàn thành') },
                  { type: 'number', min: 0, max: 100, message: t('Phần trăm hoàn thành phải từ 0 đến 100') }
                ]}
              >
                <InputNumber
                  min={0}
                  max={100}
                  precision={0}
                  formatter={value => value == null ? '' : `${value}%`}
                  parser={value => value?.replace('%', '') ?? ''}
                  style={{ width: '100%' }}
                  aria-label={t('Phần trăm hoàn thành thực tế')}
                />
              </Form.Item>
            </Col>
          </Row>
        </div>

        <div className="actual-progress-form-section">
          <div className="actual-progress-form-heading">{t('NGÀY THỰC TẾ')}</div>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label={t('Ngày bắt đầu thực tế')} name="actualStartDate">
                <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} placeholder={t('Chọn ngày bắt đầu thực tế')} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label={t('Ngày kết thúc thực tế')}
                name="actualEndDate"
                dependencies={['actualStartDate']}
                validateTrigger={['onChange', 'onBlur']}
                validateStatus={actualDatesInvalid ? 'error' : undefined}
                help={actualDatesInvalid ? t('Ngày kết thúc thực tế không được trước ngày bắt đầu thực tế') : undefined}
                rules={[
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      const start = getFieldValue('actualStartDate');
                      if (!value || !start || !value.isBefore(start, 'day')) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error(
                        t('Ngày kết thúc thực tế không được trước ngày bắt đầu thực tế')
                      ));
                    }
                  })
                ]}
              >
                <DatePicker format="DD/MM/YYYY" style={{ width: '100%' }} placeholder={t('Chọn ngày kết thúc thực tế')} />
              </Form.Item>
            </Col>
          </Row>
        </div>

        {/* ROW 6: Description */}
        <Form.Item label={t('Mô tả công việc')} name="description">
          <TextArea rows={3} placeholder={t('Nhập chi tiết mô tả công việc, quy chuẩn thi công...')} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default WorkFormModal;
