<template>
  <div class="creator-layout">
    <!-- 顶部栏 -->
    <header class="creator-header">
      <div class="header-left">
        <router-link to="/recommend" class="logo-link">
          <img src="/logo3.png" alt="Logo" class="creator-logo" />
        </router-link>
        <span class="creator-title">创作者中心</span>
      </div>
      <div class="header-right">
        <span class="creator-avatar">
          <img v-if="avatarUrl" :src="avatarUrl" alt="头像" class="avatar-img" />
          <span v-else class="avatar-initial">{{ avatarInitial }}</span>
        </span>
      </div>
    </header>

    <!-- 主体：左侧栏 + 中间 + 右侧 -->
    <div class="creator-body">
      <aside class="creator-sidebar-left">
        <router-link to="/creator/write" custom v-slot="{ navigate }">
          <el-button class="create-btn create-btn-full" type="primary" @click="navigate">
            <el-icon><Plus /></el-icon>
            创作
          </el-button>
        </router-link>
        <nav class="creator-nav">
          <router-link to="/creator" :class="['nav-item', { active: route.path === '/creator' }]">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </router-link>
          <div class="nav-group">
            <button type="button" class="nav-item nav-group-title" @click="contentManageOpen = !contentManageOpen">
              <el-icon><Folder /></el-icon>
              <span>内容管理</span>
              <el-icon class="nav-chevron" :class="{ open: contentManageOpen }"><ArrowDown /></el-icon>
            </button>
            <div v-show="contentManageOpen" class="nav-sub">
              <router-link to="/creator/content" :class="['nav-item nav-sub-item', { active: route.path === '/creator/content' }]">
                <span>内容管理</span>
              </router-link>
              <router-link to="/creator/comments" :class="['nav-item nav-sub-item', { active: route.path === '/creator/comments' }]">
                <span>评论管理</span>
              </router-link>
              <router-link to="/creator/columns" :class="['nav-item nav-sub-item', { active: route.path === '/creator/columns' }]">
                <span>专栏管理</span>
              </router-link>
            </div>
          </div>
          <div class="nav-group">
            <button type="button" class="nav-item nav-group-title" @click="aiWorkbenchOpen = !aiWorkbenchOpen">
              <el-icon><Document /></el-icon>
              <span>AI 工作台</span>
              <el-icon class="nav-chevron" :class="{ open: aiWorkbenchOpen }"><ArrowDown /></el-icon>
            </button>
            <div v-show="aiWorkbenchOpen" class="nav-sub">
              <router-link to="/creator/ai/blog" :class="['nav-item nav-sub-item', { active: route.path === '/creator/ai/blog' }]">
                <span>博客机器人</span>
              </router-link>
            </div>
          </div>
          <div class="nav-group">
            <button type="button" class="nav-item nav-group-title" @click="analyticsOpen = !analyticsOpen">
              <el-icon><TrendCharts /></el-icon>
              <span>数据分析</span>
              <el-icon class="nav-chevron" :class="{ open: analyticsOpen }"><ArrowDown /></el-icon>
            </button>
            <div v-show="analyticsOpen" class="nav-sub">
              <button
                v-for="p in analyticsPanels"
                :key="p.id"
                type="button"
                :class="['nav-item nav-sub-item nav-sub-btn', { active: isAnalyticsPage && analyticsPanel === p.id }]"
                @click="goAnalyticsPanel(p.id)"
              >
                <span>{{ p.label }}</span>
              </button>
            </div>
          </div>
        </nav>
      </aside>

      <main class="creator-main">
        <!-- 首页：用户信息 + 数据指标 + 近期博客 -->
        <template v-if="isCreatorHome">
        <!-- 用户信息卡片 -->
        <div class="card profile-card">
          <div class="profile-left">
            <span class="profile-avatar">
              <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
              <span v-else class="avatar-initial">{{ avatarInitial }}</span>
            </span>
            <div class="profile-info">
              <div class="profile-name">
                {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}
              </div>
              <div class="profile-stats">
                <span>{{ contentTotal ?? 0 }} 原创</span>
                <span>{{ followStats?.followerCount ?? 0 }} 粉丝</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 数据指标卡片：单卡四列 + 竖线分隔 -->
        <div class="card metrics-card metrics-card-top">
          <div class="metric-item">
            <div class="metric-label">总阅读量</div>
            <div class="metric-value">{{ formatNum(contentStats?.totalViewCount ?? 0) }}</div>
            <div class="metric-yesterday" :class="{ 'has-delta': (contentStats?.yesterdayViewDelta ?? 0) > 0 }">
              <el-icon v-if="(contentStats?.yesterdayViewDelta ?? 0) > 0" class="metric-arrow"><ArrowUp /></el-icon>
              {{ yesterdayText(contentStats?.yesterdayViewDelta ?? 0) }}
            </div>
          </div>
          <div class="metric-item">
            <div class="metric-label">总点赞量</div>
            <div class="metric-value">{{ formatNum(contentStats?.totalLikeCount ?? 0) }}</div>
            <div class="metric-yesterday" :class="{ 'has-delta': (contentStats?.yesterdayLikeDelta ?? 0) > 0 }">
              <el-icon v-if="(contentStats?.yesterdayLikeDelta ?? 0) > 0" class="metric-arrow"><ArrowUp /></el-icon>
              {{ yesterdayText(contentStats?.yesterdayLikeDelta ?? 0) }}
            </div>
          </div>
          <div class="metric-item">
            <div class="metric-label">粉丝数</div>
            <div class="metric-value">{{ formatNum(followStats?.followerCount ?? 0) }}</div>
            <div class="metric-yesterday" :class="{ 'has-delta': (followStats?.yesterdayFollowerDelta ?? 0) > 0 }">
              <el-icon v-if="(followStats?.yesterdayFollowerDelta ?? 0) > 0" class="metric-arrow"><ArrowUp /></el-icon>
              {{ yesterdayText(followStats?.yesterdayFollowerDelta ?? 0) }}
            </div>
          </div>
          <div class="metric-item">
            <div class="metric-label">收藏数</div>
            <div class="metric-value">{{ formatNum(contentStats?.totalCollectionCount ?? 0) }}</div>
            <div class="metric-yesterday" :class="{ 'has-delta': (contentStats?.yesterdayCollectionDelta ?? 0) > 0 }">
              <el-icon v-if="(contentStats?.yesterdayCollectionDelta ?? 0) > 0" class="metric-arrow"><ArrowUp /></el-icon>
              {{ yesterdayText(contentStats?.yesterdayCollectionDelta ?? 0) }}
            </div>
          </div>
        </div>

        <!-- 近期博客：与 Profile 我的博客方案一致 -->
        <div class="card blog-section-card">
          <div class="blog-header-row">
            <h2 class="section-title">近期博客</h2>
            <div class="blog-filters">
              <el-dropdown trigger="click" popper-class="creator-blog-visibility-dropdown" @command="setBlogVisibility">
                <span class="blog-sort blog-sort--dropdown">
                  {{ blogVisibilityLabel }}
                  <el-icon class="blog-sort-arrow"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="ALL">全部可见</el-dropdown-item>
                    <el-dropdown-item command="SELF">仅我可见</el-dropdown-item>
                    <el-dropdown-item command="FANS">粉丝可见</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <span class="blog-sort" :class="{ active: blogSortBy === 'time' }" @click="setBlogSort('time')">
                按时间排序 {{ blogSortBy === 'time' ? (blogOrder === 'desc' ? '↓' : '↑') : '↑↓' }}
              </span>
              <span class="blog-sort" :class="{ active: blogSortBy === 'likes' }" @click="setBlogSort('likes')">
                按点赞量排序 {{ blogSortBy === 'likes' ? (blogOrder === 'desc' ? '↓' : '↑') : '↑↓' }}
              </span>
              <span class="blog-sort" :class="{ active: blogSortBy === 'views' }" @click="setBlogSort('views')">
                按浏览量排序 {{ blogSortBy === 'views' ? (blogOrder === 'desc' ? '↓' : '↑') : '↑↓' }}
              </span>
            </div>
          </div>
          <div v-if="blogLoading" class="blog-loading">加载中…</div>
          <div v-else-if="blogList.length === 0" class="blog-empty">
            <el-icon class="blog-empty-icon"><Document /></el-icon>
            <p class="blog-empty-text">还没有博客</p>
          </div>
          <div v-else class="profile-card-list">
            <article v-for="item in blogList" :key="item.id" class="profile-card-item profile-card-item--blog">
              <div class="profile-card-body">
                <router-link :to="`/article/${item.id}`" class="profile-card-title">{{ item.title }}</router-link>
                <p class="profile-card-meta">{{ item.summary }}</p>
                <div class="profile-card-stats">
                  <span class="stat"><el-icon><View /></el-icon> 阅读 {{ formatBlogCount(item.viewCount) }}</span>
                  <span class="stat"><el-icon><Star /></el-icon> 赞 {{ formatBlogCount(item.likeCount) }}</span>
                  <span class="stat"><el-icon><Collection /></el-icon> 收藏 {{ formatBlogCount(item.collectionCount) }}</span>
                  <span class="stat profile-card-time">发布时间 {{ formatBlogCreatedAt(item.publishedAt ?? item.createdAt) }}</span>
                </div>
              </div>
              <router-link :to="`/article/${item.id}`" class="profile-card-thumb">
                <img v-if="item.cover" :src="item.cover" :alt="item.title" class="profile-card-thumb-img" />
                <span v-else class="profile-card-thumb-ph"></span>
              </router-link>
            </article>
          </div>
          <div v-if="blogTotal > 0" class="blog-pagination-wrap">
            <span class="blog-total-text">共 <span class="blog-total-num">{{ blogTotal }}</span> 条</span>
            <el-pagination
              v-model:current-page="blogPage"
              :page-size="blogPageSize"
              :total="blogTotal"
              layout="prev, pager, next"
              class="blog-pagination"
            />
          </div>
        </div>
        </template>

        <!-- 内容管理：文章列表、状态筛选、表格、操作 -->
        <div v-else-if="route.path === '/creator/content'" class="content-management-wrap">
          <div class="card content-management-card">
            <div class="cm-tabs">
              <span class="cm-tab active">文章</span>
            </div>
            <div class="cm-status-row">
              <button
                type="button"
                :class="['cm-status-tab', { active: cmStatus === 'ALL' }]"
                @click="setCmStatus('ALL')"
              >
                全部({{ cmTotalAllFiltered }})
              </button>
              <button
                type="button"
                :class="['cm-status-tab', { active: cmStatus === 'PUBLISHED' }]"
                @click="setCmStatus('PUBLISHED')"
              >
                已发布
              </button>
              <button
                type="button"
                :class="['cm-status-tab', { active: cmStatus === 'DRAFT' }]"
                @click="setCmStatus('DRAFT')"
              >
                草稿
              </button>
            </div>
            <div class="cm-filter-row">
              <el-dropdown trigger="click" popper-class="creator-blog-visibility-dropdown" @command="setCmVisibility">
                <span class="cm-filter-label">
                  可见范围: {{ cmVisibilityLabel }}
                  <el-icon class="cm-filter-arrow"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="UNLIMITED">不限</el-dropdown-item>
                    <el-dropdown-item command="ALL">全部可见</el-dropdown-item>
                    <el-dropdown-item command="SELF">仅我可见</el-dropdown-item>
                    <el-dropdown-item command="FANS">粉丝可见</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-input
                v-model="cmKeyword"
                placeholder="请输入关键词"
                clearable
                class="cm-search"
                @keyup.enter="runCmSearch"
              >
                <template #suffix>
                  <el-icon class="cm-search-icon" @click="runCmSearch"><Search /></el-icon>
                </template>
              </el-input>
            </div>
            <div v-if="cmLoading" class="cm-loading">加载中…</div>
            <div v-else class="cm-table-wrap">
              <table class="cm-table">
                <thead>
                  <tr>
                    <th class="cm-col-article">文章</th>
                    <th class="cm-col-num">阅读</th>
                    <th class="cm-col-num">点赞</th>
                    <th class="cm-col-num">评论</th>
                    <th class="cm-col-num">收藏</th>
                    <th class="cm-col-action">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in cmList" :key="item.id" class="cm-row">
                    <td class="cm-col-article">
                      <div class="cm-article-cell">
                        <router-link :to="`/article/${item.id}`" class="cm-article-thumb">
                          <img v-if="item.cover" :src="item.cover" :alt="item.title" />
                          <span v-else class="cm-article-thumb-ph"></span>
                        </router-link>
                        <div class="cm-article-info">
                          <router-link :to="`/article/${item.id}`" class="cm-article-title">{{ item.title }}</router-link>
                          <div class="cm-article-badges">
                            <span v-if="item.articleType === 'ORIGINAL'" class="cm-badge cm-badge-original">原创</span>
                            <span v-else-if="item.articleType === 'REPRINT'" class="cm-badge cm-badge-reprint">转载</span>
                            <span v-else-if="item.articleType === 'TRANSLATED'" class="cm-badge cm-badge-translated">翻译</span>
                          </div>
                          <div class="cm-article-date">{{ formatCmDate(item.publishedAt ?? item.createdAt) }}</div>
                        </div>
                      </div>
                    </td>
                    <td class="cm-col-num">{{ item.viewCount ?? 0 }}</td>
                    <td class="cm-col-num">{{ item.likeCount ?? 0 }}</td>
                    <td class="cm-col-num">{{ item.commentCount ?? 0 }}</td>
                    <td class="cm-col-num">{{ item.collectionCount ?? 0 }}</td>
                    <td class="cm-col-action">
                      <button type="button" class="cm-action cm-action-btn" @click="openContentTrendDialog(item)">数据</button>
                      <router-link :to="`/creator/write?id=${item.id}`" class="cm-action">编辑</router-link>
                      <button type="button" class="cm-action cm-action-danger" @click="confirmDeleteContent(item)">删除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!cmList.length" class="cm-empty">暂无内容</div>
            </div>
            <div v-if="cmTotal > 0" class="cm-pagination-wrap">
              <span class="cm-total-text">共 <span class="cm-total-num">{{ cmTotal }}</span> 条</span>
              <el-pagination
                v-model:current-page="cmPage"
                :page-size="cmPageSize"
                :total="cmTotal"
                layout="prev, pager, next"
                class="cm-pagination"
              />
            </div>
          </div>
        </div>

        <!-- 评论管理：左文章右评论，作者点击推荐变热评 -->
        <div v-else-if="route.path === '/creator/comments'" class="comments-management-wrap">
          <div class="card comments-management-card">
            <div class="comments-layout">
              <aside class="comments-sidebar">
                <h3 class="comments-sidebar-title">最新评论</h3>
                <div v-if="commentedArticlesLoading" class="comments-sidebar-loading">加载中…</div>
                <template v-else>
                  <button
                    v-for="art in commentedArticles"
                    :key="art.contentId"
                    type="button"
                    :class="['comments-article-item', { active: selectedContentId === art.contentId }]"
                    @click="selectArticle(art)"
                  >
                    <span class="comments-article-title">{{ art.title }}</span>
                    <span class="comments-article-meta">{{ art.commentCount }} 评论 · {{ formatCommentDate(art.lastCommentAt) }}</span>
                  </button>
                  <div v-if="!commentedArticles.length" class="comments-sidebar-empty">暂无带评论的文章</div>
                </template>
              </aside>
              <main class="comments-main">
                <template v-if="selectedContentId == null">
                  <div class="comments-placeholder">请从左侧选择一篇文章查看评论</div>
                </template>
                <template v-else>
                  <div class="comments-toolbar">
                    <div class="comments-sort">
                      <button
                        type="button"
                        :class="['comments-sort-btn', { active: commentSort === 'default' }]"
                        @click="commentSort = 'default'"
                      >
                        默认
                      </button>
                      <button
                        type="button"
                        :class="['comments-sort-btn', { active: commentSort === 'latest' }]"
                        @click="commentSort = 'latest'"
                      >
                        最新
                      </button>
                    </div>
                    <el-icon class="comments-refresh" @click="fetchCommentList"><Refresh /></el-icon>
                  </div>
                  <div v-if="commentListLoading" class="comments-loading">加载中…</div>
                  <div v-else-if="!displayCommentList.length" class="comments-empty">暂无评论</div>
                  <div v-else class="comment-list">
                    <article v-for="c in displayCommentList" :key="c.id" class="comment-item">
                      <div class="comment-head">
                        <el-avatar :size="36" class="comment-avatar" :src="c.userAvatar">
                          {{ (c.userNickname || '用户').charAt(0) }}
                        </el-avatar>
                        <span class="comment-username">{{ c.userNickname || '用户' }}</span>
                        <span v-if="c.isAuthor" class="comment-tag-author">作者</span>
                        <span class="comment-time">{{ formatCommentTime(c.createdAt) }}</span>
                        <el-dropdown trigger="click" class="comment-more-wrap" @command="(cmd: string) => cmd === 'delete' && deleteCommentItem(c)">
                          <el-icon class="comment-more" title="更多"><MoreFilled /></el-icon>
                          <template #dropdown>
                            <el-dropdown-menu>
                              <el-dropdown-item command="delete" style="color: var(--el-color-danger)">删除</el-dropdown-item>
                            </el-dropdown-menu>
                          </template>
                        </el-dropdown>
                      </div>
                      <div class="comment-body">
                        <span v-if="c.isHot" class="comment-hot-label">热评</span>
                        <span class="comment-text">{{ c.body }}</span>
                      </div>
                      <div class="comment-actions">
                        <span
                          class="comment-action"
                          :class="{ 'comment-action--active': c.likedByMe }"
                          @click="toggleCommentLike(c)"
                        >
                          <el-icon><Star /></el-icon> 点赞 {{ c.likeCount ?? 0 }}
                        </span>
                        <span
                          class="comment-action comment-action-recommend"
                          :class="{ active: c.isHot }"
                          @click="toggleCommentHot(c)"
                        >
                          {{ c.isHot ? '取消推荐' : '推荐' }}
                        </span>
                      </div>
                    </article>
                  </div>
                </template>
              </main>
            </div>
          </div>
        </div>

        <!-- 专栏管理：参考个人主页「我的专栏」实现 -->
        <div v-else-if="route.path === '/creator/columns'" class="card column-management-card">
          <div class="blog-header-row column-management-header">
            <h2 class="section-title">专栏管理</h2>
            <button type="button" class="btn-new-column" @click="openCreateColumn">
              <el-icon><Plus /></el-icon>
              新建专栏
            </button>
          </div>
          <div v-if="columnLoading" class="cm-loading">加载中…</div>
          <div v-else-if="columnList.length === 0" class="column-empty">
            <el-icon class="column-empty-icon"><FolderOpened /></el-icon>
            <p class="column-empty-text">还没有专栏</p>
            <button type="button" class="btn-new-column-inline" @click="openCreateColumn">新建专栏</button>
          </div>
          <div v-else class="column-list">
            <article v-for="item in columnList" :key="item.id" class="column-list-item">
              <div class="column-list-body">
                <div class="column-main">
                  <router-link :to="`/column/${item.id}`" class="column-list-title column-title-link">{{ item.name }}</router-link>
                  <div class="column-actions">
                    <button type="button" class="folder-action-btn" @click.stop="openEditColumn(item)">编辑</button>
                    <button type="button" class="folder-action-btn folder-action-btn--danger" @click.stop="confirmDeleteColumn(item)">删除</button>
                  </div>
                </div>
                <p v-if="item.description" class="column-list-meta">{{ item.description }}</p>
                <p class="column-list-time">{{ item.articleCount }} 篇内容 · 更新于 {{ item.updatedAt ? item.updatedAt.slice(0, 10) : '—' }}</p>
              </div>
              <router-link :to="`/column/${item.id}`" class="column-list-cover">
                <img v-if="item.cover" :src="item.cover" :alt="item.name" class="column-cover-img" />
                <span v-else class="column-cover-ph"></span>
              </router-link>
            </article>
          </div>
        </div>

        <!-- 博客机器人：标题 + 描述 + 新建机器人 -->
        <div v-else-if="route.path === '/creator/ai/blog'" class="card blog-bot-card">
          <div class="blog-header-row blog-bot-header">
            <h2 class="section-title">博客机器人</h2>
            <button type="button" class="btn-new-column" @click="openCreateBot">
              <el-icon><Plus /></el-icon>
              新建机器人
            </button>
          </div>
          <p class="blog-bot-desc">使用 AI 辅助生成博客内容，可配置发文风格、主标签等，输入主题或大纲即可快速成文。</p>
          <div v-if="botLoading" class="cm-loading">加载中...</div>
          <div v-else-if="botList.length === 0" class="blog-bot-empty">
            <p class="blog-bot-empty-text">暂无机器人，点击「新建机器人」添加写博客 AI 配置</p>
          </div>
          <div v-else class="blog-bot-list">
            <article v-for="bot in botList" :key="bot.id" class="blog-bot-item">
              <img v-if="bot.avatar" :src="bot.avatar" :alt="bot.name" class="blog-bot-item-avatar" />
              <span v-else class="blog-bot-item-avatar-ph">{{ bot.name.charAt(0) }}</span>
              <div class="blog-bot-item-body">
                <div class="blog-bot-item-name">{{ bot.name }}</div>
                <div class="blog-bot-item-meta">风格：{{ BOT_STYLE_LABELS[bot.style] || bot.style }} · 主标签：{{ bot.mainTagName || '未设置' }}</div>
              </div>
              <button type="button" class="blog-bot-item-delete" title="删除" @click="onDeleteBot(bot)">
                <el-icon><Delete /></el-icon>
              </button>
            </article>
          </div>
        </div>

        <div v-else-if="isAnalyticsPage" class="card analytics-card analytics-dash">
          <div class="analytics-toolbar">
            <div class="analytics-toolbar-left">
              <h2 class="section-title analytics-toolbar-title">{{ activeAnalyticsLabel }}</h2>
              <p class="analytics-toolbar-desc">多维聚合分析，聚焦内容表现和创作节奏。</p>
            </div>
            <div class="analytics-toolbar-right">
              <el-select v-model="analyticsDays" class="analytics-days-select" size="default">
                <el-option :value="7" label="近 7 天" />
                <el-option :value="14" label="近 14 天" />
                <el-option :value="30" label="近 30 天" />
                <el-option :value="60" label="近 60 天" />
                <el-option :value="90" label="近 90 天" />
              </el-select>
              <el-button :loading="analyticsLoading" @click="loadCreatorAnalytics">刷新</el-button>
            </div>
          </div>

          <div v-loading="analyticsLoading" class="analytics-body">
            <template v-if="creatorAnalytics">
              <div class="analytics-stage">
              <transition name="analytics-fade" mode="out-in">
                <div v-show="analyticsPanel === 'overview'" key="ov" class="analytics-panel">
                  <div class="analytics-kpi-grid">
                    <div
                      v-for="(kpi, i) in overviewKpis"
                      :key="kpi.key"
                      class="analytics-stat-card"
                      :style="{ animationDelay: `${i * 0.04}s` }"
                    >
                      <div class="analytics-stat-label">{{ kpi.label }}</div>
                      <div class="analytics-stat-value">{{ kpi.value }}</div>
                      <div v-if="kpi.sub" class="analytics-stat-sub">{{ kpi.sub }}</div>
                    </div>
                  </div>
                  <div class="analytics-chart-card analytics-chart-card--glass analytics-chart-rise">
                    <div class="analytics-chart-title">近期综合分走势（最近 14 日窗口内）</div>
                    <svg viewBox="0 0 640 140" class="analytics-sparkline-svg" aria-hidden="true">
                      <defs>
                        <linearGradient id="gradScoreMini" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stop-color="#e85a5a" stop-opacity="0.45" />
                          <stop offset="100%" stop-color="#e85a5a" stop-opacity="0" />
                        </linearGradient>
                      </defs>
                      <line x1="16" y1="126" x2="624" y2="126" class="analytics-axis-baseline" />
                      <polygon :points="trendMiniAreaPoints" fill="url(#gradScoreMini)" />
                      <polyline :points="trendMiniLinePoints" class="analytics-trend-line" />
                    </svg>
                    <div
                      class="analytics-trend-x analytics-trend-x--grid"
                      :style="{ gridTemplateColumns: `repeat(${Math.max(trendMiniSlice.length, 1)}, minmax(0, 1fr))` }"
                    >
                      <span v-for="(lab, i) in trendMiniXLabels" :key="i" class="analytics-trend-x-item">{{ lab || '\u00a0' }}</span>
                    </div>
                  </div>
                </div>
              </transition>

              <transition name="analytics-fade" mode="out-in">
                <div v-show="analyticsPanel === 'trend'" key="tr" class="analytics-panel">
                  <div class="analytics-chart-card analytics-chart-card--wide analytics-chart-rise">
                    <div class="analytics-chart-title">综合分趋势（阅读 + 互动加权）</div>
                    <svg viewBox="0 0 920 260" class="analytics-trend-svg" aria-hidden="true">
                      <defs>
                        <linearGradient id="gradScoreFull" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stop-color="#b31b1b" stop-opacity="0.35" />
                          <stop offset="100%" stop-color="#b31b1b" stop-opacity="0" />
                        </linearGradient>
                      </defs>
                      <line x1="16" y1="246" x2="904" y2="246" class="analytics-axis-baseline" />
                      <polygon :points="trendFullAreaPoints" fill="url(#gradScoreFull)" />
                      <polyline :points="trendFullLinePoints" class="analytics-trend-line analytics-trend-line--thick" />
                    </svg>
                    <div
                      class="analytics-trend-x analytics-trend-x--grid"
                      :style="{ gridTemplateColumns: `repeat(${Math.max(trendFullSlice.length, 1)}, minmax(0, 1fr))` }"
                    >
                      <span v-for="(lab, i) in trendXLabels" :key="i" class="analytics-trend-x-item">{{ lab || '\u00a0' }}</span>
                    </div>
                  </div>
                  <div class="analytics-subgrid">
                    <div class="analytics-mini-metric">
                      <div class="analytics-mini-label">日均发布</div>
                      <div class="analytics-mini-value">{{ trendAvgPublished }}</div>
                    </div>
                    <div class="analytics-mini-metric">
                      <div class="analytics-mini-label">窗口内总阅读</div>
                      <div class="analytics-mini-value">{{ formatNum(trendSumViews) }}</div>
                    </div>
                    <div class="analytics-mini-metric">
                      <div class="analytics-mini-label">窗口峰值分</div>
                      <div class="analytics-mini-value">{{ trendMaxScore }}</div>
                    </div>
                  </div>
                  <div class="analytics-chart-card analytics-chart-rise">
                    <div class="analytics-chart-title">每日发布篇数</div>
                    <div class="analytics-publish-bars">
                      <div
                        v-for="(b, i) in trendPublishBars"
                        :key="i"
                        class="analytics-publish-bar-wrap"
                        :title="`${b.date}：${b.n} 篇`"
                      >
                        <div class="analytics-publish-bar" :style="{ height: b.pct + '%' }" />
                      </div>
                    </div>
                  </div>
                </div>
              </transition>

              <transition name="analytics-fade" mode="out-in">
                <div v-show="analyticsPanel === 'tags'" key="tg" class="analytics-panel">
                  <div class="analytics-chart-card analytics-chart-rise">
                    <div class="analytics-chart-title">标签 × 互动（Top 标签按互动量）</div>
                    <div v-if="!creatorAnalytics.tagInsights?.length" class="analytics-empty-inline">暂无标签数据</div>
                    <div v-else class="bar-list">
                      <div v-for="t in creatorAnalytics.tagInsights" :key="t.tagId" class="bar-item">
                        <span class="bar-label">{{ t.tagName }}</span>
                        <div class="bar-track">
                          <div
                            class="bar-fill bar-fill-tag"
                            :style="{ width: tagBarPercent(t.engagement) + '%' }"
                          />
                        </div>
                        <span class="bar-value">{{ t.engagement }} 互动</span>
                      </div>
                    </div>
                  </div>
                </div>
              </transition>

              <transition name="analytics-fade" mode="out-in">
                <div v-show="analyticsPanel === 'length'" key="ln" class="analytics-panel">
                  <div class="analytics-chart-card analytics-chart-rise">
                    <div class="analytics-chart-title">正文长度分布</div>
                    <div class="analytics-length-grid">
                      <div
                        v-for="(b, i) in creatorAnalytics.lengthDistribution"
                        :key="b.bucket"
                        class="analytics-length-cell"
                        :style="{ animationDelay: `${i * 0.07}s` }"
                      >
                        <div class="analytics-length-ring" :style="{ '--p': (b.ratio ?? 0) * 100 + '%' }">
                          <span class="analytics-length-pct">{{ ((b.ratio ?? 0) * 100).toFixed(0) }}%</span>
                        </div>
                        <div class="analytics-length-bucket">{{ b.bucket }} 字符</div>
                        <div class="analytics-length-count">{{ b.count }} 篇</div>
                      </div>
                    </div>
                  </div>
                </div>
              </transition>

              <transition name="analytics-fade" mode="out-in">
                <div v-show="analyticsPanel === 'heatmap'" key="hm" class="analytics-panel analytics-panel--heatmap">
                  <div class="analytics-chart-card analytics-chart-rise">
                    <div class="analytics-chart-title">24 小时创作分布（按创建时间）</div>
                    <div class="analytics-heat-row">
                      <div
                        v-for="(h, i) in heatHourCells"
                        :key="'h' + i"
                        class="analytics-heat-cell"
                        :style="{ background: `rgba(179,27,27,${0.12 + h.int * 0.82})` }"
                        :title="`${i} 点：${h.n} 篇`"
                      />
                    </div>
                    <div class="analytics-heat-labels">
                      <span v-for="i in 24" :key="i" class="analytics-heat-h">{{ i - 1 }}</span>
                    </div>
                  </div>
                  <div class="analytics-chart-card analytics-chart-rise">
                    <div class="analytics-chart-title">星期分布（周一至周日）</div>
                    <div class="analytics-week-bars">
                      <div v-for="(w, i) in heatWeekCells" :key="'w' + i" class="analytics-week-item">
                        <div class="analytics-week-bar" :style="{ height: w.pct + '%' }" />
                        <span class="analytics-week-label">{{ weekLabels[i] }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </transition>

              <transition name="analytics-fade" mode="out-in">
                <div v-show="analyticsPanel === 'top'" key="tp" class="analytics-panel">
                  <div class="analytics-chart-card analytics-chart-rise">
                    <div class="analytics-chart-title">爆款内容（综合分 Top）</div>
                    <div v-if="!creatorAnalytics.topContents?.length" class="analytics-empty-inline">暂无已发布内容</div>
                    <ul v-else class="analytics-top-list">
                      <li v-for="(c, i) in creatorAnalytics.topContents" :key="c.contentId" class="analytics-top-item" :style="{ animationDelay: `${i * 0.06}s` }">
                        <span class="analytics-top-rank">{{ i + 1 }}</span>
                        <router-link :to="'/article/' + c.contentId" class="analytics-top-title">{{ c.title }}</router-link>
                        <span class="analytics-top-meta">{{ formatNum(c.views) }} 阅 · {{ c.engagement }} 互动 · 分 {{ formatScore(c.score) }}</span>
                      </li>
                    </ul>
                  </div>
                </div>
              </transition>
              </div>
            </template>
            <div v-else-if="!analyticsLoading" class="analytics-empty">
              <p>暂无分析数据，请先发布博客后再试。</p>
            </div>
          </div>
        </div>

        <!-- 其他占位 -->
        <div v-else class="management-placeholder-wrap">
          <div class="card management-placeholder-card">
            <h2 class="management-placeholder-title">{{ managementPlaceholderTitle }}</h2>
            <p class="management-placeholder-desc">{{ managementPlaceholderDesc }}</p>
          </div>
        </div>
      </main>

      <aside v-if="isCreatorHome" class="creator-sidebar-right">
        <div class="card ranking-card">
          <div class="ranking-tabs">
            <button
              type="button"
              :class="['ranking-tab', { active: rankingTab === 'influence' }]"
              @click="rankingTab = 'influence'"
            >
              影响力榜
            </button>
            <button
              type="button"
              :class="['ranking-tab', { active: rankingTab === 'growth' }]"
              @click="rankingTab = 'growth'"
            >
              成长力榜
            </button>
          </div>
          <div class="ranking-list">
            <div v-if="rankingLoading" class="ranking-empty">加载中…</div>
            <div
              v-else
              v-for="(item, index) in rankingList"
              :key="item.id"
              class="ranking-item"
            >
              <span class="ranking-num">{{ item.rank }}</span>
              <span class="ranking-avatar">
                <img v-if="item.avatar" :src="item.avatar" alt="" />
                <span v-else class="avatar-initial">{{ item.name.charAt(0) }}</span>
              </span>
              <div class="ranking-info">
                <span class="ranking-name">{{ item.name }}</span>
                <span class="ranking-desc">{{ item.desc }}</span>
              </div>
              <button v-if="!item.following" type="button" class="ranking-follow" @click="onRankingFollow(item)">+ 关注</button>
              <span v-else class="ranking-followed">已关注</span>
            </div>
            <div v-if="!rankingList.length" class="ranking-empty">暂无数据</div>
          </div>
        </div>
      </aside>
    </div>

    <!-- 新建专栏弹窗（专栏管理页） -->
    <el-dialog
      v-model="createColumnVisible"
      title="新建专栏"
      width="480px"
      top="12vh"
      class="creator-column-dialog"
      @closed="resetCreateColumnForm"
    >
      <el-form :model="createColumnForm" label-position="top">
        <el-form-item label="专栏名称">
          <el-input v-model="createColumnForm.name" placeholder="专栏名称" maxlength="128" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="专栏描述（可选）">
          <el-input v-model="createColumnForm.description" type="textarea" :rows="3" placeholder="专栏描述 (可选)" maxlength="512" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="封面图（可选）">
          <div class="column-cover-upload" :class="{ 'has-cover': !!createColumnForm.cover }">
            <div v-if="createColumnForm.cover" class="column-cover-actions">
              <span class="column-cover-action-link" @click="triggerColumnCoverUpload" @keydown.enter.prevent="triggerColumnCoverUpload" role="button" tabindex="0">更换</span>
              <span class="column-cover-action-link column-cover-action-remove" @click.stop="createColumnForm.cover = ''" @keydown.enter.prevent="createColumnForm.cover = ''" role="button" tabindex="0">移除</span>
            </div>
            <div class="column-cover-preview-wrap">
              <div class="column-cover-preview" :style="createColumnForm.cover ? { backgroundImage: `url(${createColumnForm.cover})` } : {}">
                <input
                  ref="columnCoverFileInputRef"
                  type="file"
                  accept="image/*"
                  class="cover-file-input"
                  @change="onColumnCoverFileChange"
                />
                <button v-if="!createColumnForm.cover" type="button" class="cover-upload-btn column-cover-upload-btn" @click="triggerColumnCoverUpload">
                  <el-icon><Camera /></el-icon>
                  上传封面图片
                </button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createColumnVisible = false">取消</el-button>
        <el-button class="btn-confirm-column" type="primary" :loading="createColumnSubmitting" @click="submitCreateColumn">确认</el-button>
      </template>
    </el-dialog>

    <!-- 编辑专栏弹窗 -->
    <el-dialog
      v-model="editColumnVisible"
      title="编辑专栏"
      width="480px"
      top="12vh"
      class="creator-column-dialog edit-column-dialog"
      @closed="resetEditColumnForm"
    >
      <el-form :model="editColumnForm" label-position="top">
        <el-form-item label="专栏名称">
          <el-input v-model="editColumnForm.name" placeholder="专栏名称" maxlength="128" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="专栏描述（可选）">
          <el-input v-model="editColumnForm.description" type="textarea" :rows="3" placeholder="专栏描述 (可选)" maxlength="512" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="封面图（可选）">
          <div class="column-cover-upload" :class="{ 'has-cover': !!editColumnForm.cover }">
            <div v-if="editColumnForm.cover" class="column-cover-actions">
              <span class="column-cover-action-link" @click="triggerEditColumnCoverUpload" @keydown.enter.prevent="triggerEditColumnCoverUpload" role="button" tabindex="0">更换</span>
              <span class="column-cover-action-link column-cover-action-remove" @click.stop="editColumnForm.cover = ''" @keydown.enter.prevent="editColumnForm.cover = ''" role="button" tabindex="0">移除</span>
            </div>
            <div class="column-cover-preview-wrap">
              <div class="column-cover-preview" :style="editColumnForm.cover ? { backgroundImage: `url(${editColumnForm.cover})` } : {}">
                <input
                  ref="editColumnCoverFileInputRef"
                  type="file"
                  accept="image/*"
                  class="cover-file-input"
                  @change="onEditColumnCoverFileChange"
                />
                <button v-if="!editColumnForm.cover" type="button" class="cover-upload-btn column-cover-upload-btn" @click="triggerEditColumnCoverUpload">
                  <el-icon><Camera /></el-icon>
                  上传封面图片
                </button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editColumnVisible = false">取消</el-button>
        <el-button class="btn-confirm-column" type="primary" :loading="editColumnSubmitting" @click="submitEditColumn">保存</el-button>
      </template>
    </el-dialog>

    <!-- 专栏封面裁剪弹窗：比例与专栏卡片封面一致 120:84 -->
    <el-dialog
      v-model="columnCropDialogVisible"
      title="选择封面区域"
      width="440px"
      :close-on-click-modal="false"
      class="creator-column-crop-dialog"
      @closed="resetColumnCropState"
    >
      <div class="column-crop-viewport-wrap">
        <div class="crop-viewport column-crop-viewport" ref="columnCropViewportRef">
          <img
            v-if="columnCropImageUrl"
            ref="columnCropImageRef"
            :src="columnCropImageUrl"
            class="crop-image"
            :style="columnCropImageStyle"
            draggable="false"
            @mousedown.prevent="onColumnCropMouseDown"
            @load="onColumnCropImageLoad"
          />
        </div>
        <div class="column-crop-frame" aria-hidden="true"></div>
      </div>
      <p class="crop-tip">拖动图片调整位置，将截取与封面相同比例的区域</p>
      <template #footer>
        <el-button @click="columnCropDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="columnCoverUploading" @click="confirmColumnCrop">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建博客机器人弹窗 -->
    <el-dialog
      v-model="createBotVisible"
      title="新建机器人"
      width="480px"
      top="12vh"
      class="creator-bot-dialog"
      @closed="resetCreateBotForm"
    >
      <el-form :model="createBotForm" label-position="top">
        <el-form-item label="机器人名称">
          <el-input v-model="createBotForm.name" placeholder="例如：技术博客助手" maxlength="32" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="头像（可选）">
          <div class="bot-avatar-upload" :class="{ uploading: botAvatarUploading }">
            <div v-if="createBotForm.avatar" class="bot-avatar-preview">
              <img :src="createBotForm.avatar" alt="头像" class="bot-avatar-img" />
              <button type="button" class="bot-avatar-remove" @click="createBotForm.avatar = ''">移除</button>
            </div>
            <template v-else>
              <input ref="botAvatarInputRef" type="file" accept="image/*" class="bot-avatar-input" @change="onBotAvatarChange" />
              <button type="button" class="bot-avatar-btn" :disabled="botAvatarUploading" @click="botAvatarInputRef?.click()">
                <el-icon v-if="botAvatarUploading" class="bot-avatar-icon is-loading"><Loading /></el-icon>
                <el-icon v-else class="bot-avatar-icon"><Camera /></el-icon>
                <span>上传头像</span>
              </button>
            </template>
          </div>
        </el-form-item>
        <el-form-item label="发文风格">
          <el-select v-model="createBotForm.style" placeholder="选择风格" class="bot-form-select">
            <el-option label="专业严谨" value="professional" />
            <el-option label="轻松活泼" value="casual" />
            <el-option label="技术向" value="technical" />
            <el-option label="故事向" value="narrative" />
          </el-select>
        </el-form-item>
        <el-form-item label="博客主标签">
          <el-select v-model="createBotForm.mainTagId" placeholder="选择主标签（可选）" clearable class="bot-form-select">
            <el-option v-for="t in mainTagListForBot" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认摘要风格">
          <el-select v-model="createBotForm.summaryStyle" placeholder="选择摘要风格" class="bot-form-select">
            <el-option label="简洁" value="concise" />
            <el-option label="详细" value="detailed" />
            <el-option label="金句提炼" value="quote" />
          </el-select>
        </el-form-item>
        <el-form-item label="字数偏好">
          <el-select v-model="createBotForm.wordCountPreference" placeholder="选择字数偏好" class="bot-form-select">
            <el-option label="短篇（约 500～1000 字）" value="short" />
            <el-option label="中篇（约 1000～2000 字）" value="medium" />
            <el-option label="长篇（2000 字以上）" value="long" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createBotVisible = false">取消</el-button>
        <el-button type="primary" :loading="createBotSubmitting" @click="submitCreateBot">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="contentTrendDialogVisible"
      width="900px"
      top="8vh"
      class="content-trend-dialog"
      destroy-on-close
    >
      <template #header>
        <div class="content-trend-head">
          <div class="content-trend-title">单篇文章数据趋势</div>
          <div class="content-trend-sub">{{ contentTrendTarget?.title || '未命名内容' }}</div>
        </div>
      </template>
      <div v-loading="contentTrendLoading" class="content-trend-body">
        <div class="content-trend-toolbar">
          <el-select v-model="contentTrendDays" size="small" class="content-trend-days">
            <el-option :value="7" label="近 7 天" />
            <el-option :value="14" label="近 14 天" />
            <el-option :value="30" label="近 30 天" />
          </el-select>
          <span class="content-trend-tip">用于快速观察单篇内容在窗口期内的增长走势</span>
        </div>
        <div class="content-trend-cards">
          <div class="content-trend-kpi"><span>阅读</span><b>{{ formatNum(contentTrendLast.views) }}</b></div>
          <div class="content-trend-kpi"><span>点赞</span><b>{{ formatNum(contentTrendLast.likes) }}</b></div>
          <div class="content-trend-kpi"><span>评论</span><b>{{ formatNum(contentTrendLast.comments) }}</b></div>
          <div class="content-trend-kpi"><span>收藏</span><b>{{ formatNum(contentTrendLast.collections) }}</b></div>
        </div>
        <div class="content-trend-chart">
          <svg viewBox="0 0 860 280" class="content-trend-svg" aria-hidden="true">
            <defs>
              <linearGradient id="articleViewsArea" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#7f56d9" stop-opacity="0.35" />
                <stop offset="100%" stop-color="#7f56d9" stop-opacity="0" />
              </linearGradient>
            </defs>
            <line x1="16" y1="264" x2="844" y2="264" class="content-trend-axis-line" />
            <polygon :points="contentTrendArea.views" fill="url(#articleViewsArea)" />
            <polyline :points="contentTrendLine.views" class="content-trend-line line-views" />
            <polyline :points="contentTrendLine.likes" class="content-trend-line line-likes" />
            <polyline :points="contentTrendLine.comments" class="content-trend-line line-comments" />
            <polyline :points="contentTrendLine.collections" class="content-trend-line line-collections" />
          </svg>
          <div
            class="content-trend-x content-trend-x--grid"
            :style="{ gridTemplateColumns: `repeat(${Math.max(contentTrendSeries.length, 1)}, minmax(0, 1fr))` }"
          >
            <span v-for="(d, i) in contentTrendXLabels" :key="i" class="content-trend-x-tick">{{ d || '\u00a0' }}</span>
          </div>
        </div>
        <div class="content-trend-legend">
          <span><i class="dot views" /> 阅读</span>
          <span><i class="dot likes" /> 点赞</span>
          <span><i class="dot comments" /> 评论</span>
          <span><i class="dot collections" /> 收藏</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Plus, House, Folder, ArrowDown, ArrowUp, Document, View, Star, Collection, Search, FolderOpened, Refresh, Camera, Loading, Delete, MoreFilled, TrendCharts } from '@element-plus/icons-vue'
