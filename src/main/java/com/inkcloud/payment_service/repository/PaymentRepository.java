package com.inkcloud.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inkcloud.payment_service.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, String>  {

}
