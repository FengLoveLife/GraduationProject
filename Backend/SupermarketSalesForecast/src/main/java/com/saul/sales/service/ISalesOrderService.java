package com.saul.sales.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.sales.dto.SalesOrderQueryDTO;
import com.saul.sales.entity.SalesOrder;
import com.saul.sales.vo.SalesOrderVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 销售订单主表 Service 接口
 */
public interface ISalesOrderService extends IService<SalesOrder> {

    /**
     * 导入销售日结数据
     * @param file Excel文件
     * @param operator 操作人
     */
    void importSalesData(MultipartFile file, String operator);

    /**
     * 分页查询销售订单
     */
    Page<SalesOrderVO> queryPage(SalesOrderQueryDTO queryDTO);
}
