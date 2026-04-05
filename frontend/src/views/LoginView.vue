<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <h1 class="login-logo">Enchelin</h1>
        <p class="login-tagline">우리만의 맛집 가이드</p>
      </div>
      <p class="login-desc">
        주변 식당을 탐색하고, 리뷰를 남겨보세요.
      </p>
      <a :href="githubLoginUrl" class="btn btn-primary btn-block login-btn">
        <svg class="github-icon" viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
          <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
        </svg>
        GitHub로 로그인
      </a>
    </div>
  </div>
</template>

<script setup>
import { getGithubLoginUrl } from '@/api/auth'

const githubLoginUrl = getGithubLoginUrl()
</script>

<style scoped>
.login-page {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
  background-color: var(--color-bg-secondary);
}

.login-card {
  width: 100%;
  max-width: 360px;
  text-align: center;
  padding: var(--space-8) var(--space-5);
}

.login-brand {
  margin-bottom: var(--space-5);
}

.login-logo {
  font-size: var(--font-size-2xl);
  color: var(--color-primary);
  letter-spacing: -0.5px;
}

.login-tagline {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: var(--space-1);
}

.login-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-6);
  line-height: 1.6;
}

.login-btn {
  font-size: var(--font-size-base);
  padding: var(--space-3) var(--space-5);
}

.github-icon {
  flex-shrink: 0;
}
</style>
