<template>
  <div class="knowledge-page">
    <!-- 左侧：知识库导航栏（顶栏下方固定，可展开/收起） -->
    <aside class="knowledge-sidebar" :class="{ expanded: sidebarExpanded }">
      <div class="knowledge-sidebar-inner">
        <!-- 折叠标：放在导航栏最上面 -->
        <button
          type="button"
          class="knowledge-collapse-btn"
          :title="sidebarExpanded ? '收起' : '展开'"
          @click="sidebarExpanded = !sidebarExpanded"
        >
          <span class="knowledge-collapse-icon" :class="{ expanded: sidebarExpanded }">
            <span class="collapse-panel collapse-panel-left">
              <span class="collapse-line"></span>
              <span class="collapse-line"></span>
            </span>
            <span class="collapse-panel collapse-panel-right"></span>
          </span>
        </button>

        <!-- 1. 知识库（当前选中） -->
        <router-link to="/knowledge" class="knowledge-nav-item active" title="知识库">
          <el-icon><FolderOpened /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">知识库</span>
        </router-link>
        <!-- 2. 搜索 -->
        <router-link to="/knowledge" class="knowledge-nav-item" title="搜索">
          <el-icon><Search /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">搜索</span>
        </router-link>
        <!-- 3. 收藏 -->
        <a href="#" class="knowledge-nav-item" title="收藏">
          <el-icon><Star /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">收藏</span>
        </a>

        <div class="knowledge-nav-spacer"></div>
      </div>
    </aside>

    <!-- 中间偏左：知识库内容边栏（标题、热门、搜索、我的知识库、我的订阅） -->
    <aside class="knowledge-library-sidebar" :class="{ expanded: sidebarExpanded }">
      <div class="knowledge-library-inner">
        <h1 class="knowledge-page-title">知识库</h1>
        <button type="button" class="knowledge-hot-tab" @click="openPopularList">
          <el-icon class="knowledge-hot-tab-icon"><FolderOpened /></el-icon>
          <span>热门知识库</span>
        </button>
        <div class="knowledge-main-divider" />
        <div class="knowledge-search-row">
          <div class="knowledge-search-inner">
            <el-icon class="knowledge-search-icon"><Search /></el-icon>
            <input
              v-model="searchKeyword"
              type="text"
              class="knowledge-search-input"
              placeholder="搜索知识库/文件"
              autocomplete="off"
            />
          </div>
          <el-dropdown trigger="click" placement="bottom-end" popper-class="knowledge-add-dropdown-bbc" @command="onAddDropdownCommand">
            <button type="button" class="knowledge-add-btn" title="添加">
              <el-icon><Plus /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="newKb">新建知识库</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <section class="knowledge-section">
          <h2 class="knowledge-section-title">我的知识库</h2>
          <ul class="knowledge-my-list">
            <li
              v-for="kb in myKnowledgeBasesWithDefaultFiltered"
              :key="kb.id"
              class="knowledge-my-item"
              :class="{ active: selectedKb?.id === kb.id && selectedKbSource === 'mine' }"
              @click="selectedKb = { ...kb }; selectedKbSource = 'mine'"
            >
              <el-icon class="knowledge-my-icon"><Reading /></el-icon>
              <span class="knowledge-my-name">{{ kb.name || '未命名' }}</span>
            </li>
          </ul>
        </section>
        <section class="knowledge-section">
          <h2 class="knowledge-section-title">我的订阅</h2>
          <ul class="knowledge-my-list">
            <li
              v-for="sub in mySubscriptionsFiltered"
              :key="sub.id"
              class="knowledge-my-item"
              :class="{ active: selectedKb?.id === sub.id && selectedKbSource === 'sub' }"
              @click="selectedKb = { ...sub }; selectedKbSource = 'sub'"
            >
              <el-icon class="knowledge-my-icon"><Reading /></el-icon>
              <span class="knowledge-my-name">{{ sub.name || '未命名' }}</span>
            </li>
          </ul>
        </section>
      </div>
    </aside>

    <!-- 右侧：知识库详情边栏（点击某个知识库时显示） -->
    <aside
      v-show="selectedKb"
      class="knowledge-detail-sidebar"
      :class="{ expanded: sidebarExpanded }"
    >
      <div class="knowledge-detail-inner">
        <div class="knowledge-detail-header">
          <div class="knowledge-detail-cover-row">
            <div v-if="selectedKb?.cover" class="knowledge-detail-cover-wrap">
              <img :src="selectedKb.cover" :alt="selectedKb.name" class="knowledge-detail-cover" />
            </div>
            <div v-else class="knowledge-detail-cover-placeholder">
              <el-icon><FolderOpened /></el-icon>
              <span>暂无封面</span>
            </div>
            <div class="knowledge-detail-cover-side">
              <h2 class="knowledge-detail-title">{{ selectedKb?.name || '未命名' }}</h2>
              <div class="knowledge-detail-cover-meta">
                <div class="knowledge-detail-author">
                  <img
                    v-if="detailOwnerAvatar"
                    :src="detailOwnerAvatar"
                    :alt="detailOwnerName"
                    class="knowledge-detail-author-avatar"
                  />
                  <span v-else class="knowledge-detail-author-avatar-ph">{{ detailOwnerName.charAt(0) }}</span>
                  <span class="knowledge-detail-author-name">{{ detailOwnerName }}</span>
                </div>
                <div class="knowledge-detail-stats">
                  {{ selectedKb?.subCount ?? 0 }} 订阅 · {{ selectedKb?.contentCount ?? detailContents.length }} 内容
                </div>
                <span class="knowledge-detail-visibility">{{ selectedKb?.visibility === 'PRIVATE' ? '私有' : '公开' }}</span>
              </div>
            </div>
          </div>
          <div class="knowledge-detail-header-actions">
            <template v-if="isOwnDetail && !isDefaultKb">
              <button type="button" class="knowledge-detail-btn small" title="编辑" @click="openEditKbDialog">编辑</button>
              <button type="button" class="knowledge-detail-btn small danger" title="删除该知识库" @click="deleteKb">删除</button>
            </template>
            <button type="button" class="knowledge-detail-close" title="关闭" @click="selectedKb = null; selectedKbSource = null">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>
        <dl v-if="selectedKb?.description" class="knowledge-detail-meta">
          <dt>简介</dt>
          <dd>{{ selectedKb.description }}</dd>
        </dl>

        <!-- 操作：自己的显示添加/批量删除，别人的显示订阅 -->
        <div class="knowledge-detail-actions">
          <template v-if="isOwnDetail">
            <button type="button" class="knowledge-detail-btn primary" @click="openAddContentDialog">添加</button>
            <template v-if="detailContents.length > 0">
              <template v-if="!detailBatchMode">
                <button type="button" class="knowledge-detail-btn" @click="detailBatchMode = true">批量删除</button>
              </template>
              <template v-else>
                <button
                  type="button"
                  class="knowledge-detail-btn"
                  :disabled="detailSelectedIds.length === 0"
                  @click="batchRemoveFromKb"
                >
                  删除选中{{ detailSelectedIds.length > 0 ? ` (${detailSelectedIds.length})` : '' }}
                </button>
                <button type="button" class="knowledge-detail-btn" @click="exitDetailBatchMode">取消</button>
              </template>
            </template>
          </template>
          <template v-else>
            <button
              type="button"
              class="knowledge-detail-btn primary"
              :class="{ subscribed: isDetailSubscribed }"
              @click="toggleDetailSubscribe"
            >
              {{ isDetailSubscribed ? '已订阅' : '订阅' }}
            </button>
          </template>
        </div>

        <!-- 收录的文章 -->
        <h3 class="knowledge-detail-list-title">收录的文章</h3>
        <div v-if="detailContents.length === 0" class="knowledge-detail-empty">暂无收录</div>
        <ul v-else class="knowledge-detail-article-list">
          <li v-for="art in detailContents" :key="art.id" class="knowledge-detail-article-item">
            <el-checkbox
              v-if="isOwnDetail && detailBatchMode"
              :model-value="detailSelectedIds.includes(art.id)"
              class="knowledge-detail-article-checkbox"
              @update:model-value="(v: boolean) => toggleDetailSelect(art.id, v)"
              @click.stop
            />
            <button
              type="button"
              class="knowledge-detail-article-link"
              :class="{ active: selectedContentId === art.id }"
              @click="selectArticleInMain(art.id)"
            >
              {{ art.title }}
            </button>
            <el-dropdown
              v-if="isOwnDetail && !detailBatchMode"
              trigger="click"
              placement="bottom-end"
              popper-class="knowledge-article-dropdown-bbc"
              @command="(cmd: string) => removeContentFromKb(Number(cmd))"
            >
              <button type="button" class="knowledge-detail-article-more" title="更多" @click.stop>
                <el-icon><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="String(art.id)" class="knowledge-article-dropdown-danger">删除这篇博客</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </li>
        </ul>
      </div>
    </aside>

    <!-- 添加文章到知识库 弹窗 -->
    <el-dialog
      v-model="addContentDialogVisible"
      title="添加文章到知识库"
      width="520px"
      class="knowledge-add-dialog"
    >
      <div class="knowledge-add-tabs">
        <button
          type="button"
          class="knowledge-add-tab"
          :class="{ active: addContentTab === 'general' }"
          @click="addContentTab = 'general'"
        >
          综合
        </button>
        <button
          type="button"
          class="knowledge-add-tab"
          :class="{ active: addContentTab === 'column' }"
          @click="switchAddContentTab('column')"
        >
          从专栏批量添加
        </button>
        <button
          type="button"
          class="knowledge-add-tab"
          :class="{ active: addContentTab === 'folder' }"
          @click="switchAddContentTab('folder')"
        >
          从收藏夹批量添加
        </button>
      </div>

      <!-- 综合：我的已发布文章 + 搜索 -->
      <template v-if="addContentTab === 'general'">
        <div class="knowledge-add-search-row">
          <el-input
            v-model="addContentKeyword"
            placeholder="搜索标题或摘要"
            clearable
            size="small"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <ul v-if="addContentCandidatesFiltered.length === 0" class="knowledge-add-empty">暂无已发布文章或没有匹配结果</ul>
        <ul v-else class="knowledge-add-list">
          <li v-for="art in addContentCandidatesFiltered" :key="art.id" class="knowledge-add-item">
            <el-checkbox
              v-if="!detailContentIds.has(art.id)"
              :model-value="addContentGeneralSelectedIds.includes(art.id)"
              @update:model-value="toggleAddContentGeneralSelect(art.id, $event)"
            />
            <span v-else class="knowledge-add-checkbox-ph" />
            <span class="knowledge-add-title">{{ art.title }}</span>
            <span v-if="detailContentIds.has(art.id)" class="knowledge-add-tag">已收录</span>
            <button
              v-else
              type="button"
              class="knowledge-add-btn-inline"
              :disabled="addingContentId === art.id"
              @click="addContentToKb(art.id)"
            >
              添加
            </button>
          </li>
        </ul>
        <div v-if="addContentTab === 'general' && addContentGeneralSelectedIds.length > 0" class="knowledge-add-batch-bar">
          <span>已选 {{ addContentGeneralSelectedIds.length }} 篇</span>
          <button
            type="button"
            class="knowledge-add-btn-batch"
            :disabled="addContentBatchAdding"
            @click="batchAddContentToKb(addContentGeneralSelectedIds)"
          >
            添加选中
          </button>
        </div>
      </template>

      <!-- 从专栏批量添加：选专栏 → 显示该专栏文章列表 -->
      <template v-else-if="addContentTab === 'column'">
        <div class="knowledge-add-source-list">
          <template v-if="addContentColumnList.length === 0 && !addContentColumnLoading">
            <p class="knowledge-add-empty">暂无专栏</p>
          </template>
          <template v-else-if="addContentColumnLoading">
            <p class="knowledge-add-empty"><el-icon class="is-loading"><Loading /></el-icon> 加载中…</p>
          </template>
          <template v-else>
            <ul class="knowledge-add-column-folder-list">
              <li
                v-for="col in addContentColumnList"
                :key="col.id"
                class="knowledge-add-source-item"
                :class="{ active: addContentSelectedColumnId === col.id }"
                @click="selectAddContentColumn(col.id)"
              >
                <span class="knowledge-add-source-name">{{ col.name }}</span>
                <span class="knowledge-add-source-count">{{ col.articleCount }} 篇</span>
              </li>
            </ul>
            <div v-if="addContentSelectedColumnId != null" class="knowledge-add-articles-in-source">
              <p class="knowledge-add-articles-title">该专栏下的文章</p>
              <ul v-if="addContentColumnArticles.length === 0" class="knowledge-add-empty">该专栏暂无文章或均已收录</ul>
              <ul v-else class="knowledge-add-list">
                <li v-for="art in addContentColumnArticles" :key="art.id" class="knowledge-add-item">
                  <el-checkbox
                    v-if="!detailContentIds.has(art.id)"
                    :model-value="addContentColumnSelectedIds.includes(art.id)"
                    @update:model-value="toggleAddContentColumnSelect(art.id, $event)"
                  />
                  <span v-else class="knowledge-add-checkbox-ph" />
                  <span class="knowledge-add-title">{{ art.title }}</span>
                  <span v-if="detailContentIds.has(art.id)" class="knowledge-add-tag">已收录</span>
                  <button
                    v-else
                    type="button"
                    class="knowledge-add-btn-inline"
                    :disabled="addingContentId === art.id"
                    @click="addContentToKb(art.id)"
                  >
                    添加
                  </button>
                </li>
              </ul>
              <div v-if="addContentColumnSelectedIds.length > 0" class="knowledge-add-batch-bar">
                <span>已选 {{ addContentColumnSelectedIds.length }} 篇</span>
                <button
                  type="button"
                  class="knowledge-add-btn-batch"
                  :disabled="addContentBatchAdding"
                  @click="batchAddContentToKb(addContentColumnSelectedIds)"
                >
                  添加选中
                </button>
              </div>
            </div>
          </template>
        </div>
      </template>

      <!-- 从收藏夹批量添加：选收藏夹 → 显示该收藏夹文章列表 -->
      <template v-else-if="addContentTab === 'folder'">
        <div class="knowledge-add-source-list">
          <template v-if="addContentFolderList.length === 0 && !addContentFolderLoading">
            <p class="knowledge-add-empty">暂无收藏夹</p>
          </template>
          <template v-else-if="addContentFolderLoading">
            <p class="knowledge-add-empty"><el-icon class="is-loading"><Loading /></el-icon> 加载中…</p>
          </template>
          <template v-else>
            <ul class="knowledge-add-column-folder-list">
              <li
                v-for="f in addContentFolderList"
                :key="f.id"
                class="knowledge-add-source-item"
                :class="{ active: addContentSelectedFolderId === f.id }"
                @click="selectAddContentFolder(f.id)"
              >
                <span class="knowledge-add-source-name">{{ f.name }}</span>
                <span class="knowledge-add-source-count">{{ f.count }} 篇</span>
              </li>
            </ul>
            <div v-if="addContentSelectedFolderId != null" class="knowledge-add-articles-in-source">
              <p class="knowledge-add-articles-title">该收藏夹下的文章</p>
              <ul v-if="addContentFolderArticles.length === 0" class="knowledge-add-empty">该收藏夹暂无文章或均已收录</ul>
              <ul v-else class="knowledge-add-list">
                <li v-for="art in addContentFolderArticles" :key="art.id" class="knowledge-add-item">
                  <el-checkbox
                    v-if="!detailContentIds.has(art.id)"
                    :model-value="addContentFolderSelectedIds.includes(art.id)"
                    @update:model-value="toggleAddContentFolderSelect(art.id, $event)"
                  />
                  <span v-else class="knowledge-add-checkbox-ph" />
                  <span class="knowledge-add-title">{{ art.title }}</span>
                  <span v-if="detailContentIds.has(art.id)" class="knowledge-add-tag">已收录</span>
                  <button
                    v-else
                    type="button"
                    class="knowledge-add-btn-inline"
                    :disabled="addingContentId === art.id"
                    @click="addContentToKb(art.id)"
                  >
                    添加
                  </button>
                </li>
              </ul>
              <div v-if="addContentFolderSelectedIds.length > 0" class="knowledge-add-batch-bar">
                <span>已选 {{ addContentFolderSelectedIds.length }} 篇</span>
                <button
                  type="button"
                  class="knowledge-add-btn-batch"
                  :disabled="addContentBatchAdding"
                  @click="batchAddContentToKb(addContentFolderSelectedIds)"
                >
                  添加选中
                </button>
              </div>
            </div>
          </template>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑知识库 弹窗 -->
    <el-dialog
      v-model="editKbDialogVisible"
      title="编辑知识库"
      width="440px"
      class="knowledge-create-dialog knowledge-create-dialog--bbc"
      @closed="editKbCoverInputRef = null"
    >
      <div class="knowledge-create-form">
        <div class="knowledge-create-field">
          <label>封面（选填）</label>
          <div
            class="knowledge-create-cover"
            :class="{ 'has-cover': !!editKbCover, uploading: editKbCoverUploading }"
            @click="!editKbCoverUploading && triggerEditKbCoverSelect()"
          >
            <template v-if="editKbCoverUploading">
              <el-icon class="knowledge-create-cover-loading"><Loading /></el-icon>
              <span>上传中…</span>
            </template>
            <template v-else-if="editKbCover">
              <img :src="editKbCover" alt="封面" class="knowledge-create-cover-img" />
              <button type="button" class="knowledge-create-cover-remove" @click.stop="editKbCover = ''">移除封面</button>
            </template>
            <template v-else>
              <el-icon class="knowledge-create-cover-plus"><Plus /></el-icon>
              <span>上传封面图片</span>
            </template>
          </div>
          <input
            ref="editKbCoverInputRef"
            type="file"
            accept="image/*"
            class="knowledge-create-cover-input"
            @change="onEditKbCoverFileChange"
          />
        </div>
        <div class="knowledge-create-field">
          <label>名称</label>
          <el-input v-model="editKbName" placeholder="请输入知识库名称" maxlength="128" show-word-limit clearable />
        </div>
        <div class="knowledge-create-field">
          <label>简介</label>
          <el-input
            v-model="editKbDescription"
            type="textarea"
            placeholder="选填"
            :rows="3"
            maxlength="512"
            show-word-limit
          />
        </div>
        <div class="knowledge-create-field">
          <label>可见性</label>
          <el-radio-group v-model="editKbVisibility">
            <el-radio value="PRIVATE">私有</el-radio>
            <el-radio value="PUBLIC">公开</el-radio>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <span class="knowledge-create-footer">
          <el-button @click="editKbDialogVisible = false">取消</el-button>
          <el-button type="primary" class="knowledge-create-submit" @click="submitEditKb">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 新建知识库 弹窗（BBC 风格） -->
    <el-dialog
      v-model="createKbDialogVisible"
      title="新建知识库"
      width="440px"
      class="knowledge-create-dialog knowledge-create-dialog--bbc"
      @closed="createKbName = ''; createKbDescription = ''; createKbVisibility = 'PRIVATE'; createKbCover = ''"
    >
      <div class="knowledge-create-form">
        <div class="knowledge-create-field">
          <label>封面（选填）</label>
          <div
            class="knowledge-create-cover"
            :class="{ 'has-cover': !!createKbCover, uploading: createKbCoverUploading }"
            @click="!createKbCoverUploading && triggerCreateKbCoverSelect()"
          >
            <template v-if="createKbCoverUploading">
              <el-icon class="knowledge-create-cover-loading"><Loading /></el-icon>
              <span>上传中…</span>
            </template>
            <template v-else-if="createKbCover">
              <img :src="createKbCover" alt="封面" class="knowledge-create-cover-img" />
              <button type="button" class="knowledge-create-cover-remove" @click.stop="createKbCover = ''">移除封面</button>
            </template>
            <template v-else>
              <el-icon class="knowledge-create-cover-plus"><Plus /></el-icon>
              <span>上传封面图片</span>
            </template>
          </div>
          <input
            ref="createKbCoverInputRef"
            type="file"
            accept="image/*"
            class="knowledge-create-cover-input"
            @change="onCreateKbCoverFileChange"
          />
        </div>
        <div class="knowledge-create-field">
          <label>名称</label>
          <el-input v-model="createKbName" placeholder="请输入知识库名称" maxlength="128" show-word-limit clearable />
        </div>
        <div class="knowledge-create-field">
          <label>简介</label>
          <el-input
            v-model="createKbDescription"
            type="textarea"
            placeholder="选填"
            :rows="3"
            maxlength="512"
            show-word-limit
          />
        </div>
        <div class="knowledge-create-field">
          <label>可见性</label>
          <el-radio-group v-model="createKbVisibility">
            <el-radio value="PRIVATE">私有</el-radio>
            <el-radio value="PUBLIC">公开</el-radio>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <span class="knowledge-create-footer">
          <el-button @click="createKbDialogVisible = false">取消</el-button>
          <el-button type="primary" class="knowledge-create-submit" @click="submitCreateKb">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 中间：主内容区（文章阅读优先，其次热门/搜索列表，再占位） -->
    <main class="knowledge-main" :class="{ expanded: sidebarExpanded }">
      <!-- 文章阅读区：有选中文章时优先显示 -->
      <div v-if="selectedContentId != null" class="knowledge-main-reader">
        <div v-if="mainArticleLoading" class="knowledge-main-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中…</span>
        </div>
        <template v-else-if="mainArticle">
          <div class="knowledge-main-card">
            <div class="knowledge-article-title-row">
              <h1 class="knowledge-article-title">{{ mainArticle.title }}</h1>
              <button type="button" class="knowledge-main-close" title="关闭" @click="selectedContentId = null">
                <el-icon><Close /></el-icon>
              </button>
            </div>
            <p v-if="mainArticle.summary" class="knowledge-article-summary">{{ mainArticle.summary }}</p>
            <div v-if="mainArticle.tagNames?.length" class="knowledge-article-tags">
              <span v-for="t in mainArticle.tagNames" :key="t" class="knowledge-article-tag">{{ t }}</span>
            </div>
            <div class="knowledge-article-author">
              <template v-if="mainArticleAuthor">
                <el-avatar :src="mainArticleAuthor.avatar" :size="44">
                  {{ (mainArticleAuthor.nickname || mainArticleAuthor.username || '用').charAt(0).toUpperCase() }}
                </el-avatar>
                <div class="knowledge-article-author-info">
                  <span class="knowledge-article-author-name">{{ mainArticleAuthor.nickname || mainArticleAuthor.username || '用户' }}</span>
                  <template v-if="mainArticle.userId && mainArticle.userId !== userStore.userInfo?.id">
                    <router-link :to="{ path: '/blog', query: { userId: String(mainArticle.userId) } }" class="knowledge-article-author-link">TA的博客</router-link>
                    <router-link :to="{ path: '/profile', query: { userId: String(mainArticle.userId) } }" class="knowledge-article-author-link">个人主页</router-link>
                  </template>
                </div>
              </template>
              <template v-else>
                <el-avatar :size="44">作</el-avatar>
                <div class="knowledge-article-author-info">
                  <span class="knowledge-article-author-name">作者</span>
                </div>
              </template>
            </div>
            <div class="knowledge-article-stats">
              <span>阅读 {{ mainArticle.viewCount }}</span>
              <span>点赞 {{ mainArticle.likeCount }}</span>
              <span>评论 {{ mainArticle.commentCount }}</span>
              <span class="knowledge-article-date">{{ formatArticleDate(mainArticle.publishedAt ?? mainArticle.createdAt) }}</span>
            </div>
            <div ref="mainPreviewRef" class="knowledge-article-body vditor-reset" />
          </div>
        </template>
      </div>
      <!-- 热门知识库 / 搜索结果列表 -->
      <div v-else-if="showListPanel" class="knowledge-main-list-panel">
        <div class="knowledge-list-panel-header">
          <h2 class="knowledge-list-panel-title">{{ listQuery.q ? '搜索结果' : '热门知识库' }}</h2>
          <div class="knowledge-list-panel-search">
            <el-icon class="knowledge-search-icon"><Search /></el-icon>
            <input
              v-model="listQuery.q"
              type="text"
              class="knowledge-search-input"
              placeholder="按名称或简介搜索"
              autocomplete="off"
              @keyup.enter="loadPopularList"
            />
            <button type="button" class="knowledge-list-search-btn" @click="loadPopularList">搜索</button>
          </div>
        </div>
        <div v-if="listLoading" class="knowledge-list-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中…</span>
        </div>
        <template v-else>
          <ul v-if="popularListItems.length === 0" class="knowledge-list-empty">暂无知识库</ul>
          <ul v-else class="knowledge-list-cards">
            <li
              v-for="kb in popularListItems"
              :key="kb.id"
              class="knowledge-list-card"
              @click="selectKbFromList(kb)"
            >
              <div class="knowledge-list-card-cover">
                <img v-if="kb.cover" :src="kb.cover" :alt="kb.name" />
              </div>
              <div class="knowledge-list-card-body">
                <h3 class="knowledge-list-card-title">{{ kb.name }}</h3>
                <p v-if="kb.description" class="knowledge-list-card-desc">{{ kb.description }}</p>
                <div class="knowledge-list-card-meta">
                  <span>{{ kb.subCount ?? 0 }} 订阅</span>
                  <span>{{ kb.contentCount ?? 0 }} 内容</span>
                  <span v-if="kb.subscribed" class="knowledge-list-card-subscribed">已订阅</span>
                </div>
              </div>
            </li>
          </ul>
          <div v-if="listTotal > listPageSize" class="knowledge-list-pagination">
            <button
              type="button"
              class="knowledge-list-page-btn"
              :disabled="listQuery.page <= 1"
              @click="listQuery.page = Math.max(1, listQuery.page - 1); loadPopularList()"
            >
              上一页
            </button>
            <span class="knowledge-list-page-info">第 {{ listQuery.page }} 页 / 共 {{ Math.max(1, Math.ceil(listTotal / listPageSize)) }} 页</span>
            <button
              type="button"
              class="knowledge-list-page-btn"
              :disabled="listQuery.page >= Math.max(1, Math.ceil(listTotal / listPageSize))"
              @click="listQuery.page += 1; loadPopularList()"
            >
              下一页
            </button>
          </div>
        </template>
      </div>
      <div v-else class="knowledge-main-placeholder">
        点击左侧收录的文章标题，在此处阅读；或点击「热门知识库」浏览、搜索知识库
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick, reactive } from 'vue'
import { Search, FolderOpened, Star, Plus, Reading, Close, Delete, Loading, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { uploadImage } from '@/api/upload'
import * as knowledgeApi from '@/api/knowledge'
import { getContentsMe, getContentView, type ContentView } from '@/api/content'
import { getMe, getUserById, type UserMe } from '@/api/user'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { getColumnsMe } from '@/api/column'
import {
  getCollectionFoldersMe,
  getCollectionFolderContents,
  type CollectionFolderItem,
} from '@/api/collectionFolder'
import type { ColumnItem } from '@/api/column'

const userStore = useUserStore()

interface KnowledgeBaseItem {
  id: string
  name: string
  cover?: string
  description?: string
  visibility?: 'PRIVATE' | 'PUBLIC'
  ownerId?: number
  ownerAvatar?: string
  ownerName?: string
  subCount?: number
  contentCount?: number
  subscribed?: boolean
}

interface DetailContentItem {
  id: number
  title: string
  summary?: string
  cover?: string
}

const sidebarExpanded = ref(false)
const searchKeyword = ref('')
const myKnowledgeBases = ref<KnowledgeBaseItem[]>([])
const mySubscriptions = ref<KnowledgeBaseItem[]>([])
const knowledgeLoading = ref(false)

/** 按关键词过滤「我的知识库」和「我的订阅」（名称、简介匹配） */
function matchKbKeyword(kb: KnowledgeBaseItem, q: string): boolean {
  if (!q || !q.trim()) return true
  const k = q.trim().toLowerCase()
  const name = (kb.name ?? '').toLowerCase()
  const desc = (kb.description ?? '').toLowerCase()
  return name.includes(k) || desc.includes(k)
}

const myKnowledgeBasesFiltered = computed(() => {
  const list = myKnowledgeBases.value
  const q = searchKeyword.value
  if (!q || !q.trim()) return list
  return list.filter((kb) => matchKbKeyword(kb, q))
})

const mySubscriptionsFiltered = computed(() => {
  const list = mySubscriptions.value
  const q = searchKeyword.value
  if (!q || !q.trim()) return list
  return list.filter((kb) => matchKbKeyword(kb, q))
})

/** 我的知识库列表：过滤后若为空且从未创建过知识库则显示占位「默认知识库」 */
const myKnowledgeBasesWithDefaultFiltered = computed(() => {
  const list = myKnowledgeBasesFiltered.value
  if (myKnowledgeBases.value.length === 0 && list.length === 0) {
    return [{ id: 'default', name: '默认知识库', description: '默认创建的知识库，可在此收录文章与文件。', visibility: 'PRIVATE' as const }]
  }
  return list
})
const selectedKb = ref<KnowledgeBaseItem | null>(null)
const selectedKbSource = ref<'mine' | 'sub' | null>(null)

const isOwnDetail = computed(() => selectedKbSource.value === 'mine')
const isDefaultKb = computed(() => selectedKb.value?.id === 'default')

/** 详情作者头像：自己的知识库优先用当前用户头像，否则用知识库的 ownerAvatar */
const detailOwnerAvatar = computed(() => {
  const kb = selectedKb.value
  if (!kb) return ''
  if (selectedKbSource.value === 'mine') {
    return kb.ownerAvatar ?? (userStore.userInfo as { avatar?: string } | null)?.avatar ?? ''
  }
  return kb.ownerAvatar ?? ''
})

/** 详情作者昵称：自己的知识库优先用当前用户昵称，否则用知识库的 ownerName */
const detailOwnerName = computed(() => {
  const kb = selectedKb.value
  if (!kb) return '我'
  if (selectedKbSource.value === 'mine') {
    return kb.ownerName ?? userStore.userInfo?.nickname ?? userStore.userInfo?.username ?? '我'
  }
  return kb.ownerName ?? '我'
})
const detailContents = ref<DetailContentItem[]>([])
const detailBatchMode = ref(false)
const detailSelectedIds = ref<number[]>([])

const selectedContentId = ref<number | null>(null)
const mainArticle = ref<ContentView | null>(null)
const mainArticleLoading = ref(false)
const mainArticleAuthor = ref<UserMe | null>(null)
const mainPreviewRef = ref<HTMLDivElement | null>(null)
const isDetailSubscribed = computed(() => selectedKb.value != null && selectedKb.value.subscribed === true)

/** 右侧「热门知识库/搜索结果」列表面板 */
const showListPanel = ref(false)
const listQuery = reactive({ q: '', page: 1 })
const listPageSize = 20
const listResult = ref<{ list: knowledgeApi.KnowledgeBaseItem[]; total: number }>({ list: [], total: 0 })
const listLoading = ref(false)
const listTotal = computed(() => listResult.value.total)
const popularListItems = computed(() => listResult.value.list ?? [])

const addContentDialogVisible = ref(false)
const addContentTab = ref<'general' | 'column' | 'folder'>('general')
const addContentKeyword = ref('')
const addContentCandidates = ref<DetailContentItem[]>([])
const addContentCandidatesFiltered = computed(() => {
  const q = addContentKeyword.value.trim().toLowerCase()
  if (!q) return addContentCandidates.value
  return addContentCandidates.value.filter(
    (c) =>
      (c.title ?? '').toLowerCase().includes(q) || (c.summary ?? '').toLowerCase().includes(q)
  )
})
const addingContentId = ref<number | null>(null)

const addContentColumnList = ref<ColumnItem[]>([])
const addContentSelectedColumnId = ref<number | null>(null)
const addContentColumnArticles = ref<DetailContentItem[]>([])
const addContentColumnLoading = ref(false)

const addContentFolderList = ref<CollectionFolderItem[]>([])
const addContentSelectedFolderId = ref<number | null>(null)
const addContentFolderArticles = ref<DetailContentItem[]>([])
const addContentFolderLoading = ref(false)

/** 综合 / 专栏 / 收藏夹 下可多选的文章 ID，用于批量添加 */
const addContentGeneralSelectedIds = ref<number[]>([])
const addContentColumnSelectedIds = ref<number[]>([])
const addContentFolderSelectedIds = ref<number[]>([])
const addContentBatchAdding = ref(false)
const detailContentIds = computed(() => new Set(detailContents.value.map((c) => c.id)))

watch(selectedKb, (kb, oldKb) => {
  if (!kb) {
    detailContents.value = []
    detailBatchMode.value = false
    detailSelectedIds.value = []
    selectedContentId.value = null
    mainArticle.value = null
    return
  }
  selectedContentId.value = null
  mainArticle.value = null
  mainArticleAuthor.value = null
  const prevId = oldKb?.id
  if (prevId !== undefined && prevId === kb.id) {
    return
  }
  detailBatchMode.value = false
  detailSelectedIds.value = []
  loadDetailContents(kb.id)
}, { immediate: true })

watch(selectedContentId, (id) => {
  if (id == null) {
    mainArticle.value = null
    mainArticleAuthor.value = null
    return
  }
  loadMainArticle(id)
})

watch(
  () => mainArticle.value?.body,
  () => renderMainMarkdown(),
  { flush: 'post' }
)

function mapKbToItem(kb: knowledgeApi.KnowledgeBaseItem): KnowledgeBaseItem {
  return {
    id: String(kb.id),
    name: kb.name,
    cover: kb.cover ?? undefined,
    description: kb.description ?? undefined,
    visibility: kb.visibility,
    ownerId: kb.ownerId,
    ownerName: kb.ownerName ?? undefined,
    ownerAvatar: kb.ownerAvatar ?? undefined,
    subCount: kb.subCount ?? 0,
    contentCount: kb.contentCount ?? 0,
    subscribed: kb.subscribed,
  }
}

async function loadDetailContents(kbId: string) {
  if (kbId === 'default') {
    detailContents.value = []
    return
  }
  const id = Number(kbId)
  if (Number.isNaN(id)) {
    detailContents.value = []
    return
  }
  try {
    const [detailRes, contentsRes] = await Promise.all([
      knowledgeApi.getKnowledgeBaseById(id),
      knowledgeApi.getKnowledgeBaseContents(id, { page: 1, pageSize: 100 }),
    ])
    selectedKb.value = mapKbToItem(detailRes)
    detailContents.value = (contentsRes.list ?? []).map((c) => ({
      id: c.id,
      title: c.title,
      summary: c.summary ?? undefined,
      cover: c.cover ?? undefined,
    }))
  } catch {
    detailContents.value = []
  }
}

function openPopularList() {
  showListPanel.value = true
  listQuery.q = ''
  listQuery.page = 1
  loadPopularList()
}

async function loadPopularList() {
  listLoading.value = true
  try {
    const res = await knowledgeApi.getKnowledgeBasesPopular({
      page: listQuery.page,
      pageSize: listPageSize,
      q: listQuery.q || undefined,
    })
    listResult.value = { list: res.list ?? [], total: res.total ?? 0 }
  } catch {
    listResult.value = { list: [], total: 0 }
  } finally {
    listLoading.value = false
  }
}

function selectKbFromList(kb: knowledgeApi.KnowledgeBaseItem) {
  selectedKbSource.value = null
  selectedKb.value = mapKbToItem(kb)
  loadDetailContents(String(kb.id))
}

function selectArticleInMain(contentId: number) {
  if (detailBatchMode.value) return
  selectedContentId.value = contentId
}

function formatArticleDate(iso?: string) {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    return d.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return iso
  }
}

