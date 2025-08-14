<template>
  <div class="home">
    <!-- 头图 -->
    <section class="hero text-center">
      <img src="@/assets/cppo_background.png" alt="Background" class="hero-background" />
      <div class="hero-content">
        <h1 class="display-3 fw-bold">智慧新药开发 · 提升临床治疗</h1>
      </div>
    </section>

    <!-- 核心数据 -->
    <section class="container my-5 text-center">
      <h2 class="section-title">核心数据</h2>
      <div class="stats-container">
        <div class="stat-card">
          <h3>50+</h3>
          <p>1类新药支持</p>
        </div>
        <div class="stat-card">
          <h3>11</h3>
          <p>FIC创新药</p>
        </div>
        <div class="stat-card">
          <h3>7</h3>
          <p>新药获批上市</p>
        </div>
        <div class="stat-card">
          <h3>20+</h3>
          <p>临床试验通过伦理</p>
        </div>
        <div class="stat-card">
          <h3>5</h3>
          <p>重点研究领域</p>
        </div>
        <div class="stat-card">
          <h3>3</h3>
          <p>技术单元</p>
        </div>
      </div>
    </section>

    <!-- 联系方式 -->
    <footer class="contact-footer">
      <div class="container text-center">
        <p>地址：北京市海淀区花园北路49号 北京大学第三医院</p>
        <p>Email: example@bjmu.edu.cn | 电话：010-12345678</p>
        <p>© 2025 CPPO 北京大学第三医院</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
// 不需要导入背景图片，直接在CSS中使用
</script>

<style scoped>
.home {
  font-family: 'Noto Sans SC', 'Microsoft YaHei', sans-serif;
}

.hero {
  position: relative;
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: -1;
}

.hero-content {
  position: relative;
  z-index: 1;
  color: white;
  text-shadow: 0 2px 6px rgba(0,0,0,0.5);
}

.stats-container {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
  max-width: 1000px;
  margin: 0 auto;
}

.stat-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px 10px;
  text-align: center;
  transition: transform 0.2s;
  min-width: 140px;
  flex: 1;
  max-width: 160px;
}

.stat-card:hover {
  transform: scale(1.05);
}

.stat-card h3 {
  margin: 0 0 5px 0;
  font-size: 1.8rem;
  font-weight: bold;
  color: rgb(102, 8, 116);
}

.stat-card p {
  margin: 0;
  font-size: 0.9rem;
  color: #666;
}

.section-title {
  margin-top: 50px;
  margin-bottom: 30px;
  text-align: center;
  font-weight: bold;
}

.contact-footer {
  background: #003366;
  color: white;
  padding: 30px 0;
  text-align: center;
}

.contact-footer .container {
  max-width: 1200px;
  margin: 0 auto;
}

.contact-footer p {
  margin: 5px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.my-5 {
  margin-top: 3rem;
  margin-bottom: 3rem;
}

.g-4 {
  gap: 1.5rem;
}

.row {
  display: flex;
  flex-wrap: wrap;
  margin: 0 -0.75rem;
}

.col-md-2 {
  flex: 0 0 auto;
  width: 16.66666667%;
}

.col-6 {
  flex: 0 0 auto;
  width: 50%;
}

@media (max-width: 768px) {
  .stats-container {
    flex-direction: column;
    align-items: center;
  }
  
  .stat-card {
    max-width: 200px;
    width: 100%;
  }
  
  .hero h1 {
    font-size: 2rem;
  }
}

.text-center {
  text-align: center;
}

.display-3 {
  font-size: 3rem;
  font-weight: 300;
  line-height: 1.2;
}

.fw-bold {
  font-weight: bold;
}
</style>