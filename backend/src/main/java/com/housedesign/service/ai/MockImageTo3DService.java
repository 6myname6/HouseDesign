package com.housedesign.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.housedesign.entity.DesignStyle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 内置降级实现：不依赖任何外部服务，根据设计图"分析"出一个程序化户型场景，
 * 并按用户所选的装修风格套用对应的配色方案（墙 / 地板 / 木作 / 软装 / 点缀），
 * 前端使用 Three.js 依据 sceneConfig 渲染出可交互的 3D 施工效果。
 * 便于在未配置真实 AI Key 时，整条链路即开即用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockImageTo3DService implements ImageTo3DService {

    private final ObjectMapper objectMapper;

    @Override
    public String provider() {
        return "mock";
    }

    /** 生成程序化户型场景：读取设计图作为随机种子，按风格配色组装房间与家具，输出 sceneConfig。 */
    @Override
    public GenerationOutput generate(GenerationContext context) throws Exception {
        // 用文件内容 hash 作为随机种子，保证同一张图结果稳定、不同图结果有差异
        long seed = 42L;
        try {
            if (context.getImageAbsolutePath() != null && Files.exists(context.getImageAbsolutePath())) {
                byte[] bytes = Files.readAllBytes(context.getImageAbsolutePath());
                seed = 0;
                for (int i = 0; i < Math.min(bytes.length, 4096); i++) {
                    seed = seed * 31 + bytes[i];
                }
            }
        } catch (Exception e) {
            log.warn("读取设计图失败，使用默认种子: {}", e.getMessage());
        }

        DesignStyle style = DesignStyle.fromCode(context.getStyle());
        log.info("按风格生成户型: style={}", style.getDisplayName());

        String sceneConfig = objectMapper.writeValueAsString(buildScene(new Random(seed), style));

        // 模拟生成耗时
        Thread.sleep(1500);

        return GenerationOutput.builder()
                .provider(provider())
                .modelUrl(null) // 程序化渲染，无 glb 文件
                .previewImageUrl(context.getImageUrl())
                .sceneConfig(sceneConfig)
                .build();
    }

    /** 组装完整场景：房间布局 + 家具摆放 + 配色，返回可序列化的 Map。 */
    private Map<String, Object> buildScene(Random rnd, DesignStyle style) {
        String wall = style.getWallColor();
        String floor = style.getFloorColor();
        String wood = style.getWoodColor();
        String fabric = style.getFabricColor();
        String accent = style.getAccentColor();
        // 厨卫使用偏冷的中性色，与风格主色形成对比
        String coolWall = mix(wall, "#e6ecef", 0.5);
        String coolFloor = mix(floor, "#c8ced2", 0.55);

        double livingW = 4.5 + rnd.nextInt(3);   // 4.5 ~ 7.5
        double livingD = 3.5 + rnd.nextInt(2);    // 3.5 ~ 5.5
        double bedW = 3.0 + rnd.nextInt(2);
        double bedD = 3.0 + rnd.nextInt(2);
        double height = 2.8;

        List<Map<String, Object>> rooms = new ArrayList<>();
        rooms.add(room(0, "客厅", 0, 0, livingW, livingD, height, wall, floor));
        rooms.add(room(1, "主卧", livingW + 0.1, 0, bedW, bedD, height, wall, mix(floor, wood, 0.35)));
        rooms.add(room(2, "厨房", 0, livingD + 0.1, livingW * 0.55, 2.6, height, coolWall, coolFloor));
        rooms.add(room(3, "卫生间", livingW * 0.55 + 0.1, livingD + 0.1, livingW * 0.45, 2.6, height, coolWall, coolFloor));
        linkRooms(rooms);

        List<Map<String, Object>> furniture = new ArrayList<>();
        // 客厅
        furniture.add(item("sofa", "客厅", livingW * 0.3, livingD * 0.75, 0, fabric));
        furniture.add(item("tv_cabinet", "客厅", livingW * 0.3, 0.4, 0, wood));
        furniture.add(item("coffee_table", "客厅", livingW * 0.3, livingD * 0.5, 0, wood));
        furniture.add(item("plant", "客厅", livingW * 0.85, livingD * 0.85, 0, "#3f7d3f"));
        furniture.add(item("rug", "客厅", livingW * 0.3, livingD * 0.55, 0, mix(fabric, accent, 0.4)));
        // 主卧
        furniture.add(item("bed", "主卧", livingW + 0.1 + bedW * 0.5, bedD * 0.55, 0, mix(fabric, "#ffffff", 0.25)));
        furniture.add(item("wardrobe", "主卧", livingW + 0.1 + bedW * 0.85, bedD * 0.3, 0, wood));
        furniture.add(item("nightstand", "主卧", livingW + 0.1 + bedW * 0.15, bedD * 0.4, 0, wood));
        // 厨房
        furniture.add(item("kitchen_counter", "厨房", livingW * 0.55 * 0.5, livingD + 0.1 + 0.4, 0, mix(wood, "#9e9e9e", 0.5)));
        // 卫生间
        furniture.add(item("bathtub", "卫生间", livingW * 0.55 + 0.1 + livingW * 0.45 * 0.5, livingD + 0.1 + 1.6, 0, "#eceff1"));

        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("type", "procedural-apartment");
        scene.put("unit", "meter");
        scene.put("style", style.getCode());
        scene.put("styleLabel", style.getDisplayName());
        scene.put("palette", palette(wall, floor, wood, fabric, accent));
        scene.put("wallHeight", height);
        scene.put("wallColor", wall);
        scene.put("rooms", rooms);
        scene.put("furniture", furniture);
        return scene;
    }

    /** 构建配色板（墙/地/木/软装/点缀五色）。 */
    private Map<String, Object> palette(String wall, String floor, String wood, String fabric, String accent) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("wall", wall);
        m.put("floor", floor);
        m.put("wood", wood);
        m.put("fabric", fabric);
        m.put("accent", accent);
        return m;
    }

    /** 构造一个房间矩形（含位置、尺寸、墙地色）。 */
    private Map<String, Object> room(int id, String name, double x, double z, double w, double d,
                                     double h, String wallColor, String floorColor) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("x", round(x));
        m.put("z", round(z));
        m.put("width", round(w));
        m.put("depth", round(d));
        m.put("height", round(h));
        m.put("wallColor", wallColor);
        m.put("floorColor", floorColor);
        return m;
    }

    /** 根据房间矩形布局计算相邻关系，写入每个房间的 connections（相邻房间 id 列表），供前端 VR 热点跳转。 */
    private void linkRooms(List<Map<String, Object>> rooms) {
        for (Map<String, Object> a : rooms) {
            List<Integer> conn = new ArrayList<>();
            for (Map<String, Object> b : rooms) {
                if (a == b) continue;
                if (adjacent(a, b)) conn.add((Integer) b.get("id"));
            }
            a.put("connections", conn);
        }
    }

    /** 判断两个房间矩形是否相邻（共边或间距在阈值内）。 */
    private boolean adjacent(Map<String, Object> a, Map<String, Object> b) {
        double ax = (Double) a.get("x"), az = (Double) a.get("z");
        double aw = (Double) a.get("width"), ad = (Double) a.get("depth");
        double bx = (Double) b.get("x"), bz = (Double) b.get("z");
        double bw = (Double) b.get("width"), bd = (Double) b.get("depth");
        double gap = 0.25;
        boolean xOverlap = Math.max(ax, bx) <= Math.min(ax + aw, bx + bw) + 1e-6;
        boolean zOverlap = Math.max(az, bz) <= Math.min(az + ad, bz + bd) + 1e-6;
        boolean xClose = Math.abs((ax + aw) - bx) <= gap || Math.abs((bx + bw) - ax) <= gap;
        boolean zClose = Math.abs((az + ad) - bz) <= gap || Math.abs((bz + bd) - az) <= gap;
        return (xOverlap && zClose) || (zOverlap && xClose);
    }

    /** 构造一件家具的占位描述（类型/所属房间/坐标/朝向/颜色）。 */
    private Map<String, Object> item(String type, String room, double x, double z, double rotation, String color) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("room", room);
        m.put("x", round(x));
        m.put("z", round(z));
        m.put("rotation", rotation);
        m.put("color", color);
        return m;
    }

    /** 坐标保留两位小数，便于前端解析。 */
    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * 按比例混合两个 #rrggbb 颜色。ratio 为 b 的占比（0~1）。
     */
    private String mix(String a, String b, double ratio) {
        try {
            int[] ca = hex(a);
            int[] cb = hex(b);
            int r = (int) Math.round(ca[0] * (1 - ratio) + cb[0] * ratio);
            int g = (int) Math.round(ca[1] * (1 - ratio) + cb[1] * ratio);
            int bl = (int) Math.round(ca[2] * (1 - ratio) + cb[2] * ratio);
            return String.format("#%02x%02x%02x", clamp(r), clamp(g), clamp(bl));
        } catch (Exception e) {
            return a;
        }
    }

    /** 解析 #rrggbb 为 RGB 三个分量。 */
    private int[] hex(String h) {
        String s = h.startsWith("#") ? h.substring(1) : h;
        return new int[]{
                Integer.parseInt(s.substring(0, 2), 16),
                Integer.parseInt(s.substring(2, 4), 16),
                Integer.parseInt(s.substring(4, 6), 16)
        };
    }

    /** 将颜色分量限制在 0-255 范围。 */
    private int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