import { getContentMeStats, getContentsMe, deleteContent, getCreatorAnalytics } from '@/api/content'
import type { ContentMeStats, ContentListItem, CreatorAnalytics } from '@/api/content'
import { getCommentedArticles, getContentComments, setCommentHot, likeComment, unlikeComment, deleteComment as apiDeleteComment } from '@/api/comment'
import type { CommentedArticle, CommentItem } from '@/api/comment'
import { getFollowMe, getLeaderboardInfluence, getLeaderboardGrowth, followUser, checkFollow } from '@/api/follow'
import type { FollowStats } from '@/api/follow'
import { getUsersBatch } from '@/api/user'
import { getColumnsMe, createColumn, updateColumn, deleteColumn, type ColumnItem } from '@/api/column'
import { getMainTags, type TagItem } from '@/api/tag'
import { getBlogBotsMe, createBlogBot, deleteBlogBot, type BlogBotItem } from '@/api/blogBot'
import { uploadImage } from '@/api/upload'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const contentManageOpen = ref(true)
const aiWorkbenchOpen = ref(true)
const analyticsOpen = ref(true)
const userStore = useUserStore()
const isAnalyticsPage = computed(() => route.path === '/creator/analytics')

const isCreatorHome = computed(() => route.path === '/creator')
const managementPlaceholderTitle = computed(() => {
  if (route.path === '/creator/content') return '内容管理'
  if (route.path === '/creator/comments') return '评论管理'
  if (route.path === '/creator/columns') return '专栏管理'
  return '管理'
})
const managementPlaceholderDesc = computed(() => {
  if (route.path === '/creator/comments') return '在此查看与管理读者评论，后续将支持回复、置顶、删除等操作。'
  return '请从左侧选择具体管理项。'
})