async function loadMainArticle(contentId: number) {
  mainArticle.value = null
  mainArticleAuthor.value = null
  mainArticleLoading.value = true
  try {
    const data = await getContentView(contentId)
    mainArticle.value = data
    if (data.userId) {
      try {
        mainArticleAuthor.value =
          userStore.userInfo?.id === data.userId
            ? await getMe()
            : await getUserById(data.userId)
      } catch {
        mainArticleAuthor.value = null
      }
    }
    await nextTick()
    await renderMainMarkdown()
  } catch {
    mainArticle.value = null
  } finally {
    mainArticleLoading.value = false
  }
}

async function renderMainMarkdown() {
  const a = mainArticle.value
  if (!a?.body) return
  await nextTick()
  const el = mainPreviewRef.value
  if (!el) {
    setTimeout(renderMainMarkdown, 50)
    return
  }
  el.innerHTML = ''
  try {
    await Vditor.preview(el, a.body, { mode: 'light', lang: 'zh_CN' })
  } catch (e) {
    el.textContent = a.body || '暂无正文'
    console.warn('Vditor.preview error', e)
  }
}

async function loadMyKnowledgeBases() {
  if (!userStore.isLoggedIn) return
  knowledgeLoading.value = true
  try {
    const list = await knowledgeApi.getKnowledgeBasesMe()
    myKnowledgeBases.value = list.map(mapKbToItem)
  } catch {
    myKnowledgeBases.value = []
  } finally {
    knowledgeLoading.value = false
  }
}

