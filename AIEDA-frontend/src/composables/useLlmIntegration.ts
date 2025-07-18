// LLM集成的Vue Composable

interface ScriptLoader {
  loadScripts: () => Promise<void>
  cleanup: () => void
}

export function useLlmIntegration(): ScriptLoader {
  let scriptModules: HTMLScriptElement[] = []

  const cleanupGlobalVariables = () => {
    // 清理可能的全局变量和事件监听器
    const keysToCleanup = [
      'currentSessionId', 'isGenerating', 'currentStreamReader', 
      'currentStreamController', 'lastClickedButton', 'welcomeShown',
      'API_BASE', 'API_ENDPOINTS', 'CONFIG'
    ]
    keysToCleanup.forEach(key => {
      if ((window as any)[key] !== undefined) {
        delete (window as any)[key]
      }
    })
  }

  const loadScript = (src: string): Promise<void> => {
    return new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = src
      script.async = false
      script.onload = () => {
        console.log(`已加载脚本: ${src}`)
        resolve()
      }
      script.onerror = () => {
        console.error(`加载脚本失败: ${src}`)
        reject(new Error(`Failed to load script: ${src}`))
      }
      document.head.appendChild(script)
      scriptModules.push(script)
    })
  }

  const loadScripts = async (): Promise<void> => {
    const scripts = [
      '/src/front_end/script/config.js',
      '/src/front_end/script/ui.js', 
      '/src/front_end/script/session.js',
      '/src/front_end/script/message.js',
      '/src/front_end/script/file.js',
      '/src/front_end/index.js'
    ]
    
    try {
      // 按顺序加载脚本
      for (const script of scripts) {
        await loadScript(script)
      }
      
      console.log('所有脚本加载完成')
      
      // 延迟初始化以确保所有脚本都解析完成
      setTimeout(() => {
        initializeVueApp()
      }, 100)
    } catch (error) {
      console.error('加载脚本时出错:', error)
    }
  }

  const initializeVueApp = () => {
    try {
      console.log('开始初始化Vue应用...')
      
      const win = window as any
      
      // 在Vue组件环境中初始化原有的JavaScript功能
      if (typeof win.initializeApp === 'function') {
        win.initializeApp()
      } else {
        console.warn('initializeApp 函数未找到')
      }
      
      if (typeof win.bindEventListeners === 'function') {
        win.bindEventListeners()
      } else {
        console.warn('bindEventListeners 函数未找到')
      }
      
      console.log('Vue应用初始化完成')
    } catch (error) {
      console.error('初始化Vue应用时出错:', error)
    }
  }

  const cleanup = () => {
    // 清理脚本
    scriptModules.forEach(script => {
      if (script.parentNode) {
        script.parentNode.removeChild(script)
      }
    })
    scriptModules = []
    
    // 清理全局变量
    cleanupGlobalVariables()
  }

  return {
    loadScripts,
    cleanup
  }
}

// 导出函数包装器，用于在Vue组件中调用原始JavaScript函数
export function createFunctionWrapper(functionName: string) {
  return (...args: any[]) => {
    const win = window as any
    if (typeof win[functionName] === 'function') {
      return win[functionName](...args)
    } else {
      console.warn(`函数 ${functionName} 未找到或未准备好`)
    }
  }
}
