package org.example.djiankang.mis.controller.from;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Schema(description = "体检项目表单")
public class ExamForm {

    @Schema(description = "体检项目名称", example = "身高体重", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "体检项目不能为空")
    @Length(max = 50, message = "体检项目不能超过50个字符")
    private String title;

    @Schema(description = "体检内容详情", example = "测量身高、体重，计算BMI指数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "体检内容不能为空")
    @Length(max = 500, message = "体检内容不能超过500个字符")
    private String content;
}