async function loadMySubscriptions() {
  if (!userStore.isLoggedIn) return
  try {
    const list = await knowledgeApi.getKnowledgeBasesSubscribed()
    mySubscriptions.value = list.map(mapKbToItem)
  } catch {
    mySubscriptions.value = []
  }
}

onMounted(() => {
  loadMyKnowledgeBases()
  loadMySubscriptions()
})

async function openAddContentDialog() {
  addContentDialogVisible.value = true
  addContentTab.value = 'general'
  addContentKeyword.value = ''
  addContentCandidates.value = []
  addContentSelectedColumnId.value = null
  addContentColumnArticles.value = []
  addContentColumnSelectedIds.value = []
  addContentSelectedFolderId.value = null
  addContentFolderArticles.value = []
  addContentFolderSelectedIds.value = []
  addContentGeneralSelectedIds.value = []
  if (!selectedKb.value || selectedKb.value.id === 'default') return
  try {
    const res = await getContentsMe({ status: 'PUBLISHED', pageSize: 50 })
    const list = (res.list ?? []).filter((c) => !detailContentIds.value.has(c.id))
    addContentCandidates.value = list.map((c) => ({
      id: c.id,
      title: c.title,
      summary: c.summary ?? undefined,
      cover: c.cover ?? undefined,
    }))
  } catch {
    addContentCandidates.value = []
  }
}

