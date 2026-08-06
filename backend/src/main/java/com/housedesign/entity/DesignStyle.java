package com.housedesign.entity;

/**
 * 装修风格枚举。
 *
 * 每种风格自带一套调色板（墙面 / 地板 / 木作 / 软装 / 点缀），
 * 供内置降级服务（MockImageTo3DService）程序化生成对应风格的户型效果；
 * 同时携带一段英文贴图提示（texturePrompt），供真实外部 AI 服务作为风格引导。
 */
public enum DesignStyle {

    MODERN_MINIMALIST(
            "modern-minimalist", "现代简约",
            "#f3f3f1", "#cfc8bd", "#8d8378", "#9aa0a6", "#b0bec5",
            "modern minimalist interior, neutral gray and white tones, clean straight lines, matte finishes, uncluttered"),

    CREAM_FRENCH(
            "cream-french", "奶油轻法式",
            "#f8f2e8", "#e0cba8", "#c9a86a", "#e8d9c3", "#b98a5e",
            "cream light French style interior, warm ivory and beige tones, soft fabrics, arched shapes, vintage gold accents, elegant"),

    ITALIAN_LUXURY(
            "italian-luxury", "现代/意式极简轻奢",
            "#e8e4de", "#c2b7a8", "#4a4038", "#6b7169", "#c0a062",
            "modern Italian minimalist light-luxury interior, high-grade gray, dark wood, marble textures, champagne gold metal accents"),

    NEW_CHINESE(
            "new-chinese", "轻量化新中式",
            "#f2ece0", "#b98a5e", "#6b4f3a", "#6e7b74", "#a88b4a",
            "light modern Chinese (new Chinese) style interior, walnut wood, ink gray and celadon tones, brass accents, zen and airy"),

    LOG_WOOD(
            "log-wood", "原木风",
            "#f6f1e9", "#d8b98c", "#b8925e", "#cbb79a", "#7a9a5b",
            "Japanese log wood (muji) style interior, natural oak wood tones, warm beige linen fabrics, green plants, cozy and bright");

    private final String code;
    private final String displayName;
    /** 墙面颜色 */
    private final String wallColor;
    /** 主要地板颜色 */
    private final String floorColor;
    /** 木作颜色（衣柜、床架、电视柜等） */
    private final String woodColor;
    /** 软装颜色（沙发、床品、地毯等） */
    private final String fabricColor;
    /** 点缀色（绿植、装饰、金属点缀） */
    private final String accentColor;
    /** 外部 AI 服务的英文风格贴图提示 */
    private final String texturePrompt;

    DesignStyle(String code, String displayName, String wallColor, String floorColor,
                String woodColor, String fabricColor, String accentColor, String texturePrompt) {
        this.code = code;
        this.displayName = displayName;
        this.wallColor = wallColor;
        this.floorColor = floorColor;
        this.woodColor = woodColor;
        this.fabricColor = fabricColor;
        this.accentColor = accentColor;
        this.texturePrompt = texturePrompt;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getWallColor() {
        return wallColor;
    }

    public String getFloorColor() {
        return floorColor;
    }

    public String getWoodColor() {
        return woodColor;
    }

    public String getFabricColor() {
        return fabricColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getTexturePrompt() {
        return texturePrompt;
    }

    /** 根据 code 解析风格，非法或为空时回退到现代简约。 */
    public static DesignStyle fromCode(String code) {
        if (code != null) {
            for (DesignStyle s : values()) {
                if (s.code.equalsIgnoreCase(code)) {
                    return s;
                }
            }
        }
        return MODERN_MINIMALIST;
    }
}
