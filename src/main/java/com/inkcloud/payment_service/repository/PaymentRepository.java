package com.inkcloud.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inkcloud.payment_service.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, String>  {
    // @Query("UPDATE Order o SET o.state = :state, o.updatedAt = :updatedAt WHERE o.id = :id")
    // void updateOrder(@Param("id") String id, @Param("state") OrderState state,
    //         @Param("updatedAt") LocalDateTime updatedAt);

    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    Payment findByOrderId(@Param("orderId") String orderId);
}
