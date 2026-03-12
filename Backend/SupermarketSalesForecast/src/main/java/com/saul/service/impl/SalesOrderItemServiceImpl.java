package com.saul.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.entity.SalesOrderItem;
import com.saul.mapper.SalesOrderItemMapper;
import com.saul.service.ISalesOrderItemService;
import com.saul.vo.SalesOrderItemVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 销售订单明细表 Service 实现类
 */
@Service
public class SalesOrderItemServiceImpl extends ServiceImpl<SalesOrderItemMapper, SalesOrderItem>
        implements ISalesOrderItemService {

    @Override
    public List<SalesOrderItemVO> getItemsByOrderId(Long orderId) {
        if (orderId == null) {
            return List.of();
        }
        
        List<SalesOrderItem> list = this.list(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, orderId));
        
        return list.stream().map(item -> {
            SalesOrderItemVO vo = new SalesOrderItemVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
