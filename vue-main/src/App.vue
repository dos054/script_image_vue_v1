<template>
  <div class="app">

    <!-- 메인 화면 -->
    <div v-if="currentView === 'main'">
      <header class="header">
        <div class="logo">LOGO</div>
        <input class="search" placeholder="검색어를 입력하세요" />
      </header>

      <nav class="nav">
        <ul class="nav-menu">
          <li>홈</li>
        </ul>
        <div>
          <button class="btn" @click="searchSimilar">유사이미지</button>
          <button class="btn" @click="compare">비교하기</button>
        </div>
      </nav>

      <div class="content">
        <aside class="category">
          <ul>
            <li v-for="c in categories" :key="c">{{ c }}</li>
          </ul>
        </aside>

        <section class="products">
          <div class="product-box" v-for="p in products" :key="p.pcode">
            <input type="checkbox" class="checkbox" :value="p.pcode" v-model="selected" :disabled="selected.length >= 2 && !selected.includes(p.pcode)" />
            <img :src="`/images/${p.image}`" class="product-image" alt="상품 이미지" />
            <div class="product-name">{{ p.productName }}</div>
            <div class="product-price">{{ p.priceMin?.toLocaleString() || '-' }}원</div>
          </div>
        </section>
      </div>
    </div>

    <!-- 비교 결과 화면 -->
    <div v-else-if="currentView === 'compare'" class="result-view">
      <button class="btn" @click="goBack">← 뒤로가기</button>
      <h2>상품 비교 결과</h2>
      <div class="result-box">
        <div v-if="loading">분석 중...</div>
        <div v-else class="result-text">{{ compareResult }}</div>
      </div>
    </div>

    <!-- 유사 이미지 결과 화면 -->
    <div v-else-if="currentView === 'similar'" class="result-view">
      <button class="btn" @click="goBack">← 뒤로가기</button>
      <h2>유사 이미지 검색 결과</h2>
      
      <div v-if="loading">검색 중...</div>
      <div v-else>
        <!-- 선택한 기준 상품 (크게) -->
        <div class="selected-product" v-if="selectedProduct">
          <img :src="`/images/${selectedProduct.image}`" class="selected-image" />
          <div class="selected-info">
            <div class="selected-name">{{ selectedProduct.productName }}</div>
            <div>최저가: {{ selectedProduct.priceMin?.toLocaleString() || '-' }}원 ~ 최고가: {{ selectedProduct.priceMax?.toLocaleString() || '-' }}원</div>
            <div v-if="selectedProduct.priceHistory">
              <span v-if="selectedProduct.priceHistory.month1">1개월 전: {{ selectedProduct.priceHistory.month1?.toLocaleString() }}원 | </span>
              <span v-if="selectedProduct.priceHistory.month3">3개월 전: {{ selectedProduct.priceHistory.month3?.toLocaleString() }}원 | </span>
              <span v-if="selectedProduct.priceHistory.month6">6개월 전: {{ selectedProduct.priceHistory.month6?.toLocaleString() }}원</span>
            </div>
          </div>
        </div>

        <h3>유사한 상품</h3>
        <div v-if="similarImages.length === 0">유사한 이미지를 찾을 수 없습니다.</div>
        <section class="products" v-else>
          <div class="product-box" v-for="img in similarImages" :key="img.productId">
            <img :src="`/images/${img.imageName}`" class="product-image" />
            <div class="product-name">{{ img.productName || '상품명 없음' }}</div>
            <div class="product-price">최저가: {{ img.priceMin?.toLocaleString() || '-' }}원</div>
            <div class="similarity">유사도: {{ (img.similarity * 100).toFixed(1) }}%</div>
          </div>
        </section>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const categories = ['기저귀', '분유', '이유식']
const products = ref([])
const selected = ref([])
const currentView = ref('main')
const compareResult = ref('')
const similarImages = ref([])
const selectedProduct = ref(null)
const loading = ref(false)

const fetchProducts = async () => {
  try {
    const res = await axios.get('/api/products')
    products.value = res.data
  } catch (e) {
    console.error('상품 조회 실패', e)
  }
}

const compare = async () => {
  if (selected.value.length !== 2) {
    alert('상품 두 개를 선택해 주세요')
    return
  }
  currentView.value = 'compare'
  loading.value = true
  try {
    const res = await axios.post('/api/compare', {
      pcode1: selected.value[0],
      pcode2: selected.value[1]
    })
    compareResult.value = res.data.analysis
  } catch (e) {
    compareResult.value = '비교 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

const searchSimilar = async () => {
  if (selected.value.length !== 1) {
    alert('상품 한 개를 선택해 주세요')
    return
  }
  currentView.value = 'similar'
  loading.value = true
  similarImages.value = []
  
  // 선택한 상품 정보 저장
  const pcode = selected.value[0]
  const product = products.value.find(p => p.pcode === pcode)
  if (product) {
    selectedProduct.value = { ...product }
    // 가격 추이 파싱
    if (product.priceBalance) {
      try {
        const priceData = JSON.parse(product.priceBalance)
        selectedProduct.value.priceHistory = {
          month1: priceData['1']?.[0]?.price,
          month3: priceData['3']?.[0]?.price,
          month6: priceData['6']?.[0]?.price
        }
      } catch (e) {
          console.log('가격 추이 파싱 실패')
        }
    }
  }
  
  try {
    const res = await axios.get('/api/similar-images', {
      params: { pcode: pcode, top: 6 }
    })
    if (res.data.success) {
      similarImages.value = res.data.similarImages
    } else {
      alert('유사 이미지 검색 실패: ' + res.data.error)
    }
  } catch (e) {
    alert('유사 이미지 검색 중 오류가 발생했습니다.')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  currentView.value = 'main'
  compareResult.value = ''
  similarImages.value = []
  selectedProduct.value = null
}

onMounted(fetchProducts)
</script>

<style scoped>
.app { font-family: Arial, sans-serif; }
.header { display: flex; align-items: center; padding: 12px 20px; border-bottom: 1px solid #ddd; }
.logo { font-weight: bold; margin-right: 20px; }
.search { flex: 1; padding: 6px; }
.nav { display: flex; justify-content: space-between; align-items: center; padding: 10px 20px; border-bottom: 1px solid #ccc; }
.nav-menu { display: flex; gap: 20px; list-style: none; padding: 0; margin: 0; }
.btn { padding: 6px 14px; cursor: pointer; margin-left: 8px; }
.content { display: flex; }
.category { width: 200px; border-right: 1px solid #ddd; padding: 10px; }
.products { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; padding: 16px; flex: 1; }
.product-box { position: relative; border: 1px solid #ddd; padding: 12px; }
.checkbox { position: absolute; top: 6px; left: 6px; }
.product-image { width: 100%; height: 140px; object-fit: cover; margin-bottom: 8px; }
.product-name { font-size: 14px; margin-bottom: 4px; }
.product-price { font-size: 14px; color: #333; }
.similarity { font-size: 13px; color: #666; margin-top: 4px; }
.result-view { padding: 20px; }
.result-box { margin-top: 20px; padding: 20px; border: 1px solid #ddd; }
.result-text { white-space: pre-wrap; line-height: 1.6; }
.selected-product { display: flex; border: 2px solid #333; padding: 20px; margin-bottom: 20px; }
.selected-image { width: 250px; height: 250px; object-fit: cover; margin-right: 20px; }
.selected-info { flex: 1; }
.selected-name { font-size: 18px; font-weight: bold; margin-bottom: 10px; }
</style>
