// 首页下半部分「装修干货」内容（静态示例数据，后续可替换为后端接口）
export const FEED_CATEGORIES = [
  { key: 'all', label: '全部' },
  { key: 'term', label: '术语解释' },
  { key: 'guide', label: '装修流程' },
  { key: 'experience', label: '避坑经验' },
  { key: 'communication', label: '沟通技巧' }
]

export const FEED_POSTS = [
  {
    id: 1,
    type: 'term',
    emoji: '🛋️',
    cover: 'linear-gradient(135deg, #2b2b2b, #5a4a3a)',
    title: '意式风格：极简线条里的高级感',
    excerpt: '意式（尤其是意式极简 / 轻奢）强调干净的体块关系与高级材质，常用深胡桃木、岩板、金属与皮革。',
    author: '筑梦家编辑部',
    date: '2026-06-12',
    readCount: 3280,
    content:
      '意式风格不等于"冷"。它追求的是"克制的奢华"：用大面积的留白、低饱和的中性色（米灰、暖咖、墨黑）打底，再用岩板、真皮、拉丝金属做点睛。\n\n落地建议：\n· 柜体尽量做到顶、隐藏把手，强调整体感；\n· 地面通铺同色系，减少拼缝切割；\n· 灯光以 3000K 暖光为主，见光不见灯。'
  },
  {
    id: 2,
    type: 'term',
    emoji: '🧱',
    cover: 'linear-gradient(135deg, #f6d365, #fda085)',
    title: '美缝：别再让瓷砖缝发黑发霉',
    excerpt: '美缝是铺砖后用环氧树脂等专用材料填充缝隙的工艺，相比传统填缝剂更防霉、耐污、颜色更统一。',
    author: '老周施工队',
    date: '2026-05-28',
    readCount: 5120,
    content:
      '什么时候做美缝？建议在瓷砖铺贴完成、彻底干透（一般 3–7 天）后进行，且要在橱柜、洁具安装前做完。\n\n常见误区：\n· 只做地面不做墙面——厨房、卫生间墙面同样需要；\n· 选太浅的颜色配深色砖，容易显脏；\n· 潮湿环境务必用环氧彩砂，耐水防霉更好。'
  },
  {
    id: 3,
    type: 'guide',
    emoji: '📋',
    cover: 'linear-gradient(135deg, #a1c4fd, #c2e9fb)',
    title: '装修一般流程：从量房到软装的全景图',
    excerpt: '量房 → 设计 → 拆改 → 水电 → 泥瓦 → 木工 → 油漆 → 安装 → 软装，循序渐进别跳步。',
    author: '装修百事通',
    date: '2026-06-01',
    readCount: 8940,
    content:
      '标准流程（毛坯为例）：\n1. 量房 & 需求确认\n2. 方案设计、出施工图与效果图\n3. 拆改（非承重墙）\n4. 水电隐蔽工程（最关键的阶段）\n5. 防水 + 泥瓦（贴砖、找平）\n6. 木工（吊顶、柜体）\n7. 油漆（刮腻子、刷漆）\n8. 主材安装（门、地板、橱柜、洁具、灯具）\n9. 软装进场、通风\n\n记住：水电和防水一旦封起来就很难改，这两个节点一定要自己验收。'
  },
  {
    id: 4,
    type: 'communication',
    emoji: '💬',
    cover: 'linear-gradient(135deg, #d4fc79, #96e6a1)',
    title: '和施工工人怎么沟通，才不返工',
    excerpt: '把模糊的口头需求落到图纸和清单上，给工人"可执行的指令"，而不是"感觉"。',
    author: '项目经理老王',
    date: '2026-06-08',
    readCount: 4210,
    content:
      '工人不是设计师，越具体越好：\n· 用"插座离地 30cm、床尾留 1.2m 通道"代替"这里装个插座"；\n· 关键节点带着图纸到现场一起核对，拍照留底；\n· 改动书面确认（微信也行），避免后期扯皮；\n· 尊重专业建议，但涉及安全与预算的节点自己拍板。\n\n态度上：专业、明确、不卑不亢，比"讨好"或"发火"都管用。'
  },
  {
    id: 5,
    type: 'experience',
    emoji: '🔧',
    cover: 'linear-gradient(135deg, #ff9a9e, #fecfef)',
    title: '水电验收避坑：这 5 个地方最易漏',
    excerpt: '水路打压、电路回路、强弱电间距、下水坡度、等电位——每一项都要亲眼确认。',
    author: '监理张工',
    date: '2026-05-20',
    readCount: 6730,
    content:
      '隐蔽工程验收清单：\n1. 水路：打压 0.8MPa 保压 30 分钟，掉压不超过 0.05MPa；\n2. 电路：按"照明 / 插座 / 空调 / 厨房 / 卫生间"分回路，大功率单独走；\n3. 强弱电交叉处包锡箔纸，间距 ≥30cm；\n4. 下水管留坡度（1–2%），倒水试流；\n5. 卫生间做等电位联结，安全关键。\n\n验收合格再封槽，封了再改要砸墙。'
  },
  {
    id: 6,
    type: 'term',
    emoji: '🍳',
    cover: 'linear-gradient(135deg, #fbc2eb, #a6c1ee)',
    title: '开放式厨房：好看，但先想清楚这三点',
    excerpt: '通透显大，但要面对油烟、收纳与燃气开通的现实问题。',
    author: '筑梦家编辑部',
    date: '2026-06-15',
    readCount: 2990,
    content:
      '适合人群：少爆炒、重社交、希望客厅餐厅一体化的家庭。\n\n三点提醒：\n· 油烟：上吸力强的烟机 + 隐形折叠门兜底；\n· 收纳：台面零杂物，藏起来的收纳要做足；\n· 燃气：部分城市开放式厨房不予开通燃气，签约前先问物业/燃气公司。'
  },
  {
    id: 7,
    type: 'guide',
    emoji: '🛒',
    cover: 'linear-gradient(135deg, #84fab0, #8fd3f4)',
    title: '主材购买顺序：别等工人催你才买',
    excerpt: '提前排好橱柜、门窗、砖、地板等的下单时间，避免工期被材料卡住。',
    author: '装修百事通',
    date: '2026-06-03',
    readCount: 5560,
    content:
      '关键节点（仅供参考，按合同调整）：\n· 开工前：中央空调 / 新风 / 地暖；\n· 水电阶段：橱柜第一次复尺、瓷砖、地漏；\n· 泥瓦阶段：门槛石、窗台石、美缝剂；\n· 木工前：室内门、集成吊顶；\n· 油漆后：地板、踢脚线、洁具、灯具、开关面板；\n· 尾期：家具软装。\n\n定制类（门、柜）周期长，越早定越稳。'
  },
  {
    id: 8,
    type: 'communication',
    emoji: '🔍',
    cover: 'linear-gradient(135deg, #fddb92, #d1fdff)',
    title: '竣工验收怎么验：带把空鼓锤就够了',
    excerpt: '空鼓、平整、密封、排水——自己也能查的简易验收法。',
    author: '监理张工',
    date: '2026-06-10',
    readCount: 3870,
    content:
      '随身带：空鼓锤、手电、卷尺、手机测距。\n\n查什么：\n· 瓷砖：轻敲听声，空鼓率单片不超过 5%、边角不能有；\n· 墙面：手电侧照看平整，无波浪；\n· 门窗：开关顺、密封条完整、锁扣对齐；\n· 地漏：倒水看是否快速排走、有无积水；\n· 所有下水：逐个试。\n\n问题列清单，整改复验后再结清尾款。'
  }
]

export const FEED_TAG_META = {
  term: { label: '术语解释', type: 'info' },
  guide: { label: '装修流程', type: 'success' },
  experience: { label: '避坑经验', type: 'warning' },
  communication: { label: '沟通技巧', type: 'danger' }
}
