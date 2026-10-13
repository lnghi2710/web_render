package com.example.meetingroom.service;

import com.example.meetingroom.model.UserAccount;
import com.example.meetingroom.model.UserRole;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {
    private final List<UserAccount> accounts = new ArrayList<>(); // Kho tài khoản có thể quản lý và lưu bền vững.
    private final ObjectMapper mapper;
    private final Path accountsFile;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(ObjectMapper mapper,
            @Value("${app.accounts-file:data/student-topic-accounts.json}") String accountsFile,
            PasswordEncoder passwordEncoder) {
        this.mapper = mapper;
        this.accountsFile = Path.of(accountsFile).toAbsolutePath().normalize();
        this.passwordEncoder = passwordEncoder;
    }

    public AuthService(ObjectMapper mapper, String accountsFile) {
        this(mapper, accountsFile, new BCryptPasswordEncoder());
    }

    @PostConstruct
    // Đọc tài khoản đã lưu; chỉ khởi tạo bộ tài khoản mẫu khi đây là lần chạy đầu.
    public synchronized void initialize() {
        if (Files.exists(accountsFile)) {
            try {
                AccountStore store = mapper.readValue(accountsFile.toFile(), AccountStore.class);
                if (store.accounts == null || store.accounts.stream().anyMatch(account ->
                        account == null || account.getUsername() == null || account.getRole() == null
                                || account.getPasswordHash() == null)) {
                    throw new IllegalStateException("Tệp tài khoản không hợp lệ: " + accountsFile);
                }
                accounts.addAll(store.accounts);
                boolean migrated = false;
                for (UserAccount account : accounts) {
                    if (!isBcryptHash(account.getPasswordHash())) {
                        account.setPasswordHash(passwordEncoder.encode(account.getPasswordHash()));
                        migrated = true;
                    }
                }
                if (store.seedVersion < 3) {
                    accounts.removeIf(account -> "gv.huongdan".equalsIgnoreCase(account.getUsername()));
                    addMissingSeedAccounts();
                    store.seedVersion = 3;
                    migrated = true;
                }
                if (migrated) {
                    save();
                }
            } catch (JacksonException exception) {
                throw new IllegalStateException("Không thể đọc tài khoản từ " + accountsFile, exception);
            }
            return;
        }

        addMissingSeedAccounts();
        save();
    }

    public synchronized UserAccount authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        return accounts.stream()
                .filter(account -> account.getUsername().equalsIgnoreCase(username)
                        && account.getPasswordHash() != null
                        && passwordEncoder.matches(password, account.getPasswordHash()))
                .findFirst().orElse(null);
    }

    public synchronized List<UserAccount> accounts() {
        return List.copyOf(accounts);
    }

    public synchronized List<UserAccount> teachers() {
        return accounts.stream().filter(account -> account.canSupervise()).toList();
    }

    public synchronized UserAccount activeAccount(String username) {
        return accounts.stream().filter(account -> account.getUsername().equalsIgnoreCase(username))
                .findFirst().orElse(null);
    }

    // Khoa chỉ tạo được hai nhóm tài khoản thông thường; quyền ADMIN không thể cấp qua biểu mẫu.
    public synchronized void createAccount(String username, String password, String displayName,
            String studentId, UserRole role) {
        if (role == null || role == UserRole.ADMIN) {
            throw new IllegalArgumentException("Chỉ Trưởng khoa mới được cấp tài khoản; không thể tạo thêm quyền Trưởng khoa");
        }
        String normalizedUsername = clean(username);
        String normalizedPassword = clean(password);
        String normalizedName = clean(displayName);
        String normalizedStudentId = role == UserRole.STUDENT ? clean(studentId) : null;
        if (normalizedUsername == null || normalizedPassword == null || normalizedName == null
                || (role == UserRole.STUDENT && normalizedStudentId == null)) {
            throw new IllegalArgumentException("Vui lòng nhập đủ thông tin tài khoản");
        }
        if (accounts.stream().anyMatch(account -> account.getUsername().equalsIgnoreCase(normalizedUsername)
                || (normalizedStudentId != null && normalizedStudentId.equals(account.getStudentId())))) {
            throw new IllegalArgumentException("Tên đăng nhập hoặc mã sinh viên đã tồn tại");
        }
        accounts.add(new UserAccount(normalizedUsername, passwordEncoder.encode(normalizedPassword), normalizedName,
                normalizedStudentId, role));
        save();
    }

    // Giữ tài khoản khoa để tránh tự khóa quyền quản trị cuối cùng của hệ thống demo.
    public synchronized void deleteAccount(String username) {
        UserAccount target = activeAccount(username);
        if (target == null) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản");
        }
        if (target.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Không thể xóa tài khoản ban chủ nhiệm khoa");
        }
        accounts.remove(target);
        save();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isBcryptHash(String value) {
        return value != null && value.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    }

    private void addMissingSeedAccounts() {
        addSeedAccount("admin.khoa", "Ban chủ nhiệm khoa", null, UserRole.ADMIN);
        addSeedAccount("bm.khoa", "Trưởng bộ môn Công nghệ phần mềm", null, UserRole.DEPARTMENT_HEAD);
        addSeedAccount("gv.khoa", "Hoàng Thái Xuân Khoa", null, UserRole.TEACHER);
        addSeedAccount("gv.hd", "Giảng viên hướng dẫn", null, UserRole.SUPERVISOR);
        addSeedAccount("gv.pb", "Giảng viên phản biện", null, UserRole.REVIEWER);
        addSeedAccount("hoi.dong", "Đại diện Hội đồng", null, UserRole.COUNCIL);
        addSeedAccount("21161067", "Hoàng Hữu Nguyên Anh", "21161067", UserRole.STUDENT);
        addSeedAccount("20146298", "Hoàng Gia Bảo", "20146298", UserRole.STUDENT);
        addSeedAccount("22146178", "Trần Hải Nam", "22146178", UserRole.STUDENT);
    }

    private void addSeedAccount(String username, String displayName, String studentId, UserRole role) {
        if (activeAccount(username) == null) {
            accounts.add(new UserAccount(username, passwordEncoder.encode("123456"), displayName, studentId, role));
        }
    }

    // Ghi tệp tạm rồi thay thế nguyên tử để tránh mất toàn bộ kho tài khoản khi ghi dở.
    private void save() {
        Path temporaryFile = accountsFile.resolveSibling(accountsFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(accountsFile.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporaryFile.toFile(), new AccountStore(accounts));
            try {
                Files.move(temporaryFile, accountsFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, accountsFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (JacksonException | IOException exception) {
            throw new IllegalStateException("Không thể lưu tài khoản vào " + accountsFile, exception);
        }
    }

    public static class AccountStore {
        public List<UserAccount> accounts = new ArrayList<>();
        public int seedVersion;

        public AccountStore() {
        }

        AccountStore(List<UserAccount> accounts) {
            this.accounts = new ArrayList<>(accounts);
            this.seedVersion = 3;
        }
    }
}
