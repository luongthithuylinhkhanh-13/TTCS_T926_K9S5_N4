import React, { useEffect, useState } from 'react';
import { Alert, Avatar, Button, Empty, Select, Spin, Tag, Timeline, message } from 'antd';
import { TeamOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  assignTaskCrew,
  getProjectCrews,
  getTaskCrewAssignmentHistory
} from '../../services/teamAssignmentApi';
import { useLocale } from '../../utils/LocaleContext';

const TeamAssignmentSection = ({ projectId, task, onAssigned }) => {
  const { t } = useLocale();
  const [crews, setCrews] = useState([]);
  const [history, setHistory] = useState([]);
  const [selectedCrewId, setSelectedCrewId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [overlaps, setOverlaps] = useState([]);

  useEffect(() => {
    let active = true;

    const loadAssignmentData = async () => {
      if (!projectId || !task?.id || task.type !== 'task') return;
      setLoading(true);
      setError('');
      try {
        const [crewResponse, historyResponse] = await Promise.all([
          getProjectCrews(projectId),
          getTaskCrewAssignmentHistory(projectId, task.id)
        ]);
        if (!active) return;
        setCrews(crewResponse?.data || []);
        setHistory(historyResponse?.data || []);
        setSelectedCrewId(task.assignedTeamMemberId || null);
      } catch (loadError) {
        if (active) setError(loadError.message || t('Không thể tải đội thi công'));
      } finally {
        if (active) setLoading(false);
      }
    };

    loadAssignmentData();
    return () => {
      active = false;
    };
  }, [projectId, task?.id, task?.type, task?.assignedTeamMemberId, t]);

  if (task?.type !== 'task') return null;

  const handleAssign = async () => {
    setSaving(true);
    setError('');
    setOverlaps([]);
    try {
      const response = await assignTaskCrew(projectId, task.id, selectedCrewId);
      const assignment = response?.data;
      const foundOverlaps = assignment?.overlaps || [];
      setOverlaps(foundOverlaps);
      if (foundOverlaps.length) {
        message.warning(t('Đã lưu phân công nhưng đội đang có công việc chồng lịch.'));
      } else {
        message.success(t('Đã cập nhật đội thi công được giao.'));
      }
      const historyResponse = await getTaskCrewAssignmentHistory(projectId, task.id);
      setHistory(historyResponse?.data || []);
      onAssigned?.();
    } catch (saveError) {
      setError(saveError.message || t('Không thể cập nhật đội thi công'));
    } finally {
      setSaving(false);
    }
  };

  const options = crews.map(crew => ({
    value: crew.userId,
    label: crew.fullName || crew.email
  }));
  const selectionChanged = (selectedCrewId || null) !== (task.assignedTeamMemberId || null);

  return (
    <div className="drawer-section team-assignment-section">
      <div className="drawer-section-title">
        <TeamOutlined style={{ marginRight: 6, color: '#2563EB' }} />
        {t('ĐỘI THI CÔNG ĐƯỢC GIAO')}
      </div>

      {error && (
        <Alert type="error" showIcon message={error} style={{ marginBottom: 12 }} />
      )}

      <Spin spinning={loading}>
        <div className="team-assignment-controls">
          <Select
            allowClear
            showSearch
            value={selectedCrewId}
            options={options}
            optionFilterProp="label"
            placeholder={t('Chọn đội thi công')}
            onChange={value => setSelectedCrewId(value || null)}
            notFoundContent={t('Chưa có thành viên dự án với vai trò Đội thi công')}
          />
          <Button
            type="primary"
            loading={saving}
            disabled={loading || saving || !selectionChanged}
            onClick={handleAssign}
          >
            {task.assignedTeamMemberId ? t('Đổi đội') : t('Giao đội')}
          </Button>
        </div>

        {!crews.length && !loading && !error && (
          <Alert
            type="info"
            showIcon
            message={t('Thêm thành viên vào dự án với vai trò WORKER để tạo danh sách đội thi công.')}
            style={{ marginTop: 12 }}
          />
        )}

        {!!overlaps.length && (
          <Alert
            type="warning"
            showIcon
            message={t('Cảnh báo chồng lịch')}
            description={(
              <ul className="team-overlap-list">
                {overlaps.map(overlap => (
                  <li key={overlap.taskId}>
                    <strong>{overlap.wbsCode}</strong> — {overlap.taskName}
                    {' '}({dayjs(overlap.startDate).format('DD/MM/YYYY')}
                    {' – '}{dayjs(overlap.endDate).format('DD/MM/YYYY')})
                  </li>
                ))}
              </ul>
            )}
            style={{ marginTop: 12 }}
          />
        )}

        <div className="team-assignment-history">
          <strong>{t('Lịch sử phân công')}</strong>
          {history.length ? (
            <Timeline
              items={history.map(entry => ({
                children: (
                  <div key={entry.id}>
                    <div>
                      {entry.fromTeamName || t('Chưa giao')}
                      {' → '}
                      <Tag color={entry.toTeamName ? 'blue' : 'default'}>
                        {entry.toTeamName || t('Đã gỡ phân công')}
                      </Tag>
                    </div>
                    <small>
                      {entry.changedByName || t('Tài khoản')}
                      {' · '}
                      {dayjs(entry.changedAt).format('DD/MM/YYYY HH:mm')}
                    </small>
                  </div>
                )
              }))}
            />
          ) : (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={t('Chưa có lịch sử phân công')}
            />
          )}
        </div>
      </Spin>
    </div>
  );
};

export default TeamAssignmentSection;
