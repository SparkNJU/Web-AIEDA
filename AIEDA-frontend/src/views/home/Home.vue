<template>
  <div class="home">    
    <div class="hero">
      <h1>全链路仿真EDA</h1>
      <p>AI赋能的电子设计自动化平台</p>
    </div>
    
    <div class="features">
      <div class="feature-card">
        <h2>AI赋能</h2>
        <p>利用人工智能技术提升EDA工具性能</p>
      </div>
      <div class="feature-card">
        <h2>实测数据</h2>
        <p>基于真实数据的验证与分析</p>
      </div>
      <div class="feature-card">
        <h2>系统级仿真</h2>
        <p>全面的系统级仿真能力</p>
      </div>
      <div class="feature-card">
        <h2>信号输入</h2>
        <p>多样化的信号输入接口</p>
      </div>
    </div>
    
    <div class="simulation-flow">
      <h2>仿真流程</h2>
      <div class="flow-container">
        <div class="flow-item">模块级仿真</div>
        <div class="flow-arrow">→</div>
        <div class="flow-item">电路仿真</div>
        <div class="flow-arrow">→</div>
        <div class="flow-item">模型构建</div>
        <div class="flow-arrow">→</div>
        <div class="flow-item">信号输出</div>
      </div>
    </div>
    
    <div class="tech-section">
      <h2>核心技术</h2>
      <div class="tech-grid">
        <div class="tech-item">模块级仿真传递函数</div>
        <div class="tech-item">材料学仿真DFT</div>
        <div class="tech-item">FDTD</div>
        <div class="tech-item">TCAD器件级仿真</div>
        <div class="tech-item">模块级仿真</div>
        <div class="tech-item">电路仿真</div>
      </div>
    </div>
    
    <footer class="footer">
      <p>清华大学深圳国际研究生院</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
// 不再需要导入Header
</script>

<style scoped>
.home {
  font-family: Arial, sans-serif;
  color: #333;
}

.hero {
  text-align: center;
  padding: 3rem 1rem;
  background-color: rgba(102, 8, 116, 0.05);
}

.hero h1 {
  color: rgb(102, 8, 116);
  font-size: 2.5rem;
  margin-bottom: 1rem;
}

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 2rem;
  padding: 2rem;
}

.feature-card {
  padding: 1.5rem;
  border-radius: 8px;
  background-color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.feature-card h2 {
  color: rgb(102, 8, 116);
  margin-top: 0;
}

.simulation-flow {
  padding: 2rem;
  background-color: #f9f9f9;
}

.simulation-flow h2 {
  text-align: center;
  color: rgb(102, 8, 116);
}

.flow-container {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 2rem;
}

.flow-item {
  padding: 1rem;
  background-color: rgb(102, 8, 116);
  color: white;
  border-radius: 4px;
  text-align: center;
}

.flow-arrow {
  margin: 0 1rem;
  color: rgb(102, 8, 116);
  font-weight: bold;
}

.tech-section {
  padding: 2rem;
}

.tech-section h2 {
  text-align: center;
  color: rgb(102, 8, 116);
}

.tech-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-top: 2rem;
}

.tech-item {
  padding: 1rem;
  background-color: rgba(102, 8, 116, 0.1);
  border-radius: 4px;
  text-align: center;
}

.footer {
  text-align: center;
  padding: 2rem;
  margin-top: 2rem;
  background-color: #333;
  color: white;
}
</style>