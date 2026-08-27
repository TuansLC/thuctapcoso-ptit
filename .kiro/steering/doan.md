---
inclusion: always
---

# Ngữ cảnh đồ án

Đặc tả chốt của đồ án nằm ở file dưới đây. Đọc và tuân thủ nó trong mọi tác vụ
liên quan tới project này, đặc biệt là mục 14 về ranh giới phạm vi.

#[[file:../../DOAN.md]]

## Nhắc nhanh

- Stack: Java 21, Spring Boot 3.5.16, Spring Data JPA, Spring Security 6,
  Thymeleaf, Flyway, MySQL 8, Bootstrap 5, Maven.
- Đúng 5 bảng, đúng 6 chức năng. Không tự thêm bảng hay chức năng ngoài đặc tả.
- Không thanh toán, không vai giảng viên, không WebSocket, không REST API + SPA.
- Trọng tâm kỹ thuật là `RegistrationService.register()` với pessimistic lock
  và 5 tầng validate. Đây là phần phải làm kỹ nhất.
- Dùng Flyway migration, không dùng `ddl-auto=update`.
- Nghiệp vụ ở service, controller chỉ điều phối.
- Trả lời bằng tiếng Việt.
