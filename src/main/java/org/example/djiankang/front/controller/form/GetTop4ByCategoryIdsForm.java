package org.example.djiankang.front.controller.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@Schema(description = "获取各分类Top4套餐表单")
public class GetTop4ByCategoryIdsForm {

    @Schema(description = "分类ID数组", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "categoryIds不能为空")
    private Integer[] categoryIds;
}