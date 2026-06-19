package com.dlight.payments.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.dlight.payments.dto.PaymentResponseDto;
import com.dlight.payments.dto.PaymentStatusResponseDto;
import com.dlight.payments.entity.Payment;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "id", target = "paymentId")
    PaymentResponseDto toResponseDto(Payment payment);

    @Mapping(source = "id", target = "paymentId")
    PaymentStatusResponseDto toStatusResponseDto(Payment payment);
}