async function switchAddContentTab(tab: 'column' | 'folder') {
  addContentTab.value = tab
  addContentGeneralSelectedIds.value = []
  if (tab === 'column') {
    addContentSelectedFolderId.value = null
    addContentFolderArticles.value = []
    addContentFolderSelectedIds.value = []
    if (addContentColumnList.value.length === 0) {
      addContentColumnLoading.value = true
      try {
        addContentColumnList.value = await getColumnsMe()
      } catch {
        addContentColumnList.value = []
      } finally {
        addContentColumnLoading.value = false
      }
    }
  } else {
    addContentSelectedColumnId.value = null
    addContentColumnArticles.value = []
    addContentColumnSelectedIds.value = []
    if (addContentFolderList.value.length === 0) {
      addContentFolderLoading.value = true
      try {
        addContentFolderList.value = await getCollectionFoldersMe()
      } catch {
        addContentFolderList.value = []
      } finally {
        addContentFolderLoading.value = false
      }
    }
  }
}

async function selectAddContentColumn(columnId: number) {
  addContentSelectedColumnId.value = columnId
  addContentColumnSelectedIds.value = []
  try {
    const res = await getContentsMe({ columnId, status: 'PUBLISHED', pageSize: 100 })
    const list = (res.list ?? []).filter((c) => !detailContentIds.value.has(c.id))
    addContentColumnArticles.value = list.map((c) => ({
      id: c.id,
      title: c.title,
      summary: c.summary ?? undefined,
      cover: c.cover ?? undefined,
    }))
  } catch {
    addContentColumnArticles.value = []
  }
}

