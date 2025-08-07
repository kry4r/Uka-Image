import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: {
      title: 'Home - Uka Image Hosting'
    }
  },
  {
    path: '/gallery',
    name: 'Gallery',
    component: () => import('@/views/Gallery.vue'),
    meta: {
      title: 'Gallery - Uka Image Hosting'
    }
  },
  {
    path: '/upload',
    name: 'Upload',
    component: () => import('@/views/Upload.vue'),
    meta: {
      title: 'Upload Images - Uka Image Hosting'
    }
  },
  {
    path: '/image/:id',
    name: 'ImageDetail',
    component: () => import('@/views/ImageDetail.vue'),
    props: true,
    meta: {
      title: 'Image Details - Uka Image Hosting'
    }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Global navigation guard for page titles
router.beforeEach((to, from, next) => {
  if (to.meta?.title) {
    document.title = to.meta.title as string
  }
  next()
})

export default router
