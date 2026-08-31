package org.example.djiankang.front.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.common.PageResult;
import org.example.djiankang.db.mapper.MedExamPackageMapper;
import org.example.djiankang.exception.HisException;
import org.example.djiankang.front.service.GoodsService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Override
    public Map<Integer, List<Map<String, Object>>> getTop4ByCategoryIds(Integer[] categoryIds) {
        if (categoryIds == null || categoryIds.length < 3) {
            return new HashMap<>();
        }

        List<Map<String, Object>> allList = medExamPackageMapper.selectTop4ByCategoryId(categoryIds[0], categoryIds[1], categoryIds[2]);

        // 把 allList 按 category_id 分组
        // 返回一个 Map<Integer, List<Map<String, Object>>>，key 是分类 ID
        // value 是该分类下的数据列表。
        return allList.stream()
                .collect(Collectors.groupingBy(
                        item -> (Integer) item.get("category_id"),
                        Collectors.toList()
                ));
    }
    @Override
    public PageResult<Map<String, Object>> queryPage(Map<String, Object> param) {
        long total = medExamPackageMapper.selectPageCountForFront(param);
        List<Map<String, Object>> records = new ArrayList<>();
        if (total > 0) {
            records = medExamPackageMapper.selectPageListForFront(param);
        }
        return PageResult.of(records, total);
    }
}