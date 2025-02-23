package com.example.workflow.emuns;

public enum OrderStatus {
    PENDING,    // Chờ xử lý
    PROCESSING, // Đang xử lý
    PAYMENT,    // Thanh toán
    SHIPPING,   // Giao hàng
    COMPLETED,  // Hoàn thành
    REJECTED,   // Bị từ chối (không đủ hàng)
    CANCELLED,  // Đã hủy
    RETURNED    // Đã trả lại
}
