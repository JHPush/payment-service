package com.inkcloud.payment_service.init;

import org.springframework.stereotype.Component;

import com.inkcloud.payment_service.domain.Payment;
import com.inkcloud.payment_service.enums.PaymentMethod;
import com.inkcloud.payment_service.enums.PaymentStatus;
import com.inkcloud.payment_service.repository.PaymentRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDataInitializer {
    private final PaymentRepository repo;

    @PostConstruct
    @Transactional
    public void initPayDatas(){
        // 이미 데이터가 있으면 스킵
        if (repo.count() > 0) {
            log.info("Payment 데이터가 이미 존재합니다. 초기화를 스킵합니다.");
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource("data/payments.csv");
            
            if (!resource.exists()) {
                log.warn("payments.csv 파일을 찾을 수 없습니다: {}", resource.getPath());
                return;
            }

            List<Payment> payments = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line = reader.readLine(); // 헤더 스킵
                
                while ((line = reader.readLine()) != null) {
                    String[] fields = parseCSVLine(line);
                    
                    if (fields.length >= 10) { // 필수 필드 수 확인
                        Payment payment = Payment.builder()
                                .paymentId(fields[0])
                                .method(PaymentMethod.valueOf(fields[1]))
                                .price(parseInt(fields[2]))
                                .count(parseInt(fields[3]))
                                .at(parseDateTime(fields[4]))
                                .pg(fields[5])
                                .txId(fields[6])
                                .applNum(fields[7])
                                .status(PaymentStatus.valueOf(fields[8]))
                                .orderId(fields[9])
                                .build();
                        
                        payments.add(payment);
                    }
                }
            }
            
            repo.saveAll(payments);
            log.info("Payment 데이터 초기화 완료: {}개 레코드", payments.size());
            
        } catch (Exception e) {
            log.error("Payment 데이터 초기화 중 오류 발생", e);
        }
    }
    
    /**
     * CSV 라인 파싱 (쉼표로 구분, 따옴표 처리)
     */
    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        fields.add(currentField.toString().trim());
        return fields.toArray(new String[0]);
    }
    
    /**
     * 문자열을 Long으로 파싱
     */
    private Long parseLong(String value) {
        try {
            return Long.parseLong(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.warn("Long 파싱 실패: {}", value);
            return 0L;
        }
    }
    
    /**
     * 문자열을 Integer로 파싱
     */
    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.warn("Integer 파싱 실패: {}", value);
            return 0;
        }
    }
    
    /**
     * 문자열을 LocalDateTime으로 파싱
     */
    private LocalDateTime parseDateTime(String value) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(value, formatter);
        } catch (DateTimeParseException e) {
            log.warn("DateTime 파싱 실패: {}", value);
            return LocalDateTime.now();
        }
    }
}