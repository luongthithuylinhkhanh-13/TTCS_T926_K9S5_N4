import React from 'react';
import './AcceptanceDossierDocument.css';

const formatDate = value => {
  if (!value) return '……/……/…………';
  const date = new Date(`${value}T00:00:00`);
  return new Intl.DateTimeFormat('vi-VN').format(date);
};

const formatNumber = value => Number(value || 0).toLocaleString('vi-VN');
const textOrBlank = value => value || '\u00a0';

const AcceptanceDossierDocument = ({ dossier }) => (
  <article className="acceptance-dossier-document">
    <header className="acceptance-dossier-heading">
      <div className="acceptance-dossier-agency">
        <strong>ĐƠN VỊ CHỦ QUẢN</strong>
        <strong>{textOrBlank(dossier.projectName)}</strong>
        <span className="acceptance-dossier-rule" />
        <p>Số: {textOrBlank(dossier.reportNumber)}</p>
      </div>
      <div className="acceptance-dossier-national">
        <strong>CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</strong>
        <strong>Độc lập - Tự do - Hạnh phúc</strong>
        <span className="acceptance-dossier-rule" />
        <p>
          {dossier.projectCode ? `Công trình: ${dossier.projectCode}` : '\u00a0'}
        </p>
      </div>
    </header>

    <h1>BIÊN BẢN NGHIỆM THU CÔNG VIỆC XÂY DỰNG</h1>
    <p className="acceptance-dossier-subtitle">
      {textOrBlank(dossier.reportName)}
      {dossier.periodNumber ? ` - Đợt ${dossier.periodNumber}` : ''}
    </p>

    <section className="acceptance-dossier-intro">
      <p><strong>1. Công trình:</strong> {textOrBlank(dossier.projectName)}</p>
      <p><strong>2. Hạng mục / vị trí thi công:</strong> {textOrBlank(dossier.location)}</p>
      <p><strong>3. Thời gian nghiệm thu:</strong> {formatDate(dossier.acceptanceDate)}</p>
      <p><strong>4. Kết quả:</strong> <strong>{textOrBlank(dossier.result)}</strong></p>
      {dossier.approvalRecordTitle && (
        <p><strong>5. Phiếu duyệt:</strong> {dossier.approvalRecordTitle}</p>
      )}
    </section>

    <section>
      <h2>I. THÀNH PHẦN THAM GIA NGHIỆM THU</h2>
      <table className="acceptance-dossier-table acceptance-dossier-participants">
        <thead>
          <tr>
            <th>Đại diện</th>
            <th>Họ và tên</th>
            <th>Ký tên</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Chủ đầu tư</td>
            <td>{textOrBlank(dossier.ownerRepresentativeName)}</td>
            <td />
          </tr>
          <tr>
            <td>Tư vấn giám sát</td>
            <td>{textOrBlank(dossier.supervisorRepresentativeName)}</td>
            <td />
          </tr>
          <tr>
            <td>Nhà thầu thi công</td>
            <td>{textOrBlank(dossier.contractorRepresentativeName)}</td>
            <td />
          </tr>
        </tbody>
      </table>
    </section>

    <section>
      <h2>II. NỘI DUNG VÀ KẾT QUẢ NGHIỆM THU</h2>
      <p>
        Các bên cùng kiểm tra, đối chiếu khối lượng và chất lượng công việc xây dựng
        tại công trình nêu trên. Kết quả: <strong>{textOrBlank(dossier.result)}</strong>.
      </p>
      <table className="acceptance-dossier-table acceptance-dossier-items">
        <thead>
          <tr>
            <th>STT</th>
            <th>Tên công việc / vị trí</th>
            <th>ĐVT</th>
            <th>KL nghiệm thu</th>
            <th>KL lũy kế</th>
            <th>Ghi chú</th>
          </tr>
        </thead>
        <tbody>
          {(dossier.items || []).map((item, index) => (
            <tr key={item.id || `${item.itemName}-${index}`}>
              <td>{index + 1}</td>
              <td>
                <strong>{item.itemName}</strong>
                {item.location && <small>{item.location}</small>}
              </td>
              <td>{textOrBlank(item.unit)}</td>
              <td>{formatNumber(item.acceptedQuantity)}</td>
              <td>{formatNumber(item.cumulativeQuantity)}</td>
              <td>{textOrBlank(item.notes)}</td>
            </tr>
          ))}
          {(!dossier.items || dossier.items.length === 0) && (
            <tr><td colSpan="6">Chưa có hạng mục chi tiết.</td></tr>
          )}
        </tbody>
      </table>
      {dossier.notes && <p><strong>Nhận xét / ghi chú:</strong> {dossier.notes}</p>}
      {dossier.approvalRecordDescription && (
        <p><strong>Nội dung phiếu S-39:</strong> {dossier.approvalRecordDescription}</p>
      )}
      {dossier.approvalComment && (
        <p><strong>Ý kiến phê duyệt:</strong> {dossier.approvalComment}</p>
      )}
    </section>

    <p className="acceptance-dossier-conclusion">
      Các bên thống nhất nghiệm thu công việc xây dựng theo nội dung và kết quả nêu trên.
      Biên bản được lập thành các bản có giá trị như nhau, mỗi bên giữ một bản.
    </p>

    <footer className="acceptance-dossier-signatures">
      <div>
        <strong>ĐẠI DIỆN CHỦ ĐẦU TƯ</strong>
        <span>(Ký, ghi rõ họ tên)</span>
        <strong>{textOrBlank(dossier.ownerRepresentativeName)}</strong>
      </div>
      <div>
        <strong>ĐẠI DIỆN TƯ VẤN GIÁM SÁT</strong>
        <span>(Ký, ghi rõ họ tên)</span>
        <strong>{textOrBlank(dossier.supervisorRepresentativeName)}</strong>
      </div>
      <div>
        <strong>ĐẠI DIỆN NHÀ THẦU</strong>
        <span>(Ký, ghi rõ họ tên)</span>
        <strong>{textOrBlank(dossier.contractorRepresentativeName)}</strong>
      </div>
    </footer>
    <p className="acceptance-dossier-generated">
      Người lập phiếu trên hệ thống: {textOrBlank(dossier.creatorName)}
      {dossier.approverName && ` · Người duyệt: ${dossier.approverName}`}
      {dossier.approvedAt && ` · ${new Intl.DateTimeFormat('vi-VN').format(new Date(dossier.approvedAt))}`}
    </p>
  </article>
);

export default AcceptanceDossierDocument;
