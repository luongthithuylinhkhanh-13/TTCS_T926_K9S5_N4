import apiClient from './apiClient.js';

/**
 * Service API cho nhật ký công trường và upload ảnh hai giai đoạn (S-23 T-62).
 */

/**
 * Tạo mới một bản ghi nhật ký.
 */
export async function createDiaryEntry(projectId, entryData) {
  const response = await apiClient.post(`/api/projects/${projectId}/diary-entries`, entryData);
  return response.data?.data || response.data;
}

/**
 * Upload ảnh hiển thị (giai đoạn 1: ảnh đã nén < 1 MiB).
 * @param {string} projectId
 * @param {string} entryId
 * @param {string} clientImageId
 * @param {string} filename
 * @param {Blob|File} displayBlob
 */
export async function uploadDisplayImage(projectId, entryId, clientImageId, filename, displayBlob) {
  const formData = new FormData();
  formData.append('clientImageId', clientImageId);
  formData.append('filename', filename);
  formData.append('file', displayBlob, filename);

  const response = await apiClient.post(
    `/api/projects/${projectId}/diary-entries/${entryId}/images/display`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data?.data || response.data;
}

/**
 * Upload ảnh gốc (giai đoạn 2: giữ nguyên bytes, tối đa 20 MiB).
 * @param {string} projectId
 * @param {string} entryId
 * @param {string} clientImageId
 * @param {string} filename
 * @param {File} originalFile
 */
export async function uploadOriginalImage(projectId, entryId, clientImageId, filename, originalFile) {
  const formData = new FormData();
  formData.append('clientImageId', clientImageId);
  formData.append('filename', filename);
  formData.append('file', originalFile, filename);

  const response = await apiClient.post(
    `/api/projects/${projectId}/diary-entries/${entryId}/images/original`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response.data?.data || response.data;
}
