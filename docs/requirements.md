# Đặc tả yêu cầu hệ thống

## Mục tiêu và phạm vi

Hệ thống hỗ trợ quản lý đề tài học thuật theo từng đợt, từ đề xuất đến công bố
điểm. Tác nhân gồm Trưởng khoa, Trưởng bộ môn, GV, GVHD, GVPB, SV và Hội đồng.
Thông báo kết quả là thông báo nội bộ trên ứng dụng (chuông/hộp thư), không yêu
cầu gửi email ra ngoài.

## Yêu cầu chức năng (FR)

| ID | Yêu cầu có thể kiểm thử |
|---|---|
| FR-001 | Hệ thống cho phép người dùng đăng nhập bằng tài khoản được cấp và đăng xuất để hủy phiên. |
| FR-002 | Hệ thống lưu mật khẩu dưới dạng hash BCrypt; đăng nhập sai không tạo phiên xác thực. |
| FR-003 | Trưởng khoa có thể tạo, xem, sửa và xóa tài khoản; không thể xóa tài khoản Trưởng khoa cuối cùng. |
| FR-004 | Trưởng khoa có thể CRUD hồ sơ giảng viên, sinh viên và gán nhiều vai trò được cấp. |
| FR-005 | Trưởng khoa có thể CRUD bộ môn; mỗi bộ môn có mã duy nhất và trạng thái hoạt động. |
| FR-006 | Trưởng khoa có thể CRUD đợt đăng ký với loại đợt, hai khoảng thời gian, hạn phản biện và ngày bảo vệ hợp lệ. |
| FR-007 | GV/GVHD có thể CRUD đề xuất đề tài trong giai đoạn tiếp nhận đề tài; đề tài thuộc một bộ môn và một đợt. |
| FR-008 | Trưởng khoa/Trưởng bộ môn có thể duyệt hoặc từ chối đề tài trước khi mở đăng ký SV; chỉ đề tài đã duyệt mới được công bố. |
| FR-009 | SV chỉ nhìn thấy và đăng ký đề tài đã công bố trong giai đoạn đăng ký SV đang mở. |
| FR-010 | Người dùng có thể tìm đề tài theo từ khóa, bộ môn, loại/trạng thái đề tài, đợt và GVHD; nhiều bộ lọc được kết hợp theo AND. |
| FR-011 | Hệ thống chỉ chấp nhận nhóm từ 3 đến 5 SV, có đúng một nhóm trưởng là thành viên; mã thành viên không trùng. |
| FR-012 | Một SV không được tham gia hai nhóm trong cùng một đợt; cạnh tranh đăng ký đồng thời không được tạo nhóm trùng. |
| FR-013 | Chỉ nhóm đã duyệt được nộp báo cáo; chỉ nhóm trưởng được nộp và một nhóm chỉ được nộp một lần. |
| FR-014 | Trưởng khoa phân công GVPB và hội đồng; mỗi hội đồng có từ 3 đến 5 giảng viên khác nhau. |
| FR-015 | GVPB và thành viên hội đồng được phân công có thể nhập điểm trong khoảng 0–10; GVHD bị từ chối nếu tự chấm đề tài mình hướng dẫn. |
| FR-016 | Điểm hội đồng bằng trung bình cộng các điểm hợp lệ của thành viên hội đồng; điểm tổng kết bằng trung bình điểm phản biện và điểm hội đồng. |
| FR-017 | Trưởng khoa công bố kết quả; hệ thống tạo một thông báo nội bộ riêng cho từng SV trong nhóm. |
| FR-018 | Người dùng xem danh sách thông báo theo quyền nhận, đánh dấu đã đọc; Trưởng khoa có thể CRUD thông báo chung. |
| FR-019 | SV chỉ tra cứu điểm của nhóm mà mình là thành viên và của đúng đợt đăng ký tương ứng; không được đọc nhóm/điểm đợt khác. |
| FR-020 | Người có quyền có thể xuất danh sách đề tài/nhóm/kết quả theo bộ lọc sang Excel hoặc PDF. |
| FR-021 | Hệ thống hiển thị lỗi nghiệp vụ dễ hiểu và rollback thao tác thất bại để không lưu dữ liệu một phần. |
| FR-022 | Hệ thống ghi nhận dấu thời gian tạo/cập nhật cho dữ liệu nghiệp vụ quan trọng. |

