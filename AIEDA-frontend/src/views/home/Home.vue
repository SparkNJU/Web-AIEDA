<template>
  <div class="home">
    <!-- Hero Section -->
    <section id="home" class="hero">
      <div class="container">
        <div class="hero-content">
          <div class="hero-text fade-in">
            <h1>Orvix：从 <span class="highlight">自动化</span> 到 <span class="highlight">自主化</span> 的范式转移</h1>
            <h2 class="hero-subtitle">超越静态工作流，开启动态智能体时代</h2>
            <p>Orvix通过自然语言编译器与系统化思维建模，让工程师从"如何实现"中解放出来，专注于"做什么"，实现更高效、更安全、更智能的工程创新。</p>
            <div class="cta-buttons">
              <router-link to="/chat" class="btn btn-primary">立即试用</router-link>
              <a href="#contact" class="btn btn-secondary">联系我们</a>
            </div>
          </div>
          <div class="hero-visual fade-in delay-1">
            <div class="modules-grid">
              <div class="module-card">
                <h3>AI + 半导体</h3>
                <p>智能芯片设计<br>优化制造流程</p>
              </div>
              <div class="module-card">
                <h3>AI + 电气</h3>
                <p>智能电网管理<br>电力系统优化</p>
              </div>
              <div class="module-card">
                <h3>AI + 新能源</h3>
                <p>智能能源调度<br>绿色技术应用</p>
              </div>
              <div class="module-card">
                <h3>AI + 医药</h3>
                <p>智能药物研发<br>精准医疗诊断</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Architecture Advantages Section -->
    <section id="features" class="features">
      <div class="container">
        <div class="section-title fade-in">
          <h2>突破传统工作流的局限</h2>
        </div>
        <div class="features-grid">
          <div class="feature-card fade-in delay-1">
            <div class="feature-icon">💬</div>
            <h3>自然语言编译器</h3>
            <p class="feature-subtitle">一句话生成工业级方案</p>
            <p class="feature-description">通过自然语言实现工业级Agent设计，工程师无需学习复杂语法即可快速搭建高效智能体。</p>
          </div>
          <div class="feature-card fade-in delay-2">
            <div class="feature-icon">🧠</div>
            <h3>系统化工程思维</h3>
            <p class="feature-subtitle">顶尖专家思维模式</p>
            <p class="feature-description">帮助工程师从点状问题提升到完整科学的系统优化，实现从"经验解"到"最优解"。</p>
          </div>
          <div class="feature-card fade-in delay-3">
            <div class="feature-icon">🚀</div>
            <h3>角色解放</h3>
            <p class="feature-subtitle">从执行者到策略制定者</p>
            <p class="feature-description">传统工程实践中，80%的时间消耗在"如何实现"。平台自动化底层实现流程，让工程师专注于目标定义和策略创新。</p>
          </div>
          <div class="feature-card fade-in delay-1">
            <div class="feature-icon">🔍</div>
            <h3>自动化设计探索</h3>
            <p class="feature-subtitle">从试错到寻优的跨越</p>
            <p class="feature-description">让设计不再局限于有限经验点，而是探索完整全局空间，在芯片设计、工业制造等领域带来决定性优势。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Problems & Solutions Section -->
    <section id="about" class="about">
      <div class="container">
        <div class="section-title fade-in">
          <h2>我们解决了行业的核心难题</h2>
        </div>
        <div class="problems-grid">
          <div class="problem-card fade-in delay-1">
            <div class="problem-header">
              <h3>工业 Agent 设计门槛高</h3>
              <p class="problem-desc">需要复合型 AI 人才，学习/招聘成本居高不下。</p>
            </div>
            <div class="solution">
              <h4>解决方案：</h4>
              <p>专业工程师可用自然语言快速设计高效 Agent</p>
              <div class="value">
                <strong>客户价值：</strong> 快速落地，减少对稀缺 AI 人才依赖
              </div>
            </div>
          </div>
          <div class="problem-card fade-in delay-2">
            <div class="problem-header">
              <h3>重复造轮子，资源分散</h3>
              <p class="problem-desc">项目间工具割裂，沉淀难，开发成本高。</p>
            </div>
            <div class="solution">
              <h4>解决方案：</h4>
              <p>统一插件与工具管理平台，开箱即用</p>
              <div class="value">
                <strong>客户价值：</strong> 降低开发成本，提高效率
              </div>
            </div>
          </div>
          <div class="problem-card fade-in delay-3">
            <div class="problem-header">
              <h3>RPA / Copilot 灵活性不足</h3>
              <p class="problem-desc">RPA 维护成本高，Copilot 停留在"助手"层面，无法应对复杂流程。</p>
            </div>
            <div class="solution">
              <h4>解决方案：</h4>
              <p>多 Agent 协作 + 自主规划，支持端到端自动化</p>
              <div class="value">
                <strong>客户价值：</strong> 提升效率和准确性
              </div>
            </div>
          </div>
          <div class="problem-card fade-in delay-1">
            <div class="problem-header">
              <h3>安全与可控性要求高</h3>
              <p class="problem-desc">公有云 API 存在数据泄露与幻觉风险。</p>
            </div>
            <div class="solution">
              <h4>解决方案：</h4>
              <p>私有化部署 + 安全中间件 + 合规审计</p>
              <div class="value">
                <strong>客户价值：</strong> 数据安全可控，满足企业合规
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Use Cases Section -->
    <section id="usecases" class="features">
      <div class="container">
        <div class="section-title fade-in">
          <h2>适用场景</h2>
          <p>Orvix 平台已在多个行业成功应用，助力企业实现智能化转型。</p>
        </div>
        <div class="usecases-grid">
          <div class="usecase-card fade-in delay-1">
            <div class="usecase-icon">🏭</div>
            <h3>制造业</h3>
            <p>质检、监控、智能制造执行</p>
          </div>
          <div class="usecase-card fade-in delay-2">
            <div class="usecase-icon">🏢</div>
            <h3>金融</h3>
            <p>风控、报告生成、合规审计</p>
          </div>
          <div class="usecase-card fade-in delay-3">
            <div class="usecase-icon">🛍️</div>
            <h3>零售电商</h3>
            <p>客服、营销、库存管理</p>
          </div>
          <div class="usecase-card fade-in delay-1">
            <div class="usecase-icon">�</div>
            <h3>医疗</h3>
            <p>诊断辅助、病历分析、医疗影像</p>
          </div>
          <div class="usecase-card fade-in delay-2">
            <div class="usecase-icon">🎓</div>
            <h3>教育</h3>
            <p>智能辅导、作业批改、课程推荐</p>
          </div>
          <div class="usecase-card fade-in delay-3">
            <div class="usecase-icon">⚡</div>
            <h3>能源</h3>
            <p>设备监控、故障预测、能效优化</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Call to Action Section -->
    <section id="cta" class="about">
      <div class="container">
        <div class="cta-content fade-in">
          <h2>加入Orvix，开启AI Agent新时代</h2>
          <p>立即体验我们的AI Agent平台，让智能化成为您业务的核心竞争力。</p>
          <div class="cta-buttons">
            <router-link to="/chat" class="btn btn-primary">立即开始</router-link>
            <a href="#features" class="btn btn-secondary">了解更多</a>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer id="contact">
      <div class="container">
        <div class="footer-content">
          <div class="footer-section">
            <h3>公司信息</h3>
            <p>深圳市枢宇科技有限公司</p>
            <p>专注于智能体平台研发</p>
            <p>第三代AI+工业软件解决方案</p>
          </div>
          <div class="footer-section">
            <h3>产品服务</h3>
            <p><a href="#features">智能体平台</a></p>
            <p><a href="#features">AI+解决方案</a></p>
            <p><a href="#features">定制化开发</a></p>
            <p><a href="#features">技术咨询</a></p>
          </div>
          <div class="footer-section">
            <h3>联系方式</h3>
            <p>邮箱：jchanf@163.com</p>
            <p>地址：深圳市南山区</p>
          </div>
        </div>
        <div class="footer-bottom">
          <p>&copy; 2025 深圳市枢宇科技有限公司. 保留所有权利.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'

