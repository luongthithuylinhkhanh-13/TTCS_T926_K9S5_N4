import apiClient from './apiClient';

const imageUploadApi = {
  async uploadImage(projectId, file) {
    if (!projectId) {
      throw new Error('Không tìm thấy mã dự án.');
    }

    if (!file) {
      throw new Error('Vui lòng chọn ảnh.');
    }

    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post(
      `/api/projects/${projectId}/images`,
      formData,
      {
        headers: {
          'Content-Type': undefined
        }
      }
    );

    const result = response.data;

    if (!result?.success || !result?.message) {
      throw new Error(result?.message || 'Tải ảnh lên thất bại.');
    }

    return result.message;
  }
};

export default imageUploadApi;