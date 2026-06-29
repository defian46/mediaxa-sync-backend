# User Guide — Cashier POS Management Console

This guide outlines the standard operating workflows for cashiers and store managers using the Mediaxa POS Client.

---

## 1. Cashier Login & Shift Opening

1. Open the Mediaxa POS application on your Android tablet or phone terminal.
2. Log in using your allocated cashier username and password.
3. Enter your **6-digit PIN** to verify authorization.
4. If this is the start of the morning shift, initialize the Cash Shift:
   - Input the initial float cash amount (e.g. `Rp 200.000` for register change).
   - Press **Buka Shift**.

---

## 2. Order Cart Checkout Workflow

1. **Adding Items**: Tap products from the catalog screen or search by name. Selected items will appear in the cart panel.
2. **Assigning Customer**: Press **Pilih Pelanggan** to link a loyalty member (accumulates points).
3. **Checkout**: Press **Bayar** to open the payment sheet.
4. **Payment Mode**: Choose **TUNAI (Cash)** or **QRIS/Electronic**.
   - If QRIS, wait for the customer to scan the payment code.
   - If Cash, enter the tender cash amount and calculate change.
5. Press **Selesaikan Pembayaran** to print structure receipt.

---

## 3. Tutup Buku Harian (Daily Closing)

At the end of the business day, the store manager performs the closing check:
1. Tap the **Notifikasi** alert on the admin screen showing "Tutup Buku Hari Ini Belum Selesai".
2. Check the counted cash in the register drawers against the system recorded totals.
3. If there are discrepancies, input adjustment notes.
4. Press **Submit Tutup Buku** to lock today's transactions and trigger background cloud synchronization.
