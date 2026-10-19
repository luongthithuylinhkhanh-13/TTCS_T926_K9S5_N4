import React, { useEffect, useState } from 'react';
import {
  Alert,
  Modal,
  Form,
  Input,
  Select,
  DatePicker,
  Slider,
  InputNumber,
  Row,
  Col,
  message,
  Upload,
  Image,
  Button
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { USERS } from '../../data/users';
import { useLocale } from '../../utils/LocaleContext';

const { Option } = Select;
const { TextArea } = Input;

const MAX_IMAGE_SIZE = 20 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/gif'
];

const WorkFormModal = ({
  visible,
  projectId,
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

  const [selectedImage, setSelectedImage] = useState(null);
  const [imagePreview, setImagePreview] = useState('');

  const actualStartDate = Form.useWatch('actualStartDate', form);
  const actualEndDate = Form.useWatch('actualEndDate', form);
  const progressPercent = Form.useWatch('progressPercent', form);

  const actualDatesInvalid = Boolean(
    actualStartDate &&
    actualEndDate &&
    actualEndDate.isBefore(actualStartDate, 'day')
  );

  const progressInvalid =
    progressPercent == null ||
    progressPercent < 0 ||
    progressPercent > 100;

  // Giải phóng URL xem trước khi ảnh thay đổi hoặc component bị hủy.
  useEffect(() => {
    return () => {
      if (imagePreview) {
        URL.revokeObjectURL(imagePreview);
      }
    };
  }, [imagePreview]);

  // Khởi tạo dữ liệu biểu mẫu.
  useEffect(() => {
    if (!visible) {
      return;
    }

    // Mỗi lần mở biểu mẫu, bắt đầu với trạng thái chọn ảnh mới.
    setSelectedImage(null);
    setImagePreview('');

    if (initialValues) {
      form.setFieldsValue({
        wbsCode: initialValues.wbsCode || '',
        name: initialValues.name || '',
        duration: initialValues.duration ?? (
          initialValues.startDate && initialValues.endDate
            ? dayjs(initialValues.endDate).diff(
                dayjs(initialValues.startDate),
                'day'
              ) + 1
            : undefined
        ),
        parentId: initialValues.parentId || null,
        assigneeId: initialValues.assignee?.id || undefined,
        status: initialValues.status || 'not_started',
        startDate: initialValues.startDate
          ? dayjs(initialValues.startDate)
          : null,
        endDate: initialValues.endDate
          ? dayjs(initialValues.endDate)
          : null,
        actualStartDate: initialValues.actualStartDate
          ? dayjs(initialValues.actualStartDate)
          : null,
        actualEndDate: initialValues.actualEndDate
          ? dayjs(initialValues.actualEndDate)
          : null,
        predecessorIds: initialValues.predecessorIds || [],
        progressPercent:
          initialValues.progressPercent ?? initialValues.progress ?? 0,
        quantity:
          initialValues.quantity ?? initialValues.totalVolume ?? undefined,
        unit: initialValues.unit || '',
        unitPrice: initialValues.unitPrice ?? undefined,
        description: initialValues.description || ''
      });
    } else {
      form.resetFields();

      form.setFieldsValue({
        status: 'not_started',
        duration: undefined,
        progressPercent: 0,
        quantity: undefined,
        unit: '',
        unitPrice: undefined,
        predecessorIds: []
      });
    }
  }, [visible, initialValues, form]);

  const handleSelectImage = (file) => {
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      message.error(
        t('Chỉ chấp nhận ảnh JPEG, PNG, WEBP hoặc GIF')
      );
      return Upload.LIST_IGNORE;
    }

    if (file.size <= 0 || file.size > MAX_IMAGE_SIZE) {
      message.error(
        t('Ảnh phải có dung lượng lớn hơn 0 và không quá 20 MB')
      );
      return Upload.LIST_IGNORE;
    }

    setSelectedImage(file);
    setImagePreview(URL.createObjectURL(file));

    // Ngăn Ant Design tự upload trực tiếp.
    // File sẽ được xử lý qua hàng đợi ở bước tích hợp tiếp theo.
    return false;
  };

  const handleRemoveImage = () => {
    setSelectedImage(null);
    setImagePreview('');
  };

  const handleFinish = (values) => {
    if (values.startDate && values.endDate) {
      if (values.endDate.isBefore(values.startDate, 'day')) {
        message.error(
          t('Ngày kết thúc phải sau hoặc cùng ngày với ngày bắt đầu')
        );
        return;
      }
    }

    if (
      values.actualStartDate &&
      values.actualEndDate &&
      values.actualEndDate.isBefore(values.actualStartDate, 'day')
    ) {
      message.error(
        t(
          'Ngày kết thúc thực tế phải sau hoặc cùng ngày với ngày bắt đầu thực tế'
        )
      );
      return;
    }

    if (selectedImage && !projectId) {
      message.error(t('Không tìm thấy mã dự án.'));
      return;
    }

    const assigneeObj =
      USERS.find(user => user.id === values.assigneeId) || null;

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
      startDate: values.startDate
        ? values.startDate.format('YYYY-MM-DD')
        : '',
      endDate: values.endDate
        ? values.endDate.format('YYYY-MM-DD')
        : '',
      actualStartDate: values.actualStartDate
        ? values.actualStartDate.format('YYYY-MM-DD')
        : null,
      actualEndDate: values.actualEndDate
        ? values.actualEndDate.format('YYYY-MM-DD')
        : null,
      quantity:
        values.quantity == null ? null : Number(values.quantity),
      totalVolume:
        values.quantity == null ? null : Number(values.quantity),
      unit: values.unit ? values.unit.trim() : null,
      unitPrice:
        values.unitPrice == null ? null : Number(values.unitPrice),
      description: values.description || '',

      // File gốc để bước tích hợp hàng đợi xử lý tiếp.
      imageFile: selectedImage || null
    };

    onSubmit(formattedData);
  };

  return (
    <Modal
      title={
        isEdit
          ? t('Chỉnh sửa công việc')
          : t('Thêm công việc mới')
      }
      open={visible}
      onCancel={onCancel}
      onOk={() => form.submit()}
      width={680}
      okText={isEdit ? t('Lưu thay đổi') : t('Thêm công việc')}
      cancelText={t('Hủy')}
      destroyOnClose
      confirmLoading={confirmLoading}
      okButtonProps={{
        disabled:
          actualDatesInvalid ||
          progressInvalid ||
          confirmLoading ||
          editConflict
      }}
    >
      {editConflict && (
        <Alert
          type="warning"
          showIcon
          message={t(
            'Công việc đã được người khác cập nhật. Hãy đóng biểu mẫu và mở lại để xem dữ liệu mới nhất. Các thay đổi chưa lưu chưa được áp dụng.'
          )}
          style={{ marginBottom: 16 }}
        />
      )}

      <div
        style={{
          marginBottom: 16,
          color: '#667085',
          fontSize: 13
        }}
      >
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
            message={t(
              'Ngày kết thúc thực tế phải sau hoặc cùng ngày với ngày bắt đầu thực tế'
            )}
            style={{ marginBottom: 16 }}
          />
        )}

        {/* ROW 1: WBS Code & Name */}
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              label={t('Mã WBS')}
              name="wbsCode"
              rules={[
                {
                  required: true,
                  message: t('Vui lòng nhập mã WBS')
                }
              ]}
            >
              <Input
                placeholder={t('Ví dụ: 1.2.5')}
                disabled={isEdit}
                style={{
                  fontFamily: 'monospace',
                  fontWeight: 600
                }}
              />
            </Form.Item>
          </Col>

          <Col span={16}>
            <Form.Item
              label={t('Tên công việc')}
              name="name"
              rules={[
                {
                  required: true,
                  message: t('Vui lòng nhập tên công việc')
                }
              ]}
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
              {
                required: true,
                type: 'number',
                min: 1,
                message: t('Thời lượng phải lớn hơn 0')
              }
            ]}
          >
            <InputNumber
              min={1}
              precision={0}
              changeOnBlur={false}
              style={{ width: '100%' }}
              placeholder={t('Nhập số ngày')}
            />
          </Form.Item>
        )}

        {/* ROW: Khối lượng và đơn giá */}
        <div
          style={{
            background: '#F8FAFC',
            padding: '12px 14px',
            borderRadius: 8,
            marginBottom: 16,
            border: '1px solid #E2E8F0'
          }}
        >
          <div
            style={{
              fontSize: 13,
              fontWeight: 700,
              color: '#334155',
              marginBottom: 10,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}
          >
            <span>{t('KHỐI LƯỢNG & ĐƠN GIÁ (DỰ TOÁN)')}</span>
            <span
              style={{
                fontSize: 11,
                fontWeight: 500,
                color: '#64748B'
              }}
            >
              T-80 / T-82
            </span>
          </div>

          <Row gutter={12}>
            <Col span={8}>
              <Form.Item
                label={t('Khối lượng')}
                name="quantity"
                rules={[
                  {
                    type: 'number',
                    min: 0,
                    message: t('Khối lượng phải lớn hơn hoặc bằng 0')
                  }
                ]}
              >
                <InputNumber
                  min={0}
                  step={0.1}
                  precision={2}
                  style={{ width: '100%' }}
                  placeholder={t('Ví dụ: 120.5')}
                />
              </Form.Item>
            </Col>

            <Col span={8}>
              <Form.Item
                label={t('Đơn vị tính')}
                name="unit"
                rules={[
                  {
                    max: 50,
                    message: t('Đơn vị tính không quá 50 ký tự')
                  },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      const qty = getFieldValue('quantity');

                      if (
                        qty != null &&
                        qty > 0 &&
                        (!value || !value.trim())
                      ) {
                        return Promise.reject(
                          new Error(
                            t(
                              'Vui lòng nhập đơn vị tính khi đã có khối lượng'
                            )
                          )
                        );
                      }

                      return Promise.resolve();
                    }
                  })
                ]}
              >
                <Select
                  placeholder={t('Chọn / nhập ĐVT')}
                  showSearch
                  allowClear
                  options={[
                    { value: 'm3', label: 'm3 (Mét khối)' },
                    { value: 'm2', label: 'm2 (Mét vuông)' },
                    { value: 'm', label: 'm (Mét dài)' },
                    { value: 'tấn', label: 'tấn' },
                    { value: 'kg', label: 'kg' },
                    { value: 'cái', label: 'cái' },
                    { value: 'bộ', label: 'bộ' },
                    { value: 'hố', label: 'hố' },
                    { value: 'gói', label: 'gói' },
                    { value: 'cột', label: 'cột' }
                  ]}
                />
              </Form.Item>
            </Col>

            <Col span={8}>
              <Form.Item
                label={t('Đơn giá (VNĐ)')}
                name="unitPrice"
                rules={[
                  {
                    type: 'number',
                    min: 0,
                    message: t('Đơn giá phải lớn hơn hoặc bằng 0')
                  }
                ]}
              >
                <InputNumber
                  min={0}
                  style={{ width: '100%' }}
                  formatter={value =>
                    value == null
                      ? ''
                      : `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
                  }
                  parser={value =>
                    value?.replace(/\$\s?|(,*)/g, '') ?? ''
                  }
                  placeholder={t('Ví dụ: 250,000')}
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            noStyle
            shouldUpdate={(prev, cur) =>
              prev.quantity !== cur.quantity ||
              prev.unitPrice !== cur.unitPrice
            }
          >
            {({ getFieldValue }) => {
              const qty = Number(getFieldValue('quantity') || 0);
              const price = Number(getFieldValue('unitPrice') || 0);
              const total = qty > 0 && price > 0 ? qty * price : 0;

              return total > 0 ? (
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'flex-end',
                    alignItems: 'center',
                    gap: 8,
                    marginTop: -4,
                    color: '#0F766E',
                    fontSize: 13,
                    fontWeight: 600
                  }}
                >
                  <span>{t('Thành tiền dự toán ước tính:')}</span>
                  <span
                    style={{
                      fontSize: 15,
                      fontWeight: 700,
                      color: '#047857'
                    }}
                  >
                    {total.toLocaleString('vi-VN')} VNĐ
                  </span>
                </div>
              ) : null;
            }}
          </Form.Item>
        </div>

        {/* ROW 2: Parent Task */}
        <Form.Item
          label={t('Công việc / Hạng mục cha')}
          name="parentId"
        >
          <Select
            placeholder={t(
              'Chọn hạng mục cha (Để trống nếu là Hạng mục chính)'
            )}
            allowClear
            disabled={isEdit}
          >
            {parentOptions.map(parent => (
              <Option key={parent.id} value={parent.id}>
                <strong>{parent.wbsCode}</strong> - {parent.name}
              </Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item
          noStyle
          shouldUpdate={(previous, current) =>
            previous.parentId !== current.parentId
          }
        >
          {({ getFieldValue }) => {
            const isTask = isEdit
              ? initialValues?.type === 'task'
              : Boolean(getFieldValue('parentId'));

            return isTask ? (
              <Form.Item
                label={t('Công việc tiền nhiệm')}
                name="predecessorIds"
                extra={t(
                  'Các công việc phải hoàn thành trước công việc này (quan hệ kết thúc-bắt đầu).'
                )}
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
              rules={[
                {
                  required: true,
                  message: t('Vui lòng chọn người phụ trách')
                }
              ]}
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
              rules={[
                {
                  required: true,
                  message: t('Vui lòng chọn trạng thái')
                }
              ]}
            >
              <Select placeholder={t('Chọn trạng thái')}>
                <Option value="not_started">
                  {t('Chưa bắt đầu')}
                </Option>
                <Option value="in_progress">
                  {t('Đang thực hiện')}
                </Option>
                <Option value="completed">
                  {t('Hoàn thành')}
                </Option>
                <Option value="paused">
                  {t('Tạm dừng')}
                </Option>
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
              rules={[
                {
                  required: true,
                  message: t('Vui lòng chọn ngày bắt đầu')
                }
              ]}
            >
              <DatePicker
                format="DD/MM/YYYY"
                style={{ width: '100%' }}
                placeholder={t('Chọn ngày')}
              />
            </Form.Item>
          </Col>

          <Col span={12}>
            <Form.Item
              label={t('Ngày kết thúc')}
              name="endDate"
              rules={[
                {
                  required: true,
                  message: t('Vui lòng chọn ngày kết thúc')
                }
              ]}
            >
              <DatePicker
                format="DD/MM/YYYY"
                style={{ width: '100%' }}
                placeholder={t('Chọn ngày')}
              />
            </Form.Item>
          </Col>
        </Row>

        {/* ROW 5: Actual Progress */}
        <div className="actual-progress-form-section">
          <div className="actual-progress-form-heading">
            {t('TIẾN ĐỘ THỰC TẾ')}
          </div>

          <Row gutter={16} align="middle">
            <Col span={18}>
              <Slider
                min={0}
                max={100}
                value={progressPercent ?? 0}
                onChange={value =>
                  form.setFieldValue('progressPercent', value)
                }
              />
            </Col>

            <Col span={6}>
              <Form.Item
                name="progressPercent"
                rules={[
                  {
                    required: true,
                    type: 'number',
                    message: t('Vui lòng nhập phần trăm hoàn thành')
                  },
                  {
                    type: 'number',
                    min: 0,
                    max: 100,
                    message: t('Phần trăm hoàn thành phải từ 0 đến 100')
                  }
                ]}
              >
                <InputNumber
                  min={0}
                  max={100}
                  precision={0}
                  formatter={value =>
                    value == null ? '' : `${value}%`
                  }
                  parser={value =>
                    value?.replace('%', '') ?? ''
                  }
                  style={{ width: '100%' }}
                  aria-label={t('Phần trăm hoàn thành thực tế')}
                />
              </Form.Item>
            </Col>
          </Row>
        </div>

        {/* ROW 6: Actual Dates */}
        <div className="actual-progress-form-section">
          <div className="actual-progress-form-heading">
            {t('NGÀY THỰC TẾ')}
          </div>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label={t('Ngày bắt đầu thực tế')}
                name="actualStartDate"
              >
                <DatePicker
                  format="DD/MM/YYYY"
                  style={{ width: '100%' }}
                  placeholder={t('Chọn ngày bắt đầu thực tế')}
                />
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item
                label={t('Ngày kết thúc thực tế')}
                name="actualEndDate"
                dependencies={['actualStartDate']}
                validateTrigger={['onChange', 'onBlur']}
                validateStatus={
                  actualDatesInvalid ? 'error' : undefined
                }
                help={
                  actualDatesInvalid
                    ? t(
                        'Ngày kết thúc thực tế không được trước ngày bắt đầu thực tế'
                      )
                    : undefined
                }
                rules={[
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      const start = getFieldValue('actualStartDate');

                      if (
                        !value ||
                        !start ||
                        !value.isBefore(start, 'day')
                      ) {
                        return Promise.resolve();
                      }

                      return Promise.reject(
                        new Error(
                          t(
                            'Ngày kết thúc thực tế không được trước ngày bắt đầu thực tế'
                          )
                        )
                      );
                    }
                  })
                ]}
              >
                <DatePicker
                  format="DD/MM/YYYY"
                  style={{ width: '100%' }}
                  placeholder={t('Chọn ngày kết thúc thực tế')}
                />
              </Form.Item>
            </Col>
          </Row>
        </div>

        {/* ROW 7: Image Upload */}
        <Form.Item label={t('Ảnh công việc')}>
          <Upload
            accept="image/jpeg,image/png,image/webp,image/gif"
            maxCount={1}
            showUploadList={false}
            beforeUpload={handleSelectImage}
          >
            <Button icon={<PlusOutlined />}>
              {t('Chọn ảnh')}
            </Button>
          </Upload>

          <div
            style={{
              marginTop: 6,
              color: '#667085',
              fontSize: 12
            }}
          >
            {t('JPEG, PNG, WEBP hoặc GIF. Dung lượng tối đa 20 MB.')}
          </div>

          {selectedImage && imagePreview && (
            <div
              style={{
                marginTop: 12,
                display: 'flex',
                alignItems: 'center',
                gap: 12
              }}
            >
              <Image
                src={imagePreview}
                alt={t('Ảnh công việc đã chọn')}
                width={120}
                height={90}
                style={{ objectFit: 'contain' }}
              />

              <div>
                <div
                  style={{
                    fontSize: 13,
                    overflowWrap: 'anywhere'
                  }}
                >
                  {selectedImage.name}
                </div>

                <div
                  style={{
                    color: '#667085',
                    fontSize: 12,
                    marginTop: 4
                  }}
                >
                  {(selectedImage.size / (1024 * 1024)).toFixed(2)} MB
                </div>

                <Button
                  type="link"
                  danger
                  onClick={handleRemoveImage}
                  disabled={confirmLoading}
                  style={{ paddingLeft: 0 }}
                >
                  {t('Xóa ảnh đã chọn')}
                </Button>
              </div>
            </div>
          )}
        </Form.Item>

        {/* ROW 8: Description */}
        <Form.Item
          label={t('Mô tả công việc')}
          name="description"
        >
          <TextArea
            rows={3}
            placeholder={t(
              'Nhập chi tiết mô tả công việc, quy chuẩn thi công...'
            )}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default WorkFormModal;