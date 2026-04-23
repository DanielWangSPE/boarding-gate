package com.boardinggate.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    private String realName;

    private String nickname;

    private String email;

    private String mobile;

    private String avatar;

    /** 1-启用 0-停用 */
    private Integer status;

    /** 1-需要强制改密 0-否 */
    private Integer forceChangePassword;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private LocalDateTime passwordUpdateTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    public boolean isEnabled() {
        return status != null && status == 1;
    }

    public boolean isForceChangePassword() {
        return forceChangePassword != null && forceChangePassword == 1;
    }
}