## Yêu cầu phi chức năng (NFR)

| ID | Yêu cầu |
|---|---|
| NFR-001 | Phân quyền được thực thi ở server; ẩn nút/giao diện không được xem là biện pháp bảo vệ. |
| NFR-002 | Tất cả mật khẩu lưu trữ phải là BCrypt hash; thông tin DB/mail chỉ được cấu hình bằng biến môi trường hoặc secret store. |
| NFR-003 | Dữ liệu giao dịch đăng ký/chấm điểm phải được lưu bền vững trong PostgreSQL và có ràng buộc toàn vẹn FK/unique/check. |
| NFR-004 | Mọi thay đổi gồm nhóm, thành viên, điểm hoặc thông báo phải nguyên tử; lỗi ghi dữ liệu phải báo lỗi, không trả thành công giả. |
| NFR-005 | Giao diện tiếng Việt dễ hiểu, có trạng thái rỗng, trạng thái lỗi và xác nhận hành động xóa/công bố. |
| NFR-006 | Các luồng chính có kiểm thử tự động: phân quyền, chuyển giai đoạn, giới hạn nhóm, chống trùng thành viên, điểm, thông báo và tra cứu theo đợt. |
| NFR-007 | Bản deploy phải có health check, cấu hình cổng theo môi trường và kiểm tra kết nối DB khi khởi động. |
| NFR-008 | Bản phát hành phải chạy trên một web host miễn phí kết nối PostgreSQL miễn phí, không yêu cầu lưu file dữ liệu cục bộ để bảo toàn dữ liệu. |

## Quy tắc nghiệp vụ

1. Giai đoạn GV tạo/đề xuất đề tài và giai đoạn SV đăng ký là hai giai đoạn độc
   lập; thời gian không hợp lệ hoặc giai đoạn chưa mở thì từ chối thao tác.
2. Trạng thái đề tài đi theo luồng `DRAFT -> APPROVED -> PUBLISHED -> ASSIGNED
   -> COMPLETED`; từ chối duyệt là trạng thái riêng và không thể đăng ký.
3. Một nhóm gắn với đúng một đề tài trong một đợt; một đề tài chỉ nhận một nhóm
   nếu chính sách đợt quy định vậy.
4. Một SV chỉ được tham gia một nhóm trên mỗi đợt; trùng thành viên được kiểm
   tra ở tầng nghiệp vụ và bằng ràng buộc cơ sở dữ liệu.
5. Hội đồng có ít nhất 3 và nhiều nhất 5 thành viên. Điểm thiếu/ngoài [0,10],
   thành viên không được phân công hoặc GVHD tự chấm đều bị từ chối.
6. Công bố kết quả chỉ xảy ra khi đủ dữ liệu đánh giá; thao tác tạo thông báo
   tới từng thành viên nằm trong cùng transaction với cập nhật trạng thái công bố.
7. SV chỉ được truy vấn điểm nếu tài khoản của SV có bản ghi thành viên thuộc
   nhóm/đợt được yêu cầu.

## Kịch bản nghiệm thu

- Tạo đợt hợp lệ, thêm đề tài, chứng minh SV chưa thấy đề tài trước khi duyệt,
  sau đó duyệt/công bố và xác nhận đề tài chỉ xuất hiện trong cửa sổ đăng ký.
- Tạo nhóm 2 người, 6 người, lặp MSSV, thiếu nhóm trưởng và nhóm trưởng không
  thuộc nhóm: từng trường hợp phải bị từ chối. Nhóm 3–5 người hợp lệ được lưu.
