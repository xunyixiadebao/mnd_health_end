package org.example.djiankang.db.entity;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "goods_snapshot") // 这个是集合名，类似于mysql中的表名。
public class GoodsSnapshotEntity implements Serializable {
    // @Id 注解是 MongoDB 实体对象的身份标识，它的作用相当于 MySQL 表中的主键
    @Id
    private String _id;
    // @Indexed 注解的作用
    // 为指定字段创建数据库索引，其目的与在 MySQL 表中为字段创建索引完全相同：为了大幅提高查询速度。
    @Indexed(unique = true)
    private Integer id;
    private String packageCode;
    private String packageName;
    private String description;
    private List<Map> departmentExam;
    private List<Map> labExam;
    private List<Map> medicalExam;
    private List<Map> otherExam;
    private String coverImage;
    private BigDecimal originalPrice;
    private BigDecimal currentPrice;
    private String packageType;
    private List<String> tags;
    private String ruleName;
    private String ruleContent;
    private List<Map> examItems;
    @Indexed
    private String md5Hash;
}
