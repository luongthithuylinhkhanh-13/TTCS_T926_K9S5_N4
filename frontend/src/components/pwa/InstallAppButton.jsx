import React, { useEffect, useState } from 'react';
import { Button, message } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';

const isRunningAsInstalledApp = () => (
  window.matchMedia('(display-mode: standalone)').matches
  || navigator.standalone === true
);

const InstallAppButton = ({ className = '', block = false }) => {
  const [installPrompt, setInstallPrompt] = useState(null);
  const [installed, setInstalled] = useState(isRunningAsInstalledApp);

  useEffect(() => {
    const handleInstallPrompt = (event) => {
      event.preventDefault();
      setInstallPrompt(event);
    };
    const handleInstalled = () => {
      setInstalled(true);
      setInstallPrompt(null);
      message.success('Ứng dụng đã được cài đặt.');
    };

    window.addEventListener('beforeinstallprompt', handleInstallPrompt);
    window.addEventListener('appinstalled', handleInstalled);

    return () => {
      window.removeEventListener('beforeinstallprompt', handleInstallPrompt);
      window.removeEventListener('appinstalled', handleInstalled);
    };
  }, []);

  const handleInstall = async () => {
    if (!installPrompt) {
      message.info('Mở menu trình duyệt rồi chọn “Cài đặt ứng dụng” hoặc “Thêm vào màn hình chính”.');
      return;
    }

    try {
      await installPrompt.prompt();
      const { outcome } = await installPrompt.userChoice;
      if (outcome === 'accepted') {
        message.success('Đang cài đặt ứng dụng.');
      }
      setInstallPrompt(null);
    } catch (error) {
      console.error('Không thể mở hộp thoại cài đặt ứng dụng:', error);
      message.error('Không thể mở hộp thoại cài đặt. Vui lòng thử lại từ menu trình duyệt.');
    }
  };

  if (installed) {
    return null;
  }

  return (
    <Button
      className={`pwa-install-button ${className}`.trim()}
      icon={<DownloadOutlined />}
      onClick={handleInstall}
      block={block}
    >
      Cài ứng dụng
    </Button>
  );
};

export default InstallAppButton;