// 评论管理：左文章右评论，推荐变热评
const commentedArticles = ref<CommentedArticle[]>([])
const commentedArticlesLoading = ref(false)
const selectedContentId = ref<number | null>(null)
const commentList = ref<CommentItem[]>([])
const commentListLoading = ref(false)
const commentSort = ref<'default' | 'latest'>('default')

/** 排序后的评论列表：与博客阅读界面一致，默认按点赞+时间，最新按时间 */
const displayCommentList = computed(() => {
  const list = [...commentList.value]
  if (commentSort.value === 'latest') {
    list.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  } else {
    list.sort((a, b) => {
      const la = a.likeCount ?? 0
      const lb = b.likeCount ?? 0
      if (lb !== la) return lb - la
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })
  }
  return list
})

function formatCommentTime(iso: string) {
  if (!iso) return ''
  return iso.replace('T', ' ')
}

async function fetchCommentedArticles() {
  if (!userStore.isLoggedIn) return
  commentedArticlesLoading.value = true
  try {
    commentedArticles.value = await getCommentedArticles()
    if (commentedArticles.value.length > 0 && selectedContentId.value == null) {
      selectArticle(commentedArticles.value[0])
    }
  } finally {
    commentedArticlesLoading.value = false
  }
}
function selectArticle(art: CommentedArticle) {
  selectedContentId.value = art.contentId
  fetchCommentList()
}
async function fetchCommentList() {
  if (selectedContentId.value == null) return
  commentListLoading.value = true
  try {
    commentList.value = await getContentComments(selectedContentId.value)
  } finally {
    commentListLoading.value = false
  }
}
function formatCommentDate(s: string) {
  if (!s) return '—'
  const d = s.slice(0, 10)
  const t = s.slice(11, 16)
  if (t && t !== '00:00') return d.slice(5) + ' ' + t
  return d.slice(5)
}
async function toggleCommentHot(c: CommentItem) {
  const nextHot = !c.isHot
  try {
    await setCommentHot(c.id, nextHot)
    c.isHot = nextHot
  } catch {
    // ignore
  }
}