async function selectAddContentFolder(folderId: number) {
  addContentSelectedFolderId.value = folderId
  addContentFolderSelectedIds.value = []
  try {
    const res = await getCollectionFolderContents(folderId, { pageSize: 100 })
    const list = (res.list ?? []).filter((c) => !detailContentIds.value.has(c.id))
    addContentFolderArticles.value = list.map((c) => ({
      id: c.id,
      title: c.title,
      summary: c.summary ?? undefined,
      cover: c.cover ?? undefined,
    }))
  } catch {
    addContentFolderArticles.value = []
  }
}

function toggleAddContentGeneralSelect(id: number, checked: boolean) {
  if (checked) addContentGeneralSelectedIds.value = [...addContentGeneralSelectedIds.value, id]
  else addContentGeneralSelectedIds.value = addContentGeneralSelectedIds.value.filter((x) => x !== id)
}

function toggleAddContentColumnSelect(id: number, checked: boolean) {
  if (checked) addContentColumnSelectedIds.value = [...addContentColumnSelectedIds.value, id]
  else addContentColumnSelectedIds.value = addContentColumnSelectedIds.value.filter((x) => x !== id)
}

function toggleAddContentFolderSelect(id: number, checked: boolean) {
  if (checked) addContentFolderSelectedIds.value = [...addContentFolderSelectedIds.value, id]
  else addContentFolderSelectedIds.value = addContentFolderSelectedIds.value.filter((x) => x !== id)
}

async function batchAddContentToKb(ids: number[]) {
  if (ids.length === 0) return
  const kbId = selectedKb.value?.id
  if (!kbId || kbId === 'default') return
  const numId = Number(kbId)
  if (Number.isNaN(numId)) return
  const idSet = new Set(ids)
  const itemsToAppend: DetailContentItem[] = []
  for (const id of ids) {
    const from =
      addContentCandidates.value.find((c) => c.id === id) ||
      addContentColumnArticles.value.find((c) => c.id === id) ||
      addContentFolderArticles.value.find((c) => c.id === id)
    if (from) itemsToAppend.push(from)
  }
  addContentBatchAdding.value = true
  try {
    await Promise.all(ids.map((contentId) => knowledgeApi.addContentToKnowledgeBase(numId, contentId)))
    addContentCandidates.value = addContentCandidates.value.filter((c) => !idSet.has(c.id))
    addContentColumnArticles.value = addContentColumnArticles.value.filter((c) => !idSet.has(c.id))
    addContentFolderArticles.value = addContentFolderArticles.value.filter((c) => !idSet.has(c.id))
    addContentGeneralSelectedIds.value = []
    addContentColumnSelectedIds.value = []
    addContentFolderSelectedIds.value = []
    if (selectedKb.value) {
      selectedKb.value = { ...selectedKb.value, contentCount: (selectedKb.value.contentCount ?? 0) + ids.length }
    }
    detailContents.value = [...detailContents.value, ...itemsToAppend]
    ElMessage.success(`已添加 ${ids.length} 篇`)
  } catch {
    // ElMessage 已在 request 拦截器处理
  } finally {
    addContentBatchAdding.value = false
  }
}

function toggleDetailSelect(id: number, checked: boolean) {
  if (checked) detailSelectedIds.value = [...detailSelectedIds.value, id]
  else detailSelectedIds.value = detailSelectedIds.value.filter((x) => x !== id)
}

function exitDetailBatchMode() {
  detailBatchMode.value = false
  detailSelectedIds.value = []
}

async function batchRemoveFromKb() {
  const ids = detailSelectedIds.value
  const kbId = selectedKb.value?.id
  if (!kbId || kbId === 'default') return
  const numId = Number(kbId)
  if (Number.isNaN(numId)) return
  try {
    await Promise.all(ids.map((contentId) => knowledgeApi.removeContentFromKnowledgeBase(numId, contentId)))
    detailContents.value = detailContents.value.filter((c) => !ids.includes(c.id))
    detailSelectedIds.value = []
    detailBatchMode.value = false
    if (selectedKb.value) {
      selectedKb.value = { ...selectedKb.value, contentCount: (selectedKb.value.contentCount ?? 0) - ids.length }
    }
    ElMessage.success('已移除')
  } catch {
    // ElMessage 已在 request 拦截器处理
  }
}

async function removeContentFromKb(id: number) {
  const kbId = selectedKb.value?.id
  if (!kbId || kbId === 'default') return
  const numId = Number(kbId)
  if (Number.isNaN(numId)) return
  try {
    await knowledgeApi.removeContentFromKnowledgeBase(numId, id)
    detailContents.value = detailContents.value.filter((c) => c.id !== id)
    if (selectedKb.value) {
      selectedKb.value = { ...selectedKb.value, contentCount: Math.max(0, (selectedKb.value.contentCount ?? 0) - 1) }
    }
    ElMessage.success('已移除')
  } catch {
    // ElMessage 已在 request 拦截器处理
  }
}

async function deleteKb() {
  if (!selectedKb.value || selectedKb.value.id === 'default') return
  const numId = Number(selectedKb.value.id)
  if (Number.isNaN(numId)) return
  try {
    await knowledgeApi.deleteKnowledgeBase(numId)
    myKnowledgeBases.value = myKnowledgeBases.value.filter((kb) => kb.id !== selectedKb.value!.id)
    selectedKb.value = null
    selectedKbSource.value = null
    ElMessage.success('已删除该知识库')
  } catch {
    // ElMessage 已在 request 拦截器处理
  }
}

const editKbDialogVisible = ref(false)
const editKbName = ref('')
const editKbDescription = ref('')
const editKbVisibility = ref<'PRIVATE' | 'PUBLIC'>('PRIVATE')
const editKbCover = ref('')
const editKbCoverUploading = ref(false)
const editKbCoverInputRef = ref<HTMLInputElement | null>(null)

function openEditKbDialog() {
  if (!selectedKb.value) return
  editKbName.value = selectedKb.value.name
  editKbDescription.value = selectedKb.value.description ?? ''
  editKbVisibility.value = selectedKb.value.visibility ?? 'PRIVATE'
  editKbCover.value = selectedKb.value.cover ?? ''
  editKbDialogVisible.value = true
}

function triggerEditKbCoverSelect() {
  editKbCoverInputRef.value?.click()
}

async function onEditKbCoverFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  editKbCoverUploading.value = true
  try {
    const meta = await uploadImage(file, 'covers')
    if (meta?.url) {
      editKbCover.value = meta.url
      ElMessage.success('封面上传成功')
    }
  } finally {
    editKbCoverUploading.value = false
  }
}

async function submitEditKb() {
  const name = editKbName.value?.trim()
  if (!name) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  if (!selectedKb.value || selectedKb.value.id === 'default') return
  const numId = Number(selectedKb.value.id)
  if (Number.isNaN(numId)) return
  try {
    const updated = await knowledgeApi.updateKnowledgeBase(numId, {
      name,
      description: editKbDescription.value?.trim() || undefined,
      visibility: editKbVisibility.value,
      cover: editKbCover.value || undefined,
    })
    const item = mapKbToItem(updated)
    const idx = myKnowledgeBases.value.findIndex((kb) => kb.id === selectedKb.value!.id)
    if (idx >= 0) {
      myKnowledgeBases.value = myKnowledgeBases.value.slice()
      myKnowledgeBases.value[idx] = item
    }
    selectedKb.value = item
    editKbDialogVisible.value = false
    ElMessage.success('已保存')
  } catch {
    // ElMessage 已在 request 拦截器处理
  }
}

