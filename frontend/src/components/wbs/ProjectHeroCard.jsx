import React from 'react';
import { Card, Tag, Progress, Avatar, Image } from 'antd';
import dayjs from 'dayjs';
import { UserOutlined } from '@ant-design/icons';

/*
 * Hiển thị trạng thái dự án theo dữ liệu backend.
 */
const PROJECT_STATUS = {
  not_started: {
    label: 'Chưa bắt đầu',
    color: 'default'
  },
  in_progress: {
    label: 'Đang thi công',
    color: 'processing'
  },
  completed: {
    label: 'Hoàn thành',
    color: 'success'
  },
  paused: {
    label: 'Tạm dừng',
    color: 'warning'
  }
};

/*
 * Format ngày an toàn.
 * Nếu backend chưa có ngày thì không hiển thị "Invalid Date".
 */
const formatDate = (date) => {
  if (!date) {
    return null;
  }

  const parsedDate = dayjs(date);

  if (!parsedDate.isValid()) {
    return null;
  }

  return parsedDate.format('DD/MM/YYYY');
};

const ProjectHeroCard = ({ project }) => {
  if (!project) {
    return null;
  }

  const status =
    PROJECT_STATUS[project.status] || {
      label: project.status || 'Chưa xác định',
      color: 'default'
    };

  const startDate = formatDate(project.startDate);
  const endDate = formatDate(project.endDate);

  let projectTime = 'Chưa cập nhật';

  if (startDate && endDate) {
    projectTime = `${startDate} - ${endDate}`;
  } else if (startDate) {
    projectTime = `Từ ${startDate}`;
  } else if (endDate) {
    projectTime = `Đến ${endDate}`;
  }

  const progress = Number(project.progress ?? 0);

  return (
    <Card
      className="project-hero-card"
      bordered={false}
    >
      <div className="project-hero-content">

        {/* LEFT */}
        <div className="project-hero-left">
          <Image
            className="project-hero-image"
            src={
              project.image ||
              '/images/construction/project.png'
            }
            preview={false}
            fallback="/images/construction/project.png"
          />

          <div className="project-hero-info">
            <div>
              <Tag color="processing">
                {project.code}
              </Tag>
            </div>

            <h2>
              {project.name}
            </h2>

            <div className="project-hero-meta">
              <span>
                Trạng thái:
              </span>

              <Tag color={status.color}>
                {status.label}
              </Tag>
            </div>

            <div className="project-hero-meta">
              <span>
                Thời gian:
              </span>

              <strong>
                {projectTime}
              </strong>
            </div>
          </div>
        </div>

        {/* RIGHT */}
        <div className="project-hero-right">
          <Avatar
            size={48}
            icon={<UserOutlined />}
          />

          <div className="project-hero-progress">
            <span>
              Tiến độ
            </span>

            <Progress
              percent={progress}
              size="small"
              status={
                progress === 100
                  ? 'success'
                  : 'active'
              }
            />
          </div>
        </div>

      </div>
    </Card>
  );
};

export default ProjectHeroCard;