async function toggleCommentLike(c: CommentItem) {
  if (!userStore.isLoggedIn) return
  try {
    if (c.likedByMe) {
      await unlikeComment(c.id)
      c.likedByMe = false
      c.likeCount = Math.max(0, (c.likeCount ?? 0) - 1)
    } else {
      await likeComment(c.id)
      c.likedByMe = true
      c.likeCount = (c.likeCount ?? 0) + 1
    }
  } catch {
    // 错误已由拦截器提示
  }
}

async function deleteCommentItem(c: CommentItem) {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？删除后不可恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 用户取消
  }
  try {
    await apiDeleteComment(c.id)
    const replyIds = new Set(commentList.value.filter((x) => x.parentId === c.id).map((x) => x.id))
    replyIds.add(c.id)
    commentList.value = commentList.value.filter((x) => !replyIds.has(x.id))
    ElMessage.success('已删除')
  } catch {
    // 错误已由拦截器提示
  }
}

// 专栏管理：参考个人主页「我的专栏」
const columnList = ref<ColumnItem[]>([])
const columnLoading = ref(false)
const createColumnVisible = ref(false)
const createColumnSubmitting = ref(false)
const createColumnForm = ref({ name: '', description: '', cover: '' })
async function fetchColumnList() {
  if (!userStore.isLoggedIn) return
  columnLoading.value = true
  try {
    columnList.value = await getColumnsMe()
  } finally {
    columnLoading.value = false
  }
}
function openCreateColumn() {
  createColumnForm.value = { name: '', description: '', cover: '' }
  createColumnVisible.value = true
}
function resetCreateColumnForm() {
  createColumnForm.value = { name: '', description: '', cover: '' }
}
function submitCreateColumn() {
  const name = createColumnForm.value.name?.trim()
  if (!name) {
    ElMessage.warning('请输入专栏名称')
    return
  }
  createColumnSubmitting.value = true
  createColumn({
    name,
    description: createColumnForm.value.description?.trim() || undefined,
    cover: createColumnForm.value.cover?.trim() || undefined,
  })
    .then((created) => {
      columnList.value = [...columnList.value, created]
      createColumnVisible.value = false
      resetCreateColumnForm()
      ElMessage.success('专栏已创建')
    })
    .finally(() => { createColumnSubmitting.value = false })
}

// 编辑专栏
const editColumnVisible = ref(false)
const editColumnId = ref<number | null>(null)
const editColumnSubmitting = ref(false)
const editColumnForm = ref({ name: '', description: '', cover: '' })
function openEditColumn(item: ColumnItem) {
  editColumnId.value = item.id
  editColumnForm.value = {
    name: item.name,
    description: item.description ?? '',
    cover: item.cover ?? '',
  }
  editColumnVisible.value = true
}
function resetEditColumnForm() {
  editColumnId.value = null
  editColumnForm.value = { name: '', description: '', cover: '' }
}
function submitEditColumn() {
  const id = editColumnId.value
  if (id == null) return
  const name = editColumnForm.value.name?.trim()
  if (!name) {
    ElMessage.warning('请输入专栏名称')
    return
  }
  editColumnSubmitting.value = true
  updateColumn(id, {
    name,
    description: editColumnForm.value.description?.trim() || undefined,
    cover: editColumnForm.value.cover?.trim() || undefined,
  })
    .then((updated) => {
      columnList.value = columnList.value.map((c) => (c.id === id ? updated : c))
      editColumnVisible.value = false
      resetEditColumnForm()
      ElMessage.success('专栏已更新')
    })
    .finally(() => { editColumnSubmitting.value = false })
}
function confirmDeleteColumn(item: ColumnItem) {
  ElMessageBox.confirm(`确定要删除专栏「${item.name}」吗？该专栏下的文章将变为未归类。`, '删除专栏', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => deleteColumn(item.id))
    .then(() => {
      columnList.value = columnList.value.filter((c) => c.id !== item.id)
      ElMessage.success('专栏已删除')
    })
}