async function toggleDetailSubscribe() {
  if (!selectedKb.value || selectedKb.value.id === 'default') return
  const numId = Number(selectedKb.value.id)
  if (Number.isNaN(numId)) return
  try {
    if (isDetailSubscribed.value) {
      await knowledgeApi.unsubscribeKnowledgeBase(numId)
      selectedKb.value = { ...selectedKb.value, subscribed: false }
      ElMessage.success('已取消订阅')
    } else {
      await knowledgeApi.subscribeKnowledgeBase(numId)
      selectedKb.value = { ...selectedKb.value, subscribed: true }
      ElMessage.success('已订阅')
    }
  } catch {
    // ElMessage 已在 request 拦截器处理
  }
}

async function addContentToKb(id: number) {
  const kbId = selectedKb.value?.id
  if (!kbId || kbId === 'default') return
  const numId = Number(kbId)
  if (Number.isNaN(numId)) return
  addingContentId.value = id
  try {
    await knowledgeApi.addContentToKnowledgeBase(numId, id)
    const art = addContentCandidates.value.find((a) => a.id === id)
    if (art) {
      detailContents.value = [...detailContents.value, art]
      if (selectedKb.value) {
        selectedKb.value = { ...selectedKb.value, contentCount: (selectedKb.value.contentCount ?? 0) + 1 }
      }
    }
    addContentCandidates.value = addContentCandidates.value.filter((c) => c.id !== id)
    addContentColumnArticles.value = addContentColumnArticles.value.filter((c) => c.id !== id)
    addContentFolderArticles.value = addContentFolderArticles.value.filter((c) => c.id !== id)
    ElMessage.success('已添加')
  } catch {
    // ElMessage 已在 request 拦截器处理
  } finally {
    addingContentId.value = null
  }
}

function onAddDropdownCommand(command: string) {
  if (command === 'newKb') {
    createKbDialogVisible.value = true
    createKbName.value = ''
    createKbDescription.value = ''
    createKbVisibility.value = 'PRIVATE'
  }
}

const createKbDialogVisible = ref(false)
const createKbName = ref('')
const createKbDescription = ref('')
const createKbVisibility = ref<'PRIVATE' | 'PUBLIC'>('PRIVATE')
const createKbCover = ref('')
const createKbCoverUploading = ref(false)
const createKbCoverInputRef = ref<HTMLInputElement | null>(null)

function triggerCreateKbCoverSelect() {
  createKbCoverInputRef.value?.click()
}

async function onCreateKbCoverFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  createKbCoverUploading.value = true
  try {
    const meta = await uploadImage(file, 'covers')
    if (meta?.url) {
      createKbCover.value = meta.url
      ElMessage.success('封面上传成功')
    }
  } finally {
    createKbCoverUploading.value = false
  }
}

async function submitCreateKb() {
  const name = createKbName.value?.trim()
  if (!name) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  try {
    const created = await knowledgeApi.createKnowledgeBase({
      name,
      description: createKbDescription.value?.trim() || undefined,
      cover: createKbCover.value || undefined,
      visibility: createKbVisibility.value,
    })
    const item = mapKbToItem(created)
    item.ownerName = userStore.userInfo?.nickname ?? userStore.userInfo?.username ?? '我'
    item.ownerAvatar = (userStore.userInfo as { avatar?: string } | null)?.avatar
    myKnowledgeBases.value = [...myKnowledgeBases.value, item]
    createKbDialogVisible.value = false
    ElMessage.success('已创建')
  } catch {
    // ElMessage 已在 request 拦截器处理
  }
}
</script>

<style scoped>
.knowledge-page {
  display: flex;
  height: calc(100vh - 64px);
  max-height: calc(100vh - 64px);
  overflow: hidden;
  background: #fff;
}

/* 左侧导航栏：顶栏下方固定，可展开/收起 */
.knowledge-sidebar {
  position: fixed;
  top: 64px;
  left: 0;
  bottom: 0;
  width: 72px;
  background: #fff;
  border-right: 1px solid #eee;
  z-index: 100;
  transition: width 0.2s ease;
}

.knowledge-sidebar.expanded {
  width: 200px;
}

.knowledge-sidebar-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 12px 14px 12px 14px;
  min-width: 72px;
  box-sizing: border-box;
}

.knowledge-sidebar:not(.expanded) .knowledge-sidebar-inner {
  padding-left: 14px;
  padding-right: 14px;
  align-items: center;
}

.knowledge-sidebar.expanded .knowledge-sidebar-inner {
  padding-left: 14px;
  padding-right: 14px;
}

/* 统一：所有导航项同高、同内边距、同图标尺寸，左对齐 */
.knowledge-nav-item {
  height: 44px;
  min-width: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  padding: 0 12px;
  color: #555;
  text-decoration: none;
  transition: color 0.2s, background 0.2s;
  margin-bottom: 2px;
  box-sizing: border-box;
}

.knowledge-sidebar:not(.expanded) .knowledge-nav-item {
  justify-content: center;
  padding: 0;
}

.knowledge-nav-item:hover {
  color: #111;
  background: #f0f0f0;
}

.knowledge-nav-item.active {
  color: #111;
  background: #f0f0f0;
  font-weight: 500;
}

.knowledge-nav-item .el-icon {
  font-size: 20px;
  flex-shrink: 0;
  width: 20px;
  height: 20px;
}

.knowledge-nav-text {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
}

.knowledge-nav-spacer {
  flex: 1;
  min-height: 12px;
}

/* 折叠标：与导航项同宽同高，对齐 */
.knowledge-collapse-btn {
  width: 44px;
  height: 44px;
  margin-bottom: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  flex-shrink: 0;
}

.knowledge-sidebar:not(.expanded) .knowledge-collapse-btn {
  width: 44px;
}

.knowledge-sidebar.expanded .knowledge-collapse-btn {
  width: 100%;
  min-width: 0;
  justify-content: flex-start;
  padding-left: 12px;
}

.knowledge-collapse-btn:hover {
  background: #f5f5f5;
}

/* 展开/折叠 icon：两栏并排，左侧带两条横线（打开的书/分栏） */
.knowledge-collapse-icon {
  position: relative;
  width: 20px;
  height: 14px;
  display: flex;
  border: 1.5px solid #555;
  border-radius: 3px;
  box-sizing: border-box;
  overflow: hidden;
}

.knowledge-collapse-btn:hover .knowledge-collapse-icon {
  border-color: #333;
}

.collapse-panel {
  box-sizing: border-box;
}

.collapse-panel-left {
  width: 7px;
  flex-shrink: 0;
  border-right: 1.5px solid #555;
  position: relative;
  background: #fff;
}

.knowledge-collapse-btn:hover .collapse-panel-left {
  border-color: #333;
}

.collapse-panel-left .collapse-line {
  position: absolute;
  left: 2px;
  width: 2px;
  height: 1px;
  background: #555;
  border-radius: 0.5px;
}

.knowledge-collapse-btn:hover .collapse-line {
  background: #333;
}

.collapse-panel-left .collapse-line:first-child {
  top: 4px;
}

.collapse-panel-left .collapse-line:last-child {
  top: 8px;
}

.collapse-panel-right {
  flex: 1;
  min-width: 0;
  background: #fff;
}

/* 展开时图标左右对调，表示“可收起” */
.knowledge-collapse-icon.expanded {
  flex-direction: row-reverse;
}

.knowledge-collapse-icon.expanded .collapse-panel-left {
  border-right: none;
  border-left: 1.5px solid #555;
}

.knowledge-collapse-btn:hover .knowledge-collapse-icon.expanded .collapse-panel-left {
  border-left-color: #333;
}

/* 知识库内容边栏（仅此栏内容多时可内部滚动） */
.knowledge-library-sidebar {
  width: 260px;
  flex-shrink: 0;
  margin-left: 72px;
  height: calc(100vh - 64px);
  max-height: calc(100vh - 64px);
  background: #fff;
  border-right: 1px solid #eee;
  overflow-y: auto;
  overflow-x: hidden;
  transition: margin-left 0.2s ease;
}

.knowledge-library-sidebar.expanded {
  margin-left: 200px;
}

.knowledge-library-inner {
  padding: 20px 18px 32px;
  box-sizing: border-box;
  min-height: min-content;
}

/* 右侧知识库详情边栏（点击某项时显示） */
.knowledge-detail-sidebar {
  width: 280px;
  flex-shrink: 0;
  height: calc(100vh - 64px);
  max-height: calc(100vh - 64px);
  background: #fff;
  border-right: 1px solid #eee;
  overflow-y: auto;
  overflow-x: hidden;
}

.knowledge-detail-inner {
  padding: 20px 18px 32px;
  box-sizing: border-box;
}

.knowledge-detail-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.knowledge-detail-cover-row {
  display: flex;
  gap: 12px;
  min-width: 0;
}

.knowledge-detail-cover-side {
  flex: 1;
  min-width: 100px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  overflow: hidden;
}

.knowledge-detail-cover-side .knowledge-detail-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #111;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.knowledge-detail-cover-meta {
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 8px;
  min-width: 0;
}

.knowledge-detail-visibility {
  display: inline-block;
  font-size: 11px;
  color: #BB1919;
  background: rgba(187, 25, 25, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
}

.knowledge-detail-author {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  flex-shrink: 0;
}

.knowledge-detail-author-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  flex-shrink: 0;
}

.knowledge-detail-author-avatar-ph {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #e0e0e0;
  color: #666;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.knowledge-detail-author-name {
  font-size: 12px;
  color: #333;
}

