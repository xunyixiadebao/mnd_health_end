package org.example.djiankang.mis.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.djiankang.common.MinIO;
import org.example.djiankang.common.PageResult;
import org.example.djiankang.db.entity.MedExamPackageEntity;
import org.example.djiankang.db.mapper.MedExamPackageMapper;
import org.example.djiankang.exception.HisException;
import org.example.djiankang.mis.service.MedExamPackageService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MedExamPackageServiceImpl implements MedExamPackageService {
    private final MedExamPackageMapper medExamPackageMapper;
    private final MinIO minIO;
    @Override
    public PageResult<Map<String, Object>> queryPage(Map<String, Object> params) {
        long count = medExamPackageMapper.selectPageCount(params);
        List<Map<String, Object>> records = new ArrayList<>();
        if (count > 0) {
            records = medExamPackageMapper.selectPageList(params);
        }
        return PageResult.of(records, count);
    }

    @Override
    public String uploadImg(MultipartFile file) {
        // 获取原始文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String fileName = IdUtil.simpleUUID() + extension;
        String path = "front/goods/" + fileName;
        minIO.uploadImage(path, file);
        return path;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(MedExamPackageEntity medExamPackageEntity) {
        // 生成md5,并设置
        String md5Hash = generateMd5Hash(medExamPackageEntity);
        medExamPackageEntity.setMd5Hash(md5Hash);
        // 判断套餐是否已存在，防止重复提交
        MedExamPackageEntity goods = medExamPackageMapper.selectByMd5Hash(md5Hash);
        if (goods != null) {
            throw new HisException("体检套餐已存在", 409);
        }
        // 保存体检套餐
        int rows = medExamPackageMapper.insert(medExamPackageEntity);
        if (rows != 1) {
            throw new HisException("体检套餐保存失败", 500);
        }
        log.info("体检套餐保存成功：packageCode={}", medExamPackageEntity.getPackageCode());
    }

    private String generateMd5Hash(MedExamPackageEntity entity) {
        // 将entity对象转换成json对象
        JSONObject json = JSONUtil.parseObj(entity);
        // 将无关紧要的属性移除，因为这些属性在生成md5 hash时是非必要属性
        json.remove("id");
        json.remove("categoryId");
        json.remove("salesVolume");
        json.remove("status");
        json.remove("md5Hash");
        json.remove("createTime");
        json.remove("updateTime");
        // 生成md5 hash并返回
        return MD5.create().digestHex(json.toString()).toUpperCase();
    }

    @Override
    public Map<String, Object> getById(Integer id) {
        Map<String, Object> map = medExamPackageMapper.selectById(Map.of("id", id));
        if (CollUtil.isEmpty(map)) {
            throw new HisException("套餐不存在", 404);
        }
        // map是数据库的查询结果，需要将map中查询的json字符串转换成json对象，返回给前端
        // 从数据库中查询出来的是："[{\"title\": \"血脂全套\", \"content\": \"血脂七项检测\"}]"
        //应该返回JSON对象：[{"title": "血脂全套", "content": "血脂七项检测"}]
        String[] jsonFields = {"departmentExam", "labExam", "medicalExam", "otherExam", "tags"};
        for (String field : jsonFields) {
            String value = MapUtil.getStr(map, field);
            if (StrUtil.isNotBlank(value)) {
                map.put(field, JSONUtil.parseArray(value));
            }
        }
        return map;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "goods", key = "#goods.id")
    public void update(MedExamPackageEntity goods) {
        String md5Hash = generateMd5Hash(goods);
        goods.setMd5Hash(md5Hash);
        int rows = medExamPackageMapper.update(goods);
        if (rows != 1) {
            throw new HisException("套餐更新失败", 500);
        }
        log.info("套餐更新成功：packageCode={}", goods.getPackageCode());
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "goods",key = "#medExamPackageId")
    public void uploadExamItems(Integer medExamPackageId, MultipartFile file) {
        List<Map<String, String>> list = new ArrayList<>();

        try (BufferedInputStream in = new BufferedInputStream(file.getInputStream());
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {

            XSSFSheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            // 从第2行开始（第1行是表头）
            for (int i = 1; i <= lastRowNum; i++) {
                XSSFRow row = sheet.getRow(i);
                if (row == null) continue; // 跳过空行

                // 使用工具类读取，避免空指针
                String place = getCellValue(row.getCell(0));
                String name = getCellValue(row.getCell(1));
                String item = getCellValue(row.getCell(2));
                String type = getCellValue(row.getCell(3));
                String code = getCellValue(row.getCell(4));
                String sex = getCellValue(row.getCell(5));
                String value = getCellValue(row.getCell(6));
                String template = getCellValue(row.getCell(7));

                // 跳过空行（所有字段都为空）
                if (StringUtils.isAllBlank(place, name, item, type, code, sex, value, template)) {
                    continue;
                }

                // 为了保证数据插入顺序和Excel读取顺序一致，使用LinkedHashMap
                Map<String, String> examItem = new LinkedHashMap<>();
                examItem.put("place", place);
                examItem.put("name", name);
                examItem.put("item", item);
                examItem.put("type", type);
                examItem.put("code", code);
                examItem.put("sex", sex);
                examItem.put("value", value);
                examItem.put("template", template);
                list.add(examItem);
            }

            if (list.isEmpty()) {
                throw new HisException("Excel文件为空或格式不正确", 400);
            }

        } catch (Exception e) {
            log.error("解析Excel失败", e);
            throw new HisException("解析Excel出错了：" + e.getMessage(), 500);
        }

        // 将excel文件上传到minio服务器
        String path = "mis/goods/exam_items/" + medExamPackageId + ".xlsx";
        minIO.uploadExcel(path, file);
        // 根据id查询套餐对象
        MedExamPackageEntity medExamPackageEntity = medExamPackageMapper.selectEntityById(medExamPackageId);
        String examItems = JSONUtil.parseArray(list).toString();
        medExamPackageEntity.setExamItems(examItems);
        // 重新生md5Hash值
        String md5Hash = generateMd5Hash(medExamPackageEntity);
        // 更新套餐数据到数据库表
        Map<String, Object> param = Map.of("examItems", examItems,
                "md5Hash", md5Hash, "id", medExamPackageId);
        int rows = medExamPackageMapper.updateExamItems(param);
        if (rows != 1) {
            throw new HisException("更新具体的体检项目失败", 500);
        }
        log.info("成功更新了具体的体检项目：id={}", medExamPackageId);
    }

    /**
     * 安全获取单元格字符串值
     */
    private String getCellValue(XSSFCell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = "goods", key = "#params.get('id')", condition = "#params.get('status') == false")
    public void updateStatus(Map<String, Object> params) {
        int rows = medExamPackageMapper.updateStatus(params);
        if (rows != 1) {
            throw new HisException("套餐状态更新失败", 500);
        }
        log.info("套餐状态更新成功，最新状态为：{}", MapUtil.getStr(params, "status"));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByIds(Integer[] ids) {
        if(ArrayUtil.isEmpty(ids)) {
            throw new HisException("ids不能为空", 400);
        }

        // 先获取图片路径
        List<String> images = medExamPackageMapper.selectImagesByIds(ids);

        // 删除套餐
        int rows = medExamPackageMapper.deleteByIds(ids);
        if(rows != ids.length){
            throw new HisException("套餐删除失败", 500);
        }
        for(Integer id: ids){
            cacheEvictGoods(id);
        }
        // 事务提交后再异步删除图片（通过事务同步器）
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        asyncDeleteCoverImages(images);
                    }
                }
        );
    }
    @CacheEvict(cacheNames = "goods", key = "#id")
    public void cacheEvictGoods(Integer id) {
    }

    @Async("taskExecutor")
    public void asyncDeleteCoverImages(List<String> images) {
        if(CollUtil.isEmpty(images)) {
            log.warn("图片集合为空，无需删除");
            return;
        }

        // 记录删除失败的图片，便于后续重试
        List<String> failedImages = new ArrayList<>();
        images.forEach(image -> {
            try {
                minIO.deleteFile(image);
                log.info("图片删除成功: {}", image);
            } catch (Exception e) {
                log.error("图片删除失败: {}", image, e);
                failedImages.add(image);
            }
        });

        if(!failedImages.isEmpty()) {
            // 可以将失败的图片记录到数据库，供后续重试
            log.error("以下图片删除失败: {}", failedImages);
        }
    }
}