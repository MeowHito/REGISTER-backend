package com.actionth.membership.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.actionth.membership.model.CountryState;
import com.actionth.membership.model.Coupon;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.OrderDetail;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.dto.CountryStateDto;
import com.actionth.membership.model.dto.EventViewDto;
import com.actionth.membership.model.dto.OrderDetailDto;
import com.actionth.membership.model.dto.OrderDto;
import com.actionth.membership.model.dto.UserDto;
import com.actionth.membership.model.request.CouponDTO;
import com.actionth.membership.model.request.OrderDetailRequest;
import com.actionth.membership.model.request.OrderRequest;

@Configuration
public class ModelMapperConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setAmbiguityIgnored(true).setFullTypeMatchingRequired(true);

        modelMapper.addMappings(new PropertyMap<CouponDTO, Coupon>() {
            @Override
            protected void configure() {
                skip().setEvent(null);
                skip().setOldEvent(null);
                skip().setId(null);
            }
        });

        modelMapper.addMappings(new PropertyMap<OrderRequest, Orders>() {
            @Override
            protected void configure() {
                skip().setId(null);
                skip().setUuid(null);
                skip().setEvent(null);
                skip().setOrderDetails(null);
            }
        });

        modelMapper.addMappings(new PropertyMap<OrderDetailRequest, OrderDetail>() {
            @Override
            protected void configure() {
                skip().setId(null);
                skip().setUuid(null);
                skip().setEventType(null);
                skip().setShirtType(null);
                skip().setShirtSize(null);
                skip().setPricing(null);
                skip().setOrder(null);
            }
        });

        modelMapper.addMappings(new PropertyMap<CountryState, CountryStateDto>() {
            @Override
            protected void configure() {
                map().setId(source.getUuid());
            }
        });

        modelMapper.addMappings(new PropertyMap<OrderDetail, UserDto>() {
            @Override
            protected void configure() {
                map().setId(source.getUuid());
            }
        });

        modelMapper.addMappings(new PropertyMap<Event, EventViewDto>() {
            @Override
            protected void configure() {
                map().setId(source.getUuid());
            }
        });

        modelMapper.addMappings(new PropertyMap<Orders, OrderDto>() {
            @Override
            protected void configure() {
                map().setId(source.getUuid());
                using(ctx -> {
                    Event event = ((Orders) ctx.getSource()).getEvent();
                    return event != null ? event.getUuid() : null;
                }).map(source).setEventId(null);
            }
        });

        modelMapper.addMappings(new PropertyMap<OrderDetail, OrderDetailDto>() {
            @Override
            protected void configure() {
                map().setId(source.getUuid());
                using(ctx -> {
                    OrderDetail od = (OrderDetail) ctx.getSource();
                    return od.getEventType() != null ? od.getEventType().getUuid() : null;
                }).map(source).setEventTypeId(null);

                using(ctx -> {
                    OrderDetail od = (OrderDetail) ctx.getSource();
                    return od.getShirtType() != null ? od.getShirtType().getUuid() : null;
                }).map(source).setShirtTypeId(null);

                using(ctx -> {
                    OrderDetail od = (OrderDetail) ctx.getSource();
                    return od.getShirtSize() != null ? od.getShirtSize().getUuid() : null;
                }).map(source).setShirtSizeId(null);

                using(ctx -> {
                    OrderDetail od = (OrderDetail) ctx.getSource();
                    return od.getPricing() != null ? od.getPricing().getUuid() : null;
                }).map(source).setPricingId(null);
            }
        });

        return modelMapper;
    }
}
