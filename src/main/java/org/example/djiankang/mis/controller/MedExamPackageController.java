package org.example.djiankang.mis.controller;


import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.common.MinIO;
import org.example.djiankang.common.PageResult;
import org.example.djiankang.common.R;
import org.example.djiankang.db.entity.MedExamPackageEntity;
import org.example.djiankang.exception.HisException;
import org.example.djiankang.mis.controller.from.ExamPackagePageQueryForm;
import org.example.djiankang.mis.controller.from.RemoveExamPackageByIdsForm;
import org.example.djiankang.mis.controller.from.SaveMedExamPackageForm;
import org.example.djiankang.mis.controller.from.UpdateMedExamPackageForm;
import org.example.djiankang.mis.service.MedExamPackageService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.util.Map;

@RestController("misMedExamPackageController")
@RequestMapping("/mis/goods")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "套餐管理")
public class MedExamPackageController {
    private final MedExamPackageService medExamPackageService;
    // 注入MinIO工具
    private final MinIO minIO;

    @GetMapping("/list")
    @Operation(summary = "分页查询", description = "根据查询条件分页查询套餐信息", operationId = "MisGoodsPageQuery")
    @SaCheckPermission(value = {"ROOT", "GOODS:SELECT"}, mode = SaMode.OR)
    public R list(@ParameterObject @Valid ExamPackagePageQueryForm form) {
        Integer pageNo = form.getPageNo();
        Integer pageSize = form.getPageSize();
        Integer startIndex = (pageNo - 1) * pageSize;
        Map<String, Object> params = BeanUtil.beanToMap(form);
        params.put("startIndex", startIndex);
        PageResult<Map<String, Object>> pageResult = medExamPackageService.queryPage(params);
        return R.ok().put("pageResult", pageResult);
    }

    @PostMapping("/images")
    @SaCheckPermission(value = {"ROOT", "GOODS:INSERT", "GOODS:UPDATE"}, mode = SaMode.OR)
    @Operation(summary = "上传体检套餐封面图片", description = "上传图片到MinIO，返回存储路径")
    public R uploadImage(@RequestParam("file") MultipartFile file) {
        // 校验1：文件不能为空
        if (file == null || file.isEmpty()) {
            return R.error("请选择要上传的图片文件");
        }

        // 校验2：文件大小不能超过5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            return R.error("图片大小不能超过5MB");
        }

