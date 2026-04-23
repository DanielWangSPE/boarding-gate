package com.boardinggate.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private Long userId;

    private LocalDateTime loginTime;

    private String loginIp;

    private String userAgent;

    /** 1-成功 0-失败 */
    private Integer loginResult;

    private String failReason;

    private String sessionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
