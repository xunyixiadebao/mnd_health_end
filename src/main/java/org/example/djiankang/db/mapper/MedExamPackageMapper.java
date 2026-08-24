package org.example.djiankang.db.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.djiankang.db.entity.MedExamPackageEntity;

import java.util.List;
import java.util.Map;

/**
* @author zl
* @description 针对表【med_exam_package(体检套餐表)】的数据库操作Mapper
* @createDate 2026-08-17 20:34:37
* @Entity org.example.djiankang.db.entity.MedExamPackageEntity
*/
public interface MedExamPackageMapper {

    /**
     * 查询符合条件的总记录条数
     * @param params 查询条件
     * @return 总记录条数
     */
    long selectPageCount(Map<String, Object> params);

    /**
     * 查询符合条件的数据
     * @param params 查询条件
     * @return 数据列表
     */
    List<Map<String, Object>> selectPageList(Map<String, Object> params);
    /**
     * 根据md5Hash值查询套餐
     *
     * @param md5Hash 哈希值
     * @return 套餐实体对象
     */
    MedExamPackageEntity selectByMd5Hash(@Param("md5Hash") String md5Hash);

    /**
     * 保存套餐
     * @param entity 套餐
     * @return 1表示成功
     */
    int insert(MedExamPackageEntity entity);

    /**
     * 根据id查询套餐
     *
     * @param param 查询条件
     * @return 套餐
     */
    Map<String, Object> selectById(Map<String, Object> param);

    /**
     * 更新套餐
     *
     * @param goods 商品信息
     * @return 1表示成功
     */
    int update(MedExamPackageEntity goods);
    /**
     * 根据套餐id查询套餐
     *
     * @param id 套餐id
     * @return 套餐实体
     */
    MedExamPackageEntity selectEntityById(Integer id);

    /**
     * 更新套餐的exam_items字段
     *
     * @param params 包含md5Hash、examItems、id
     * @return 1表示更新成功
     */
    int updateExamItems(Map<String, Object> params);
    /**
     * 更新套餐的状态：上架/下架
     *
     * @param param 套餐id以及状态1/0
     * @return 1 表示更新成功
     */
    int updateStatus(Map<String, Object> param);
    /**
     * 根据id查询封面图片路径
     *
     * @param ids ids数组
     * @return 图片路径列表
     */
    List<String> selectImagesByIds(Integer[] ids);

    /**
     * 批量删除套餐
     *
     * @param ids 套餐id数组
     * @return 删除套餐的个数
     */
    int deleteByIds(Integer[] ids);
}




