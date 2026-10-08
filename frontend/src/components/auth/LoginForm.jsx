import React, { useState } from 'react';
import { Form, Input, Button, Checkbox, Alert } from 'antd';
import { MailOutlined, LockOutlined, LoginOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { setAuthUser } from '../../utils/auth';

const LoginForm = () => {
  const [mode, setMode] = useState('login');
  const [loading, setLoading] = useState(false);
  const [loginError, setLoginError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const navigate = useNavigate();
  const [form] = Form.useForm();

  const handleFinish = async (values) => {
    setLoginError(null);
    setSuccessMessage(null);
    setLoading(true);

    try {
      const registering = mode === 'register';
      const response = await fetch(`/api/auth/${registering ? 'register' : 'login'}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(registering
          ? {
              fullName: values.fullName.trim(),
              email: values.email.trim(),
              password: values.password,
              confirmPassword: values.confirmPassword,
            }
          : {
              email: values.email.trim(),
              password: values.password,
            }),
      });
      const data = await response.json();

      if (!response.ok || !data.success) {
        setLoginError(data.message || (registering
          ? 'Đăng ký không thành công.'
          : 'Đăng nhập không thành công.'));
        return;
      }

      if (registering) {
        setSuccessMessage(data.message || 'Đăng ký thành công. Bạn có thể đăng nhập.');
        setMode('login');
        form.resetFields();
        form.setFieldsValue({ email: data.email || values.email });
        return;
      }

      setAuthUser({
        userId: data.userId,
        email: data.email,
        fullName: data.fullName,
        roleId: data.roleId,
        roleName: data.roleName,
        token: data.token,
        expiresAt: data.expiresAt,
      }, values.remember);
      navigate('/wbs');
    } catch (error) {
      console.error('Login error:', error);
      setLoginError('Không thể kết nối đến máy chủ. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  };

  const switchMode = (nextMode) => {
    setMode(nextMode);
    setLoginError(null);
    setSuccessMessage(null);
  };

  return (
    <div className="login-form-container">
      <div className="login-header">
        <div className="login-mobile-logo">
          <span className="login-mobile-logo-text">CONSTRUCTFLOW</span>
        </div>
        <h2 className="login-title">
          {mode === 'login' ? 'Chào mừng trở lại' : 'Tạo tài khoản'}
        </h2>
        <p className="login-subtitle">
          {mode === 'login'
            ? 'Đăng nhập để tiếp tục quản lý công trình'
            : 'Đăng ký để bắt đầu quản lý công trình'}
        </p>
      </div>

      {successMessage && (
        <Alert
          message={successMessage}
          type="success"
          showIcon
          closable
          onClose={() => setSuccessMessage(null)}
          style={{ marginBottom: 20 }}
        />
      )}

      {loginError && (
        <Alert
          message={loginError}
          type="error"
          showIcon
          closable
          onClose={() => setLoginError(null)}
          style={{ marginBottom: 20 }}
        />
      )}

      <Form
        form={form}
        name="login_form"
        layout="vertical"
        onFinish={handleFinish}
        className="login-form"
        requiredMark={false}
      >
        {mode === 'register' && (
          <Form.Item
            label="HỌ VÀ TÊN"
            name="fullName"
            rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập họ và tên' }]}
          >
            <Input
              prefix={<UserOutlined style={{ color: '#94A3B8' }} />}
              placeholder="Nhập họ và tên"
              size="large"
              autoComplete="name"
              disabled={loading}
            />
          </Form.Item>
        )}

        <Form.Item
          label="EMAIL"
          name="email"
          rules={[
            { required: true, message: 'Vui lòng nhập email' },
            { type: 'email', message: 'Email không đúng định dạng' }
          ]}
        >
          <Input
            prefix={<MailOutlined style={{ color: '#94A3B8' }} />}
            placeholder="Nhập email"
            size="large"
            autoComplete="email"
            disabled={loading}
          />
        </Form.Item>

        <Form.Item
          label="MẬT KHẨU"
          name="password"
          rules={[
            { required: true, message: 'Vui lòng nhập mật khẩu' },
            ...(mode === 'register'
              ? [{ min: 8, message: 'Mật khẩu cần ít nhất 8 ký tự' }]
              : [])
          ]}
        >
          <Input.Password
            prefix={<LockOutlined style={{ color: '#94A3B8' }} />}
            placeholder="Nhập mật khẩu"
            size="large"
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            disabled={loading}
          />
        </Form.Item>

        {mode === 'register' ? (
          <Form.Item
            label="XÁC NHẬN MẬT KHẨU"
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: 'Vui lòng xác nhận mật khẩu' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Mật khẩu xác nhận không khớp'));
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: '#94A3B8' }} />}
              placeholder="Nhập lại mật khẩu"
              size="large"
              autoComplete="new-password"
              disabled={loading}
            />
          </Form.Item>
        ) : (
          <div className="login-options">
            <Form.Item name="remember" valuePropName="checked" noStyle initialValue={true}>
              <Checkbox>Ghi nhớ đăng nhập</Checkbox>
            </Form.Item>
            <a href="#forgot" onClick={(event) => event.preventDefault()} className="login-link">
              Quên mật khẩu?
            </a>
          </div>
        )}

        <Form.Item style={{ marginBottom: 0 }}>
          <Button
            type="primary"
            htmlType="submit"
            loading={loading}
            disabled={loading}
            block
            icon={mode === 'login' ? <LoginOutlined /> : <UserOutlined />}
            className="login-btn"
          >
            {mode === 'login' ? 'ĐĂNG NHẬP' : 'TẠO TÀI KHOẢN'}
          </Button>
        </Form.Item>
      </Form>

      <div className="auth-mode-switch">
        <span>{mode === 'login' ? 'Chưa có tài khoản?' : 'Đã có tài khoản?'}</span>
        <Button
          type="link"
          disabled={loading}
          onClick={() => switchMode(mode === 'login' ? 'register' : 'login')}
        >
          {mode === 'login' ? 'Đăng ký' : 'Đăng nhập'}
        </Button>
      </div>
    </div>
  );
};

export default LoginForm;