// 滚动动画设置
const observerOptions = {
  threshold: 0.1,
  rootMargin: '0px 0px -50px 0px'
}

let observer: IntersectionObserver | null = null

onMounted(() => {
  // 平滑滚动
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e: Event) {
      e.preventDefault()
      const element = e.target as HTMLAnchorElement
      const target = document.querySelector(element.getAttribute('href') || '')
      if (target) {
        target.scrollIntoView({
          behavior: 'smooth',
          block: 'start'
        })
      }
    })
  })

  // 滚动动画观察器
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        (entry.target as HTMLElement).style.opacity = '1';
        (entry.target as HTMLElement).style.transform = 'translateY(0)'
      }
    })
  }, observerOptions)

  document.querySelectorAll('.fade-in').forEach(el => {
    if (observer) observer.observe(el)
  })
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
})
</script>

<style scoped>
:root {
  --bg-primary: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%);
  --bg-secondary: rgba(0, 0, 0, 0.3);
  --bg-card: rgba(255, 255, 255, 0.05);
  --text-primary: #ffffff;
  --text-secondary: #cbd5e1;
  --text-muted: #94a3b8;
  --border-color: rgba(255, 255, 255, 0.1);
  --shadow-color: rgba(0, 0, 0, 0.3);
}

[data-theme="light"] {
  --bg-primary: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  --bg-secondary: rgba(255, 255, 255, 0.9);
  --bg-card: rgba(255, 255, 255, 0.95);
  --text-primary: #0f172a;
  --text-secondary: #334155;
  --text-muted: #64748b;
  --border-color: rgba(15, 23, 42, 0.1);
  --shadow-color: rgba(0, 0, 0, 0.08);
}

