import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Modal,
  Spin,
  message
} from 'antd';
import {
  PlusOutlined,
  DownloadOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons';

import ProjectHeroCard from '../components/wbs/ProjectHeroCard';
import WbsStats from '../components/wbs/WbsStats';
import WbsToolbar from '../components/wbs/WbsToolbar';
import WbsTreeTable from '../components/wbs/WbsTreeTable';
import WorkDetailDrawer from '../components/wbs/WorkDetailDrawer';
import WorkFormModal from '../components/wbs/WorkFormModal';
import EmptyWbs from '../components/wbs/EmptyWbs';

import {
  calculateStats,
  getAllNodeKeys,
  filterTreeData,
  findNodeById
} from '../utils/wbsUtils';

import {
  getProjects,
  getProjectWbs,
  createWbsItem,
  updateWbsItem,
  deleteWbsItem
} from '../services/wbsApi';

/*
 * Backend hiện lưu 1 trường image.
 * Giao diện Drawer hiện sử dụng mảng images.
 * Vì vậy chuyển image -> images tại đây.
 */
const mapApiItem = (item) => ({
  ...item,

  assignee: item.assigneeId
    ? {
        id: item.assigneeId,
        name: item.assigneeName,
        initials: item.assigneeInitials
      }
    : null,

  images: item.image ? [item.image] : [],

  children: []
});

/*
 * Backend trả danh sách WBS dạng phẳng:
 * id + parentId.
 *
 * Frontend cần dạng cây children.
 */
const buildWbsTree = (items = []) => {
  const nodeMap = new Map();

  items.forEach(item => {
    nodeMap.set(item.id, mapApiItem(item));
  });

  const roots = [];

  items.forEach(item => {
    const node = nodeMap.get(item.id);

    if (item.parentId && nodeMap.has(item.parentId)) {
      nodeMap.get(item.parentId).children.push(node);
    } else {
      roots.push(node);
    }
  });

  const sortNodes = nodes => {
    nodes.sort((a, b) =>
      String(a.wbsCode || '').localeCompare(
        String(b.wbsCode || ''),
        undefined,
        { numeric: true }
      )
    );

    nodes.forEach(node => {
      if (node.children?.length) {
        sortNodes(node.children);
      }
    });
  };

  sortNodes(roots);

  return roots;
};

/*
 * Chuẩn hóa dữ liệu từ WorkFormModal
 * trước khi gửi Spring Boot API.
 */
const normalizeFormData = formData => {
  const assignee = formData.assignee || null;

  return {
    wbsCode: formData.wbsCode,
    name: formData.name,
    type: formData.type,

    parentId: formData.parentId || null,

    assigneeId:
      formData.assigneeId ||
      assignee?.id ||
      null,

    assigneeName:
      formData.assigneeName ||
      assignee?.name ||
      null,

    assigneeInitials:
      formData.assigneeInitials ||
      assignee?.initials ||
      null,

    status: formData.status || 'not_started',

    progress: Number(formData.progress || 0),

    startDate: formData.startDate || null,
    endDate: formData.endDate || null,

    description: formData.description || '',

    image:
      formData.image ||
      formData.images?.[0] ||
      null
  };
};

const WBSPage = () => {
  // =========================================================
  // PROJECT / WBS DATA
  // =========================================================

  const [projects, setProjects] = useState([]);

  const [
    selectedProjectId,
    setSelectedProjectId
  ] = useState(null);

  const [
    rawTreeNodes,
    setRawTreeNodes
  ] = useState([]);

  // =========================================================
  // LOADING / ERROR
  // =========================================================

  const [
    loadingProjects,
    setLoadingProjects
  ] = useState(true);

  const [
    loadingWbs,
    setLoadingWbs
  ] = useState(false);

  const [
    pageError,
    setPageError
  ] = useState('');

  // =========================================================
  // SEARCH / FILTER
  // =========================================================

  const [
    searchText,
    setSearchText
  ] = useState('');

  const [
    statusFilter,
    setStatusFilter
  ] = useState('ALL');

  // =========================================================
  // TREE
  // =========================================================

  const [
    expandedKeys,
    setExpandedKeys
  ] = useState([]);

  // =========================================================
  // DRAWER
  // =========================================================

  const [
    selectedNodeId,
    setSelectedNodeId
  ] = useState(null);

  const [
    drawerVisible,
    setDrawerVisible
  ] = useState(false);

  // =========================================================
  // MODAL
  // =========================================================

  const [
    modalVisible,
    setModalVisible
  ] = useState(false);

  const [
    modalIsEdit,
    setModalIsEdit
  ] = useState(false);

  const [
    modalInitialValues,
    setModalInitialValues
  ] = useState(null);

  const [
    submitting,
    setSubmitting
  ] = useState(false);

  // =========================================================
  // CURRENT PROJECT
  // =========================================================

  const currentProject = useMemo(() => {
    return (
      projects.find(
        project => project.id === selectedProjectId
      ) || null
    );
  }, [projects, selectedProjectId]);

  // =========================================================
  // LOAD PROJECTS
  // =========================================================

  const loadProjects = async () => {
    setLoadingProjects(true);
    setPageError('');

    try {
      const data = await getProjects();

      const projectList =
        Array.isArray(data) ? data : [];

      setProjects(projectList);

      if (projectList.length > 0) {
        setSelectedProjectId(current => {
          const stillExists = projectList.some(
            project => project.id === current
          );

          return stillExists
            ? current
            : projectList[0].id;
        });
      } else {
        setSelectedProjectId(null);
        setRawTreeNodes([]);
      }
    } catch (error) {
      setPageError(
        error.message ||
        'Không thể tải danh sách dự án'
      );
    } finally {
      setLoadingProjects(false);
    }
  };

  // =========================================================
  // LOAD WBS
  // =========================================================

  const loadWbs = async projectId => {
    if (!projectId) {
      setRawTreeNodes([]);
      return;
    }

    setLoadingWbs(true);
    setPageError('');

    try {
      const data =
        await getProjectWbs(projectId);

      const tree = buildWbsTree(
        Array.isArray(data) ? data : []
      );

      setRawTreeNodes(tree);

      setExpandedKeys(
        getAllNodeKeys(tree)
      );
    } catch (error) {
      setRawTreeNodes([]);
      setExpandedKeys([]);

      setPageError(
        error.message ||
        'Không thể tải dữ liệu WBS'
      );
    } finally {
      setLoadingWbs(false);
    }
  };

  // =========================================================
  // INITIAL LOAD
  // =========================================================

  useEffect(() => {
    loadProjects();
  }, []);

  useEffect(() => {
    if (selectedProjectId) {
      loadWbs(selectedProjectId);
    }
  }, [selectedProjectId]);

  // =========================================================
  // FILTER
  // =========================================================

  const {
    filteredNodes,
    matchingKeys
  } = useMemo(() => {
    return filterTreeData(
      rawTreeNodes,
      searchText,
      statusFilter
    );
  }, [
    rawTreeNodes,
    searchText,
    statusFilter
  ]);

  const effectiveExpandedKeys =
    useMemo(() => {
      if (
        searchText ||
        (
          statusFilter &&
          statusFilter !== 'ALL'
        )
      ) {
        return Array.from(
          new Set([
            ...expandedKeys,
            ...matchingKeys
          ])
        );
      }

      return expandedKeys;
    }, [
      expandedKeys,
      matchingKeys,
      searchText,
      statusFilter
    ]);

  // =========================================================
  // STATS
  // =========================================================

  const stats = useMemo(() => {
    return calculateStats(rawTreeNodes);
  }, [rawTreeNodes]);

  // =========================================================
  // PROJECT SELECT
  // =========================================================

  const handleSelectProject = projectId => {
    setSelectedProjectId(projectId);

    setSearchText('');
    setStatusFilter('ALL');

    setSelectedNodeId(null);
    setDrawerVisible(false);
  };

  // =========================================================
  // TREE EXPAND
  // =========================================================

  const handleToggleExpand = nodeId => {
    setExpandedKeys(previous => {
      if (previous.includes(nodeId)) {
        return previous.filter(
          key => key !== nodeId
        );
      }

      return [
        ...previous,
        nodeId
      ];
    });
  };

  const handleExpandAll = () => {
    setExpandedKeys(
      getAllNodeKeys(rawTreeNodes)
    );

    message.info(
      'Đã mở rộng tất cả các nhánh cây WBS'
    );
  };

  const handleCollapseAll = () => {
    setExpandedKeys([]);

    message.info(
      'Đã thu gọn tất cả cây WBS'
    );
  };

  const handleClearFilters = () => {
    setSearchText('');
    setStatusFilter('ALL');
  };

  // =========================================================
  // EXPORT CSV
  // =========================================================

  const handleExport = () => {
    if (!currentProject) {
      message.warning(
        'Chưa có dự án để xuất dữ liệu'
      );
      return;
    }

    const rows = [];

    const flatten = (
      items,
      level = 0
    ) => {
      items.forEach(item => {
        rows.push({
          level,
          wbsCode: item.wbsCode,
          name: item.name,
          type: item.type,
          assignee:
            item.assignee?.name || '',
          status: item.status,
          progress:
            `${item.progress || 0}%`,
          startDate:
            item.startDate || '',
          endDate:
            item.endDate || ''
        });

        if (item.children?.length) {
          flatten(
            item.children,
            level + 1
          );
        }
      });
    };

    flatten(rawTreeNodes);

    const headers = [
      'Cấp',
      'Mã WBS',
      'Tên công việc',
      'Loại',
      'Người phụ trách',
      'Trạng thái',
      'Tiến độ',
      'Ngày bắt đầu',
      'Ngày kết thúc'
    ];

    const escapeCsv = value =>
      `"${String(value ?? '')
        .replaceAll('"', '""')}"`;

    const csv = [
      headers,

      ...rows.map(row => [
        row.level,
        row.wbsCode,
        row.name,
        row.type,
        row.assignee,
        row.status,
        row.progress,
        row.startDate,
        row.endDate
      ])
    ]
      .map(row =>
        row
          .map(escapeCsv)
          .join(',')
      )
      .join('\n');

    const blob = new Blob(
      [`\uFEFF${csv}`],
      {
        type:
          'text/csv;charset=utf-8;'
      }
    );

    const url =
      URL.createObjectURL(blob);

    const link =
      document.createElement('a');

    link.href = url;

    link.download =
      `${currentProject.code}-wbs.csv`;

    link.click();

    URL.revokeObjectURL(url);

    message.success(
      'Đã xuất dữ liệu WBS'
    );
  };

  // =========================================================
  // DRAWER
  // =========================================================

  const handleSelectNode = node => {
    setSelectedNodeId(node.id);
    setDrawerVisible(true);
  };

  // =========================================================
  // ADD
  // =========================================================

  const handleOpenAddModal = (
    parent = null
  ) => {
    if (!selectedProjectId) {
      message.warning(
        'Vui lòng chọn dự án'
      );
      return;
    }

    setModalIsEdit(false);

    if (parent) {
      setModalInitialValues({
        parentId: parent.id,

        wbsCode:
          `${parent.wbsCode}.` +
          `${(parent.children?.length || 0) + 1}`
      });
    } else {
      setModalInitialValues(null);
    }

    setModalVisible(true);
  };

  // =========================================================
  // EDIT
  // =========================================================

  const handleOpenEditModal = node => {
    setModalIsEdit(true);

    setModalInitialValues({
      ...node,

      assigneeId:
        node.assigneeId ||
        node.assignee?.id ||
        null
    });

    setModalVisible(true);
  };

  // =========================================================
  // DELETE
  // =========================================================

  const handleDeleteNodeConfirm =
    node => {
      const hasChildren =
        node.children &&
        node.children.length > 0;

      Modal.confirm({
        title: 'Xóa công việc?',

        icon: (
          <ExclamationCircleOutlined
            style={{
              color: '#DC2626'
            }}
          />
        ),

        content: (
          <div>
            <p>
              Bạn có chắc chắn muốn
              xóa công việc{' '}
              <strong>
                "{node.name}"
              </strong>
              ?
            </p>

            {hasChildren && (
              <div
                style={{
                  color: '#DC2626',
                  fontWeight: 600,
                  marginTop: 8
                }}
              >
                ⚠️ Công việc này có
                công việc con. Toàn bộ
                công việc con cũng sẽ
                bị xóa.
              </div>
            )}
          </div>
        ),

        okText: 'Xóa công việc',
        okType: 'danger',
        cancelText: 'Hủy',

        async onOk() {
          try {
            await deleteWbsItem(
              selectedProjectId,
              node.id
            );

            if (
              selectedNodeId ===
              node.id
            ) {
              setSelectedNodeId(null);
              setDrawerVisible(false);
            }

            await loadWbs(
              selectedProjectId
            );

            message.success(
              'Xóa công việc thành công'
            );
          } catch (error) {
            message.error(
              error.message ||
              'Không thể xóa công việc'
            );

            throw error;
          }
        }
      });
    };

  // =========================================================
  // CREATE / UPDATE
  // =========================================================

  const handleFormSubmit =
    async formData => {
      if (!selectedProjectId) {
        message.error(
          'Chưa chọn dự án'
        );
        return;
      }

      setSubmitting(true);

      try {
        const payload =
          normalizeFormData(formData);

        if (modalIsEdit) {
          const itemId =
            formData.id ||
            modalInitialValues?.id;

          if (!itemId) {
            throw new Error(
              'Không xác định được công việc cần cập nhật'
            );
          }

          await updateWbsItem(
            selectedProjectId,
            itemId,
            payload
          );

          message.success(
            'Cập nhật công việc thành công'
          );
        } else {
          await createWbsItem(
            selectedProjectId,
            payload
          );

          message.success(
            'Thêm công việc thành công'
          );
        }

        setModalVisible(false);

        await loadWbs(
          selectedProjectId
        );
      } catch (error) {
        message.error(
          error.message ||
          (
            modalIsEdit
              ? 'Không thể cập nhật công việc'
              : 'Không thể thêm công việc'
          )
        );
      } finally {
        setSubmitting(false);
      }
    };

  // =========================================================
  // PARENT OPTIONS
  // =========================================================

  const parentOptions =
    useMemo(() => {
      const options = [];

      const traverse = items => {
        items.forEach(item => {
          options.push({
            id: item.id,
            name: item.name,
            wbsCode: item.wbsCode
          });

          if (item.children?.length) {
            traverse(item.children);
          }
        });
      };

      traverse(rawTreeNodes);

      return options;
    }, [rawTreeNodes]);

  // =========================================================
  // SELECTED NODE
  // =========================================================

  const activeSelectedNode =
    useMemo(() => {
      return findNodeById(
        rawTreeNodes,
        selectedNodeId
      );
    }, [
      rawTreeNodes,
      selectedNodeId
    ]);

  const activeParentName =
    useMemo(() => {
      if (
        !activeSelectedNode ||
        !activeSelectedNode.parentId
      ) {
        return null;
      }

      const parent =
        findNodeById(
          rawTreeNodes,
          activeSelectedNode.parentId
        );

      return parent
        ? `${parent.wbsCode} - ${parent.name}`
        : null;
    }, [
      rawTreeNodes,
      activeSelectedNode
    ]);

  // =========================================================
  // PROJECT LOADING
  // =========================================================

  if (loadingProjects) {
    return (
      <div
        style={{
          minHeight: 400,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}
      >
        <Spin
          size="large"
          tip="Đang tải dự án..."
        />
      </div>
    );
  }

  // =========================================================
  // UI
  // =========================================================

  return (
    <div>

      {/* PAGE HEADER */}
      <div className="page-header">
        <div>
          <h1 className="page-title">
            Cây cơ cấu công việc
          </h1>

          <p className="page-subtitle">
            Quản lý và theo dõi cấu trúc
            phân rã công việc
            (Work Breakdown Structure)
            của dự án
          </p>
        </div>

        <div className="page-actions">
          <Button
            icon={<DownloadOutlined />}
            onClick={handleExport}
            disabled={!currentProject}
          >
            Xuất dữ liệu
          </Button>

          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() =>
              handleOpenAddModal(null)
            }
            disabled={!currentProject}
          >
            + Thêm công việc
          </Button>
        </div>
      </div>

      {/* ERROR */}
      {pageError && (
        <Alert
          type="error"
          showIcon
          closable
          message="Không thể tải dữ liệu"
          description={pageError}
          style={{
            marginBottom: 16
          }}
          onClose={() =>
            setPageError('')
          }
        />
      )}

      {/* PROJECT HERO */}
      <ProjectHeroCard
        project={currentProject}
      />

      {/* STATISTICS */}
      <WbsStats
        stats={stats}
      />

      {/* WBS */}
      <div className="wbs-main-card">

        <div className="wbs-card-header">
          <div className="wbs-card-title-box">

            <span className="wbs-card-title">
              Cơ cấu phân rã công việc
            </span>

            <span className="wbs-card-subtitle">
              Work Breakdown Structure
              (WBS)
            </span>

          </div>
        </div>

        {/* TOOLBAR */}
        <WbsToolbar
          projects={projects}
          selectedProjectId={
            selectedProjectId
          }
          onSelectProject={
            handleSelectProject
          }
          searchText={searchText}
          onSearchChange={
            setSearchText
          }
          statusFilter={
            statusFilter
          }
          onStatusFilterChange={
            setStatusFilter
          }
          onExpandAll={
            handleExpandAll
          }
          onCollapseAll={
            handleCollapseAll
          }
          onClearFilters={
            handleClearFilters
          }
        />

        {/* TREE */}
        <Spin
          spinning={loadingWbs}
        >
          {!loadingWbs &&
          rawTreeNodes.length === 0 ? (
            <EmptyWbs
              onCreateFirstTask={() =>
                handleOpenAddModal(null)
              }
            />
          ) : (
            <WbsTreeTable
              nodes={filteredNodes}

              expandedKeys={
                effectiveExpandedKeys
              }

              onToggleExpand={
                handleToggleExpand
              }

              selectedNodeId={
                selectedNodeId
              }

              onSelectNode={
                handleSelectNode
              }

              onViewDetails={node => {
                setSelectedNodeId(
                  node.id
                );

                setDrawerVisible(true);
              }}

              onAddChild={node =>
                handleOpenAddModal(
                  node
                )
              }

              onEditNode={node =>
                handleOpenEditModal(
                  node
                )
              }

              onDeleteNode={node =>
                handleDeleteNodeConfirm(
                  node
                )
              }
            />
          )}
        </Spin>
      </div>

      {/* DETAIL DRAWER */}
      <WorkDetailDrawer
        visible={drawerVisible}
        allTasks={flatTaskList}

        onClose={() =>
          setDrawerVisible(false)
        }

        node={activeSelectedNode}

        parentName={
          activeParentName
        }

        onAddChild={node => {
          setDrawerVisible(false);

          handleOpenAddModal(
            node
          );
        }}

        onEditNode={node => {
          setDrawerVisible(false);

          handleOpenEditModal(
            node
          );
        }}

        onDeleteNode={node => {
          handleDeleteNodeConfirm(
            node
          );
        }}
      />

      {/* ADD / EDIT MODAL */}
      <WorkFormModal
        visible={modalVisible}

        onCancel={() => {
          if (!submitting) {
            setModalVisible(false);
          }
        }}

        onSubmit={
          handleFormSubmit
        }

        initialValues={
          modalInitialValues
        }

        parentOptions={
          parentOptions
        }

        isEdit={
          modalIsEdit
        }

        confirmLoading={
          submitting
        }
      />

    </div>
  );
};

export default WBSPage;