package org.example.djiankang.mis.controller.from;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@Schema(description = "删除体检套餐请求参数")
public class RemoveExamPackageByIdsForm {

    @Schema(description = "体检套餐ID数组", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1, 2, 3]")
    @NotEmpty(message = "ids不能为空")
    private Integer[] ids;
}