// 博客机器人：前端先做配置列表与新建弹窗，后续接后端
const BOT_STYLE_LABELS: Record<string, string> = {
  professional: '专业严谨',
  casual: '轻松活泼',
  technical: '技术向',
  narrative: '故事向',
}
const botList = ref<BlogBotItem[]>([])
const botLoading = ref(false)
const createBotVisible = ref(false)
const createBotSubmitting = ref(false)
const botAvatarUploading = ref(false)
const botAvatarInputRef = ref<HTMLInputElement | null>(null)
const mainTagListForBot = ref<TagItem[]>([])
const createBotForm = ref({
  name: '',
  avatar: '' as string,
  style: 'professional',
  mainTagId: undefined as number | undefined,
  summaryStyle: 'concise',
  wordCountPreference: 'medium',
})
async function onBotAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !file.type.startsWith('image/')) return
  botAvatarUploading.value = true
  try {
    const meta = await uploadImage(file, 'bot-avatar')
    if (meta?.url) createBotForm.value.avatar = meta.url
  } finally {
    botAvatarUploading.value = false
  }
}
function openCreateBot() {
  resetCreateBotForm()
  createBotVisible.value = true
  getMainTags().then((list) => { mainTagListForBot.value = list ?? [] })
}
function resetCreateBotForm() {
  createBotForm.value = {
    name: '',
    avatar: '',
    style: 'professional',
    mainTagId: undefined,
    summaryStyle: 'concise',
    wordCountPreference: 'medium',
  }
}
async function submitCreateBot() {
  const name = createBotForm.value.name?.trim()
  if (!name) {
    ElMessage.warning('请输入机器人名称')
    return
  }
  createBotSubmitting.value = true
  try {
    const created = await createBlogBot({
      name,
      avatar: createBotForm.value.avatar || undefined,
      style: createBotForm.value.style,
      mainTagId: createBotForm.value.mainTagId,
      summaryStyle: createBotForm.value.summaryStyle,
      wordCountPreference: createBotForm.value.wordCountPreference,
    })
    botList.value = [...botList.value, created]
    createBotVisible.value = false
    resetCreateBotForm()
    ElMessage.success('机器人已添加')
  } finally {
    createBotSubmitting.value = false
  }
}
function onDeleteBot(bot: BlogBotItem) {
  ElMessageBox.confirm(`确定要删除「${bot.name}」吗？删除后不可恢复。`, '删除机器人', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
    .then(() => deleteBlogBot(bot.id))
    .then(() => {
      botList.value = botList.value.filter((b) => b.id !== bot.id)
      ElMessage.success('已删除')
    })
    .catch((err) => {
      if (err !== 'cancel') throw err
    })
}

// 专栏封面裁剪：比例与列表展示一致 120:84（约 10:7）
const COLUMN_COVER_VIEWPORT_W = 400
const COLUMN_COVER_VIEWPORT_H = 280
const COLUMN_COVER_OUTPUT_W = 400
const COLUMN_COVER_OUTPUT_H = 280
const columnCoverFileInputRef = ref<HTMLInputElement | null>(null)
const columnCropDialogVisible = ref(false)
const columnCropImageUrl = ref('')
const columnCropImageRef = ref<HTMLImageElement | null>(null)
const columnCropViewportRef = ref<HTMLDivElement | null>(null)
const columnCoverUploading = ref(false)
const columnCropState = ref({
  naturalW: 0,
  naturalH: 0,
  scale: 1,
  translateX: 0,
  translateY: 0,
  isDragging: false,
  startX: 0,
  startY: 0,
  startTranslateX: 0,
  startTranslateY: 0,
})
const columnCropImageStyle = computed(() => {
  const s = columnCropState.value
  const w = s.naturalW * s.scale
  const h = s.naturalH * s.scale
  return {
    width: `${w}px`,
    height: `${h}px`,
    transform: `translate(${s.translateX}px, ${s.translateY}px)`,
    cursor: s.isDragging ? 'grabbing' : 'grab',
  }
})
const columnCropTarget = ref<'create' | 'edit'>('create')
function triggerColumnCoverUpload() {
  columnCropTarget.value = 'create'
  columnCoverFileInputRef.value?.click()
}
const editColumnCoverFileInputRef = ref<HTMLInputElement | null>(null)
function triggerEditColumnCoverUpload() {
  columnCropTarget.value = 'edit'
  editColumnCoverFileInputRef.value?.click()
}
function openColumnCropWithFile(file: File) {
  columnCropImageUrl.value = URL.createObjectURL(file)
  columnCropDialogVisible.value = true
}
function onColumnCoverFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !file.type.startsWith('image/')) return
  openColumnCropWithFile(file)
  input.value = ''
}
function onEditColumnCoverFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !file.type.startsWith('image/')) return
  openColumnCropWithFile(file)
  input.value = ''
}
function onColumnCropImageLoad() {
  const img = columnCropImageRef.value
  if (!img || !columnCropViewportRef.value) return
  const nw = img.naturalWidth
  const nh = img.naturalHeight
  const scale = Math.max(COLUMN_COVER_VIEWPORT_W / nw, COLUMN_COVER_VIEWPORT_H / nh)
  const displayW = nw * scale
  const displayH = nh * scale
  columnCropState.value = {
    ...columnCropState.value,
    naturalW: nw,
    naturalH: nh,
    scale,
    translateX: (COLUMN_COVER_VIEWPORT_W - displayW) / 2,
    translateY: (COLUMN_COVER_VIEWPORT_H - displayH) / 2,
  }
}
function onColumnCropMouseDown(e: MouseEvent) {
  columnCropState.value.isDragging = true
  columnCropState.value.startX = e.clientX
  columnCropState.value.startY = e.clientY
  columnCropState.value.startTranslateX = columnCropState.value.translateX
  columnCropState.value.startTranslateY = columnCropState.value.translateY
  const onMove = (e2: MouseEvent) => {
    const dx = e2.clientX - columnCropState.value.startX
    const dy = e2.clientY - columnCropState.value.startY
    const s = columnCropState.value
    const displayW = s.naturalW * s.scale
    const displayH = s.naturalH * s.scale
    let tx = s.startTranslateX + dx
    let ty = s.startTranslateY + dy
    tx = Math.max(COLUMN_COVER_VIEWPORT_W - displayW, Math.min(0, tx))
    ty = Math.max(COLUMN_COVER_VIEWPORT_H - displayH, Math.min(0, ty))
    columnCropState.value.translateX = tx
    columnCropState.value.translateY = ty
  }
  const onUp = () => {
    columnCropState.value.isDragging = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}
function confirmColumnCrop() {
  const img = columnCropImageRef.value
  if (!img || !img.complete) return
  const s = columnCropState.value
  const sx = -s.translateX / s.scale
  const sy = -s.translateY / s.scale
  const sw = COLUMN_COVER_VIEWPORT_W / s.scale
  const sh = COLUMN_COVER_VIEWPORT_H / s.scale
  const canvas = document.createElement('canvas')
  canvas.width = COLUMN_COVER_OUTPUT_W
  canvas.height = COLUMN_COVER_OUTPUT_H
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.drawImage(img, sx, sy, sw, sh, 0, 0, COLUMN_COVER_OUTPUT_W, COLUMN_COVER_OUTPUT_H)
  columnCoverUploading.value = true
  canvas.toBlob(
    (blob) => {
      if (!blob) {
        columnCoverUploading.value = false
        return
      }
      const file = new File([blob], 'column-cover.jpg', { type: 'image/jpeg' })
      uploadImage(file, 'cover')
        .then((meta) => {
          if (columnCropTarget.value === 'edit') {
            editColumnForm.value.cover = meta.url
          } else {
            createColumnForm.value.cover = meta.url
          }
          columnCropDialogVisible.value = false
        })
        .finally(() => { columnCoverUploading.value = false })
    },
    'image/jpeg',
    0.9
  )
}
function resetColumnCropState() {
  if (columnCropImageUrl.value) URL.revokeObjectURL(columnCropImageUrl.value)
  columnCropImageUrl.value = ''
  columnCropState.value = {
    naturalW: 0,
    naturalH: 0,
    scale: 1,
    translateX: 0,
    translateY: 0,
    isDragging: false,
    startX: 0,
    startY: 0,
    startTranslateX: 0,
    startTranslateY: 0,
  }
}

// 内容管理：文章列表
const cmStatus = ref<'ALL' | 'PUBLISHED' | 'DRAFT'>('ALL')
const cmPage = ref(1)
const cmPageSize = ref(5)
const cmList = ref<ContentListItem[]>([])
const cmTotal = ref(0)
/** 「全部」Tab 旁展示的总数：始终为 status=ALL 且与当前可见范围/关键词一致的总条数，不随已发布/草稿 Tab 变化 */
const cmTotalAllFiltered = ref(0)
const cmLoading = ref(false)
const cmVisibility = ref<'' | 'ALL' | 'SELF' | 'FANS'>('')
const cmKeyword = ref('')
const cmSortBy = ref<'time' | 'likes' | 'views'>('time')
const cmOrder = ref<'asc' | 'desc'>('desc')
const contentTrendDialogVisible = ref(false)
const contentTrendLoading = ref(false)
const contentTrendDays = ref(14)
const contentTrendTarget = ref<ContentListItem | null>(null)
type ContentTrendPoint = { date: string; views: number; likes: number; comments: number; collections: number }
const contentTrendSeries = ref<ContentTrendPoint[]>([])
const cmVisibilityLabel = computed(() => {
  if (cmVisibility.value === '') return '不限'
  const map = { ALL: '全部可见', SELF: '仅我可见', FANS: '粉丝可见' }
  return map[cmVisibility.value]
})
function setCmStatus(s: 'ALL' | 'PUBLISHED' | 'DRAFT') {
  cmStatus.value = s
  cmPage.value = 1
  fetchCmList()
}
function setCmVisibility(cmd: string) {
  if (cmd === 'UNLIMITED') cmVisibility.value = ''
  else if (cmd === 'ALL' || cmd === 'SELF' || cmd === 'FANS') cmVisibility.value = cmd
  else cmVisibility.value = ''
  cmPage.value = 1
  fetchCmList()
}
/** 内容管理日期显示：去掉 ISO 里的 T */
function formatCmDate(iso: string | undefined) {
  if (!iso) return ''
  return iso.replace('T', ' ')
}

function buildArticleTrend(item: ContentListItem, days: number): ContentTrendPoint[] {
  const points: ContentTrendPoint[] = []
  const end = new Date()
  const startPublish = new Date(item.publishedAt || item.createdAt || end.toISOString())
  const spanMs = Math.max(1, end.getTime() - startPublish.getTime())
  let prev = { views: 0, likes: 0, comments: 0, collections: 0 }
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(end.getTime() - i * 24 * 3600 * 1000)
    const p = Math.max(0, Math.min(1, (d.getTime() - startPublish.getTime()) / spanMs))
    const eased = Math.pow(p, 1.45)
    const seed = (item.id * 97 + i * 13) % 11
    const wobble = (Math.sin(seed) + 1) * 0.02
    const progress = Math.min(1, eased + wobble)
    const cur = {
      views: Math.max(prev.views, Math.round((item.viewCount || 0) * progress)),
      likes: Math.max(prev.likes, Math.round((item.likeCount || 0) * progress)),
      comments: Math.max(prev.comments, Math.round((item.commentCount || 0) * progress)),
      collections: Math.max(prev.collections, Math.round((item.collectionCount || 0) * progress)),
    }
    prev = cur
    points.push({
      date: d.toISOString().slice(0, 10),
      ...cur,
    })
  }
  return points
}

function openContentTrendDialog(item: ContentListItem) {
  contentTrendTarget.value = item
  contentTrendDialogVisible.value = true
  contentTrendLoading.value = true
  setTimeout(() => {
    contentTrendSeries.value = buildArticleTrend(item, contentTrendDays.value)
    contentTrendLoading.value = false
  }, 120)
}

