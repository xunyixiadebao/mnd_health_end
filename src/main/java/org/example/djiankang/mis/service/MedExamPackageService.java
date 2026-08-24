package org.example.djiankang.mis.service;

import org.example.djiankang.common.PageResult;
import org.example.djiankang.db.entity.MedExamPackageEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface MedExamPackageService {
    /**
     * 分页查询体检套餐
     *
     * @param params 查询条件
     * @return 体检套餐列表
     */
    PageResult<Map<String, Object>> queryPage(Map<String, Object> params);
    /**
     * 上传图片到MinIO服务器
     *
     * @param file 文件本身
     * @return 图片在MinIO服务器上存储的相对路径
     */
    String uploadImg(MultipartFile file);
    /**
     * 保存套餐
     * @param medExamPackageEntity 套餐
     */
    void save(MedExamPackageEntity medExamPackageEntity);

    /**
     * 根据id获取套餐
     *
     * @param id 套餐id
     * @return 套餐
     */
    Map<String, Object> getById(Integer id);
    /**
     * 更新套餐
     *
     * @param goods 商品信息
     */
    void update(MedExamPackageEntity goods);
    /**
     * 上传体检项目数据
     *
     * @param goodsId 套餐id
     * @param file    要上传的文件
     */
    void uploadExamItems(Integer goodsId, MultipartFile file);
    /**
     * 更新套餐状态
     *
     * @param params 状态1/0，套餐id
     */
    void updateStatus(Map<String, Object> params);
    /**
     * 批量删除套餐
     *
     * @param ids 套餐id数组
     */
    void removeByIds(Integer[] ids);


}
