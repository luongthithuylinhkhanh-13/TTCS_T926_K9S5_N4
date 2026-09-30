import '../styles/login.css';
import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { BuildOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { Progress } from 'antd';
import LoginForm from '../components/auth/LoginForm';
import { isAuthenticated } from '../utils/auth';

const LoginPage = () => {
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated()) {
      navigate('/wbs');
    }
  }, [navigate]);

  return (
    <div className="login-container">
      {/* Header Bar */}
      <div className="login-header-bar">
        <div className="logo">
          <BuildOutlined className="hero-logo-icon" />
          <span>CONSTRUCTFLOW</span>
        </div>
        {/* Add navigation links if needed */}
      </div>

      {/* Centered Login Card */}
      <div className="login-panel">
        
        <LoginForm />
      </div>

      {/* Footer */}
      <div className="login-footer">
        © 2026 CONSTRUCTFLOW. Nền tảng điều hành thi công công trình.
      </div>
    </div>
  );
};

export default LoginPage;