.home {
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  background: var(--bg-primary);
  color: var(--text-primary);
  line-height: 1.6;
  overflow-x: hidden;
  transition: all 0.3s ease;
  min-height: 100vh;
  padding-top: 80px; /* 桌面端避开Header */
  box-sizing: border-box;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Hero Section */
.hero {
  min-height: 100vh;
  display: flex;
  align-items: center;
  position: relative;
  background: radial-gradient(circle at 30% 50%, rgba(139, 92, 246, 0.1) 0%, transparent 50%),
              radial-gradient(circle at 70% 20%, rgba(59, 130, 246, 0.1) 0%, transparent 50%);
}

.hero-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: center;
}

.hero-text h1 {
  font-size: 3.5rem;
  font-weight: 800;
  margin-bottom: 20px;
  background: linear-gradient(45deg, var(--text-primary), #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.hero-text .highlight {
  background: linear-gradient(45deg, #8b5cf6, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.8rem;
  font-weight: 600;
  margin-bottom: 20px;
  color: var(--text-secondary);
  line-height: 1.3;
}

.hero-text p {
  font-size: 1.3rem;
  color: var(--text-secondary);
  margin-bottom: 30px;
  line-height: 1.8;
}

.cta-buttons {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  justify-content: center;
}

.btn {
  padding: 15px 30px;
  border: none;
  border-radius: 50px;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-block;
  text-align: center;
}

.btn-primary {
  background: linear-gradient(45deg, #8b5cf6, #3b82f6);
  color: white;
  box-shadow: 0 10px 30px rgba(139, 92, 246, 0.3);
}

.btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 15px 40px rgba(139, 92, 246, 0.4);
}

.btn-secondary {
  background: transparent;
  color: var(--text-primary);
  border: 2px solid #8b5cf6;
}

.btn-secondary:hover {
  background: #8b5cf6;
  color: #ffffff;
  transform: translateY(-3px);
}

.hero-visual {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* Modules Grid */
.modules-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 25px;
  max-width: 600px;
}

.module-card {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 35px;
  text-align: center;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.module-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.1), transparent);
  transition: left 0.5s ease;
}

.module-card:hover::before {
  left: 100%;
}

.module-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(139, 92, 246, 0.2);
  border-color: rgba(139, 92, 246, 0.3);
}

.module-card h3 {
  font-size: 1.4rem;
  margin-bottom: 20px;
  background: linear-gradient(45deg, #8b5cf6, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 600;
}

.module-card p {
  color: var(--text-secondary);
  font-size: 1rem;
  line-height: 1.5;
}

/* Features Section */
.features {
  padding: 100px 0;
  background: var(--bg-secondary);
}

.section-title {
  text-align: center;
  margin-bottom: 60px;
}

.section-title h2 {
  font-size: 2.5rem;
  margin-bottom: 20px;
  background: linear-gradient(45deg, var(--text-primary), #e0e7ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.section-title p {
  font-size: 1.2rem;
  color: var(--text-secondary);
  max-width: 600px;
  margin: 0 auto;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.feature-card {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.1), transparent);
  transition: left 0.5s ease;
}

.feature-card:hover::before {
  left: 100%;
}

.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 20px 40px rgba(139, 92, 246, 0.2);
}

.feature-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(45deg, #8b5cf6, #3b82f6);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  font-size: 2rem;
  box-shadow: 0 10px 30px rgba(139, 92, 246, 0.3);
}

.feature-card h3 {
  font-size: 1.5rem;
  margin-bottom: 15px;
  color: var(--text-primary);
}

