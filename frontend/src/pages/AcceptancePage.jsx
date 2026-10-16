import React, { useState, useEffect, useRef } from 'react';
import {
  Card,
  Table,
  Button,
  Tag,
  Modal,
  Form,
  Input,
  InputNumber,
  DatePicker,
  Select,
  Row,
  Col,
  Space,
  Typography,
  notification,
  Popconfirm,
  Tooltip,
  Divider,
  Alert
} from 'antd';
import {
  AuditOutlined,
  PlusOutlined,
  ReloadOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  CalculatorOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  FileTextOutlined,
  FilePdfOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { getProjects, getProjectWbs } from '../services/wbsApi';
import {
  getAcceptanceReports,
  getAcceptanceReport,
  getAcceptanceDossier,
  getAcceptanceSummary,
  getNextPeriod,
  createAcceptanceReport,
  updateAcceptanceReport,
  deleteAcceptanceReport,
  calculateItemAmount,
  calculateTotalAmount
} from '../services/acceptanceApi';
import { getAcceptanceRecords } from '../services/acceptanceRecordApi';
import AcceptanceDossierDocument from '../components/acceptance/AcceptanceDossierDocument.jsx';

const { Title, Text } = Typography;
const { TextArea } = Input;

const formatCurrency = amount => {
  if (amount == null || isNaN(amount)) return '0 VNĐ';
  return Number(amount).toLocaleString('vi-VN') + ' VNĐ';
};

const AcceptancePage = () => {
  const [form] = Form.useForm();
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [reports, setReports] = useState([]);
  const [acceptanceRecords, setAcceptanceRecords] = useState([]);
  const [wbsItems, setWbsItems] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // State cho Modal Form (T-83, T-84, T-85)
  const [modalOpen, setModalOpen] = useState(false);
  const [editingReportId, setEditingReportId] = useState(null);
  const [items, setItems] = useState([]);

  // State cho Modal Chi tiết
  const [detailReport, setDetailReport] = useState(null);
  const [pdfExportData, setPdfExportData] = useState(null);
  const [pdfExportingId, setPdfExportingId] = useState(null);
  const pdfDocumentRef = useRef(null);

  // State cho Modal chọn hạng mục WBS (T-84)
  const [wbsSelectModalOpen, setWbsSelectModalOpen] = useState(false);

  // 1. Tải danh sách dự án
  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const list = await getProjects();
        if (Array.isArray(list) && list.length > 0) {
          setProjects(list);
          setSelectedProjectId(list[0].id);
        }
      } catch (err) {
        console.error('Lỗi tải danh sách dự án:', err);
      }
    };
    fetchProjects();
  }, []);

  // 2. Tải phiếu nghiệm thu và WBS items khi chọn dự án
  const loadData = async () => {
    if (!selectedProjectId) return;
    setLoading(true);
    try {
      const [reportsData, summaryData, wbsData] = await Promise.all([
        getAcceptanceReports(selectedProjectId),
        getAcceptanceSummary(selectedProjectId),
        getProjectWbs(selectedProjectId).catch(() => [])
      ]);
      setReports(reportsData);
      setSummary(summaryData);
      setWbsItems(Array.isArray(wbsData) ? wbsData : []);
      try {
        const recordsData = await getAcceptanceRecords(selectedProjectId);
        setAcceptanceRecords(Array.isArray(recordsData) ? recordsData : []);
      } catch (err) {
        setAcceptanceRecords([]);
        notification.warning({
          message: 'Không thể tải phiếu duyệt S-39',
          description: err.response?.data?.message || err.message
        });
      }
    } catch (err) {
      console.error('Lỗi tải dữ liệu nghiệm thu:', err);
      notification.error({
        message: 'Lỗi tải dữ liệu',
        description: err.message
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [selectedProjectId]);

  // 3. Xử lý mở Modal tạo mới phiếu nghiệm thu (T-83)
  const handleOpenCreateModal = async () => {
    setEditingReportId(null);
    form.resetFields();
    setItems([]);

    let defaultPeriod = 1;
    let defaultCode = 'NT-01';

    try {
      const nextData = await getNextPeriod(selectedProjectId);
      if (nextData) {
        defaultPeriod = nextData.nextPeriodNumber || 1;
        defaultCode = nextData.suggestedReportNumber || `NT-${String(defaultPeriod).padStart(2, '0')}`;
      }
    } catch (e) {
      defaultPeriod = (reports.length || 0) + 1;
      defaultCode = `NT-${String(defaultPeriod).padStart(2, '0')}`;
    }

    form.setFieldsValue({
      reportNumber: defaultCode,
      name: `Nghiệm thu khối lượng đợt ${defaultPeriod}`,
      periodNumber: defaultPeriod,
      acceptanceDate: dayjs(),
      status: 'DRAFT',
      notes: ''
    });

    setModalOpen(true);
  };

  // 4. Xử lý mở Modal chỉnh sửa phiếu nghiệm thu
  const handleOpenEditModal = async record => {
    try {
      const detail = await getAcceptanceReport(selectedProjectId, record.id);
      setEditingReportId(record.id);
      form.setFieldsValue({
        reportNumber: detail.reportNumber,
        name: detail.name,
        periodNumber: detail.periodNumber,
        acceptanceDate: detail.acceptanceDate ? dayjs(detail.acceptanceDate) : dayjs(),
        status: detail.status,
        notes: detail.notes,
        location: detail.location,
        ownerRepresentativeName: detail.ownerRepresentativeName,
        supervisorRepresentativeName: detail.supervisorRepresentativeName,
        contractorRepresentativeName: detail.contractorRepresentativeName,
        acceptanceRecordId: detail.acceptanceRecordId
      });
      setItems(detail.items || []);
      setModalOpen(true);
    } catch (err) {
      notification.error({
        message: 'Không thể mở phiếu nghiệm thu',
        description: err.message
      });
    }
  };

  // 5. T-84: Chọn hạng mục từ WBS thêm vào phiếu nghiệm thu
  const handleAddWbsItem = wbs => {
    // Kiểm tra xem đã có hạng mục này chưa
    const exists = items.some(i => i.wbsItemId === wbs.id);
    if (exists) {
      notification.warning({ message: 'Hạng mục này đã có trong danh sách nghiệm thu' });
      return;
    }

    const newItem = {
      wbsItemId: wbs.id,
      itemName: wbs.name,
      unit: wbs.unit || 'm3',
      plannedQuantity: wbs.quantity || wbs.totalVolume || 0,
      acceptedQuantity: wbs.quantity || wbs.totalVolume || 0,
      unitPrice: wbs.unitPrice || 0,
      amount: calculateItemAmount(wbs.quantity || wbs.totalVolume || 0, wbs.unitPrice || 0),
      cumulativeQuantity: 0,
      notes: ''
    };

    setItems(prev => [...prev, newItem]);
    setWbsSelectModalOpen(false);
  };

  // Thêm dòng tự do ngoài WBS
  const handleAddCustomItem = () => {
    const newItem = {
      wbsItemId: null,
      itemName: `Hạng mục ${items.length + 1}`,
      unit: 'công việc',
      plannedQuantity: 1,
      acceptedQuantity: 1,
      unitPrice: 0,
      amount: 0,
      cumulativeQuantity: 0,
      notes: ''
    };
    setItems(prev => [...prev, newItem]);
  };

  // Xóa một hạng mục khỏi danh sách
  const handleRemoveItem = index => {
    setItems(prev => prev.filter((_, idx) => idx !== index));
  };

  // T-85: Cập nhật khối lượng hoặc đơn giá và tự động tính thành tiền realtime
  const handleItemFieldChange = (index, field, value) => {
    setItems(prev => {
      const updated = [...prev];
      const target = { ...updated[index], [field]: value };

      if (field === 'acceptedQuantity' || field === 'unitPrice') {
        target.amount = calculateItemAmount(
          field === 'acceptedQuantity' ? value : target.acceptedQuantity,
          field === 'unitPrice' ? value : target.unitPrice
        );
      }
      updated[index] = target;
      return updated;
    });
  };

  // T-85: Tổng giá trị nghiệm thu tự động tính của toàn bộ phiếu
  const currentTotalAmount = calculateTotalAmount(items);

  // 6. Gửi biểu mẫu tạo hoặc cập nhật phiếu nghiệm thu
  const onFinish = async values => {
    if (!selectedProjectId) {
      notification.warning({ message: 'Vui lòng chọn một dự án' });
      return;
    }

    if (items.length === 0) {
      notification.warning({
        message: 'Chưa có hạng mục nghiệm thu',
        description: 'Vui lòng thêm ít nhất một hạng mục nghiệm thu (T-84).'
      });
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        reportNumber: values.reportNumber,
        name: values.name,
        periodNumber: values.periodNumber,
        acceptanceDate: values.acceptanceDate.format('YYYY-MM-DD'),
        status: values.status,
        notes: values.notes,
        location: values.location,
        ownerRepresentativeName: values.ownerRepresentativeName,
        supervisorRepresentativeName: values.supervisorRepresentativeName,
        contractorRepresentativeName: values.contractorRepresentativeName,
        acceptanceRecordId: values.acceptanceRecordId || null,
        items: items.map(item => ({
          wbsItemId: item.wbsItemId,
          taskId: item.taskId,
          itemName: item.itemName,
          unit: item.unit,
          plannedQuantity: item.plannedQuantity || 0,
          acceptedQuantity: item.acceptedQuantity || 0,
          unitPrice: item.unitPrice || 0,
          cumulativeQuantity: item.cumulativeQuantity || 0,
          notes: item.notes
        }))
      };

      if (editingReportId) {
        await updateAcceptanceReport(selectedProjectId, editingReportId, payload);
        notification.success({
          message: 'Cập nhật thành công',
          description: 'Phiếu nghiệm thu đã được cập nhật thành công.'
        });
      } else {
        await createAcceptanceReport(selectedProjectId, payload);
        notification.success({
          message: 'Tạo phiếu nghiệm thu thành công (T-83)',
          description: `Đã tạo phiếu nghiệm thu đợt ${values.periodNumber} với tổng giá trị ${formatCurrency(currentTotalAmount)} (T-85).`
        });
      }

      setModalOpen(false);
      await loadData();
    } catch (err) {
      notification.error({
        message: 'Lỗi lưu phiếu nghiệm thu',
        description: err.response?.data?.message || err.message
      });
    } finally {
      setSubmitting(false);
    }
  };

  // 7. Xóa phiếu nghiệm thu
  const handleDeleteReport = async reportId => {
    try {
      await deleteAcceptanceReport(selectedProjectId, reportId);
      notification.success({ message: 'Đã xóa phiếu nghiệm thu' });
      await loadData();
    } catch (err) {
      notification.error({
        message: 'Không thể xóa phiếu nghiệm thu',
        description: err.response?.data?.message || err.message
      });
    }
  };

  const handleExportDossier = async report => {
    setPdfExportingId(report.id);
    try {
      const dossier = await getAcceptanceDossier(report.id);
      setPdfExportData(dossier);
    } catch (err) {
      setPdfExportingId(null);
      notification.error({
        message: 'Không thể xuất hồ sơ nghiệm thu',
        description: err.response?.data?.message || err.message
      });
    }
  };

  useEffect(() => {
    if (!pdfExportData || !pdfDocumentRef.current) return;

    const generatePdf = async () => {
      try {
        const html2pdf = (await import('html2pdf.js')).default;
        const safeReportNumber = String(pdfExportData.reportNumber || 'nghiem-thu')
          .replace(/[<>:"/\\|?*\u0000-\u001F]/g, '-');
        await html2pdf()
          .set({
            margin: 0,
            filename: `Bien-ban-nghiem-thu-${safeReportNumber}.pdf`,
            image: { type: 'jpeg', quality: 0.98 },
            html2canvas: {
              scale: 2,
              useCORS: true,
              backgroundColor: '#ffffff',
              scrollX: 0,
              scrollY: 0
            },
            jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
            pagebreak: { mode: ['css', 'legacy'] }
          })
          .from(pdfDocumentRef.current)
          .save();
        notification.success({ message: 'Đã tải biên bản nghiệm thu PDF' });
      } catch (err) {
        notification.error({
          message: 'Tạo PDF thất bại',
          description: err.message || 'Không thể tạo tệp PDF.'
        });
      } finally {
        setPdfExportData(null);
        setPdfExportingId(null);
      }
    };

    generatePdf();
  }, [pdfExportData]);

  // Cột bảng danh sách phiếu nghiệm thu
  const columns = [
    {
      title: 'Mã số biên bản',
      dataIndex: 'reportNumber',
      key: 'reportNumber',
      width: 140,
      render: text => <strong style={{ color: '#1890ff' }}>{text}</strong>
    },
    {
      title: 'Tên phiếu nghiệm thu',
      dataIndex: 'name',
      key: 'name',
      width: 280,
      render: (text, r) => (
        <div>
          <div style={{ fontWeight: 600 }}>{text}</div>
          <div style={{ fontSize: '12px', color: '#8c8c8c' }}>
            Đợt số: <strong>{r.periodNumber}</strong> · Ngày: {dayjs(r.acceptanceDate).format('DD/MM/YYYY')}
          </div>
        </div>
      )
    },
    {
      title: 'Số hạng mục',
      dataIndex: 'itemCount',
      key: 'itemCount',
      width: 120,
      align: 'center',
      render: count => <Tag color="blue">{count} hạng mục</Tag>
    },
    {
      title: 'Giá trị nghiệm thu (T-85)',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 200,
      align: 'right',
      render: amount => (
        <span style={{ fontWeight: 700, color: '#389e0d', fontSize: '14px' }}>
          {formatCurrency(amount)}
        </span>
      )
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: status => {
        if (status === 'APPROVED') {
          return <Tag icon={<CheckCircleOutlined />} color="success">Đã phê duyệt</Tag>;
        }
        if (status === 'SUBMITTED') {
          return <Tag icon={<ClockCircleOutlined />} color="processing">Chờ duyệt</Tag>;
        }
        if (status === 'REJECTED') {
          return <Tag icon={<CloseCircleOutlined />} color="error">Từ chối</Tag>;
        }
        return <Tag color="default">Bản nháp</Tag>;
      }
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 240,
      render: (_, record) => (
        <Space size="small">
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={async () => {
              const detail = await getAcceptanceReport(selectedProjectId, record.id);
              setDetailReport(detail);
            }}
          >
            Chi tiết
          </Button>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleOpenEditModal(record)}
            disabled={record.status === 'APPROVED'}
          >
            Sửa
          </Button>
          <Button
            size="small"
            icon={<FilePdfOutlined />}
            loading={pdfExportingId === record.id}
            disabled={pdfExportingId !== null && pdfExportingId !== record.id}
            onClick={() => handleExportDossier(record)}
          >
            Xuất PDF
          </Button>
          <Popconfirm
            title="Xác nhận xóa phiếu nghiệm thu này?"
            onConfirm={() => handleDeleteReport(record.id)}
            disabled={record.status === 'APPROVED'}
            okText="Xóa"
            cancelText="Hủy"
          >
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              disabled={record.status === 'APPROVED'}
            />
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <div style={{ padding: '24px', width: '100%', maxWidth: '1440px', margin: '0 auto', boxSizing: 'border-box' }}>
      {/* Tiêu đề trang */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <Title level={2} style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AuditOutlined style={{ color: '#fa8c16' }} /> Nghiệm thu khối lượng theo đợt
          </Title>
          <Text type="secondary">
            Lập phiếu nghiệm thu theo từng đợt, chọn hạng mục và tự động tính toán giá trị nghiệm thu (Story S-36).
          </Text>
        </div>
      </div>

      {/* Chọn dự án & Thao tác */}
      <Card size="small" style={{ marginBottom: '20px', background: '#fafafa' }}>
        <Row gutter={[16, 12]} align="middle">
          <Col xs={24} md={10}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Text strong style={{ flexShrink: 0 }}>Dự án:</Text>
              <Select
                value={selectedProjectId}
                onChange={setSelectedProjectId}
                style={{ flex: 1 }}
                options={projects.map(p => ({ label: `${p.code} - ${p.name}`, value: p.id }))}
                placeholder="Chọn dự án..."
                showSearch
                optionFilterProp="label"
              />
            </div>
          </Col>
          <Col xs={24} md={14}>
            <Space wrap style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button icon={<ReloadOutlined />} onClick={loadData}>
                Làm mới
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleOpenCreateModal}
                disabled={!selectedProjectId}
                style={{ backgroundColor: '#fa8c16', borderColor: '#fa8c16' }}
              >
                Tạo phiếu nghiệm thu (T-83)
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Thẻ thống kê (Metrics Cards) */}
      <Row gutter={[16, 16]} style={{ marginBottom: '20px' }}>
        <Col xs={24} sm={6}>
          <Card size="small" style={{ borderRadius: '6px', borderLeft: '4px solid #1890ff' }}>
            <Text type="secondary" style={{ fontSize: '12px' }}>Tổng số phiếu</Text>
            <div style={{ fontSize: '20px', fontWeight: 700, color: '#262626' }}>
              {reports.length} đợt
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={6}>
          <Card size="small" style={{ borderRadius: '6px', borderLeft: '4px solid #52c41a' }}>
            <Text type="secondary" style={{ fontSize: '12px' }}>Đã phê duyệt</Text>
            <div style={{ fontSize: '20px', fontWeight: 700, color: '#389e0d' }}>
              {reports.filter(r => r.status === 'APPROVED').length} phiếu
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={6}>
          <Card size="small" style={{ borderRadius: '6px', borderLeft: '4px solid #faad14' }}>
            <Text type="secondary" style={{ fontSize: '12px' }}>Chờ duyệt / Nháp</Text>
            <div style={{ fontSize: '20px', fontWeight: 700, color: '#d48806' }}>
              {reports.filter(r => r.status !== 'APPROVED').length} phiếu
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={6}>
          <Card size="small" style={{ borderRadius: '6px', borderLeft: '4px solid #fa8c16', background: '#fffbe6' }}>
            <Text type="secondary" style={{ fontSize: '12px' }}>Tổng giá trị nghiệm thu</Text>
            <div style={{ fontSize: '18px', fontWeight: 700, color: '#d48806' }}>
              {formatCurrency(reports.reduce((sum, r) => sum + (Number(r.totalAmount) || 0), 0))}
            </div>
          </Card>
        </Col>
      </Row>

      {/* Bảng danh sách phiếu nghiệm thu */}
      <Card
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>Danh sách phiếu nghiệm thu theo từng đợt</span>
            <Text type="secondary" style={{ fontSize: '13px' }}>
              Tổng cộng: {reports.length} phiếu
            </Text>
          </div>
        }
      >
        <Table
          dataSource={reports}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 6, showSizeChanger: true }}
          size="middle"
          scroll={{ x: 1000 }}
        />
      </Card>

      {/* Modal Lập / Cập nhật Phiếu Nghiệm Thu (T-83, T-84, T-85) */}
      <Modal
        title={
          <Space>
            <AuditOutlined style={{ color: '#fa8c16' }} />
            <span>{editingReportId ? 'Chỉnh sửa phiếu nghiệm thu' : 'Tạo phiếu nghiệm thu theo đợt (T-83)'}</span>
          </Space>
        }
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        width={920}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Row gutter={12}>
            <Col xs={24} sm={8}>
              <Form.Item
                name="reportNumber"
                label="Mã số biên bản nghiệm thu"
                rules={[{ required: true, message: 'Vui lòng nhập mã biên bản' }]}
              >
                <Input placeholder="Ví dụ: NT-01" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={10}>
              <Form.Item
                name="name"
                label="Tên phiếu nghiệm thu"
                rules={[{ required: true, message: 'Vui lòng nhập tên phiếu' }]}
              >
                <Input placeholder="Ví dụ: Nghiệm thu khối lượng đợt 1" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={6}>
              <Form.Item
                name="periodNumber"
                label="Đợt số"
                rules={[{ required: true, message: 'Vui lòng nhập số đợt' }]}
              >
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={12}>
            <Col xs={24} sm={12}>
              <Form.Item
                name="acceptanceDate"
                label="Ngày nghiệm thu"
                rules={[{ required: true, message: 'Vui lòng chọn ngày nghiệm thu' }]}
              >
                <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item name="status" label="Trạng thái">
                <Select
                  options={[
                    { label: 'Bản nháp (DRAFT)', value: 'DRAFT' },
                    { label: 'Gửi phê duyệt (SUBMITTED)', value: 'SUBMITTED' },
                    { label: 'Đã phê duyệt (APPROVED)', value: 'APPROVED' }
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="notes" label="Ghi chú / Nhận xét nghiệm thu">
            <TextArea rows={2} placeholder="Ghi nhận đánh giá hiện trường hoặc ghi chú hồ sơ..." />
          </Form.Item>

          <Divider orientation="left" style={{ margin: '12px 0' }}>
            Thông tin hồ sơ nghiệm thu
          </Divider>
          <Form.Item name="location" label="Vị trí / đoạn thi công">
            <Input placeholder="Ví dụ: Trục A-B, tầng 2, khu vực phía Đông..." />
          </Form.Item>
          <Row gutter={12}>
            <Col xs={24} md={8}>
              <Form.Item name="ownerRepresentativeName" label="Đại diện Chủ đầu tư">
                <Input placeholder="Họ và tên" />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="supervisorRepresentativeName" label="Đại diện Tư vấn giám sát">
                <Input placeholder="Họ và tên" />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="contractorRepresentativeName" label="Đại diện Nhà thầu">
                <Input placeholder="Họ và tên" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="acceptanceRecordId" label="Liên kết phiếu nghiệm thu / phê duyệt S-39">
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="Chọn phiếu S-39 để đưa kết quả và người duyệt vào hồ sơ"
              options={acceptanceRecords.map(acceptanceRecord => ({
                value: acceptanceRecord.id,
                label: `${acceptanceRecord.title} · ${acceptanceRecord.inspectionDate ? dayjs(acceptanceRecord.inspectionDate).format('DD/MM/YYYY') : 'Chưa có ngày'} · ${
                  acceptanceRecord.status === 'APPROVED'
                    ? 'Đã duyệt'
                    : acceptanceRecord.status === 'RETURNED'
                      ? 'Trả lại'
                      : 'Chờ duyệt'
                }`
              }))}
            />
          </Form.Item>

          <Divider orientation="left" style={{ margin: '16px 0 12px 0' }}>
            <span style={{ fontSize: '14px', fontWeight: 600 }}>
              Hạng mục nghiệm thu chi tiết (T-84)
            </span>
          </Divider>

          {/* Nút thêm hạng mục */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <Text type="secondary">
              Chọn công việc từ WBS để tự động điền đơn giá và khối lượng thiết kế.
            </Text>
            <Space>
              <Button
                type="dashed"
                icon={<PlusOutlined />}
                onClick={() => setWbsSelectModalOpen(true)}
              >
                Chọn từ WBS (T-84)
              </Button>
              <Button size="small" onClick={handleAddCustomItem}>
                Thêm dòng tự do
              </Button>
            </Space>
          </div>

          {/* Bảng danh sách hạng mục nhập liệu động (T-84 & T-85) */}
          <Table
            dataSource={items}
            rowKey={(_, idx) => idx}
            pagination={false}
            size="small"
            bordered
            locale={{ emptyText: 'Chưa có hạng mục nào được chọn. Nhấn "Chọn từ WBS" để thêm.' }}
            columns={[
              {
                title: 'Tên hạng mục (T-84)',
                dataIndex: 'itemName',
                key: 'itemName',
                width: 220,
                render: (val, _, idx) => (
                  <Input
                    size="small"
                    value={val}
                    onChange={e => handleItemFieldChange(idx, 'itemName', e.target.value)}
                    placeholder="Tên công việc"
                  />
                )
              },
              {
                title: 'ĐVT',
                dataIndex: 'unit',
                key: 'unit',
                width: 80,
                render: (val, _, idx) => (
                  <Input
                    size="small"
                    value={val}
                    onChange={e => handleItemFieldChange(idx, 'unit', e.target.value)}
                    placeholder="m3"
                  />
                )
              },
              {
                title: 'KL Thiết kế',
                dataIndex: 'plannedQuantity',
                key: 'plannedQuantity',
                width: 100,
                align: 'right',
                render: val => Number(val || 0).toLocaleString('vi-VN')
              },
              {
                title: 'KL Nghiệm thu (T-84)',
                dataIndex: 'acceptedQuantity',
                key: 'acceptedQuantity',
                width: 130,
                render: (val, _, idx) => (
                  <InputNumber
                    size="small"
                    min={0}
                    style={{ width: '100%' }}
                    value={val}
                    onChange={v => handleItemFieldChange(idx, 'acceptedQuantity', v)}
                  />
                )
              },
              {
                title: 'Đơn giá (VNĐ)',
                dataIndex: 'unitPrice',
                key: 'unitPrice',
                width: 130,
                render: (val, _, idx) => (
                  <InputNumber
                    size="small"
                    min={0}
                    style={{ width: '100%' }}
                    value={val}
                    formatter={v => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                    parser={v => v.replace(/\$\s?|(,*)/g, '')}
                    onChange={v => handleItemFieldChange(idx, 'unitPrice', v)}
                  />
                )
              },
              {
                title: 'Thành tiền (T-85)',
                dataIndex: 'amount',
                key: 'amount',
                width: 140,
                align: 'right',
                render: (_, r) => (
                  <strong style={{ color: '#389e0d' }}>
                    {formatCurrency(calculateItemAmount(r.acceptedQuantity, r.unitPrice))}
                  </strong>
                )
              },
              {
                title: '',
                key: 'actions',
                width: 50,
                align: 'center',
                render: (_, __, idx) => (
                  <Button
                    type="text"
                    danger
                    size="small"
                    icon={<DeleteOutlined />}
                    onClick={() => handleRemoveItem(idx)}
                  />
                )
              }
            ]}
          />

          {/* T-85: Hộp hiển thị tổng giá trị tự động tính của phiếu */}
          <div
            style={{
              marginTop: 16,
              padding: '12px 16px',
              borderRadius: '6px',
              background: '#fffbe6',
              border: '1px solid #ffe58f',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <CalculatorOutlined style={{ fontSize: 20, color: '#fa8c16' }} />
              <div>
                <strong style={{ fontSize: 14 }}>Tự động tính tổng giá trị nghiệm thu (T-85):</strong>
                <div style={{ fontSize: 12, color: '#8c8c8c' }}>
                  Tổng cộng thành tiền của {items.length} hạng mục nghiệm thu
                </div>
              </div>
            </div>
            <div style={{ fontSize: 20, fontWeight: 700, color: '#d48806' }}>
              {formatCurrency(currentTotalAmount)}
            </div>
          </div>

          <div style={{ marginTop: 24, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
            <Button onClick={() => setModalOpen(false)}>Hủy</Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={submitting}
              style={{ backgroundColor: '#fa8c16', borderColor: '#fa8c16' }}
            >
              {editingReportId ? 'Lưu thay đổi' : 'Tạo phiếu nghiệm thu'}
            </Button>
          </div>
        </Form>
      </Modal>

      {/* Modal Chọn Hạng Mục WBS (T-84) */}
      <Modal
        title="Chọn công việc từ cấu trúc WBS của dự án (T-84)"
        open={wbsSelectModalOpen}
        onCancel={() => setWbsSelectModalOpen(false)}
        footer={null}
        width={750}
      >
        <Table
          dataSource={wbsItems}
          rowKey="id"
          size="small"
          pagination={{ pageSize: 6 }}
          columns={[
            {
              title: 'Mã WBS',
              dataIndex: 'code',
              key: 'code',
              width: 100,
              render: code => <code>{code || '--'}</code>
            },
            {
              title: 'Tên công việc / hạng mục',
              dataIndex: 'name',
              key: 'name'
            },
            {
              title: 'ĐVT',
              dataIndex: 'unit',
              key: 'unit',
              width: 80,
              render: u => u || 'm3'
            },
            {
              title: 'Khối lượng thiết kế',
              key: 'qty',
              width: 130,
              align: 'right',
              render: (_, r) => (r.quantity || r.totalVolume || 0).toLocaleString('vi-VN')
            },
            {
              title: 'Đơn giá (VNĐ)',
              dataIndex: 'unitPrice',
              key: 'unitPrice',
              width: 130,
              align: 'right',
              render: price => formatCurrency(price || 0)
            },
            {
              title: 'Chọn',
              key: 'select',
              width: 80,
              align: 'center',
              render: (_, record) => (
                <Button
                  type="primary"
                  size="small"
                  onClick={() => handleAddWbsItem(record)}
                >
                  Chọn
                </Button>
              )
            }
          ]}
        />
      </Modal>

      <div className="acceptance-dossier-export-host" aria-hidden="true">
        {pdfExportData && (
          <div ref={pdfDocumentRef}>
            <AcceptanceDossierDocument dossier={pdfExportData} />
          </div>
        )}
      </div>

      {/* Modal Xem Chi Tiết Phiếu Nghiệm Thu */}
      <Modal
        title={
          <Space>
            <FileTextOutlined style={{ color: '#1890ff' }} />
            <span>Chi tiết phiếu nghiệm thu {detailReport?.reportNumber}</span>
          </Space>
        }
        open={Boolean(detailReport)}
        onCancel={() => setDetailReport(null)}
        footer={[
          <Button key="close" onClick={() => setDetailReport(null)}>
            Đóng
          </Button>
        ]}
        width={800}
      >
        {detailReport && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Row gutter={[16, 8]}>
              <Col span={12}>
                <Text strong>Mã số biên bản: </Text>{detailReport.reportNumber}
              </Col>
              <Col span={12}>
                <Text strong>Đợt nghiệm thu: </Text>Đợt số {detailReport.periodNumber}
              </Col>
              <Col span={12}>
                <Text strong>Tên phiếu: </Text>{detailReport.name}
              </Col>
              <Col span={12}>
                <Text strong>Ngày nghiệm thu: </Text>{dayjs(detailReport.acceptanceDate).format('DD/MM/YYYY')}
              </Col>
              <Col span={12}>
                <Text strong>Trạng thái: </Text>
                {detailReport.status === 'APPROVED' ? (
                  <Tag color="success">Đã phê duyệt</Tag>
                ) : detailReport.status === 'SUBMITTED' ? (
                  <Tag color="processing">Chờ duyệt</Tag>
                ) : (
                  <Tag>Bản nháp</Tag>
                )}
              </Col>
              <Col span={12}>
                <Text strong>Ghi chú: </Text>{detailReport.notes || 'Không có'}
              </Col>
            </Row>

            <Divider style={{ margin: '8px 0' }} />

            <div style={{ fontWeight: 600, marginBottom: 8 }}>
              Danh sách hạng mục nghiệm thu:
            </div>

            <Table
              dataSource={detailReport.items || []}
              rowKey="id"
              size="small"
              pagination={false}
              bordered
              columns={[
                { title: 'STT', key: 'idx', width: 50, render: (_, __, i) => i + 1 },
                { title: 'Hạng mục', dataIndex: 'itemName', key: 'itemName' },
                { title: 'ĐVT', dataIndex: 'unit', key: 'unit', width: 70 },
                {
                  title: 'KL Nghiệm thu',
                  dataIndex: 'acceptedQuantity',
                  key: 'acceptedQuantity',
                  width: 120,
                  align: 'right',
                  render: v => Number(v || 0).toLocaleString('vi-VN')
                },
                {
                  title: 'Đơn giá',
                  dataIndex: 'unitPrice',
                  key: 'unitPrice',
                  width: 120,
                  align: 'right',
                  render: v => formatCurrency(v)
                },
                {
                  title: 'Thành tiền (T-85)',
                  dataIndex: 'amount',
                  key: 'amount',
                  width: 140,
                  align: 'right',
                  render: v => <strong style={{ color: '#389e0d' }}>{formatCurrency(v)}</strong>
                }
              ]}
            />

            <div
              style={{
                display: 'flex',
                justifyContent: 'flex-end',
                alignItems: 'center',
                gap: 16,
                padding: '12px 16px',
                background: '#f6ffed',
                borderRadius: '6px',
                border: '1px solid #b7eb8f'
              }}
            >
              <span style={{ fontSize: 14, fontWeight: 600 }}>Tổng giá trị nghiệm thu phiếu:</span>
              <span style={{ fontSize: 20, fontWeight: 700, color: '#389e0d' }}>
                {formatCurrency(detailReport.totalAmount)}
              </span>
            </div>
          </Space>
        )}
      </Modal>
    </div>
  );
};

export default AcceptancePage;
