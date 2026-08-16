<script setup lang="ts">
defineProps<{ amount?: string; currency?: string }>()
</script>
<template><text>{{ currency === 'CNY' || !currency ? '¥' : currency + ' ' }}{{ amount || '0.00' }}</text></template>