        // 校验3：文件类型必须是图片
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg")
                || contentType.equals("image/jpg") || contentType.equals("image/png"))) {
            return R.error("仅支持jpg/jpeg/png格式的图片");
        }

        String path = medExamPackageService.uploadImg(file);
        return R.ok().put("result", path);
    }
    @PostMapping
    @SaCheckPermission(value = {"ROOT", "GOODS:INSERT"}, mode = SaMode.OR)
    @Operation(summary = "新增体检套餐", description = "保存体检套餐信息，包含体检项目列表和标签")
    public R save(@RequestBody @Valid SaveMedExamPackageForm form) {
        MedExamPackageEntity entity = BeanUtil.toBean(form, MedExamPackageEntity.class,
                CopyOptions.create().setIgnoreProperties("departmentExam", "labExam",
                        "medicalExam", "otherExam", "tags"));

        // 将List转JSON存储
        if (CollUtil.isNotEmpty(form.getDepartmentExam())) {
            entity.setDepartmentExam(JSONUtil.toJsonStr(form.getDepartmentExam()));
        }
        if (CollUtil.isNotEmpty(form.getLabExam())) {
            entity.setLabExam(JSONUtil.toJsonStr(form.getLabExam()));
        }
        if (CollUtil.isNotEmpty(form.getMedicalExam())) {
            entity.setMedicalExam(JSONUtil.toJsonStr(form.getMedicalExam()));
        }
        if (CollUtil.isNotEmpty(form.getOtherExam())) {
            entity.setOtherExam(JSONUtil.toJsonStr(form.getOtherExam()));
        }
        if (ArrayUtil.isNotEmpty(form.getTags())) {
            entity.setTags(JSONUtil.toJsonStr(form.getTags()));
        }
        medExamPackageService.save(entity);
        return R.ok("套餐保存成功");
    }

    @GetMapping("/{id}")
    @SaCheckPermission(value = {"ROOT", "GOODS:SELECT"}, mode = SaMode.OR)
    @Operation(summary = "根据ID查询体检套餐", description = "返回套餐详情，包含体检项目列表")
    public R getById(
            @Parameter(description = "体检套餐ID", example = "1", required = true)
            @PathVariable
            @Min(value = 1, message = "id不能小于1")
            Integer id) {
        Map<String, Object> goods = medExamPackageService.getById(id);
        return R.ok().put("goods", goods);
    }

    @PutMapping
    @SaCheckPermission(value = {"ROOT", "GOODS:UPDATE"}, mode = SaMode.OR)
    @Operation(summary = "修改体检套餐")
    public R update(@RequestBody @Valid UpdateMedExamPackageForm form) {
        MedExamPackageEntity entity = BeanUtil.toBean(form, MedExamPackageEntity.class,
                CopyOptions.create().setIgnoreProperties("departmentExam", "labExam", "medicalExam", "otherExam", "tags"));

        // 将List转JSON存储
        if (CollUtil.isNotEmpty(form.getDepartmentExam())) {
            entity.setDepartmentExam(JSONUtil.toJsonStr(form.getDepartmentExam()));
        }
        if (CollUtil.isNotEmpty(form.getLabExam())) {
            entity.setLabExam(JSONUtil.toJsonStr(form.getLabExam()));
        }
        if (CollUtil.isNotEmpty(form.getMedicalExam())) {
            entity.setMedicalExam(JSONUtil.toJsonStr(form.getMedicalExam()));
        }
        if (CollUtil.isNotEmpty(form.getOtherExam())) {
            entity.setOtherExam(JSONUtil.toJsonStr(form.getOtherExam()));
        }
        if (ArrayUtil.isNotEmpty(form.getTags())) {
            entity.setTags(JSONUtil.toJsonStr(form.getTags()));
        }

        medExamPackageService.update(entity);
        return R.ok("套餐修改成功");
    }
    @PostMapping("/{id}/examItems")
    @Operation(summary = "上传检查项目数据", description = "通过Excel文件批量上传套餐的检查项目")
    @SaCheckPermission(value = {"ROOT", "GOODS:INSERT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public R uploadExamItems(
            @Parameter(description = "套餐ID", required = true, example = "1")
            @PathVariable("id")
            @NotNull(message = "id不能为空")
            @Min(value = 1, message = "id不能小于1")
            Integer id,
            @Parameter(description = "上传的Excel文件", required = true)
            @RequestParam("file")
            MultipartFile file) {
        // 校验文件不能为空
        if (file == null || file.isEmpty()) {
            throw new HisException("文件不能为空", 400);
        }
        // 校验文件扩展名必须是 xlsx/xls
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            throw new HisException("只支持 .xlsx 或 .xls 格式的Excel文件", 400);
        }
        // 校验文件大小不能超过20MB
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new HisException("文件大小不能超过20MB", 400);
        }
        // 执行service导入
        medExamPackageService.uploadExamItems(id, file);
        return R.ok();
    }

    @GetMapping("/{id}/examItems")
    @Operation(summary = "下载体检项目文档", description = "根据套餐ID下载对应的Excel文档")
    @SaCheckPermission(value = {"ROOT", "GOODS:SELECT", "GOODS:INSERT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public void downloadExamItems(
            @Parameter(description = "套餐ID", required = true)
            @PathVariable("id")
            @NotNull(message = "id不能为空")
            @Min(value = 1, message = "id不能小于1")
            Integer id,
            HttpServletResponse response
    ) {
        try {
            String fileName = id + ".xlsx";
            // 设置响应头
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.setContentType("application/x-download");
            response.setCharacterEncoding("UTF-8");

            String path = "mis/goods/exam_items/" + fileName;

            try (InputStream in = minIO.downloadFile(path);
                 BufferedInputStream bin = new BufferedInputStream(in);
                 ServletOutputStream out = response.getOutputStream();
                 BufferedOutputStream bout = new BufferedOutputStream(out)) {

                IoUtil.copy(bin, bout);
            }
        } catch (Exception e) {
            log.error("文档下载失败，id: {}", id, e);
            throw new HisException("文档下载失败");
        }
    }
    @PutMapping("/{id}/status/{status}")
    @SaCheckPermission(value = {"ROOT", "GOODS:UPDATE"}, mode = SaMode.OR)
    @Operation(summary = "修改体检套餐状态", description = "上架或下架指定的体检套餐")
    public R updatePackageStatus(
            @Parameter(description = "体检套餐ID", required = true, example = "1")
            @PathVariable
            @NotNull(message = "id不能为空")
            @Min(value = 1, message = "id不能小于1")
            Integer id,

            @Parameter(description = "套餐状态（true-上架，false-下架）", required = true, example = "true")
            @PathVariable
            @NotNull(message = "status不能为空")
            Boolean status) {

        Map<String, Object> param = Map.of("id", id, "status", status);
        medExamPackageService.updateStatus(param);
        return R.ok("套餐状态更新成功");
    }
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除体检套餐", description = "根据ID数组批量删除体检套餐")
    @SaCheckPermission(value = {"ROOT", "GOODS:DELETE"}, mode = SaMode.OR)
    public R deleteBatch(@Valid @RequestBody RemoveExamPackageByIdsForm form) {
        medExamPackageService.removeByIds(form.getIds());
        return R.ok("删除成功");
    }
}
