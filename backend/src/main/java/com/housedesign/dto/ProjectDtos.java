package com.housedesign.dto;

import com.housedesign.entity.DesignProject;
import com.housedesign.entity.DesignStyle;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目相关 DTO。
 */
public class ProjectDtos {

    @Data
    public static class ProjectResponse {
        private Long id;
        private String name;
        private String description;
        /** 装修风格 code */
        private String style;
        /** 装修风格中文名 */
        private String styleLabel;
        private String designImageUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

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
