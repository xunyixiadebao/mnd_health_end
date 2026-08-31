package org.example.djiankang.db.mapper;

import org.example.djiankang.db.entity.CrmCustomerEntity;

import java.util.Map;

/**
* @author zl
* &#064;description  针对表【crm_customer(客户信息表)】的数据库操作Mapper
* &#064;createDate  2026-08-17 20:34:37
* &#064;Entity  org.example.djiankang.db.entity.CrmCustomerEntity
 */
public interface CrmCustomerMapper {

    /**
     * 保存客户信息
     *
     * @param customer 客户信息
     * @return 1表示成功
     */
    int insert(CrmCustomerEntity customer);

    /**
     * 根据电话号码获取客户id
     *
     * @param phone 手机号
     * @return 客户id
     */
    Integer findIdByPhone(String phone);

    Map<String, Object> selectById(Integer id);

    int update(Map<String, Object> param);
}