.feature-card p {
  color: var(--text-secondary);
  line-height: 1.6;
}

.feature-subtitle {
  font-size: 1.1rem;
  font-weight: 600;
  color: #8b5cf6;
  margin-bottom: 15px;
  line-height: 1.4;
}

.feature-description {
  font-size: 0.95rem;
  color: #94a3b8;
  line-height: 1.6;
  margin-top: 10px;
}

/* About Section */
.about {
  padding: 100px 0;
  background: var(--bg-primary);
}

/* Problems & Solutions Section */
.problems-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 30px;
  margin-top: 40px;
}

.problem-card {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 30px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.problem-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.1), transparent);
  transition: left 0.5s ease;
}

.problem-card:hover::before {
  left: 100%;
}

.problem-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(139, 92, 246, 0.2);
}

.problem-header h3 {
  font-size: 1.4rem;
  margin-bottom: 10px;
  color: var(--text-primary);
  font-weight: 600;
}

.problem-desc {
  color: var(--text-muted);
  font-size: 0.9rem;
  margin-bottom: 20px;
  line-height: 1.5;
}

.solution {
  border-top: 1px solid var(--border-color);
  padding-top: 20px;
}

.solution h4 {
  color: #8b5cf6;
  font-size: 1.1rem;
  margin-bottom: 10px;
  font-weight: 600;
}

.solution p {
  color: var(--text-secondary);
  font-size: 1rem;
  margin-bottom: 15px;
  line-height: 1.5;
}

.value {
  background: rgba(139, 92, 246, 0.1);
  padding: 12px 15px;
  border-radius: 10px;
  border-left: 3px solid #8b5cf6;
  color: var(--text-primary);
  font-size: 0.9rem;
  line-height: 1.4;
}

.value strong {
  color: #8b5cf6;
}

/* Use Cases Section */
.usecases-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 30px;
  margin-top: 40px;
}

.usecase-card {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 30px;
  text-align: center;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.usecase-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.1), transparent);
  transition: left 0.5s ease;
}

.usecase-card:hover::before {
  left: 100%;
}

.usecase-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(139, 92, 246, 0.2);
}

.usecase-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(45deg, #8b5cf6, #3b82f6);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  font-size: 2rem;
  box-shadow: 0 10px 30px rgba(139, 92, 246, 0.3);
}

.usecase-card h3 {
  font-size: 1.5rem;
  margin-bottom: 15px;
  color: var(--text-primary);
}

.usecase-card p {
  color: var(--text-secondary);
  line-height: 1.6;
}

/* CTA Section */
.cta-content {
  text-align: center;
  max-width: 800px;
  margin: 0 auto;
}

.cta-content h2 {
  font-size: 2.5rem;
  margin-bottom: 20px;
  background: linear-gradient(45deg, var(--text-primary), #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.cta-content p {
  font-size: 1.2rem;
  color: var(--text-secondary);
  margin-bottom: 30px;
}

/* Footer */
footer {
  background: var(--bg-secondary);
  padding: 60px 0 30px;
  border-top: 1px solid var(--border-color);
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 40px;
  margin-bottom: 30px;
}

.footer-section h3 {
  font-size: 1.2rem;
  margin-bottom: 15px;
  color: var(--text-primary);
}

.footer-section p,
.footer-section a {
  color: var(--text-secondary);
  text-decoration: none;
  line-height: 1.8;
}

.footer-section a:hover {
  color: #8b5cf6;
}

.footer-bottom {
  border-top: 1px solid var(--border-color);
  padding-top: 20px;
  text-align: center;
  color: var(--text-muted);
}

/* Animations */
.fade-in {
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.6s ease;
}

.delay-1 { animation-delay: 0.2s; }
.delay-2 { animation-delay: 0.4s; }
.delay-3 { animation-delay: 0.6s; }

/* Responsive Design */
@media (max-width: 768px) {
  .home {
    padding-top: 60px; /* 移动端避开Header */
  }
  
  .hero-content {
    grid-template-columns: 1fr;
    gap: 40px;
  }
  
  .hero-text h1 {
    font-size: 2.5rem;
  }
  
  .hero-subtitle {
    font-size: 1.4rem;
  }
  
  .features-grid {
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  }
  
  .problems-grid {
    grid-template-columns: 1fr;
  }
  
  .modules-grid {
    grid-template-columns: 1fr;
  }
  
  .cta-buttons {
    flex-direction: column;
    align-items: center;
  }
}
</style>