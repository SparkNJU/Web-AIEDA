declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'element-plus/es/locale/lang/zh-cn' {
  import type { Language } from 'element-plus/es/locale'
  const locale: Language
  export default locale
}

declare module 'element-plus/dist/locale/zh-cn.mjs' {
  import type { Language } from 'element-plus/es/locale'
  const locale: Language
  export default locale
}
