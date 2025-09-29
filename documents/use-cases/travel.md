### 🎯 Use Case: Công ty du lịch “TravelOne”

TravelOne muốn cá nhân hoá trải nghiệm cho khách hàng, từ lúc tìm tour đến khi đi du lịch xong.

---

#### 1. **Thu thập dữ liệu (Data Collection)**

* Khách hàng truy cập **website** TravelOne để xem tour Đà Nẵng, để lại email nhưng chưa đặt.
* Sau đó khách dùng **mobile app** để tìm vé máy bay.
* TravelOne cũng có dữ liệu từ **CRM** (từng đặt tour Nha Trang năm ngoái) và **Payment** (thanh toán bằng thẻ Visa).

👉 Tất cả các dữ liệu này được **LEO CDP Event Observer + Data Agents** gom về **Data Queue (Kafka/Redis)**.

---

#### 2. **Xử lý dữ liệu (Data Processors)**

* **Entity Resolution**: CDP nhận ra khách truy cập website và app chính là cùng 1 người (dựa trên email + số điện thoại).
* **Enrichment Agent**: bổ sung thêm thông tin từ lịch sử giao dịch → biết khách hàng thường đi biển, thích resort.
* **BI Chatbot** (nội bộ): phòng marketing có thể hỏi:

  > “Có bao nhiêu khách hàng từng đi biển nhưng chưa đặt tour mới?”

---

#### 3. **Kích hoạt AI Agents (Agentic AI)**

* **Personalized Message**: TravelOne gửi email + tin nhắn Zalo cá nhân hoá:

  > “Chào anh Nam, TravelOne có gói tour Đà Nẵng 4N3Đ kèm resort biển – đúng sở thích của anh năm ngoái tại Nha Trang.”
* **Real-time Notification**: khi khách đang lướt app, hiện push:

  > “Vé máy bay Đà Nẵng giảm 20% trong hôm nay, đặt ngay để giữ chỗ!”
* **Personal AI Mentor (Chatbot)**: khách hỏi chatbot:

  > “Tour Đà Nẵng có lịch trình gì?”
  > → Chatbot trả lời chi tiết + gợi ý option ăn uống, trải nghiệm địa phương.

---

#### 4. **Kết quả**

* Khách hàng cảm thấy được “chăm sóc riêng”, không còn quảng cáo chung chung.
* TravelOne tăng **tỷ lệ đặt tour** và **giá trị vòng đời khách hàng (CLV)**.
* Đội marketing chỉ cần thiết lập logic AI Agents, còn CDP lo phần **dữ liệu sạch, đồng bộ, real-time**.

---

👉 Tóm gọn:
**LEO CDP framework** = làm nền tảng dữ liệu khách hàng sạch và thống nhất.
**AI Agents** = biến dữ liệu đó thành **trải nghiệm du lịch cá nhân hoá theo thời gian thực**.

