/**
 * 装修风格常量（需与后端 com.housedesign.entity.DesignStyle 的 code 保持一致）。
 * colors 用于选择卡片的配色预览：[墙面, 地板, 木作, 软装, 点缀]
 */
export const DESIGN_STYLES = [
  {
    code: 'modern-minimalist',
    name: '现代简约',
    desc: '中性灰白、干净利落的线条与哑光质感',
    colors: ['#f3f3f1', '#cfc8bd', '#8d8378', '#9aa0a6', '#b0bec5']
  },
  {
    code: 'cream-french',
    name: '奶油轻法式',
    desc: '奶油米杏色调、柔和软装与复古金点缀',
    colors: ['#f8f2e8', '#e0cba8', '#c9a86a', '#e8d9c3', '#b98a5e']
  },
  {
    code: 'italian-luxury',
    name: '现代 / 意式极简轻奢',
    desc: '高级灰、深木与大理石，香槟金属点缀',
    colors: ['#e8e4de', '#c2b7a8', '#4a4038', '#6b7169', '#c0a062']
  },
  {
    code: 'new-chinese',
    name: '轻量化新中式',
    desc: '胡桃木、黛青雅灰与黄铜，禅意通透',
    colors: ['#f2ece0', '#b98a5e', '#6b4f3a', '#6e7b74', '#a88b4a']
  },
  {
    code: 'log-wood',
    name: '原木风',
    desc: '原木暖色、棉麻米色软装与绿植，温馨明亮',
    colors: ['#f6f1e9', '#d8b98c', '#b8925e', '#cbb79a', '#7a9a5b']
  }
]

/** 默认风格 code */
export const DEFAULT_STYLE = DESIGN_STYLES[0].code

/** 根据 code 取风格名 */
export function styleLabel(code) {
  const s = DESIGN_STYLES.find((x) => x.code === code)
  return s ? s.name : ''
}