function buildTrendPolyline(values: number[], width: number, height: number, pad = 16): string {
  if (!values.length) return ''
  const max = Math.max(...values, 1)
  const n = values.length
  return values
    .map((v, i) => {
      const x = pad + (i / Math.max(1, n - 1)) * (width - pad * 2)
      const y = height - pad - (v / max) * (height - pad * 2)
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
}

function buildTrendArea(line: string, width: number, height: number, pad = 16): string {
  if (!line) return ''
  return `${pad},${height - pad} ${line} ${width - pad},${height - pad}`
}

async function fetchCmList() {
  if (!userStore.isLoggedIn) return
  cmLoading.value = true
  try {
    const kw = cmKeyword.value?.trim()
    const base = {
      visibility: cmVisibility.value || undefined,
      sortBy: cmSortBy.value,
      order: cmOrder.value,
      q: kw || undefined,
      type: 'BLOG' as const,
    }
    const [res, allRes] = await Promise.all([
      getContentsMe({
        page: cmPage.value,
        pageSize: cmPageSize.value,
        status: cmStatus.value,
        ...base,
      }),
      getContentsMe({
        page: 1,
        pageSize: 1,
        status: 'ALL',
        ...base,
      }),
    ])
    cmTotal.value = res.total
    cmList.value = res.list
    cmTotalAllFiltered.value = allRes.total
  } finally {
    cmLoading.value = false
  }
}

/** 搜索时回到第一页并请求（与分页一起在后端搜） */
function runCmSearch() {
  cmPage.value = 1
  fetchCmList()
}

/** 删除博客：确认后调用接口并刷新列表 */
function confirmDeleteContent(item: ContentListItem) {
  const title = item.title?.trim() || '未命名'
  ElMessageBox.confirm(`确定要删除「${title}」吗？删除后不可恢复。`, '删除博客', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await deleteContent(item.id)
      ElMessage.success('已删除')
      getContentsMe({ page: 1, pageSize: 1, type: 'BLOG' }).then((res) => { contentTotal.value = res.total }).catch(() => {})
      fetchCmList()
    })
    .catch(() => {})
}

watch(cmPage, () => { fetchCmList() })
async function fetchBotList() {
  if (!userStore.isLoggedIn) return
  botLoading.value = true
  try {
    botList.value = await getBlogBotsMe()
  } finally {
    botLoading.value = false
  }
}
watch(() => route.path, (path) => {
  if (path === '/creator/content') fetchCmList()
  if (path === '/creator/comments') fetchCommentedArticles()
  if (path === '/creator/columns') fetchColumnList()
  if (path === '/creator/ai/blog') fetchBotList()
  if (path === '/creator/analytics') loadCreatorAnalytics()
})

const rankingTab = ref<'influence' | 'growth'>('influence')
const contentStats = ref<ContentMeStats | null>(null)
const followStats = ref<FollowStats | null>(null)
const contentTotal = ref<number | null>(null)

// 近期博客：与 Profile 我的博客一致
const blogList = ref<ContentListItem[]>([])
const blogTotal = ref(0)
const blogPage = ref(1)
const blogPageSize = 10
const blogLoading = ref(false)
const blogVisibility = ref<'ALL' | 'SELF' | 'FANS'>('ALL')
const blogSortBy = ref<'time' | 'likes' | 'views'>('time')
const blogOrder = ref<'asc' | 'desc'>('desc')
const blogVisibilityLabel = computed(() => {
  const map = { ALL: '全部可见', SELF: '仅我可见', FANS: '粉丝可见' }
  return map[blogVisibility.value]
})
function setBlogVisibility(v: 'ALL' | 'SELF' | 'FANS') {
  blogVisibility.value = v
  blogPage.value = 1
  fetchBlogList()
}
function setBlogSort(field: 'time' | 'likes' | 'views') {
  if (blogSortBy.value === field) {
    blogOrder.value = blogOrder.value === 'desc' ? 'asc' : 'desc'
  } else {
    blogSortBy.value = field
    blogOrder.value = 'desc'
  }
  blogPage.value = 1
  fetchBlogList()
}
async function fetchBlogList() {
  if (!userStore.isLoggedIn) return
  blogLoading.value = true
  try {
    const res = await getContentsMe({
      page: blogPage.value,
      pageSize: blogPageSize,
      visibility: blogVisibility.value,
      sortBy: blogSortBy.value,
      order: blogOrder.value,
      type: 'BLOG',
    })
    blogList.value = res.list
    blogTotal.value = res.total
  } finally {
    blogLoading.value = false
  }
}
function formatBlogCount(n: number) {
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
function formatBlogCreatedAt(s: string) {
  if (!s) return ''
  return s.slice(0, 10)
}

function formatNum(n: number): string {
  return n >= 10000 ? (n / 10000).toFixed(1).replace(/\.0$/, '') + '万' : n.toLocaleString()
}

function formatScore(s: number | undefined): string {
  if (s == null || Number.isNaN(Number(s))) return '0.0'
  return Number(s).toFixed(1)
}

const contentTrendLine = computed(() => {
  const arr = contentTrendSeries.value
  const views = arr.map((d) => d.views)
  const likes = arr.map((d) => d.likes)
  const comments = arr.map((d) => d.comments)
  const collections = arr.map((d) => d.collections)
  return {
    views: buildTrendPolyline(views, 860, 280),
    likes: buildTrendPolyline(likes, 860, 280),
    comments: buildTrendPolyline(comments, 860, 280),
    collections: buildTrendPolyline(collections, 860, 280),
  }
})
const contentTrendArea = computed(() => ({
  views: buildTrendArea(contentTrendLine.value.views, 860, 280),
}))
const contentTrendXLabels = computed(() =>
  buildSparseAxisLabels(
    contentTrendSeries.value.map((d) => d.date),
    8,
  ),
)
const contentTrendLast = computed(() => {
  const last = contentTrendSeries.value[contentTrendSeries.value.length - 1]
  return last || { views: 0, likes: 0, comments: 0, collections: 0 }
})

function yesterdayText(delta: number): string {
  return delta > 0 ? `昨日 +${delta}` : '昨日无变化'
}

watch(blogPage, () => { fetchBlogList() })
watch(contentTrendDays, () => {
  if (contentTrendTarget.value) {
    contentTrendSeries.value = buildArticleTrend(contentTrendTarget.value, contentTrendDays.value)
  }
})

onMounted(() => {
  getContentMeStats().then((data) => { contentStats.value = data }).catch(() => {})
  getFollowMe().then((data) => { followStats.value = data }).catch(() => {})
  getContentsMe({ page: 1, pageSize: 1, type: 'BLOG' }).then((res) => { contentTotal.value = res.total }).catch(() => {})
  if (route.path === '/creator/columns') fetchColumnList()
  if (route.path === '/creator/comments') fetchCommentedArticles()
  if (route.path === '/creator/ai/blog') fetchBotList()
  fetchBlogList()
  if (route.path === '/creator/content') fetchCmList()
  if (route.path === '/creator') fetchRankingInfluence()
  if (route.path === '/creator/analytics') loadCreatorAnalytics()
})

// 排行榜：影响力榜 = 粉丝数排行，成长力榜 = 近期涨粉排行
interface RankingItem {
  id: number
  rank: number
  name: string
  desc: string
  avatar?: string
  following?: boolean
}
const influenceList = ref<RankingItem[]>([])
const growthList = ref<RankingItem[]>([])
const rankingLoading = ref(false)

const analyticsPanels = [
  { id: 'overview', label: '总览' },
  { id: 'trend', label: '趋势' },
  { id: 'tags', label: '标签洞察' },
  { id: 'length', label: '正文长度' },
  { id: 'heatmap', label: '创作热力' },
  { id: 'top', label: '爆款内容' },
] as const
type AnalyticsPanelId = (typeof analyticsPanels)[number]['id']
const analyticsPanel = ref<AnalyticsPanelId>('overview')
const activeAnalyticsLabel = computed(() => analyticsPanels.find((p) => p.id === analyticsPanel.value)?.label || '数据洞察')
const analyticsDays = ref(30)
const analyticsLoading = ref(false)
const creatorAnalytics = ref<CreatorAnalytics | null>(null)

const weekLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

function goAnalyticsPanel(panel: AnalyticsPanelId) {
  analyticsPanel.value = panel
  if (route.path !== '/creator/analytics') {
    router.push('/creator/analytics')
  }
}

/** 与数据点等长的横轴标签：仅在若干刻度显示 MM-DD，其余为空格占位，配合 grid 等分列对齐 */
function buildSparseAxisLabels(dates: string[], maxTicks = 7): string[] {
  const n = dates.length
  if (n === 0) return []
  const out = Array.from({ length: n }, () => '')
  const ticks = Math.min(maxTicks, n)
  const idxs = new Set<number>()
  for (let t = 0; t < ticks; t++) {
    const i = ticks === 1 ? 0 : Math.round((t * (n - 1)) / Math.max(ticks - 1, 1))
    idxs.add(Math.min(n - 1, Math.max(0, i)))
  }
  idxs.forEach((i) => {
    const d = dates[i]
    out[i] = d && d.length >= 10 ? d.slice(5, 10) : (d?.slice(5) ?? '')
  })
  return out
}

function buildTrendSvg(
  points: { score?: number }[],
  w: number,
  h: number,
): { line: string; area: string } {
  if (!points.length) return { line: '', area: '' }
  const scores = points.map((p) => Number(p.score ?? 0))
  const max = Math.max(...scores, 1e-6)
  const min = Math.min(...scores, 0)
  const span = Math.max(max - min, 1e-6)
  const padX = 16
  const padY = 14
  const n = points.length
  const xs = points.map((_, i) => padX + (i / Math.max(n - 1, 1)) * (w - 2 * padX))
  const ys = scores.map((s) => padY + (1 - (s - min) / span) * (h - 2 * padY))
  const line = xs.map((x, i) => `${x.toFixed(1)},${ys[i].toFixed(1)}`).join(' ')
  const area = `${padX},${h - padY} ${xs.map((x, i) => `${x.toFixed(1)},${ys[i].toFixed(1)}`).join(' ')} ${w - padX},${h - padY}`
  return { line, area }
}

const trendMiniSlice = computed(() => {
  const t = creatorAnalytics.value?.trend || []
  return t.slice(-14)
})
const trendMiniLinePoints = computed(() => buildTrendSvg(trendMiniSlice.value, 640, 140).line)
const trendMiniAreaPoints = computed(() => buildTrendSvg(trendMiniSlice.value, 640, 140).area)
const trendMiniXLabels = computed(() =>
  buildSparseAxisLabels(
    trendMiniSlice.value.map((p) => p.date),
    7,
  ),
)

const trendFullSlice = computed(() => creatorAnalytics.value?.trend || [])
const trendFullLinePoints = computed(() => buildTrendSvg(trendFullSlice.value, 920, 260).line)
const trendFullAreaPoints = computed(() => buildTrendSvg(trendFullSlice.value, 920, 260).area)

const trendXLabels = computed(() =>
  buildSparseAxisLabels(
    trendFullSlice.value.map((p) => p.date),
    12,
  ),
)

const trendSumViews = computed(() => trendFullSlice.value.reduce((s, p) => s + Number(p.views || 0), 0))
const trendMaxScore = computed(() => {
  const t = trendFullSlice.value
  if (!t.length) return '0'
  const m = Math.max(...t.map((p) => Number(p.score ?? 0)))
  return m.toFixed(1)
})
const trendAvgPublished = computed(() => {
  const t = trendFullSlice.value
  if (!t.length) return '0'
  const sum = t.reduce((s, p) => s + Number(p.publishedCount || 0), 0)
  return (sum / t.length).toFixed(2)
})

const trendPublishBars = computed(() => {
  const t = trendFullSlice.value
  const max = Math.max(...t.map((d) => d.publishedCount || 0), 1)
  return t.map((d) => ({
    date: d.date,
    n: d.publishedCount || 0,
    pct: ((d.publishedCount || 0) / max) * 100,
  }))
})

const tagMaxEngagement = computed(() => {
  const tags = creatorAnalytics.value?.tagInsights || []
  return Math.max(...tags.map((t) => t.engagement || 0), 1)
})
function tagBarPercent(eng: number) {
  return Math.max(6, Math.round(((eng || 0) / tagMaxEngagement.value) * 100))
}

const heatHourCells = computed(() => {
  const raw = creatorAnalytics.value?.heatmap?.hourCounts || []
  const arr = [...raw]
  while (arr.length < 24) arr.push(0)
  const arr24 = arr.slice(0, 24)
  const max = Math.max(...arr24, 1)
  return arr24.map((n) => ({ n, int: n / max }))
})

const heatWeekCells = computed(() => {
  const raw = creatorAnalytics.value?.heatmap?.weekDayCounts || []
  const arr = [...raw]
  while (arr.length < 7) arr.push(0)
  const arr7 = arr.slice(0, 7)
  const max = Math.max(...arr7, 1)
  return arr7.map((n) => ({ n, pct: (n / max) * 100 }))
})

const overviewKpis = computed(() => {
  const o = creatorAnalytics.value?.overview
  if (!o) return [] as Array<{ key: string; label: string; value: string; sub?: string }>
  const pct = (x: number) => `${(x * 100).toFixed(0)}%`
  return [
    { key: 'a', label: '全部内容', value: formatNum(Number(o.totalContents ?? 0)) },
    { key: 'b', label: '已发布', value: formatNum(Number(o.publishedContents ?? 0)) },
    { key: 'c', label: '草稿', value: formatNum(Number(o.draftContents ?? 0)) },
    { key: 'd', label: '总阅读', value: formatNum(Number(o.totalViews ?? 0)) },
    { key: 'e', label: '总互动', value: formatNum(Number(o.totalEngagement ?? 0)), sub: '赞+藏+评' },
    { key: 'f', label: '均篇阅读', value: (o.avgViewsPerPublished ?? 0).toFixed(1) },
    { key: 'g', label: '均篇互动', value: (o.avgEngagementPerPublished ?? 0).toFixed(1) },
    { key: 'h', label: '发布率', value: pct(Number(o.publishRate ?? 0)) },
    { key: 'i', label: '粉丝', value: formatNum(Number(o.followers ?? 0)) },
    { key: 'j', label: '关注', value: formatNum(Number(o.following ?? 0)) },
  ]
})

async function loadCreatorAnalytics() {
  if (!userStore.isLoggedIn) return
  analyticsLoading.value = true
  try {
    creatorAnalytics.value = await getCreatorAnalytics(analyticsDays.value)
  } catch {
    creatorAnalytics.value = null
    ElMessage.error('加载数据分析失败')
  } finally {
    analyticsLoading.value = false
  }
}

watch(analyticsDays, () => {
  if (route.path === '/creator/analytics') loadCreatorAnalytics()
})

function rankingDesc(intro?: string): string {
  const text = String(intro || '').trim()
  if (!text) return '暂无简介'
  // 被资料审核拦截的账号不在排行榜泄露一句话介绍
  if (text.includes('未通过审核') || text.includes('资料暂不可见') || text.includes('资料已隐藏')) {
    return '资料暂不可见'
  }
  return text
}

async function fetchRankingInfluence() {
  rankingLoading.value = true
  try {
    const ids = await getLeaderboardInfluence(10)
    if (!ids.length) { influenceList.value = []; return }
    const users = await getUsersBatch(ids)
    const list: RankingItem[] = users.map((u, i) => ({
      id: u.id,
      rank: i + 1,
      name: u.nickname || u.username || '用户',
      desc: rankingDesc(u.intro),
      avatar: u.avatar,
      following: false,
    }))
    if (userStore.token && list.length > 0) {
      const checks = await Promise.all(list.map((u) => checkFollow(u.id)))
      list.forEach((u, i) => { u.following = checks[i]?.following ?? false })
    }
    influenceList.value = list
  } catch {
    influenceList.value = []
  } finally {
    rankingLoading.value = false
  }
}

async function fetchRankingGrowth() {
  rankingLoading.value = true
  try {
    const ids = await getLeaderboardGrowth(10)
    if (!ids.length) { growthList.value = []; return }
    const users = await getUsersBatch(ids)
    const list: RankingItem[] = users.map((u, i) => ({
      id: u.id,
      rank: i + 1,
      name: u.nickname || u.username || '用户',
      desc: rankingDesc(u.intro),
      avatar: u.avatar,
      following: false,
    }))
    if (userStore.token && list.length > 0) {
      const checks = await Promise.all(list.map((u) => checkFollow(u.id)))
      list.forEach((u, i) => { u.following = checks[i]?.following ?? false })
    }
    growthList.value = list
  } catch {
    growthList.value = []
  } finally {
    rankingLoading.value = false
  }
}

async function onRankingFollow(item: RankingItem) {
  if (!userStore.token) { ElMessage.warning('请先登录'); return }
  if (item.following) return
  try {
    await followUser(item.id)
    item.following = true
    ElMessage.success('关注成功')
  } catch {
    // 拦截器已提示
  }
}

const rankingList = computed(() =>
  rankingTab.value === 'influence' ? influenceList.value : growthList.value
)

watch(rankingTab, (tab) => {
  if (tab === 'influence') fetchRankingInfluence()
  else fetchRankingGrowth()
})

const avatarUrl = computed(() => (userStore.userInfo as { avatar?: string })?.avatar || '')
const avatarInitial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || '用'
  return name.charAt(0).toUpperCase()
})
</script>

<style scoped>
.creator-layout {
  min-height: 100vh;
  background: #f5f5f5;
}

.creator-header {
  height: 64px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-link {
  display: flex;
  align-items: center;
  cursor: pointer;
  text-decoration: none;
}

.creator-logo {
  height: 40px;
  width: auto;
  transition: transform 0.2s ease;
}

.logo-link:hover .creator-logo {
  transform: scale(1.08);
}

.creator-title {
  font-size: 18px;
  font-weight: 600;
  color: #111;
}

.creator-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #111;
  overflow: hidden;
}

