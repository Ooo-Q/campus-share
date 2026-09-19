<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getAvatarUrl } from '../utils/resource'

const props = withDefaults(
  defineProps<{
    src?: string | null
    name?: string | null
    size?: number
  }>(),
  { size: 40 },
)

const failed = ref(false)
const url = computed(() => getAvatarUrl(props.src))
const letter = computed(() => (props.name || '?').trim().charAt(0).toUpperCase() || '?')

watch(
  () => props.src,
  () => {
    failed.value = false
  },
)
</script>

<template>
  <div
    class="ua"
    :style="{ width: size + 'px', height: size + 'px', fontSize: Math.round(size * 0.4) + 'px' }"
  >
    <img v-if="url && !failed" :src="url" alt="" @error="failed = true" />
    <span v-else>{{ letter }}</span>
  </div>
</template>

<style scoped>
.ua {
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #7a9e8e;
  color: #fff;
  font-weight: 600;
  line-height: 1;
}

.ua img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}

.ua span {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}
</style>
