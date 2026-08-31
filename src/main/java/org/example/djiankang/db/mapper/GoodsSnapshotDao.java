package org.example.djiankang.db.mapper;

import lombok.RequiredArgsConstructor;
import org.example.djiankang.db.entity.GoodsSnapshotEntity;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GoodsSnapshotDao {
    private final MongoTemplate mongoTemplate;

    /**
     * 根据md5Hash判断是否存在商品快照
     *
     * @param md5Hash 商品快照的MD5哈希值
     * @return 存在则返回快照ID，不存在则返回null
     */
    public String hasGoodsSnapshot(String md5Hash) {
        // 构建查询条件：根据md5Hash字段精确匹配
        Query query = new Query(Criteria.where("md5Hash").is(md5Hash)).limit(1); // 只查询第一条记录，提高查询效率

        // 执行查询，返回单个实体对象
        GoodsSnapshotEntity entity = mongoTemplate.findOne(query, GoodsSnapshotEntity.class);

        // 如果找到记录则返回其主键_id，否则返回null
        return entity != null ? entity.get_id() : null;
    }

    /**
     * 保存或更新快照信息
     * 当entity的_id为null时执行插入，否则执行更新
     *
     * @param entity 商品快照实体
     * @return 保存后的快照ID
     */
    public String insert(GoodsSnapshotEntity entity) {
        // 使用save方法：自动判断插入或更新，返回保存后的实体主键
        return mongoTemplate.save(entity).get_id();
    }
}