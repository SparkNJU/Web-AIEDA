<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <router-link to="/">返回首页</router-link>
  </div>
</template>

<style scoped>
.not-found {
  height: 70vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  background-color: rgba(102, 8, 116, 0.05);
}

h1 {
  font-size: 6rem;
  color: rgb(102, 8, 116);
  margin-bottom: 1rem;
}

a {
  margin-top: 2rem;
  padding: 0.75rem 1.5rem;
  background-color: rgb(102, 8, 116);
  color: white;
  text-decoration: none;
  border-radius: 4px;
}
</style>