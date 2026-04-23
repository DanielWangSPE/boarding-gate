package com.boardinggate.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充 createTime/updateTime/createBy/updateBy。
 * createBy/updateBy 若后续接入用户上下文，可替换为当前登录用户名。
 */
@Component
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    private static final String SYSTEM_USER = "system";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createBy", String.class, SYSTEM_USER);
        strictInsertFill(metaObject, "updateBy", String.class, SYSTEM_USER);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updateBy", String.class, SYSTEM_USER);
    }
}