.knowledge-detail-stats {
  font-size: 11px;
  color: #888;
  white-space: nowrap;
  flex-shrink: 0;
}

.knowledge-detail-header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.knowledge-detail-btn.small {
  padding: 4px 10px;
  font-size: 12px;
}

.knowledge-detail-btn.danger {
  color: #BB1919;
  border-color: #BB1919;
  background: #fff;
}

.knowledge-detail-btn.danger:hover {
  background: #fff8f8;
  border-color: #9e1515;
  color: #9e1515;
}

.knowledge-detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #111;
  margin: 0;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
}

.knowledge-detail-close {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s, color 0.2s;
}

.knowledge-detail-close:hover {
  background: #f0f0f0;
  color: #111;
}

.knowledge-detail-cover-wrap {
  width: 120px;
  aspect-ratio: 4 / 3;
  border-radius: 8px;
  overflow: hidden;
  background: #f0f0f0;
  flex-shrink: 0;
}

.knowledge-detail-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.knowledge-detail-cover-placeholder {
  width: 120px;
  aspect-ratio: 4 / 3;
  border-radius: 8px;
  background: #f0eef5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #888;
  font-size: 11px;
  flex-shrink: 0;
}

.knowledge-detail-cover-placeholder .el-icon {
  font-size: 24px;
}

.knowledge-detail-meta {
  margin: 0 0 16px;
}

.knowledge-detail-meta:last-child {
  margin-bottom: 0;
}

.knowledge-detail-meta dt {
  font-size: 12px;
  color: #888;
  margin: 0 0 6px;
  font-weight: 500;
}

.knowledge-detail-meta dd {
  font-size: 14px;
  color: #333;
  margin: 0;
  line-height: 1.5;
}

.knowledge-detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  margin-bottom: 16px;
}

.knowledge-detail-btn {
  padding: 6px 14px;
  font-size: 13px;
  border-radius: 6px;
  border: 1px solid #ddd;
  background: #fff;
  color: #333;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.knowledge-detail-btn:hover:not(:disabled) {
  background: #f5f5f5;
  border-color: #ccc;
}

.knowledge-detail-btn.primary {
  background: #BB1919;
  border-color: #BB1919;
  color: #fff;
}

.knowledge-detail-btn.primary:hover:not(:disabled) {
  background: #9e1515;
  border-color: #9e1515;
}

.knowledge-detail-btn.primary.subscribed {
  background: #f0f0f0;
  border-color: #ddd;
  color: #666;
}

.knowledge-detail-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.knowledge-detail-list-title {
  font-size: 14px;
  font-weight: 600;
  color: #111;
  margin: 0 0 12px;
}

.knowledge-detail-empty {
  font-size: 13px;
  color: #888;
  padding: 16px 0;
}

.knowledge-detail-article-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.knowledge-detail-article-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  transition: background 0.2s;
}

.knowledge-detail-article-item:hover {
  background: #f5f5f5;
}

.knowledge-detail-article-checkbox {
  flex-shrink: 0;
}

.knowledge-detail-article-link {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #333;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-detail-article-link:hover {
  color: #BB1919;
}

.knowledge-detail-article-remove {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}

.knowledge-detail-article-remove:hover {
  color: #BB1919;
  background: #fafafa;
}

.knowledge-detail-article-more {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}

.knowledge-detail-article-more:hover {
  color: #333;
  background: #f0f0f0;
}

.knowledge-add-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 14px;
  border-bottom: 1px solid #e0e0e0;
}

.knowledge-add-tab {
  padding: 8px 14px;
  font-size: 14px;
  color: #666;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.knowledge-add-tab:hover {
  color: #333;
}

.knowledge-add-tab.active {
  color: #BB1919;
  font-weight: 600;
  border-bottom-color: #BB1919;
}

.knowledge-add-dialog .knowledge-add-search-row {
  margin-bottom: 12px;
}

.knowledge-add-source-list {
  min-height: 120px;
}

.knowledge-add-column-folder-list {
  list-style: none;
  padding: 0;
  margin: 0 0 12px 0;
  max-height: 160px;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 6px;
}

.knowledge-add-source-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.knowledge-add-source-item:last-child {
  border-bottom: none;
}

.knowledge-add-source-item:hover {
  background: #f8f8f8;
}

.knowledge-add-source-item.active {
  background: rgba(187, 25, 25, 0.08);
  color: #BB1919;
}

.knowledge-add-source-name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-add-source-count {
  font-size: 12px;
  color: #888;
  margin-left: 8px;
}

.knowledge-add-articles-in-source {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.knowledge-add-articles-title {
  font-size: 13px;
  color: #666;
  margin: 0 0 8px 0;
}

.knowledge-add-empty {
  font-size: 13px;
  color: #888;
  padding: 24px 0;
  margin: 0;
}

.knowledge-add-list {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 320px;
  overflow-y: auto;
}

.knowledge-add-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.knowledge-add-item .el-checkbox {
  flex-shrink: 0;
}

.knowledge-add-checkbox-ph {
  width: 16px;
  flex-shrink: 0;
  display: inline-block;
}

.knowledge-add-btn-inline {
  flex-shrink: 0;
  padding: 4px 12px;
  font-size: 13px;
  color: #fff;
  background: #BB1919;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.knowledge-add-btn-inline:hover:not(:disabled) {
  background: #9e1515;
}

.knowledge-add-btn-inline:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.knowledge-add-batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eee;
  font-size: 13px;
  color: #666;
}

.knowledge-add-btn-batch {
  padding: 6px 16px;
  font-size: 14px;
  color: #fff;
  background: #BB1919;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.knowledge-add-btn-batch:hover:not(:disabled) {
  background: #9e1515;
}

.knowledge-add-btn-batch:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.knowledge-add-item:last-child {
  border-bottom: none;
}

.knowledge-add-title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-add-tag {
  font-size: 12px;
  color: #888;
}

/* 新建知识库弹窗：BBC 风格（主色 #BB1919、输入框焦点、按钮） */
.knowledge-create-dialog--bbc:deep(.el-dialog__header) {
  color: #111;
  border-bottom: 1px solid #eee;
}
.knowledge-create-dialog--bbc:deep(.el-dialog__body) {
  --el-color-primary: #BB1919;
}
.knowledge-create-dialog--bbc:deep(.el-input__wrapper),
.knowledge-create-dialog--bbc:deep(.el-textarea__inner) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}
.knowledge-create-dialog--bbc:deep(.el-input__wrapper:hover),
.knowledge-create-dialog--bbc:deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #BB1919 inset;
}
.knowledge-create-dialog--bbc:deep(.el-input__wrapper:focus-within),
.knowledge-create-dialog--bbc:deep(.el-textarea__inner:focus-within) {
  box-shadow: 0 0 0 1px #BB1919 inset;
}
.knowledge-create-dialog--bbc:deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: #BB1919 !important;
  background: #BB1919 !important;
}
.knowledge-create-dialog--bbc:deep(.el-radio__input.is-checked .el-radio__inner::after) {
  background-color: #fff !important;
}
.knowledge-create-dialog--bbc:deep(.el-radio__inner:hover) {
  border-color: #BB1919 !important;
}
.knowledge-create-dialog--bbc:deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #BB1919 !important;
}
.knowledge-create-dialog--bbc:deep(.el-radio__label) {
  color: #333;
}

.knowledge-create-form .knowledge-create-field {
  margin-bottom: 16px;
}

.knowledge-create-form .knowledge-create-field:last-child {
  margin-bottom: 0;
}

.knowledge-create-form .knowledge-create-field label {
  display: block;
  font-size: 13px;
  color: #333;
  margin-bottom: 6px;
}

.knowledge-create-cover {
  width: 100%;
  aspect-ratio: 16 / 10;
  max-height: 160px;
  border-radius: 8px;
  border: 1px dashed #ddd;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: #888;
  font-size: 13px;
  transition: border-color 0.2s, background 0.2s;
  overflow: hidden;
  position: relative;
}

.knowledge-create-cover:hover {
  border-color: #BB1919;
  background: #fff8f8;
  color: #BB1919;
}

.knowledge-create-cover.uploading {
  cursor: not-allowed;
  border-color: #BB1919;
}

.knowledge-create-cover-plus {
  font-size: 28px;
}

.knowledge-create-cover-loading {
  font-size: 24px;
  color: #BB1919;
}

.knowledge-create-cover.has-cover {
  padding: 0;
  border-style: solid;
}

.knowledge-create-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.knowledge-create-cover-remove {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 4px 10px;
  font-size: 12px;
  border: none;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  cursor: pointer;
  transition: background 0.2s;
}

.knowledge-create-cover-remove:hover {
  background: rgba(0, 0, 0, 0.7);
}

.knowledge-create-cover-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
}

.knowledge-create-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.knowledge-create-submit {
  background: #BB1919 !important;
  border-color: #BB1919 !important;
}
.knowledge-create-submit:hover {
  background: #9e1515 !important;
  border-color: #9e1515 !important;
}

/* 中间主内容区 */
.knowledge-main {
  flex: 1;
  min-height: 0;
  height: calc(100vh - 64px);
  background: #f8f8f8;
  min-width: 0;
  overflow-y: auto;
}

/* 热门知识库/搜索结果列表面板 */
.knowledge-main-list-panel {
  padding: 20px 24px 40px;
  max-width: 900px;
  margin: 0 auto;
}

.knowledge-list-panel-header {
  margin-bottom: 20px;
}

