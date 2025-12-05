# Cấu hình Hệ thống LEO CDP – Tài liệu Tham Khảo cho DevOps / IT Admin

Tài liệu này mô tả các trường cấu hình quan trọng trong file cấu hình của LEO CDP. Các giá trị trong dấu `{{ }}` sẽ được thay thế trong quá trình triển khai.

---

<h4>1. Thông tin bắt buộc cho Admin Setup</h4>

<strong>Super Admin Email</strong>
Đây là tài khoản quản trị cao nhất trong toàn bộ hệ thống CDP. Mật khẩu của Super Admin được tạo khi chạy script `setup-new-leocdp.sh`.
`superAdminEmail={{superAdminEmail}}`

<strong>Data Hub cho Data Observer</strong>
Domain phục vụ cho Data Observer khi thu thập và quan sát dữ liệu.
`httpObserverDomain={{httpObserverDomain}}`

<strong>LEO Bot (FAQ & Content AI)</strong>
Cấu hình cho LEO Bot nhằm hỗ trợ FAQ và tạo nội dung tự động.
`httpLeoBotDomain={{httpLeoBotDomain}}`
`httpLeoBotApiKey={{httpLeoBotApiKey}}`

<strong>Admin Dashboard</strong>
Domain và WebSocket dành cho giao diện quản trị.
`httpAdminDomain={{httpAdminDomain}}`
`webSocketDomain={{webSocketDomain}}`
`adminLogoUrl={{adminLogoUrl}}`

<strong>SMTP / Email Server</strong>
Cấu hình hệ thống gửi email.
`smtpHost={{smtpHost}}`
`smtpPort={{smtpPort}}`
`smtpUser={{smtpUser}}`
`smtpPassword={{smtpPassword}}`
`smtpFromAddress={{smtpFromAddress}}`
`smtpFromName=CDP Admin`
`smtpTls=true`

<strong>Default CDP Database</strong>
Chỉ định cấu hình database mặc định.
`mainDatabaseConfig=cdpDbConfigs`

<strong>Database Backup</strong>
Cấu hình chu kỳ backup và nơi lưu trữ file backup.
`databaseBackupPeriodHours=24`
`databaseBackupRetentionDays=7`
`databaseBackupPath={{databaseBackupPath}}`

---

<h4>2. Metadata mặc định của hệ thống CDP</h4>

`runtimeEnvironment=PRO`
`minifySuffix=-min`
`buildEdition=PRO`
`buildVersion=v_0.9.0`

---

<h4>3. Cấu hình Profile Merge Strategy</h4>

Hệ thống CDP có cơ chế gộp profile (Profile Merge) để hợp nhất danh tính người dùng:

* `automation`: hệ thống tự động gộp dựa trên key trùng khớp (email, phone, deviceId…).
* `manually`: admin phải kiểm tra và duyệt thủ công từng trường hợp.
* `hybrid`: tự động cho phần lớn case; yêu cầu duyệt tay khi xảy ra xung đột dữ liệu.

Ví dụ:
`profileMergeStrategy=manually`

---

<h4>4. CDN Static Files</h4>

Domain cho static files được phục vụ qua CDN.
`httpStaticDomain=cdn.jsdelivr.net/gh/USPA-Technology/leo-cdp-static-files@v0.9`

---

<h4>5. Data Model Metadata</h4>

Khai báo các bộ data model theo từng lĩnh vực:
`industryDataModels=COMMERCE,MEDIA,SERVICE,FINANCE`

---

<h4>6. Runtime Path</h4>

Đường dẫn runtime và database MaxMind GeoIP2.
`runtimePath=.`
`pathMaxmindDatabaseFile=data/GeoIP2-City-Asia-Pacific.mmdb`

---

<h4>7. HTTP Routing Config</h4>

Định tuyến cho Admin và DataHub.
`httpRoutingConfigAdmin=leocdp-admin`
`httpRoutingConfigObserver=datahub`

---

<h4>8. Global System Config</h4>

Một số tham số chung:
`messageQueueType=local`
`enableCachingViewTemplates=true`

---

<h4>9. Apache Kafka</h4>

Cấu hình cho event streaming:
`kafkaBootstrapServer=localhost:9092`
`kafkaTopicEvent=LeoCdpEvent`
`kafkaTopicEventPartitions=2`
`kafkaTopicProfile=LeoCdpProfile`
`kafkaTopicProfilePartitions=2`

---

<h4>10. Data Policies</h4>

Định nghĩa chính sách lưu giữ và export dữ liệu:
`numberOfDaysToKeepDeadVisitor=30`
`maxSegmentSizeToRunInQueue=10000`
`batchSizeOfSegmentDataExport=300`

---

<h4>11. Update Script</h4>

`updateShellScriptPath=`
`updateLeoSystemSecretKey=`

---

<h4>12. SSO Login Configuration</h4>

Khi hệ thống bật Keycloak SSO, có thể cho phép hoặc ẩn nút login bằng username/password.

* `showDefaultLogin=true`: vẫn cho phép đăng nhập non-SSO.
* `showDefaultLogin=false`: chỉ được đăng nhập qua SSO.

Ví dụ:
`showDefaultLogin=false`
