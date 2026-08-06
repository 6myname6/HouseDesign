# 前端单元测试模式 (Vue 3 + Vitest)

前端测试文件命名：`*.test.js` / `*.spec.js`，放在对应模块旁或 `src/__tests__/`。
配置由技能的 `scripts/setup.py` 生成 `vitest.config.js`（环境 jsdom、v8 覆盖率）。

## 1. 纯函数 / 工具（最简单，无需 DOM）

```js
// src/utils/format.js
export function formatArea(sqm) {
  if (sqm == null || Number.isNaN(sqm)) return '-'
  return `${Number(sqm).toFixed(2)} ㎡`
}
```

```js
// src/utils/format.test.js
import { describe, it, expect } from 'vitest'
import { formatArea } from './format'

describe('formatArea', () => {
  it('正常数值保留两位小数', () => {
    expect(formatArea(12.345)).toBe('12.35 ㎡')
  })
  it('null/NaN 返回占位符', () => {
    expect(formatArea(null)).toBe('-')
    expect(formatArea(NaN)).toBe('-')
  })
})
```

## 2. Vue 组件（用 @vue/test-utils 挂载）

```js
// src/components/AreaInput.vue
<template>
  <input :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />
</template>
<script setup>
defineProps(['modelValue'])
defineEmits(['update:modelValue'])
</script>
```

```js
// src/components/AreaInput.test.js
import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import AreaInput from './AreaInput.vue'

describe('AreaInput', () => {
  it('输入时触发 update:modelValue', async () => {
    const w = mount(AreaInput, { props: { modelValue: '' } })
    await w.find('input').setValue('30')
    expect(w.emitted('update:modelValue')[0]).toEqual(['30'])
  })
})
```

要点：
- `mount` 渲染组件；`find`/`trigger`/`setValue` 模拟交互。
- 需要 Pinia / Router 时，用 `global.plugins` 注入测试用实例。
- `jsdom` 环境已配好，可直接测试 DOM 行为。

## 3. 运行与覆盖率

- 运行：`npm --prefix frontend run test`（单次）或 `test:coverage`（带覆盖率）。
- 覆盖率报告：`frontend/coverage/index.html` 与 `frontend/coverage/coverage-summary.json`。
- Vitest 默认只跑匹配 `*.test.*` / `*.spec.*` 的文件。

## 4. 常见坑

- 组件里用了 `import.meta.env` 或图片资源，可在 `vitest.config.js` 的 `test` 里加 `server.deps` 或 `alias` 处理。
- 涉及 `three.js` 的 3D 组件测试成本高，优先测纯逻辑与数据层，3D 渲染用 e2e 覆盖。
- 若前端还没装 Vitest，先运行技能的 `scripts/setup.py`。
