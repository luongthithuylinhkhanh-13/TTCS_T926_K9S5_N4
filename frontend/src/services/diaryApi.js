import apiClient from './apiClient.js';
import { getAuthUser } from '../utils/auth.js';

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

/**
 * T-63: Lấy danh sách ảnh thuộc mục nhật ký.
 * @param {string} projectId
 * @param {string} entryId
 */
export async function getDiaryImages(projectId, entryId) {
  const response = await apiClient.get(`/api/projects/${projectId}/diary-entries/${entryId}/images`);
  return response.data?.data || response.data;
}

/**
 * T-63: Sinh URL xem ảnh hiển thị (hoặc thumbnail).
 */
export function getDisplayImageUrl(projectId, entryId, imageId, isThumb = false, token = null) {
  const authToken = token || getAuthUser()?.token;
  const params = [];
  if (isThumb) {
    params.push('thumb=true');
  }
  if (authToken) {
    params.push(`token=${encodeURIComponent(authToken)}`);
  }
  const query = params.length > 0 ? `?${params.join('&')}` : '';
  return `/api/projects/${projectId}/diary-entries/${entryId}/images/${imageId}/display${query}`;
}

/**
 * T-63: Sinh URL tải ảnh gốc HD.
 */
export function getOriginalImageUrl(projectId, entryId, imageId, token = null) {
  const authToken = token || getAuthUser()?.token;
  const query = authToken ? `?token=${encodeURIComponent(authToken)}` : '';
  return `/api/projects/${projectId}/diary-entries/${entryId}/images/${imageId}/original${query}`;
}

/**
 * Xóa ảnh đính kèm khỏi nhật ký thi công.
 * @param {string} projectId
 * @param {string} entryId
 * @param {string} imageId
 */
export async function deleteDiaryImage(projectId, entryId, imageId) {
  const response = await apiClient.delete(`/api/projects/${projectId}/diary-entries/${entryId}/images/${imageId}`);
  return response.data?.data || response.data;
}
