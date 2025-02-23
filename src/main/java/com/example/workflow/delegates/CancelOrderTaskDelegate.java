package com.example.workflow.delegates;

import com.example.workflow.emuns.OrderStatus;
import com.example.workflow.models.Order;
import com.example.workflow.models.OrderProduct;
import com.example.workflow.models.Product;
import com.example.workflow.services.OrderService;
import com.example.workflow.services.ProductService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CancelOrderTaskDelegate implements JavaDelegate {
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderService orderService;
    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        Long orderId = (Long) delegateExecution.getVariable("orderId");

        Order order = orderService.findById(orderId).orElse(null);
        if (order == null) {
            throw new RuntimeException("Order ID " + orderId + " không tồn tại!");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderService.save(order);
        System.out.println("Order " + orderId + " đã bị hủy.");

        // Lấy danh sách sản phẩm trong Order và hoàn lại số lượng vào kho
        for (OrderProduct orderProduct : order.getOrderProducts()) {
            Product product = orderProduct.getProduct();
            int restoredStock = product.getStockQuantity() + orderProduct.getRequestedQuantity();
            product.setStockQuantity(restoredStock);
            productService.save(product);

            System.out.println("Hoàn lại " + orderProduct.getRequestedQuantity() +
                    " sản phẩm '" + product.getName() + "' vào kho.");
        }

        System.out.println("Hủy đơn hàng thành công.");


    }
}