.creator-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.creator-avatar .avatar-initial {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.creator-body {
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  gap: 24px;
}

.creator-sidebar-left {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  height: fit-content;
}

.create-btn-full {
  width: 100%;
  margin-bottom: 20px;
}

.create-btn {
  background: #b31b1b !important;
  border-color: #b31b1b !important;
}

.create-btn:hover {
  background: #8b0000 !important;
  border-color: #8b0000 !important;
}

.creator-nav {
  display: flex;
  flex-direction: column;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  color: #444;
  text-decoration: none;
  border-radius: 6px;
  transition: background 0.2s, color 0.2s;
}

.nav-item:hover {
  background: #f0f0f0;
  color: #111;
}

.nav-item.active {
  background: #fff5f5;
  color: #111;
  font-weight: 500;
  border-left: 3px solid #b31b1b;
  margin-left: -3px;
  padding-left: 19px;
}

.nav-group {
  margin-top: 4px;
}

.nav-group-title {
  width: 100%;
  text-align: left;
  cursor: pointer;
  border: none;
  background: transparent;
  font: inherit;
}

.nav-group-title span {
  flex: 1;
}

.nav-chevron {
  margin-left: auto;
  font-size: 12px;
  transition: transform 0.2s;
}

.nav-chevron.open {
  transform: rotate(180deg);
}

.nav-sub {
  padding-left: 8px;
  margin-top: 2px;
  margin-left: 18px;
}

.nav-sub-item {
  padding: 10px 16px 10px 12px !important;
  font-size: 14px;
}
.nav-sub-btn {
  width: 100%;
  border: none;
  text-align: left;
  background: transparent;
  cursor: pointer;
}

.creator-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.creator-sidebar-right {
  width: 280px;
  flex-shrink: 0;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

/* 内容管理页 */
.content-management-wrap {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 0;
  flex: 1;
}
.content-management-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.cm-tabs {
  margin-bottom: 16px;
}
.cm-tab {
  font-size: 16px;
  font-weight: 600;
  color: #111;
  padding-bottom: 8px;
  border-bottom: 2px solid #b31b1b;
}
.cm-status-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.cm-status-tab {
  padding: 6px 14px;
  font-size: 14px;
  color: #666;
  background: #f5f5f5;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}
.cm-status-tab:hover {
  color: #111;
  background: #eee;
}
.cm-status-tab.active {
  color: #b31b1b;
  background: #fff5f5;
}
.cm-filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.cm-filter-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
}
.cm-filter-label:hover {
  color: #111;
}
.cm-filter-arrow {
  font-size: 12px;
  color: #999;
}
.cm-search {
  width: 220px;
}
.cm-search-icon {
  cursor: pointer;
  color: #999;
}
.cm-search-icon:hover {
  color: #b31b1b;
}
.cm-loading {
  padding: 24px 0;
  text-align: center;
  color: #888;
  font-size: 14px;
}
.cm-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.cm-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.cm-table th {
  text-align: left;
  padding: 12px 16px;
  font-weight: 600;
  color: #666;
  background: #fafafa;
  border-bottom: 1px solid #eee;
}
.cm-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  color: #333;
  vertical-align: middle;
}
.cm-col-article {
  min-width: 280px;
}
.cm-col-num {
  width: 80px;
  text-align: right;
}
.cm-col-action {
  width: 1%;
  min-width: 160px;
  white-space: nowrap;
}
.cm-article-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.cm-article-thumb {
  flex-shrink: 0;
  width: 72px;
  height: 50px;
  border-radius: 4px;
  overflow: hidden;
  background: #e8e8e8;
  display: block;
}
.cm-article-thumb img,
.cm-article-thumb .cm-article-thumb-ph {
  width: 100%;
  height: 100%;
  display: block;
}
.cm-article-thumb img {
  object-fit: cover;
}
.cm-article-thumb-ph {
  background: linear-gradient(135deg, #e0e0e0 0%, #eee 100%);
}
.cm-article-info {
  flex: 1;
  min-width: 0;
}
.cm-article-title {
  display: block;
  font-weight: 500;
  color: #111;
  text-decoration: none;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cm-article-title:hover {
  color: #b31b1b;
}
.cm-article-badges {
  display: flex;
  gap: 6px;
  margin-bottom: 2px;
}
.cm-badge {
  font-size: 12px;
  padding: 0 6px;
  line-height: 18px;
  border-radius: 2px;
}
.cm-badge-original {
  color: #b31b1b;
  background: #fff5f5;
}
.cm-badge-reprint,
.cm-badge-translated {
  color: #666;
  background: #f0f0f0;
}
.cm-article-date {
  font-size: 12px;
  color: #999;
}
.cm-action {
  margin-right: 12px;
  font-size: 14px;
  color: #b31b1b;
  text-decoration: none;
  cursor: pointer;
}
.cm-action:hover {
  color: #8b0000;
}
.cm-action.cm-action-danger {
  background: none;
  border: none;
  padding: 0;
  font: inherit;
  cursor: pointer;
}
.cm-action.cm-action-btn {
  background: none;
  border: none;
  padding: 0;
  font: inherit;
}
.cm-empty {
  padding: 48px 24px;
  text-align: center;
  color: #999;
  font-size: 14px;
}
.cm-pagination-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.cm-total-text {
  font-size: 14px;
  color: #606266;
  line-height: 32px;
}
.cm-total-num {
  display: inline-block;
  transform: translateY(-2px);
}
.cm-pagination {
  justify-content: flex-start;
}
.cm-pagination :deep(.el-pager li.is-active) {
  background-color: #b31b1b;
  color: #fff;
}

.content-trend-head {
  padding-right: 20px;
}
.content-trend-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}
.content-trend-sub {
  margin-top: 6px;
  font-size: 13px;
  color: #777;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}
.content-trend-body {
  padding-top: 4px;
}
.content-trend-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}
.content-trend-days {
  width: 120px;
}
.content-trend-tip {
  font-size: 12px;
  color: #999;
}
.content-trend-cards {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.content-trend-kpi {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #f0e7f9;
  background: linear-gradient(180deg, #fff 0%, #fcf9ff 100%);
}
.content-trend-kpi span {
  font-size: 12px;
  color: #888;
}
.content-trend-kpi b {
  display: block;
  margin-top: 4px;
  font-size: 18px;
  color: #2d1f4a;
}
.content-trend-chart {
  margin-top: 12px;
  border: 1px solid #f1e8fb;
  border-radius: 12px;
  padding: 10px;
  background: #fff;
}
.content-trend-svg {
  width: 100%;
  height: auto;
  display: block;
}
.content-trend-line {
  fill: none;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.content-trend-line.line-views { stroke: #7f56d9; }
.content-trend-line.line-likes { stroke: #16a34a; }
.content-trend-line.line-comments { stroke: #f59e0b; }
.content-trend-line.line-collections { stroke: #ef4444; }
.content-trend-x--grid {
  display: grid;
  width: 100%;
  gap: 0;
  margin-top: 6px;
  font-size: 10px;
  color: #9a9a9a;
}
.content-trend-x-tick {
  min-width: 0;
  text-align: center;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.content-trend-axis-line {
  stroke: #e8e4ef;
  stroke-width: 1;
}
.content-trend-legend {
  margin-top: 10px;
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #666;
}
.content-trend-legend .dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}
.content-trend-legend .dot.views { background: #7f56d9; }
.content-trend-legend .dot.likes { background: #16a34a; }
.content-trend-legend .dot.comments { background: #f59e0b; }
.content-trend-legend .dot.collections { background: #ef4444; }

/* 评论管理：左文章右评论 */
.comments-management-wrap {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 0;
  flex: 1;
}
.comments-management-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
}
.comments-layout {
  display: flex;
  flex: 1;
  min-height: 400px;
}
.comments-sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid #f0f0f0;
  padding: 16px;
  display: flex;
  flex-direction: column;
  overflow: auto;
}
.comments-sidebar-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #111;
}
.comments-sidebar-loading,
.comments-sidebar-empty {
  padding: 24px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
}
.comments-article-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 12px;
  margin-bottom: 4px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  transition: background 0.2s;
}
.comments-article-item:hover {
  background: #f5f5f5;
}
.comments-article-item.active {
  background: #fff5f5;
  color: #b31b1b;
}
.comments-article-title {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #111;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.comments-article-item.active .comments-article-title {
  color: #b31b1b;
}
.comments-article-meta {
  font-size: 12px;
  color: #999;
}
.comments-main {
  flex: 1;
  min-width: 0;
  padding: 16px 24px;
  overflow: auto;
}
.comments-placeholder {
  padding: 48px 24px;
  text-align: center;
  color: #999;
  font-size: 14px;
}
.comments-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.comments-sort {
  display: flex;
  align-items: center;
  gap: 4px;
}
.comments-sort-btn {
  padding: 4px 12px;
  font-size: 13px;
  color: #666;
  background: #f5f5f5;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.comments-sort-btn:hover {
  color: #111;
  background: #eee;
}
.comments-sort-btn.active {
  color: #b31b1b;
  background: #fff5f5;
}
.comments-refresh {
  font-size: 18px;
  color: #999;
  cursor: pointer;
}
.comments-refresh:hover {
  color: #b31b1b;
}
.comments-loading,
.comments-empty {
  padding: 24px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
}
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.comment-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.comment-avatar {
  flex-shrink: 0;
}
.comment-more-wrap {
  margin-left: auto;
}
.comment-more {
  font-size: 16px;
  color: #999;
  cursor: pointer;
}
.comment-more:hover {
  color: #666;
}
.comment-username {
  font-size: 14px;
  font-weight: 600;
  color: #111;
}
.comment-tag-author {
  font-size: 12px;
  padding: 0 6px;
  line-height: 20px;
  border-radius: 2px;
  background: #e8e8e8;
  color: #666;
}
.comment-time {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}
.comment-body {
  margin-bottom: 8px;
}
.comment-hot-label {
  font-size: 12px;
  color: #b31b1b;
  margin-right: 8px;
}
.comment-text {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
}
.comment-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}
.comment-action {
  font-size: 13px;
  color: #999;
  cursor: pointer;
}
.comment-action:hover {
  color: #666;
}
.comment-action-recommend {
  color: #666;
}
.comment-action-recommend.active {
  color: #b31b1b;
}
.comment-action--active {
  color: #b31b1b;
}

.management-placeholder-wrap {
  display: flex;
  flex-direction: column;
  gap: 0;
  width: 100%;
  min-height: 0;
  flex: 1;
}
.management-placeholder-card {
  flex: 1;
  min-height: 420px;
  padding: 48px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.management-placeholder-title {
  margin: 0 0 20px;
  font-size: 28px;
  font-weight: 600;
  color: #111;
}
.management-placeholder-desc {
  margin: 0;
  font-size: 16px;
  color: #666;
  line-height: 1.7;
  max-width: 520px;
}

.profile-card {
  display: flex;
  align-items: center;
}

.profile-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.profile-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #111;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar .avatar-initial {
  color: #fff;
  font-size: 24px;
  font-weight: 600;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
  color: #111;
  margin-bottom: 8px;
}

.profile-stats {
  font-size: 14px;
  color: #666;
}

.profile-stats span {
  margin-right: 16px;
}

.metrics-card {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

/* 置顶数据卡：标题在上、大数字在下、昨日在下，竖线分隔 */
.metrics-card-top {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
}

.metrics-card-top .metric-item {
  flex: 1;
  min-width: 0;
  text-align: left;
  padding: 16px 20px;
  border-left: 1px solid #eee;
}

.metrics-card-top .metric-item:first-child {
  border-left: none;
}

.metrics-card-top .metric-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.metrics-card-top .metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #111;
  margin-bottom: 4px;
}

.metrics-card-top .metric-yesterday {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}

.metrics-card-top .metric-yesterday.has-delta {
  color: #b31b1b;
}

.metrics-card-top .metric-arrow {
  font-size: 12px;
  color: #b31b1b;
}

.metric-item {
  text-align: center;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #111;
  margin-bottom: 2px;
}

.metric-yesterday {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.metric-yesterday.has-delta {
  color: #b31b1b;
}

.metric-label {
  font-size: 14px;
  color: #666;
}

.analytics-dash {
  padding: 14px;
  overflow: hidden;
  border: 1px solid #efe5e5;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.04);
}
.analytics-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
  padding: 10px 12px 12px;
  border-bottom: 1px solid #f1e8e8;
}
.analytics-toolbar-left {
  min-width: 0;
}
.analytics-toolbar-title {
  margin: 0 !important;
}
.analytics-toolbar-desc {
  margin: 6px 0 0;
  color: #888;
  font-size: 13px;
}
.analytics-toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.analytics-days-select {
  width: 130px;
}
.analytics-days-select :deep(.el-input__wrapper) {
  background: #fff;
  border: 1px solid #eddede;
  box-shadow: none;
}
.analytics-body {
  padding: 6px;
  min-height: 720px;
}
.analytics-stage {
  min-height: 700px;
}
.analytics-panel {
  animation: analytics-rise 0.45s ease both;
  min-height: 680px;
}
.analytics-chart-rise {
  animation: analytics-rise 0.5s ease both;
}
@keyframes analytics-rise {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.analytics-fade-enter-active,
.analytics-fade-leave-active {
  transition: opacity 0.2s ease;
}
.analytics-fade-enter-from,
.analytics-fade-leave-to {
  opacity: 0;
}
.analytics-kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.analytics-stat-card {
  padding: 14px 12px;
  border-radius: 12px;
  background: linear-gradient(145deg, #ffffff 0%, #fff8f8 100%);
  border: 1px solid #f3e4e4;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  animation: analytics-rise 0.5s ease both;
}
.analytics-stat-label {
  font-size: 12px;
  color: #888;
}
.analytics-stat-value {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  font-variant-numeric: tabular-nums;
}
.analytics-stat-sub {
  margin-top: 4px;
  font-size: 11px;
  color: #aaa;
}
.analytics-chart-card {
  border: 1px solid #eee6e6;
  border-radius: 14px;
  padding: 16px;
  background: #fff;
}
.analytics-chart-card--glass {
  background: linear-gradient(180deg, #fff 0%, #fffbfb 100%);
}
.analytics-chart-card--wide {
  margin-bottom: 12px;
}
.analytics-chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #222;
  margin-bottom: 12px;
}
.analytics-sparkline-svg {
  width: 100%;
  height: auto;
  display: block;
}
.analytics-trend-line {
  fill: none;
  stroke: #b31b1b;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.analytics-trend-line--thick {
  stroke-width: 3;
}
.analytics-trend-svg {
  width: 100%;
  height: auto;
  display: block;
}
.analytics-trend-x {
  margin-top: 8px;
  font-size: 11px;
  color: #999;
}
.analytics-trend-x--grid {
  display: grid;
  width: 100%;
  gap: 0;
  margin-top: 6px;
}
.analytics-trend-x--grid .analytics-trend-x-item {
  min-width: 0;
  text-align: center;
  font-size: 10px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.analytics-axis-baseline {
  stroke: #e8e0e0;
  stroke-width: 1;
}
.analytics-subgrid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}
.analytics-mini-metric {
  padding: 12px;
  border-radius: 10px;
  background: #faf5f5;
  border: 1px solid #f0e6e6;
}
.analytics-mini-label {
  font-size: 12px;
  color: #888;
}
.analytics-mini-value {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 700;
  color: #b31b1b;
}
.analytics-publish-bars {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 120px;
  padding: 4px 0;
}
.analytics-publish-bar-wrap {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.analytics-publish-bar {
  width: 100%;
  max-width: 14px;
  margin: 0 auto;
  border-radius: 4px 4px 0 0;
  background: linear-gradient(180deg, #e85a5a 0%, #b31b1b 100%);
  min-height: 4px;
  transition: height 0.4s ease;
}
.analytics-empty-inline {
  color: #999;
  font-size: 14px;
  padding: 12px 0;
}
.analytics-empty {
  text-align: center;
  color: #999;
  padding: 40px 12px;
}
.bar-fill-tag {
  background: linear-gradient(90deg, #f5a0a0 0%, #8b1a1a 100%);
}
.analytics-length-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
}
.analytics-length-cell {
  text-align: center;
  animation: analytics-rise 0.55s ease both;
}
.analytics-length-ring {
  --p: 40%;
  width: 76px;
  height: 76px;
  margin: 0 auto 8px;
  border-radius: 50%;
  background: conic-gradient(#b31b1b var(--p), #f0e8e8 0);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow: inset 0 0 0 6px #fff;
}
.analytics-length-ring::after {
  content: '';
  position: absolute;
  inset: 10px;
  background: #fff;
  border-radius: 50%;
}
.analytics-length-pct {
  position: relative;
  z-index: 1;
  font-size: 14px;
  font-weight: 700;
  color: #b31b1b;
}
.analytics-length-bucket {
  font-size: 13px;
  color: #444;
}
.analytics-length-count {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.analytics-heat-row {
  display: grid;
  grid-template-columns: repeat(24, minmax(0, 1fr));
  gap: 3px;
}
.analytics-heat-cell {
  aspect-ratio: 1;
  border-radius: 4px;
  min-height: 18px;
  transition: transform 0.2s ease;
}
.analytics-heat-cell:hover {
  transform: scale(1.08);
}
.analytics-heat-labels {
  display: grid;
  grid-template-columns: repeat(24, minmax(0, 1fr));
  gap: 3px;
  margin-top: 6px;
  font-size: 10px;
  color: #aaa;
  text-align: center;
}
.analytics-heat-h {
  min-width: 0;
}
.analytics-week-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
  height: 140px;
  padding: 8px 0;
}
.analytics-week-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}
.analytics-week-bar {
  width: 100%;
  max-width: 36px;
  margin-top: auto;
  border-radius: 6px 6px 0 0;
  background: linear-gradient(180deg, #ffb3b3 0%, #b31b1b 100%);
  min-height: 6px;
  transition: height 0.45s cubic-bezier(0.34, 1.2, 0.64, 1);
}
.analytics-week-label {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}
.analytics-top-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.analytics-top-item {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 12px 10px;
  border-radius: 10px;
  border: 1px solid #f3e8e8;
  margin-bottom: 8px;
  background: linear-gradient(90deg, #fff 0%, #fffafa 100%);
  animation: analytics-rise 0.45s ease both;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}
.analytics-top-item:hover {
  box-shadow: 0 6px 18px rgba(179, 27, 27, 0.1);
  transform: translateX(4px);
}
.analytics-top-rank {
  font-weight: 800;
  color: #b31b1b;
  font-size: 14px;
}
.analytics-top-title {
  color: #1a1a1a;
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
}
.analytics-top-title:hover {
  color: #b31b1b;
}
.analytics-top-meta {
  font-size: 12px;
  color: #888;
  white-space: nowrap;
}
@media (max-width: 768px) {
  .analytics-body {
    min-height: 620px;
  }
  .analytics-stage,
  .analytics-panel {
    min-height: 600px;
  }
  .analytics-top-item {
    grid-template-columns: 24px 1fr;
  }
  .analytics-top-meta {
    grid-column: 1 / -1;
    white-space: normal;
  }
}
.bar-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.bar-item {
  display: grid;
  grid-template-columns: 110px 1fr 56px;
  align-items: center;
  gap: 10px;
}
.bar-label {
  font-size: 13px;
  color: #444;
}
.bar-track {
  height: 10px;
  border-radius: 999px;
  background: #f0f0f0;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #ff8b8b 0%, #b31b1b 100%);
}
.bar-fill-alt {
  background: linear-gradient(90deg, #f5b27a 0%, #b31b1b 100%);
}
.bar-value {
  text-align: right;
  font-size: 12px;
  color: #666;
}

/* 近期博客：与 Profile 我的博客一致 */
.blog-section-card .blog-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.blog-section-card .blog-header-row .section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111;
}
.blog-filters {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.blog-sort {
  font-size: 14px;
  color: #666;
  cursor: pointer;
  user-select: none;
}
.blog-sort:hover,
.blog-sort.active {
  color: #b31b1b;
}
.blog-sort--dropdown {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.blog-sort-arrow {
  font-size: 12px;
  color: #999;
}
.blog-sort--dropdown:hover .blog-sort-arrow {
  color: #b31b1b;
}
.blog-loading {
  padding: 24px 0;
  text-align: center;
  color: #888;
  font-size: 14px;
}
.blog-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}
.blog-empty-icon {
  font-size: 80px;
  color: #d0d0d0;
  margin-bottom: 16px;
}
.blog-empty-text {
  margin: 0;
  font-size: 15px;
  color: #999;
}
.profile-card-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.profile-card-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}
.profile-card-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.profile-card-item--blog {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}
.profile-card-thumb {
  flex-shrink: 0;
  width: 120px;
  height: 84px;
  border-radius: 6px;
  overflow: hidden;
  background: #e8e8e8;
  display: block;
}
.profile-card-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.profile-card-thumb-ph {
  display: block;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e0e0e0 0%, #eee 100%);
}
.profile-card-item--blog .profile-card-body {
  flex: 1;
  min-width: 0;
}
.profile-card-item--blog .profile-card-title {
  margin-top: 0;
}
.profile-card-title {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #111;
  margin-bottom: 6px;
  text-decoration: none;
}
.profile-card-title:hover {
  color: #b31b1b;
}
.profile-card-stats {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 16px;
  margin: 8px 0 4px;
  font-size: 13px;
  line-height: 20px;
  height: 20px;
  color: #666;
}
.profile-card-stats .stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 20px;
  line-height: 20px;
  font-size: 13px;
}
.profile-card-stats .stat :deep(.el-icon) {
  font-size: 14px;
  width: 14px;
  height: 14px;
  color: #999;
  flex-shrink: 0;
}
.profile-card-stats .profile-card-time {
  font-size: 13px;
  line-height: 20px;
  height: 20px;
  color: #999;
}
.profile-card-meta {
  font-size: 14px;
  color: #666;
  margin: 0 0 4px;
}
.blog-pagination-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}
.blog-total-text {
  font-size: 14px;
  color: #606266;
  line-height: 32px;
}
.blog-total-num {
  display: inline-block;
  transform: translateY(-2px);
}
.blog-pagination {
  justify-content: flex-start;
}
.blog-pagination :deep(.el-pager li.is-active) {
  background-color: #b31b1b;
  color: #fff;
}

/* 右侧排行榜卡片 */
.creator-sidebar-right {
  align-self: flex-start;
}

.ranking-card {
  position: sticky;
  top: 88px;
}

.ranking-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.ranking-tab {
  flex: 1;
  padding: 12px 0;
  font-size: 14px;
  color: #666;
  background: none;
  border: none;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.ranking-tab:hover {
  color: #111;
}

.ranking-tab.active {
  color: #111;
  font-weight: 600;
}

.ranking-tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: #b31b1b;
}

.ranking-list {
  max-height: 420px;
  overflow-y: auto;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.ranking-item:last-child {
  border-bottom: none;
}

.ranking-num {
  flex-shrink: 0;
  width: 24px;
  font-size: 16px;
  font-weight: 700;
  color: #111;
  text-align: center;
}

.ranking-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #111;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ranking-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ranking-avatar .avatar-initial {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.ranking-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ranking-name {
  font-size: 14px;
  font-weight: 600;
  color: #111;
}

.ranking-desc {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ranking-follow {
  flex-shrink: 0;
  padding: 4px 12px;
  font-size: 13px;
  color: #b31b1b;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}

.ranking-follow:hover {
  color: #8b0000;
}

.ranking-followed {
  flex-shrink: 0;
  font-size: 13px;
  color: #999;
}

.ranking-empty {
  padding: 24px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

/* 专栏管理：参考个人主页我的专栏 */
.column-management-card {
  padding: 24px;
}
.column-management-card .blog-header-row,
.column-management-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.column-management-card .section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111;
}
.btn-new-column {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 14px;
  color: #b31b1b;
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.2s;
}
.btn-new-column:hover {
  color: #8b0000;
}
.btn-new-column .el-icon {
  font-size: 16px;
}

/* 博客机器人 */
.blog-bot-card {
  padding: 24px;
}
.blog-bot-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.blog-bot-card .section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111;
}
.blog-bot-desc {
  margin: 0 0 24px 0;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}
.blog-bot-empty {
  padding: 32px 24px;
  text-align: center;
  background: #f9f9f9;
  border-radius: 8px;
  border: 1px dashed #e0e0e0;
}
.blog-bot-empty-text {
  margin: 0;
  font-size: 14px;
  color: #999;
}
.blog-bot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.blog-bot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #eee;
}
.blog-bot-item-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.blog-bot-item-avatar-ph {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e0e0e0;
  color: #666;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.blog-bot-item-body {
  flex: 1;
  min-width: 0;
}
.blog-bot-item-delete {
  flex-shrink: 0;
  padding: 6px;
  color: #999;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}
.blog-bot-item-delete:hover {
  color: #b31b1b;
  background: #fef0f0;
}
.blog-bot-item-name {
  font-size: 15px;
  font-weight: 600;
  color: #111;
  margin-bottom: 4px;
}
.blog-bot-item-meta {
  font-size: 13px;
  color: #666;
}

.bot-avatar-upload {
  position: relative;
}
.bot-avatar-upload.uploading .bot-avatar-btn {
  pointer-events: none;
  opacity: 0.8;
}
.bot-avatar-preview {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bot-avatar-img {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
}
.bot-avatar-remove {
  padding: 6px 12px;
  font-size: 13px;
  color: #666;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.bot-avatar-remove:hover {
  background: #eee;
  color: #333;
}
.bot-avatar-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}
.bot-avatar-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 14px;
  color: #666;
  background: #f5f5f5;
  border: 1px dashed #ccc;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.bot-avatar-btn:hover:not(:disabled) {
  background: #eee;
  border-color: #999;
}
.bot-avatar-icon {
  font-size: 18px;
}
.bot-avatar-icon.is-loading {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

.creator-bot-dialog .bot-form-select {
  width: 100%;
}

.cm-loading {
  padding: 24px 0;
  text-align: center;
  color: #888;
  font-size: 14px;
}
.column-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}
.column-empty-icon {
  font-size: 80px;
  color: #d0d0d0;
  margin-bottom: 16px;
}
.column-empty-text {
  margin: 0;
  font-size: 15px;
  color: #999;
}
.btn-new-column-inline {
  margin-top: 16px;
  padding: 8px 20px;
  font-size: 14px;
  color: #b31b1b;
  background: #fff;
  border: 1px solid #b31b1b;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.btn-new-column-inline:hover {
  background: #b31b1b;
  color: #fff;
}
.column-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.column-list-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}
.column-list-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.column-list-body {
  flex: 1;
  min-width: 0;
}
.column-list .column-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}
.column-list .column-main .column-title-link {
  flex: 1;
  min-width: 0;
  margin-bottom: 0;
}
.column-list .column-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.column-list .folder-action-btn {
  padding: 4px 12px;
  font-size: 13px;
  color: #666;
  background: transparent;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  cursor: pointer;
}
.column-list .folder-action-btn:hover {
  color: #333;
  background: #f5f5f5;
}
.column-list .folder-action-btn--danger:hover {
  color: #b31b1b;
  border-color: #b31b1b;
}
.column-list-title {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #111;
  margin-bottom: 6px;
  text-decoration: none;
}
.column-list-title:hover {
  color: #b31b1b;
}
.column-list-meta {
  font-size: 14px;
  color: #666;
  margin: 0 0 4px;
}
.column-list-time {
  font-size: 13px;
  color: #999;
  margin: 0;
}
.column-list-cover {
  flex-shrink: 0;
  width: 120px;
  height: 84px;
  border-radius: 6px;
  overflow: hidden;
  background: #e8e8e8;
  display: block;
}
.column-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.column-cover-ph {
  display: block;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e0e0e0 0%, #eee 100%);
}
.creator-column-dialog .btn-confirm-column {
  background: #b31b1b;
  border-color: #b31b1b;
}
.creator-column-dialog .btn-confirm-column:hover {
  background: #8b0000;
  border-color: #8b0000;
}

/* 新建专栏：封面上传与裁剪，预览尺寸与裁剪框一致 400:280；操作放在图片上方为文字链接 */
.creator-column-dialog .column-cover-upload {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}
.creator-column-dialog .column-cover-upload.has-cover .column-cover-actions {
  order: -1;
}
.creator-column-dialog .column-cover-preview-wrap {
  width: 320px;
  height: 224px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #e8e8e8;
}
.creator-column-dialog .column-cover-preview {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  background: #e8e8e8;
  background-size: cover;
  background-position: center;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.creator-column-dialog .column-cover-preview .cover-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
}
.creator-column-dialog .cover-upload-btn.column-cover-upload-btn {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 14px;
  color: #555;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
}
.creator-column-dialog .column-cover-actions {
  display: inline-flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  line-height: 22px;
}
.creator-column-dialog .column-cover-action-link {
  color: #666;
  cursor: pointer;
  user-select: none;
}
.creator-column-dialog .column-cover-action-link:hover {
  color: #333;
  text-decoration: underline;
}
.creator-column-dialog .column-cover-action-remove:hover {
  color: #b31b1b;
}
.creator-column-crop-dialog .column-crop-viewport-wrap {
  position: relative;
  width: 400px;
  height: 280px;
  margin: 0 auto;
}
.creator-column-crop-dialog .column-crop-viewport {
  width: 100%;
  height: 100%;
  overflow: hidden;
  position: relative;
  background: #e8e8e8;
  border-radius: 8px;
}
.creator-column-crop-dialog .column-crop-frame {
  position: absolute;
  inset: 0;
  pointer-events: none;
  border: 3px solid #b31b1b;
  border-radius: 8px;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.45);
}
.creator-column-crop-dialog .crop-image {
  position: absolute;
  left: 0;
  top: 0;
  display: block;
  user-select: none;
}
.creator-column-crop-dialog .column-crop-viewport .crop-image {
  cursor: grab;
}
.creator-column-crop-dialog .crop-tip {
  margin: 12px 0 0;
  font-size: 13px;
  color: #666;
}
</style>

<!-- 近期博客可见性下拉：红色主题（下拉挂载在 body，需全局样式） -->
<style>
.creator-blog-visibility-dropdown {
  --el-color-primary: #b31b1b;
}
.creator-blog-visibility-dropdown .el-dropdown-menu__item:hover {
  color: #b31b1b;
  background-color: rgba(179, 27, 27, 0.08);
}
</style>