.knowledge-list-panel-title {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.knowledge-list-panel-search {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 400px;
}

.knowledge-list-panel-search .knowledge-search-icon {
  color: #999;
  font-size: 16px;
}

.knowledge-list-panel-search .knowledge-search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.knowledge-list-search-btn {
  padding: 8px 16px;
  font-size: 14px;
  color: #fff;
  background: #BB1919;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.knowledge-list-search-btn:hover {
  background: #9e1515;
}

.knowledge-list-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px;
  color: #666;
}

.knowledge-list-empty {
  margin: 0;
  padding: 48px 0;
  list-style: none;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.knowledge-list-cards {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 16px;
}

.knowledge-list-card {
  display: flex;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.knowledge-list-card:hover {
  border-color: #BB1919;
  box-shadow: 0 2px 8px rgba(187, 25, 25, 0.1);
}

.knowledge-list-card-cover {
  width: 120px;
  min-width: 120px;
  height: 90px;
  background: #f0f0f0;
}

.knowledge-list-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.knowledge-list-card-body {
  flex: 1;
  min-width: 0;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.knowledge-list-card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  line-height: 1.3;
}

.knowledge-list-card-desc {
  margin: 0;
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.knowledge-list-card-meta {
  margin-top: auto;
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 12px;
}

.knowledge-list-card-subscribed {
  color: #BB1919;
  font-weight: 500;
}

.knowledge-list-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.knowledge-list-page-btn {
  padding: 6px 14px;
  font-size: 13px;
  color: #333;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
}

.knowledge-list-page-btn:hover:not(:disabled) {
  border-color: #BB1919;
  color: #BB1919;
}

.knowledge-list-page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.knowledge-list-page-info {
  font-size: 13px;
  color: #666;
}

.knowledge-main-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 15px;
  color: #999;
}

.knowledge-main-reader {
  padding: 20px 24px 40px;
  max-width: 800px;
  margin: 0 auto;
}

.knowledge-main-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px;
  color: #666;
}

.knowledge-article-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.knowledge-article-title-row .knowledge-article-title {
  flex: 1;
  min-width: 0;
  margin: 0;
}

.knowledge-main-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  font-size: 18px;
  color: #666;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.knowledge-main-close:hover {
  color: #BB1919;
  border-color: #BB1919;
}

.knowledge-main-card {
  background: #fff;
  border-radius: 8px;
  padding: 28px 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.knowledge-article-title {
  font-size: 24px;
  font-weight: 700;
  color: #111;
  line-height: 1.35;
}

.knowledge-article-summary {
  font-size: 15px;
  color: #555;
  line-height: 1.6;
  margin: 0 0 14px;
}

.knowledge-article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.knowledge-article-tag {
  font-size: 12px;
  color: #666;
  background: #f0f0f0;
  padding: 4px 10px;
  border-radius: 4px;
}

.knowledge-article-author {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.knowledge-article-author-info {
  flex: 1;
  min-width: 0;
}

.knowledge-article-author-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 4px;
}

.knowledge-article-author-link {
  font-size: 13px;
  color: #BB1919;
  margin-right: 12px;
  text-decoration: none;
}

.knowledge-article-author-link:hover {
  text-decoration: underline;
}

.knowledge-article-stats {
  font-size: 13px;
  color: #888;
  margin-bottom: 20px;
}

.knowledge-article-stats span {
  margin-right: 16px;
}

.knowledge-article-date {
  margin-left: 4px;
}

.knowledge-article-body {
  font-size: 15px;
  line-height: 1.75;
  color: #333;
  min-height: 120px;
}

.knowledge-article-body :deep(.vditor-reset) {
  padding: 0;
}

.knowledge-detail-article-link {
  flex: 1;
  min-width: 0;
  text-align: left;
  padding: 0;
  border: none;
  background: none;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}

.knowledge-detail-article-link:hover {
  color: #BB1919;
}

.knowledge-detail-article-link.active {
  color: #BB1919;
  font-weight: 600;
}


.knowledge-page-title {
  font-size: 22px;
  font-weight: 700;
  color: #111;
  margin: 0 0 24px;
  line-height: 1.3;
}

.knowledge-hot-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin-bottom: 0;
  border: none;
  border-radius: 10px;
  background: #f0eef5;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.knowledge-hot-tab:hover {
  background: #e5e0f0;
  color: #111;
}

.knowledge-hot-tab-icon {
  font-size: 18px;
}

.knowledge-main-divider {
  height: 1px;
  background: #ddd;
  margin: 24px 0;
  border: none;
  flex-shrink: 0;
}

.knowledge-search-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 0;
  margin-bottom: 28px;
}

.knowledge-search-inner {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  transition: border-color 0.2s;
}

.knowledge-search-inner:focus-within {
  border-color: #999;
}

.knowledge-search-icon {
  font-size: 14px;
  color: #999;
  flex-shrink: 0;
}

.knowledge-search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  font-size: 12px;
  color: #111;
  background: transparent;
}

.knowledge-search-input::placeholder {
  color: #999;
}

.knowledge-add-btn {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.knowledge-add-btn:hover {
  background: #f5f5f5;
  color: #111;
  border-color: #ddd;
}

.knowledge-add-btn .el-icon {
  font-size: 16px;
}

.knowledge-section {
  margin-top: 0;
  margin-bottom: 28px;
}

.knowledge-section:last-child {
  margin-bottom: 0;
}

.knowledge-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #111;
  margin: 0 0 12px;
}

.knowledge-my-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.knowledge-my-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background 0.2s;
  cursor: pointer;
}

.knowledge-my-item:hover {
  background: #f5f5f5;
}

.knowledge-my-item.active {
  background: #f0eef5;
  color: #333;
}

.knowledge-my-icon {
  font-size: 16px;
  color: #409eff;
  flex-shrink: 0;
}

.knowledge-my-name {
  font-size: 13px;
  color: #333;
}

.knowledge-input-wrap {
  margin-bottom: 56px;
}

.knowledge-input-box {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 16px 20px 12px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.knowledge-input-box:focus-within {
  border-color: #BB1919;
  box-shadow: 0 0 0 2px rgba(187, 25, 25, 0.12);
}

.knowledge-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 16px;
  line-height: 1.5;
  color: #111;
  background: transparent;
  display: block;
  margin-bottom: 12px;
}

.knowledge-input::placeholder {
  color: #999;
}

.knowledge-input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.knowledge-input-opt {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #888;
  cursor: pointer;
}

.knowledge-input-opt:hover {
  color: #111;
}

.knowledge-input-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.knowledge-input-action {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
}

.knowledge-input-action:hover {
  color: #BB1919;
  background: #fafafa;
}

.knowledge-input-send {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #BB1919;
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.knowledge-input-send:hover {
  background: #9e1515;
}

/* 两列 */
.knowledge-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
}

.knowledge-col-title {
  font-size: 18px;
  font-weight: 600;
  color: #111;
  margin: 0 0 20px;
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.knowledge-list-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  border: 1px solid transparent;
  transition: border-color 0.2s, background 0.2s;
}

.knowledge-list-item:hover {
  background: #fafafa;
  border-color: #eee;
}

.knowledge-list-thumb {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  background: #eee;
}

.knowledge-list-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.knowledge-list-thumb-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: #999;
}

.knowledge-list-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.knowledge-list-name {
  font-size: 15px;
  font-weight: 600;
  color: #111;
}

.knowledge-list-meta {
  font-size: 13px;
  color: #888;
}

.knowledge-col-link {
  font-size: 14px;
  color: #BB1919;
  text-decoration: none;
}

.knowledge-col-link:hover {
  text-decoration: underline;
}

.knowledge-questions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.knowledge-question-btn {
  width: 100%;
  padding: 14px 18px;
  text-align: left;
  font-size: 15px;
  color: #333;
  background: #f7f7f7;
  border: 1px solid #eee;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.knowledge-question-btn:hover {
  background: #f0f0f0;
  border-color: #e0e0e0;
  color: #111;
}

@media (max-width: 900px) {
  .knowledge-cols {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
/* 加号下拉菜单：BBC 红，无 scoped 以便作用于 teleport 出的 popper */
.knowledge-add-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item {
  color: #333;
}
.knowledge-add-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item:hover,
.knowledge-add-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item:focus {
  background: #fff8f8;
  color: #BB1919;
}

/* 新建知识库弹窗内可见性单选：强制 BBC 红（弹窗 teleport 到 body，需全局样式） */
.knowledge-create-dialog--bbc .el-dialog__body {
  --el-color-primary: #BB1919;
}
.knowledge-create-dialog--bbc .el-radio__input.is-checked .el-radio__inner {
  border-color: #BB1919 !important;
  background: #BB1919 !important;
}
.knowledge-create-dialog--bbc .el-radio__input.is-checked .el-radio__inner::after {
  background-color: #fff !important;
}
.knowledge-create-dialog--bbc .el-radio__inner:hover {
  border-color: #BB1919 !important;
}
.knowledge-create-dialog--bbc .el-radio__input.is-checked + .el-radio__label {
  color: #BB1919 !important;
}
.knowledge-create-dialog--bbc .el-radio__label {
  color: #606266;
}

/* 收录文章右侧「...」下拉：删除这篇博客 用 BBC 红 */
.knowledge-article-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item {
  color: #333;
}
.knowledge-article-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item:hover,
.knowledge-article-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item:focus {
  background: #fff8f8;
  color: #BB1919;
}
.knowledge-article-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item.knowledge-article-dropdown-danger {
  color: #BB1919;
}
.knowledge-article-dropdown-bbc.el-dropdown__popper .el-dropdown-menu__item.knowledge-article-dropdown-danger:hover {
  background: #fff8f8;
  color: #9e1515;
}
</style>
