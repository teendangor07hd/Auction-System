# Hướng dẫn đóng góp code — BidHub

## Quy trình làm việc

### 1. Lấy task từ tuần hiện tại

Đọc file `WEEK[X]_TASKS.md` trong repo → tìm task của bạn → tạo branch.

### 2. Tạo branch đúng format

```bash
git checkout develop
git pull origin develop
git checkout -b feature/tuan-[X]-[ten]-[mo-ta-ngan]

# Ví dụ:
# feature/tuan-1-dang-maven-setup
# feature/tuan-3-quocminh-user-dao
# feature/tuan-6-congminh-auction-detail-view
```

### 3. Code và commit thường xuyên

Commit mỗi khi hoàn thành 1 phần nhỏ (1 class, 1 tính năng nhỏ):

```bash
git add [file cụ thể, không dùng git add .]
git commit -m "[type]: [mô tả ngắn gọn tiếng Việt/Anh]"
```

**Các loại commit (Conventional Commits):**

| Type     | Dùng khi                                      | Ví dụ                                       |
|----------|-----------------------------------------------|---------------------------------------------|
| `feat`   | Thêm tính năng mới                            | `feat: thêm class BidValidator`             |
| `fix`    | Sửa bug                                       | `fix: sửa lỗi null pointer trong UserDao`   |
| `test`   | Thêm/sửa test                                 | `test: thêm test cho AuctionStatus enum`    |
| `docs`   | Sửa tài liệu, Javadoc, README                 | `docs: cập nhật API_PROTOCOL.md`            |
| `refactor` | Refactor code không thay đổi behavior       | `refactor: tách BidService thành interface` |
| `build`  | Thay đổi build system (pom.xml, .github)      | `build: thêm dependency jackson`            |
| `chore`  | Việc lặt vặt (gitignore, cấu hình IDE)        | `chore: cập nhật .gitignore`                |

### 4. Tạo Pull Request trước deadline

```bash
git push origin feature/tuan-X-[ten]-[mo-ta]
# Vào GitHub → Compare & Pull Request
# Title: [Tuần X] Tên người - Mô tả ngắn
# Gán 2 reviewers
```

### 5. Review và merge

- Tối thiểu **1 người approve** trước khi merge
- KHÔNG merge nếu CI fail (badge đỏ)
- Merge vào `develop`, KHÔNG merge vào `main`
- Merge vào `main` chỉ vào cuối Tuần 4, 7, 10 (milestone)

## Quy tắc branch

```
main          ← production-ready, chỉ merge develop vào cuối milestone
  └── develop ← integration branch, merge feature vào đây
        ├── feature/tuan-1-dang-maven-setup
        ├── feature/tuan-1-quocminh-git-cicd
        ├── feature/tuan-1-congminh-javafx-skeleton
        └── feature/tuan-1-khoa-junit-convention
```

## Quy tắc KHÔNG được làm

- ❌ Push thẳng vào `main` hoặc `develop` (vi phạm branch protection)
- ❌ Merge PR của chính mình (phải có ít nhất 1 người khác approve)
- ❌ Commit file binary lớn (`.jar`, `.db`, ảnh, video) vào repo
- ❌ Commit `target/` (đã có trong `.gitignore`)
- ❌ Code chưa compile hoặc test đang fail → push lên → làm CI fail của cả nhóm