- Đăng ký hai nhóm cho cùng MSSV trong một đợt, kể cả request đồng thời: chỉ
  một giao dịch được chấp nhận.
- Thử nộp báo cáo bằng thành viên thường và nộp lặp: bị từ chối; nhóm trưởng
  nộp một lần thành công.
- Tạo hội đồng 2 người và 6 người: bị từ chối; 3–5 người được chấp nhận.
  Chấm thử bằng GVHD chính: bị từ chối. Hội đồng nhập các điểm kiểm thử để
  xác nhận trung bình cộng chính xác.
- Công bố điểm: từng SV trong nhóm thấy thông báo chuông; SV ngoài nhóm và SV
  thuộc đợt khác không thể truy cập kết quả.

## Tiêu chí thành công

- 100% endpoint nghiệp vụ từ chối vai trò không hợp lệ, kiểm tra tại server.
- Không có mật khẩu plain-text trong bản ghi tài khoản.
- Không có trường hợp một SV thuộc hai nhóm trong cùng một đợt sau khi hoàn tất
  các request đồng thời.
- 100% kiểm thử biên về số thành viên, nhóm trưởng, hội đồng và điểm đạt đúng
  kết quả mong đợi.
- SV chỉ thấy điểm các đợt mình có đăng ký; người ngoài nhóm không thể lấy
  điểm bằng cách sửa URL/tham số.
- Mọi thông báo kết quả được tạo cho đủ từng thành viên trong cùng transaction
  với việc công bố.
- Người vận hành có thể dựng ứng dụng từ cấu hình hosting và secret môi trường
  mà không sửa mã nguồn.

## Giả định

- “Gửi email” được thay bằng thông báo trong ứng dụng qua chuông/hộp thông báo,
  theo yêu cầu làm rõ của người dùng.
- Đăng ký nhóm được khóa theo từng đợt; cùng SV có thể tham gia một nhóm khác ở
  một đợt khác.
- Trưởng khoa có thể giao nhiều vai trò cho cùng một tài khoản; không tự cấp
  quyền Trưởng khoa qua form tài khoản thông thường.

## Tình trạng hiện thực trong mã nguồn

Các FR/NFR phía trên là tiêu chí mục tiêu, không phải xác nhận toàn bộ chức năng
đã hoàn thành. Mã hiện tại có Spring Security với BCrypt, đăng nhập/đăng xuất,
kiểm tra một số quyền ở server, thông báo chuông, lọc đề tài, CRUD bộ môn và
xuất danh sách đề tài Excel. Dữ liệu demo được lưu ở tệp JSON.

Các hạng mục còn thiếu trước khi nghiệm thu/deploy thật:

- Thay JSON bằng Spring Data JPA và PostgreSQL/Neon; tạo migration, cấu hình
  database production và chuyển dữ liệu hiện hữu. ERD trong `erd.md` mới là mô
  hình đích, chưa phải schema đang chạy.
- Hoàn thiện CRUD hồ sơ/tài khoản, đợt đăng ký và thông báo theo đủ thao tác,
  cùng với phân quyền theo phạm vi bộ môn.
- Lưu phân công và phiếu điểm riêng cho từng phản biện/thành viên hội đồng;
  kiểm tra hội đồng 3–5 người, tính trung bình từ phiếu và chặn GVHD tự chấm.
- Thực thi nhất quán các giai đoạn đăng ký, khóa thành viên đồng thời, nộp báo
  cáo duy nhất bởi trưởng nhóm, và endpoint tra cứu điểm có kiểm tra đợt ở
  phía máy chủ.
- Thêm kiểm thử cho các nghiệp vụ còn thiếu, health check thực tế và cấu hình
  hosting bền vững. `render.yaml` hiện chưa đủ để xác nhận ứng dụng đã deploy
  thành công hoặc dữ liệu được lưu bền vững.
