declare module 'markdown-it-texmath' {
  import { PluginWithOptions } from 'markdown-it'
  
  interface TexmathOptions {
    engine?: any
    delimiters?: 'dollars' | 'brackets' | 'doxygen' | 'gitlab' | 'julia' | 'kramdown' | 'beg_end'
    katexOptions?: {
      throwOnError?: boolean
      errorColor?: string
      displayMode?: boolean
      macros?: Record<string, string>
      [key: string]: any
    }
    [key: string]: any
  }
  
  const texmath: PluginWithOptions<TexmathOptions>
  export default texmath
}
