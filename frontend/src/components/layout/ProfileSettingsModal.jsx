import React, { useEffect, useState } from 'react';
import { Button, Form, Input, message, Modal, Radio, Select, Spin } from 'antd';
import { getCurrentProfile, updateCurrentProfile } from '../../services/wbsApi';
import { updateAuthUser } from '../../utils/auth';
import { saveUiPreferences } from '../../utils/uiPreferences';

const COPY = {
  vi: {
    profile: 'Thông tin cá nhân',
    settings: 'Cài đặt hệ thống',
    name: 'Họ và tên',
    email: 'Email',
    currentPassword: 'Mật khẩu hiện tại',
    newPassword: 'Mật khẩu mới (để trống nếu không đổi)',
    requiredName: 'Vui lòng nhập họ và tên',
    requiredEmail: 'Vui lòng nhập email',
    validEmail: 'Email không đúng định dạng',
    requiredPassword: 'Vui lòng nhập mật khẩu hiện tại để xác nhận thay đổi',
    save: 'Lưu thay đổi',
    cancel: 'Hủy',
    saved: 'Đã cập nhật thông tin cá nhân',
    theme: 'Giao diện',
    light: 'Sáng',
    dark: 'Tối',
    language: 'Ngôn ngữ',
    vietnamese: 'Tiếng Việt',
    english: 'English',
    preferencesSaved: 'Đã lưu cài đặt giao diện',
  },
  en: {
    profile: 'Personal information',
    settings: 'System settings',
    name: 'Full name',
    email: 'Email',
    currentPassword: 'Current password',
    newPassword: 'New password (leave blank to keep current)',
    requiredName: 'Enter your full name',
    requiredEmail: 'Enter your email',
    validEmail: 'Enter a valid email address',
    requiredPassword: 'Enter your current password to confirm changes',
    save: 'Save changes',
    cancel: 'Cancel',
    saved: 'Personal information updated',
    theme: 'Appearance',
    light: 'Light',
    dark: 'Dark',
    language: 'Language',
    vietnamese: 'Vietnamese',
    english: 'English',
    preferencesSaved: 'Appearance settings saved',
  },
};

const ProfileSettingsModal = ({
  open,
  mode,
  preferences,
  onPreferencesChange,
  onClose,
  onProfileUpdated,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [draftPreferences, setDraftPreferences] = useState(preferences);
  const text = COPY[preferences.language] || COPY.vi;

  useEffect(() => {
    if (!open || mode !== 'profile') return undefined;
    let active = true;
    setLoading(true);
    getCurrentProfile()
      .then(profile => {
        if (!active) return;
        form.setFieldsValue({
          fullName: profile.fullName || '',
          email: profile.email || '',
          currentPassword: '',
          newPassword: '',
        });
        onProfileUpdated(profile);
      })
      .catch(error => {
        if (active) message.error(error.message || 'Không thể tải thông tin cá nhân');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [open, mode, form, onProfileUpdated]);

  useEffect(() => {
    if (open) setDraftPreferences(preferences);
  }, [open, preferences]);

  const handleProfileSubmit = async values => {
    setSaving(true);
    try {
      const profile = await updateCurrentProfile(values);
      updateAuthUser({
        email: profile.email,
        fullName: profile.fullName,
      });
      onProfileUpdated(profile);
      message.success(text.saved);
      form.resetFields();
      onClose();
    } catch (error) {
      message.error(error.message || 'Không thể cập nhật thông tin cá nhân');
    } finally {
      setSaving(false);
    }
  };

  const handlePreferencesSave = () => {
    const saved = saveUiPreferences(draftPreferences);
    onPreferencesChange(saved);
    message.success(text.preferencesSaved);
    onClose();
  };

  const isProfile = mode === 'profile';

  return (
    <Modal
      title={isProfile ? text.profile : text.settings}
      open={open}
      onCancel={onClose}
      footer={null}
      destroyOnClose
    >
      {isProfile ? (
        <Spin spinning={loading}>
          <Form
            form={form}
            layout="vertical"
            onFinish={handleProfileSubmit}
            requiredMark={false}
          >
            <Form.Item
              label={text.name}
              name="fullName"
              rules={[{ required: true, whitespace: true, message: text.requiredName }]}
            >
              <Input autoComplete="name" maxLength={255} />
            </Form.Item>
            <Form.Item
              label={text.email}
              name="email"
              rules={[
                { required: true, message: text.requiredEmail },
                { type: 'email', message: text.validEmail },
              ]}
            >
              <Input autoComplete="email" maxLength={255} />
            </Form.Item>
            <Form.Item
              label={text.currentPassword}
              name="currentPassword"
              rules={[{ required: true, message: text.requiredPassword }]}
            >
              <Input.Password autoComplete="current-password" />
            </Form.Item>
            <Form.Item label={text.newPassword} name="newPassword">
              <Input.Password autoComplete="new-password" />
            </Form.Item>
            <div className="account-modal-actions">
              <Button onClick={onClose}>{text.cancel}</Button>
              <Button type="primary" htmlType="submit" loading={saving}>
                {text.save}
              </Button>
            </div>
          </Form>
        </Spin>
      ) : (
        <div className="account-settings-form">
          <label>{text.theme}</label>
          <Radio.Group
            value={draftPreferences.theme}
            onChange={event => setDraftPreferences(current => ({
              ...current,
              theme: event.target.value,
            }))}
            optionType="button"
            buttonStyle="solid"
            options={[
              { label: text.light, value: 'light' },
              { label: text.dark, value: 'dark' },
            ]}
          />
          <label>{text.language}</label>
          <Select
            value={draftPreferences.language}
            onChange={language => setDraftPreferences(current => ({ ...current, language }))}
            options={[
              { label: text.vietnamese, value: 'vi' },
              { label: text.english, value: 'en' },
            ]}
          />
          <div className="account-modal-actions">
            <Button onClick={onClose}>{text.cancel}</Button>
            <Button type="primary" onClick={handlePreferencesSave}>{text.save}</Button>
          </div>
        </div>
      )}
    </Modal>
  );
};

export default ProfileSettingsModal;
