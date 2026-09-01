package com.housedesign.dto;

import com.housedesign.entity.DesignProject;
import com.housedesign.entity.DesignStyle;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目相关 DTO，封装设计项目的响应对象。
 */
public class ProjectDtos {

    /** 项目详情响应。 */
    @Data
    public static class ProjectResponse {
        /** 项目 ID。 */
        private Long id;
        /** 项目名称。 */
        private String name;
        /** 项目描述。 */
        private String description;
        /** 装修风格 code（如 modern-minimalist）。 */
        private String style;
        /** 装修风格中文名（由 code 反查得到）。 */
        private String styleLabel;
        /** 设计图可访问 URL。 */
        private String designImageUrl;
        /** 创建时间。 */
        private LocalDateTime createdAt;
        /** 更新时间。 */
        private LocalDateTime updatedAt;

        /** 由项目实体转换为响应 DTO，并补全风格中文名。 */
        public static ProjectResponse from(DesignProject p) {
            ProjectResponse r = new ProjectResponse();
            r.id = p.getId();
            r.name = p.getName();
            r.description = p.getDescription();
            DesignStyle s = DesignStyle.fromCode(p.getStyle());
            r.style = s.getCode();
            r.styleLabel = s.getDisplayName();
            r.designImageUrl = p.getDesignImageUrl();
            r.createdAt = p.getCreatedAt();
            r.updatedAt = p.getUpdatedAt();
            return r;
        }
    }
}
