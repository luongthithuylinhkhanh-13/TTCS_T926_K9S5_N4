import React, { useEffect, useState } from 'react';
import { Alert, Empty, Select, Spin } from 'antd';
import { FlagOutlined } from '@ant-design/icons';
import { getProjects } from '../services/wbsApi';
import MilestoneAlertSection from '../components/wbs/MilestoneAlertSection';

const MilestonesPage = () => {
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getProjects()
      .then(data => {
        const projectList = Array.isArray(data) ? data : [];
        setProjects(projectList);
        setSelectedProjectId(projectList[0]?.id || null);
      })
      .catch(loadError => setError(loadError.message || 'Không thể tải danh sách dự án.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="page-container milestones-page">
      <div className="page-header">
        <div>
          <h1 className="page-title"><FlagOutlined /> Mốc tiến độ</h1>
          <p className="page-subtitle">Quản lý các mốc bắt buộc và cảnh báo công việc vượt hạn</p>
        </div>
        <Select
          value={selectedProjectId}
          onChange={setSelectedProjectId}
          loading={loading}
          disabled={loading || projects.length === 0}
          placeholder="Chọn dự án"
          style={{ width: 280 }}
          options={projects.map(project => ({
            value: project.id,
            label: `${project.code} - ${project.name}`
          }))}
        />
      </div>

      {error && <Alert type="error" showIcon message={error} />}
      <Spin spinning={loading}>
        {!loading && projects.length === 0 ? (
          <div className="wbs-main-card progress-empty-state">
            <Empty description="Chưa có dự án để quản lý mốc tiến độ" />
          </div>
        ) : selectedProjectId ? (
          <MilestoneAlertSection projectId={selectedProjectId} />
        ) : null}
      </Spin>
    </div>
  );
};

export default MilestonesPage;
