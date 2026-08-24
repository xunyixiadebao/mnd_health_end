package org.example.djiankang.front.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.db.mapper.MedExamPackageMapper;
import org.example.djiankang.exception.HisException;
import org.example.djiankang.front.service.GoodsService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {
    private final MedExamPackageMapper medExamPackageMapper;

    @Override
    @Cacheable(value = "goods", key = "#id")
    public Map<String, Object> getById(Integer id) {
        Map<String, Object> map = medExamPackageMapper.selectById(Map.of("id", id, "status", 1));
        if (CollUtil.isEmpty(map)) {
            throw new HisException("商品不存在", 404);
        }
        String[] jsonFields = {"departmentExam", "labExam", "medicalExam", "otherExam", "tags"};
        for (String field : jsonFields) {
            String value = MapUtil.getStr(map, field);
            if (StrUtil.isNotBlank(value)) {
                map.put(field, JSONUtil.parseArray(value));
            }
        }
        return map;
    }
}