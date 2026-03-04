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

        <!-- 1. 知识库：点击与「返回知识库」同效，退出问答模式 -->
        <router-link
          to="/knowledge"
          class="knowledge-nav-item"
          :class="{ active: !showQAMode }"
          title="知识库"
          @click="exitQAMode"
        >
          <el-icon><FolderOpened /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">知识库</span>
        </router-link>
        <!-- 2. 搜索：点击进入问答模式（右侧 GPT 问答 + 左侧历史记录） -->
        <button
          type="button"
          class="knowledge-nav-item"
          :class="{ active: showQAMode }"
          title="搜索"
          @click="enterQAMode"
        >
          <el-icon><Search /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">搜索</span>
        </button>
        <!-- 3. 知识图谱 -->
        <button type="button" class="knowledge-nav-item" title="知识图谱">
          <el-icon><Connection /></el-icon>
          <span v-if="sidebarExpanded" class="knowledge-nav-text">知识图谱</span>
        </button>

        <div class="knowledge-nav-spacer"></div>
      </div>
    </aside>

    <!-- 中间偏左：知识库内容边栏（问答模式下显示历史记录，否则为热门/搜索/我的知识库/订阅） -->
    <aside class="knowledge-library-sidebar" :class="{ expanded: sidebarExpanded }">
      <div class="knowledge-library-inner">
        <h1 class="knowledge-page-title">知识库</h1>
        <template v-if="showQAMode">
          <button type="button" class="knowledge-hot-tab knowledge-qa-back" @click="exitQAMode">
            <el-icon class="knowledge-hot-tab-icon"><ArrowLeft /></el-icon>
            <span>返回知识库</span>
          </button>
          <div class="knowledge-main-divider" />
          <section class="knowledge-section">
            <h2 class="knowledge-section-title">历史记录</h2>
            <p v-if="qaHistory.length === 0" class="knowledge-search-no-result">暂无搜索记录</p>
            <ul v-else class="knowledge-my-list">
              <li
                v-for="item in qaHistory"
                :key="item.id"
                class="knowledge-my-item knowledge-qa-history-item"
                @click="applyHistoryQuery(item)"
              >
                <el-icon class="knowledge-my-icon"><Search /></el-icon>
                <span class="knowledge-my-name knowledge-qa-history-query">{{ item.query }}</span>
              </li>
            </ul>
          </section>
        </template>
        <template v-else>
          <button type="button" class="knowledge-hot-tab" @click="openPopularList">
            <el-icon class="knowledge-hot-tab-icon"><FolderOpened /></el-icon>
            <span>热门知识库</span>
          </button>
          <div class="knowledge-main-divider" />
          <div class="knowledge-search-row">
            <div class="knowledge-search-inner">
              <el-icon class="knowledge-search-icon"><Search /></el-icon>
              <input
                v-model="leftSearchQuery"
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
            <p v-if="leftSearchQuery.trim() && myKnowledgeBasesWithDefaultFiltered.length === 0" class="knowledge-search-no-result">
              无匹配的知识库
            </p>
            <ul v-else class="knowledge-my-list">
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
            <p v-if="leftSearchQuery.trim() && mySubscriptionsFiltered.length === 0" class="knowledge-search-no-result">
              无匹配的订阅
            </p>
            <ul v-else class="knowledge-my-list">
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
        </template>
      </div>
    </aside>

    <!-- 右侧：知识库详情边栏（问答模式下不显示） -->
    <aside
      v-show="selectedKb && !showQAMode"
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

        <!-- 操作：自己的显示添加下拉/批量删除，别人的显示订阅 -->
        <div class="knowledge-detail-actions">
          <template v-if="isOwnDetail">
            <el-dropdown trigger="click" placement="bottom-start" popper-class="knowledge-add-dropdown-bbc" @command="onAddContentCommand">
              <button type="button" class="knowledge-detail-btn primary">
                添加
                <el-icon class="knowledge-detail-btn-arrow"><ArrowDown /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="fromBlog">从博客添加</el-dropdown-item>
                  <el-dropdown-item command="newFile">添加文件</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
            <template v-if="editingContentId === art.id">
              <input
                ref="editingTitleInputRef"
                v-model="editingTitle"
                type="text"
                class="knowledge-detail-article-edit-input"
                @blur="saveEditingTitle"
                @keydown.enter.exact.prevent="saveEditingTitle"
              />
            </template>
            <button
              v-else
              type="button"
              class="knowledge-detail-article-link"
              :class="{ active: selectedContentId === art.id }"
              @click="selectArticleInMain(art.id)"
            >
              {{ art.title }}
            </button>
            <el-dropdown
              v-if="isOwnDetail && !detailBatchMode && editingContentId !== art.id"
              trigger="click"
              placement="bottom-end"
              popper-class="knowledge-article-dropdown-bbc"
              @command="onDetailArticleDropdownCommand"
            >
              <button type="button" class="knowledge-detail-article-more" title="更多" @click.stop>
                <el-icon><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="'rename:' + art.id">重命名</el-dropdown-item>
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

    <!-- 中间：主内容区（问答模式 > 文章阅读 > 热门/搜索列表 > 占位） -->
    <main class="knowledge-main" :class="{ expanded: sidebarExpanded }">
      <!-- 问答模式：GPT 风格对话 -->
      <div v-if="showQAMode" class="knowledge-qa-panel">
        <div class="knowledge-qa-header">
          <h2 class="knowledge-qa-title">知识库问答</h2>
          <p class="knowledge-qa-desc">基于你的知识库与订阅进行智能问答</p>
        </div>
        <div ref="qaListRef" class="knowledge-qa-messages">
          <template v-if="qaMessages.length === 0">
            <div class="knowledge-qa-welcome">
              <p>输入你的问题，我会基于知识库内容为你解答。</p>
              <p class="knowledge-qa-welcome-hint">左侧将显示历史提问记录</p>
            </div>
          </template>
          <template v-else>
            <div
              v-for="(msg, i) in qaMessages"
              :key="i"
              class="knowledge-qa-msg-wrap"
              :class="msg.role === 'user' ? 'is-user' : 'is-assistant'"
            >
              <div class="knowledge-qa-msg">
                <template v-if="msg.role === 'assistant'">
                  <div class="knowledge-qa-avatar knowledge-qa-avatar-bot">AI</div>
                  <div class="knowledge-qa-bubble knowledge-qa-bubble-assistant">{{ msg.content }}</div>
                </template>
                <template v-else>
                  <div class="knowledge-qa-bubble knowledge-qa-bubble-user">{{ msg.content }}</div>
                  <div class="knowledge-qa-avatar knowledge-qa-avatar-user">我</div>
                </template>
              </div>
            </div>
            <div v-if="qaLoading" class="knowledge-qa-msg-wrap is-assistant">
              <div class="knowledge-qa-msg">
                <div class="knowledge-qa-avatar knowledge-qa-avatar-bot">AI</div>
                <div class="knowledge-qa-bubble knowledge-qa-bubble-assistant knowledge-qa-typing">
                  <span class="knowledge-qa-dot"></span><span class="knowledge-qa-dot"></span><span class="knowledge-qa-dot"></span>
                </div>
              </div>
            </div>
          </template>
        </div>
        <div class="knowledge-qa-input-row">
          <button
            type="button"
            class="knowledge-qa-add-kb"
            title="添加知识库"
            @click="onAddDropdownCommand('newKb')"
          >
            <el-icon><Plus /></el-icon>
          </button>
          <textarea
            v-model="qaInput"
            class="knowledge-qa-input"
            placeholder="输入问题，按 Enter 发送"
            rows="1"
            @keydown.enter.exact.prevent="sendQAMessage()"
          />
          <button
            type="button"
            class="knowledge-qa-send"
            :disabled="!qaInput.trim() || qaLoading"
            @click="sendQAMessage"
          >
            发送
          </button>
        </div>
      </div>
      <!-- 知识库文件编辑：与博客创作相同的工具栏 + 纯 Markdown 正文（无标题） -->
      <div v-else-if="selectedContentId != null && isKnowledgeEditor" class="knowledge-main-editor">
        <div v-if="knowledgeEditLoading" class="knowledge-main-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中…</span>
        </div>
        <template v-else>
          <div class="knowledge-editor-card">
            <div class="knowledge-editor-toolbar">
              <button type="button" class="knowledge-tool-btn" @click="kbOnUndo">
              <el-icon><RefreshLeft /></el-icon>
              <span class="knowledge-tool-label">撤销</span>
            </button>
            <button type="button" class="knowledge-tool-btn" @click="kbOnRedo">
              <el-icon><RefreshRight /></el-icon>
              <span class="knowledge-tool-label">重做</span>
            </button>
            <el-divider direction="vertical" />
            <el-dropdown @command="kbOnHeadingCommand">
              <button type="button" class="knowledge-tool-btn">
                <span class="knowledge-tool-icon">H</span>
                <span class="knowledge-tool-label">标题</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="1">一级标题</el-dropdown-item>
                  <el-dropdown-item command="2">二级标题</el-dropdown-item>
                  <el-dropdown-item command="3">三级标题</el-dropdown-item>
                  <el-dropdown-item command="4">四级标题</el-dropdown-item>
                  <el-dropdown-item command="5">五级标题</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <button type="button" class="knowledge-tool-btn" @click="kbOnBold">
              <span class="knowledge-tool-icon">B</span>
              <span class="knowledge-tool-label">加粗</span>
            </button>
            <button type="button" class="knowledge-tool-btn" @click="kbOnItalic">
              <span class="knowledge-tool-icon">I</span>
              <span class="knowledge-tool-label">斜体</span>
            </button>
            <button type="button" class="knowledge-tool-btn" @click="kbOnStrikethrough">
              <span class="knowledge-tool-icon knowledge-tool-icon-strike">S</span>
              <span class="knowledge-tool-label">删除线</span>
            </button>
            <el-divider direction="vertical" />
            <button type="button" class="knowledge-tool-btn" @click="kbOnBulletList">
              <el-icon><List /></el-icon>
              <span class="knowledge-tool-label">无序列表</span>
            </button>
            <button type="button" class="knowledge-tool-btn" @click="kbOnOrderedList">
              <el-icon><Rank /></el-icon>
              <span class="knowledge-tool-label">有序列表</span>
            </button>
            <button type="button" class="knowledge-tool-btn" @click="kbOnTaskList">
              <el-icon><CircleCheck /></el-icon>
              <span class="knowledge-tool-label">任务列表</span>
            </button>
            <button type="button" class="knowledge-tool-btn" @click="kbOnInsertBefore">
              <el-icon><Top /></el-icon>
              <span class="knowledge-tool-label">前插入行</span>
            </button>
            <button type="button" class="knowledge-tool-btn" @click="kbOnInsertAfter">
              <el-icon><Bottom /></el-icon>
              <span class="knowledge-tool-label">后插入行</span>
            </button>
            <el-divider direction="vertical" />
            <button type="button" class="knowledge-tool-btn" @click="kbOnQuote">
              <span class="knowledge-tool-icon">″</span>
              <span class="knowledge-tool-label">引用</span>
            </button>
            <button type="button" class="knowledge-tool-btn" @click="kbOnHorizontalRule">
              <span class="knowledge-tool-icon knowledge-tool-icon-hr">—</span>
              <span class="knowledge-tool-label">分隔线</span>
            </button>
            <el-divider direction="vertical" />
            <button type="button" class="knowledge-tool-btn" @click="kbOnCode">
              <span class="knowledge-tool-icon">&lt;/&gt;</span>
              <span class="knowledge-tool-label">代码</span>
            </button>
            <button type="button" class="knowledge-tool-btn" @click="kbOnInlineCode">
              <span class="knowledge-tool-icon knowledge-tool-icon-inline-code">&lt;&nbsp;&gt;</span>
              <span class="knowledge-tool-label">行内代码</span>
            </button>
            <button type="button" class="knowledge-tool-btn" @click="kbOnTable">
              <span class="knowledge-tool-icon">▦</span>
              <span class="knowledge-tool-label">表格</span>
            </button>
            <input
              ref="kbImageInputRef"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              class="hidden-input"
              @change="kbOnImageFileChange"
            />
            <button type="button" class="knowledge-tool-btn" :disabled="kbImageUploading" @click="kbTriggerImageSelect">
              <span class="knowledge-tool-icon">🖼</span>
              <span class="knowledge-tool-label">{{ kbImageUploading ? '上传中…' : '图片' }}</span>
            </button>
            <button type="button" class="knowledge-tool-btn" @click="kbOnLink">
              <span class="knowledge-tool-icon">🔗</span>
              <span class="knowledge-tool-label">链接</span>
            </button>
            <button type="button" class="knowledge-editor-close" title="关闭" @click="selectedContentId = null">
              <el-icon><Close /></el-icon>
            </button>
            </div>
            <div class="knowledge-editor-paper" @mousedown="onKbPaperMouseDown">
              <div ref="kbVditorRef" class="vditor-wrap"></div>
            </div>
          </div>
        </template>
      </div>
      <!-- 文章阅读区：选中博客时显示 -->
      <div v-else-if="selectedContentId != null" class="knowledge-main-reader">
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
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Search, FolderOpened, Connection, Plus, Reading, Close, Delete, Loading, MoreFilled, ArrowLeft, ArrowDown, RefreshLeft, RefreshRight, List, Rank, CircleCheck, Top, Bottom } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { uploadImage } from '@/api/upload'
import * as knowledgeApi from '@/api/knowledge'
import { getContentsMe, getContentView, getContentForEdit, updateContentTitle, saveDraft, type ContentView } from '@/api/content'
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
const router = useRouter()

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
  /** BLOG-博客 / KNOWLEDGE-知识库文件，用于重命名时分支 */
  type?: string
  userId?: number
}

const sidebarExpanded = ref(false)
const myKnowledgeBases = ref<KnowledgeBaseItem[]>([])
const mySubscriptions = ref<KnowledgeBaseItem[]>([])
const knowledgeLoading = ref(false)

/** 左侧搜索关键词（用于「我的知识库」「我的订阅」本地过滤） */
const leftSearchQuery = ref('')

/** 按关键词过滤「我的知识库」和「我的订阅」（名称、简介匹配） */
function matchKbKeyword(kb: KnowledgeBaseItem, q: string): boolean {
  if (!q || !q.trim()) return true
  const k = q.trim().toLowerCase()
  const name = String(kb.name ?? '').toLowerCase()
  const desc = String(kb.description ?? '').toLowerCase()
  return name.includes(k) || desc.includes(k)
}

const myKnowledgeBasesFiltered = computed(() => {
  const list = myKnowledgeBases.value
  const q = leftSearchQuery.value
  if (!q || !q.trim()) return list
  return list.filter((kb) => matchKbKeyword(kb, q))
})

const mySubscriptionsFiltered = computed(() => {
  const list = mySubscriptions.value
  const q = leftSearchQuery.value
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
/** 行内编辑标题（VSCode 风格：新建文件后自动进入编辑，回车或失焦保存） */
const editingContentId = ref<number | null>(null)
const editingTitle = ref('')
const editingTitleInputRef = ref<HTMLInputElement | null>(null)

const selectedContentId = ref<number | null>(null)
const mainArticle = ref<ContentView | null>(null)
const mainArticleLoading = ref(false)
const mainArticleAuthor = ref<UserMe | null>(null)
const mainPreviewRef = ref<HTMLDivElement | null>(null)

/** 当前选中的收录项（用于区分知识库文件 vs 博客） */
const selectedDetailItem = computed(() =>
  selectedContentId.value == null ? undefined : detailContents.value.find((c) => c.id === selectedContentId.value!)
)
/** 是否为知识库类型：显示工具栏 + 纯 Markdown 编辑区 */
const isKnowledgeEditor = computed(() => selectedContentId.value != null && selectedDetailItem.value?.type === 'KNOWLEDGE')

/** 知识库文件编辑：加载态、标题（保存用）、Vditor 实例（所见即所得，与创作中心一致） */
const knowledgeEditLoading = ref(false)
const knowledgeEditTitle = ref('')
const kbVditorRef = ref<HTMLElement | null>(null)
const kbImageInputRef = ref<HTMLInputElement | null>(null)
const kbImageUploading = ref(false)
let kbVditor: Vditor | null = null
let kbSaveTimer: ReturnType<typeof setTimeout> | null = null

const isDetailSubscribed = computed(() => selectedKb.value != null && selectedKb.value.subscribed === true)

/** 问答模式：点击左侧搜索后右侧为 GPT 风格问答，左侧为历史记录 */
const showQAMode = ref(false)
const qaMessages = ref<{ role: 'user' | 'assistant'; content: string }[]>([])
const qaInput = ref('')
const qaLoading = ref(false)
const qaListRef = ref<HTMLElement | null>(null)

const QA_HISTORY_KEY = 'knowledge-qa-history'
const QA_HISTORY_MAX = 50
interface QAHistoryItem {
  id: string
  query: string
  createdAt: number
}
const qaHistory = ref<QAHistoryItem[]>([])

function loadQAHistory() {
  try {
    const raw = localStorage.getItem(QA_HISTORY_KEY)
    const list = raw ? (JSON.parse(raw) as QAHistoryItem[]) : []
    qaHistory.value = Array.isArray(list) ? list.slice(0, QA_HISTORY_MAX) : []
  } catch {
    qaHistory.value = []
  }
}

function saveQAHistory() {
  try {
    localStorage.setItem(QA_HISTORY_KEY, JSON.stringify(qaHistory.value.slice(0, QA_HISTORY_MAX)))
  } catch {}
}

function addQAHistoryItem(query: string) {
  const q = query.trim()
  if (!q) return
  const item: QAHistoryItem = { id: String(Date.now()), query: q, createdAt: Date.now() }
  qaHistory.value = [item, ...qaHistory.value.filter((h) => h.query !== q)].slice(0, QA_HISTORY_MAX)
  saveQAHistory()
}

function enterQAMode() {
  showQAMode.value = true
}

function exitQAMode() {
  showQAMode.value = false
}

async function sendQAMessage() {
  const text = qaInput.value.trim()
  if (!text || qaLoading.value) return
  qaInput.value = ''
  qaMessages.value.push({ role: 'user', content: text })
  addQAHistoryItem(text)
  qaLoading.value = true
  nextTick(() => scrollQAToBottom())
  // 占位回复（后续可接真实 RAG 接口）
  await new Promise((r) => setTimeout(r, 600))
  qaMessages.value.push({
    role: 'assistant',
    content: '基于知识库的智能问答功能即将上线，敬请期待。您刚才的问题是：「' + text + '」',
  })
  qaLoading.value = false
  nextTick(() => scrollQAToBottom())
}

function scrollQAToBottom() {
  nextTick(() => {
    const el = qaListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function applyHistoryQuery(item: QAHistoryItem) {
  qaInput.value = item.query
}

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
    destroyKbVditor()
    return
  }
  const item = detailContents.value.find((c) => c.id === id)
  if (item?.type === 'KNOWLEDGE') {
    loadKnowledgeForEdit(id)
  } else {
    loadMainArticle(id)
  }
})

onBeforeUnmount(() => {
  destroyKbVditor()
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
      type: c.type,
      userId: c.userId,
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

function destroyKbVditor() {
  if (kbSaveTimer) {
    clearTimeout(kbSaveTimer)
    kbSaveTimer = null
  }
  kbVditor?.destroy()
  kbVditor = null
}

/** 让 Vditor 编辑区获得焦点并显示光标，多次重试以兼容异步渲染 */
function focusKbVditor() {
  const tryFocus = (attempt: number) => {
    if (!kbVditorRef.value || !kbVditor) return
    try {
      const inner = kbVditor.vditor as unknown as Record<string, { element?: HTMLElement } | undefined>
      const wysiwyg = inner?.wysiwyg
      const el = wysiwyg?.element
      if (el && typeof el.focus === 'function') {
        el.setAttribute?.('tabindex', '0')
        el.focus()
        return
      }
      const editable = kbVditorRef.value.querySelector('[contenteditable="true"]') as HTMLElement | null
      if (editable?.focus) {
        editable.setAttribute?.('tabindex', '0')
        editable.focus()
      }
    } catch {}
    if (attempt < 6) setTimeout(() => tryFocus(attempt + 1), 60 + attempt * 40)
  }
  nextTick(() => setTimeout(() => tryFocus(0), 100))
}

/** 点击白纸区域时若未点在编辑元素上则主动聚焦，便于显示光标 */
function onKbPaperMouseDown(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (target.closest?.('[contenteditable="true"]')) return
  focusKbVditor()
}

function saveKnowledgeBody() {
  const id = selectedContentId.value
  if (id == null || !kbVditor) return
  const body = kbVditor.getValue() ?? ''
  // 知识库允许正文为空，直接保存
  saveDraft({ id, body, title: knowledgeEditTitle.value || undefined })
    .then(() => ElMessage.success('已保存'))
    .catch((err: { response?: { data?: { message?: string } }; message?: string }) => {
      const msg = err?.response?.data?.message || err?.message || '保存失败'
      ElMessage.warning(msg)
    })
}

async function loadKnowledgeForEdit(contentId: number) {
  knowledgeEditLoading.value = true
  destroyKbVditor()
  try {
    const data = await getContentForEdit(contentId)
    knowledgeEditTitle.value = data.title ?? ''
    // 必须先关闭 loading，模板才会渲染出 vditor-wrap，ref 才会挂上
    knowledgeEditLoading.value = false
    await nextTick()
    if (!kbVditorRef.value) return
    kbVditor = new Vditor(kbVditorRef.value, {
      height: 420,
      value: data.body ?? '',
      placeholder: '在此编写知识库文件内容…',
      lang: 'zh_CN',
      mode: 'wysiwyg',
      theme: 'classic',
      cache: { enable: false },
      toolbarConfig: { hide: true },
      customWysiwygToolbar: () => [],
      counter: { enable: true, type: 'markdown' },
      input() {
        if (kbSaveTimer) clearTimeout(kbSaveTimer)
        kbSaveTimer = setTimeout(saveKnowledgeBody, 800)
      },
      after() {
        focusKbVditor()
      },
    })
  } catch {
    knowledgeEditLoading.value = false
    ElMessage.warning('加载失败')
  }
}

/** 知识库编辑器工具栏：与创作中心一致，操作 Vditor（所见即所得） */
function kbInsertMD(md: string) {
  if (!kbVditor) return
  kbVditor.insertMD(md)
}
function kbGetSelectionInlineFormat(): { bold: boolean; italic: boolean; strike: boolean } {
  const v = kbVditor?.vditor
  if (!v) return { bold: false, italic: false, strike: false }
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return { bold: false, italic: false, strike: false }
  const range = sel.getRangeAt(0)
  const modeEl = v[v.currentMode]
  const editorEl = modeEl?.element
  if (!editorEl?.contains(range.startContainer)) return { bold: false, italic: false, strike: false }
  let node: Node | null = range.startContainer
  if (node.nodeType === Node.TEXT_NODE) node = (node as Text).parentElement
  const el = node as HTMLElement
  if (!el?.closest) return { bold: false, italic: false, strike: false }
  return {
    bold: !!(el.closest('strong') || el.closest('b') || el.closest('[data-type="strong"]')),
    italic: !!(el.closest('em') || el.closest('i') || el.closest('[data-type="em"]')),
    strike: !!(el.closest('s') || el.closest('strike') || el.closest('[data-type="s"]')),
  }
}
function kbTriggerVditorToolbar(name: 'list' | 'ordered-list' | 'check' | 'insert-before' | 'insert-after' | 'table' | 'inline-code' | 'quote' | 'line'): boolean {
  const btn = kbVditor?.vditor?.toolbar?.elements?.[name]?.firstElementChild as HTMLElement | undefined
  if (btn) {
    btn.click()
    return true
  }
  return false
}
function kbOnUndo() {
  if (!kbVditor?.vditor?.undo) return
  kbVditor.vditor.undo.undo(kbVditor.vditor)
}
function kbOnRedo() {
  if (!kbVditor?.vditor?.undo) return
  kbVditor.vditor.undo.redo(kbVditor.vditor)
}
function kbOnBold() {
  if (!kbVditor) return
  const sel = kbVditor.getSelection() || '加粗文本'
  const fmt = kbVditor.getSelection() ? kbGetSelectionInlineFormat() : { bold: false, italic: false, strike: false }
  if (fmt.bold) {
    kbInsertMD(fmt.italic ? `*${sel}*` : sel)
    return
  }
  kbInsertMD(fmt.italic ? `***${sel}***` : `**${sel}**`)
}
function kbOnItalic() {
  if (!kbVditor) return
  const sel = kbVditor.getSelection() || '斜体文本'
  const fmt = kbVditor.getSelection() ? kbGetSelectionInlineFormat() : { bold: false, italic: false, strike: false }
  if (fmt.italic) {
    kbInsertMD(fmt.bold ? `**${sel}**` : sel)
    return
  }
  kbInsertMD(fmt.bold ? `***${sel}***` : `*${sel}*`)
}
function kbOnStrikethrough() {
  if (!kbVditor) return
  const sel = kbVditor.getSelection() || '删除线文本'
  const fmt = kbVditor.getSelection() ? kbGetSelectionInlineFormat() : { bold: false, italic: false, strike: false }
  if (fmt.strike) {
    kbInsertMD(sel)
    return
  }
  kbInsertMD(`~~${sel}~~`)
}
function kbOnBulletList() {
  if (!kbVditor) return
  if (!kbTriggerVditorToolbar('list')) kbInsertMD('\n- 列表项\n')
}
function kbOnOrderedList() {
  if (!kbVditor) return
  if (!kbTriggerVditorToolbar('ordered-list')) kbInsertMD('\n1. 列表项 1\n2. 列表项 2\n')
}
function kbOnTaskList() {
  if (!kbVditor) return
  if (!kbTriggerVditorToolbar('check')) kbInsertMD('\n- [ ] 待办事项\n')
}
function kbOnInsertBefore() {
  kbTriggerVditorToolbar('insert-before')
}
function kbOnInsertAfter() {
  kbTriggerVditorToolbar('insert-after')
}
function kbOnQuote() {
  if (!kbVditor) return
  if (!kbTriggerVditorToolbar('quote')) {
    const sel = kbVditor.getSelection()
    kbInsertMD(sel ? `\n> ${sel.split(/\r?\n/).join('\n> ')}\n` : '\n> 引用内容\n')
  }
}
function kbOnHorizontalRule() {
  if (!kbVditor) return
  if (!kbTriggerVditorToolbar('line')) kbInsertMD('\n---\n')
}
function kbOnCode() {
  kbInsertMD('\n```lang\n代码块\n```\n')
}
function kbOnInlineCode() {
  if (!kbVditor) return
  if (!kbTriggerVditorToolbar('inline-code')) {
    const sel = kbVditor.getSelection() || '行内代码'
    kbInsertMD(`\`${sel}\``)
  }
}
function kbOnTable() {
  if (!kbVditor) return
  if (!kbTriggerVditorToolbar('table')) kbInsertMD('\n| 列1 | 列2 | 列3 |\n| --- | --- | --- |\n|  |  |  |\n')
}
function kbTriggerImageSelect() {
  if (kbImageUploading.value) return
  kbImageInputRef.value?.click()
}
async function kbOnImageFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !kbVditor) return
  kbImageUploading.value = true
  try {
    const res = await uploadImage(file, 'images')
    const url = (res.url || '').replace(/\s/g, '%20')
    kbInsertMD(`\n![image](${url})\n`)
    ElMessage.success('图片已插入')
  } finally {
    kbImageUploading.value = false
  }
}
function kbOnLink() {
  if (!kbVditor) return
  const sel = kbVditor.getSelection() || '链接文本'
  kbInsertMD(`[${sel}](https://example.com)\n`)
}
function kbOnHeadingCommand(level: string | number) {
  const n = Number(level)
  if (!kbVditor) return
  const hashes = '#'.repeat(n >= 1 && n <= 6 ? n : 1)
  const sel = kbVditor.getSelection()
  if (sel) {
    const lines = sel.split(/\r?\n/)
    const md = lines.map((line) => `${hashes} ${line.replace(/^(#{1,6})\s+/, '').trim() || '标题'}`).join('\n')
    kbInsertMD(md)
  } else {
    kbInsertMD(`\n${hashes} 标题\n`)
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
  loadQAHistory()
})

/** 根据当前列表计算下一个「未命名」标题：未命名、未命名 (1)、未命名 (2)... */
function computeNextUntitledTitle(): string {
  const titles = new Set(detailContents.value.map((c) => c.title))
  if (!titles.has('未命名')) return '未命名'
  let n = 1
  while (titles.has(`未命名 (${n})`)) n++
  return `未命名 (${n})`
}

async function addFileInKb() {
  const kb = selectedKb.value
  if (!kb || kb.id === 'default') return
  const kbId = Number(kb.id)
  if (Number.isNaN(kbId)) return
  const title = computeNextUntitledTitle()
  try {
    const item = await knowledgeApi.createKnowledgeBaseFile(kbId, title)
    const currentUserId = userStore.userInfo?.id
    detailContents.value = [
      ...detailContents.value,
      {
        id: item.id,
        title: item.title,
        summary: item.summary ?? undefined,
        cover: item.cover ?? undefined,
        type: item.type ?? 'KNOWLEDGE',
        userId: item.userId ?? currentUserId,
      },
    ]
    if (selectedKb.value) {
      selectedKb.value = { ...selectedKb.value, contentCount: (selectedKb.value.contentCount ?? 0) + 1 }
    }
    editingContentId.value = item.id
    editingTitle.value = item.title
    nextTick(() => {
      const el = Array.isArray(editingTitleInputRef.value) ? editingTitleInputRef.value[0] : editingTitleInputRef.value
      ;(el as HTMLInputElement | undefined)?.focus()
    })
  } catch (e) {
    ElMessage.error('新建文件失败')
  }
}

async function saveEditingTitle() {
  const id = editingContentId.value
  if (id == null) return
  const title = editingTitle.value.trim() || '未命名'
  try {
    await updateContentTitle(id, title)
    const idx = detailContents.value.findIndex((c) => c.id === id)
    if (idx >= 0) {
      const next = [...detailContents.value]
      next[idx] = { ...next[idx], title }
      detailContents.value = next
    }
  } catch {
    ElMessage.error('更新标题失败')
  }
  editingContentId.value = null
  editingTitle.value = ''
}

function onAddContentCommand(command: string) {
  if (command === 'fromBlog') {
    openAddContentDialog()
  } else if (command === 'newFile') {
    addFileInKb()
  }
}

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

function onDetailArticleDropdownCommand(cmd: string) {
  if (cmd.startsWith('rename:')) {
    const id = Number(cmd.slice(7))
    if (!Number.isNaN(id)) handleRenameInDetail(id)
    return
  }
  removeContentFromKb(Number(cmd))
}

function handleRenameInDetail(contentId: number) {
  const art = detailContents.value.find((c) => c.id === contentId)
  if (!art) return
  const type = art.type ?? 'BLOG'
  const currentUserId = userStore.userInfo?.id
  if (type === 'KNOWLEDGE') {
    editingContentId.value = contentId
    editingTitle.value = art.title
    nextTick(() => {
      const el = Array.isArray(editingTitleInputRef.value) ? editingTitleInputRef.value[0] : editingTitleInputRef.value
      ;(el as HTMLInputElement | undefined)?.focus()
    })
    return
  }
  if (type === 'BLOG') {
    if (art.userId != null && art.userId === currentUserId) {
      router.push({ path: '/creator/write', query: { id: String(contentId) } })
      return
    }
    ElMessage.info('他人的博客不可编辑')
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
  width: 56px;
  background: #fff;
  border-right: 1px solid #eee;
  z-index: 100;
  transition: width 0.2s ease;
}

.knowledge-sidebar.expanded {
  width: 176px;
}

.knowledge-sidebar-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 12px 10px 12px 10px;
  min-width: 56px;
  box-sizing: border-box;
}

.knowledge-sidebar:not(.expanded) .knowledge-sidebar-inner {
  padding-left: 10px;
  padding-right: 10px;
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
  border: none;
  background: transparent;
  cursor: pointer;
  font: inherit;
  width: 100%;
  text-align: left;
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
  width: 220px;
  flex-shrink: 0;
  margin-left: 56px;
  height: calc(100vh - 64px);
  max-height: calc(100vh - 64px);
  background: #fff;
  border-right: 1px solid #eee;
  overflow-y: auto;
  overflow-x: hidden;
  transition: margin-left 0.2s ease, width 0.2s ease;
}

.knowledge-library-sidebar.expanded {
  margin-left: 176px;
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

.knowledge-detail-btn .knowledge-detail-btn-arrow {
  margin-left: 4px;
  font-size: 12px;
  vertical-align: middle;
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
  gap: 2px;
}

.knowledge-detail-article-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
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
  font-size: 12px;
  color: #333;
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-detail-article-link:hover {
  color: #BB1919;
}

.knowledge-detail-article-edit-input {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  padding: 2px 6px;
  border: 1px solid #BB1919;
  border-radius: 4px;
  outline: none;
  background: #fff;
}

.knowledge-detail-article-edit-input:focus {
  border-color: #BB1919;
  box-shadow: 0 0 0 1px #BB1919;
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

/* 问答模式：GPT 风格面板 */
.knowledge-qa-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 960px;
  margin: 0 auto;
  width: 100%;
  background: #fff;
  box-shadow: 0 0 1px rgba(0, 0, 0, 0.08);
}

.knowledge-qa-header {
  flex-shrink: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eee;
}

.knowledge-qa-title {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #111;
}

.knowledge-qa-desc {
  margin: 0;
  font-size: 13px;
  color: #666;
}

.knowledge-qa-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.knowledge-qa-welcome {
  padding: 40px 0;
  text-align: center;
  color: #666;
  font-size: 14px;
}

.knowledge-qa-welcome-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.knowledge-qa-msg-wrap {
  display: flex;
  width: 100%;
}
.knowledge-qa-msg-wrap.is-user {
  justify-content: flex-end;
}
.knowledge-qa-msg-wrap.is-assistant {
  justify-content: flex-start;
}

.knowledge-qa-msg {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 85%;
}
.knowledge-qa-msg-wrap.is-user .knowledge-qa-msg {
  flex-direction: row-reverse;
}

.knowledge-qa-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}
.knowledge-qa-avatar-bot {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
.knowledge-qa-avatar-user {
  background: #e8e8e8;
  color: #333;
}

.knowledge-qa-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}
.knowledge-qa-bubble-user {
  background: #BB1919;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.knowledge-qa-bubble-assistant {
  background: #f0f0f0;
  color: #333;
  border-bottom-left-radius: 4px;
}

.knowledge-qa-typing {
  display: flex;
  align-items: center;
  gap: 4px;
}
.knowledge-qa-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
  animation: knowledge-qa-dot 1.4s ease-in-out infinite both;
}
.knowledge-qa-dot:nth-child(2) { animation-delay: 0.2s; }
.knowledge-qa-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes knowledge-qa-dot {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.knowledge-qa-input-row {
  flex-shrink: 0;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px 24px 24px;
  border-top: 1px solid #eee;
  background: #fff;
}

.knowledge-qa-add-kb {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid #ddd;
  border-radius: 10px;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}
.knowledge-qa-add-kb:hover {
  border-color: #BB1919;
  color: #BB1919;
}

.knowledge-qa-add-kb .el-icon {
  font-size: 20px;
}

.knowledge-qa-input {
  flex: 1;
  min-height: 44px;
  max-height: 120px;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  font-family: inherit;
}
.knowledge-qa-input:focus {
  outline: none;
  border-color: #BB1919;
}

.knowledge-qa-send {
  flex-shrink: 0;
  padding: 10px 20px;
  font-size: 14px;
  color: #fff;
  background: #BB1919;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}
.knowledge-qa-send:hover:not(:disabled) {
  background: #9e1515;
}
.knowledge-qa-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 问答模式左侧：返回 + 历史记录 */
.knowledge-qa-back {
  width: 100%;
  justify-content: flex-start;
}

.knowledge-qa-history-item {
  cursor: pointer;
}
.knowledge-qa-history-item:hover {
  background: #f0eef5;
}
.knowledge-qa-history-query {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  max-width: 100%;
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

/* 知识库文件编辑：外围灰底 + 卡片（圆角、阴影），中间正文区白底 */
.knowledge-main-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #e8ecf0;
  padding: 16px 0 24px;
}

/* 整张编辑卡片：工具栏 + 白纸一体，居中，保留卡片样式 */
.knowledge-editor-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  max-width: 880px;
  width: 100%;
  margin: 0 auto;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 8px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.knowledge-editor-toolbar {
  flex-shrink: 0;
  height: 34px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 12px;
  color: #666;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  position: relative;
}

.knowledge-editor-toolbar .knowledge-tool-btn {
  padding: 2px 4px;
  min-width: 28px;
  height: auto;
  color: #666;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: color 0.15s;
}

.knowledge-editor-toolbar .knowledge-tool-btn:hover {
  color: #111 !important;
}

.knowledge-editor-toolbar .knowledge-tool-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.knowledge-editor-toolbar .knowledge-tool-btn .el-icon {
  font-size: 14px;
}

.knowledge-editor-toolbar .knowledge-tool-label {
  font-size: 10px;
  line-height: 1.1;
}

.knowledge-editor-toolbar .knowledge-tool-icon {
  font-size: 12px;
}

.knowledge-tool-icon-strike { text-decoration: line-through; }
.knowledge-tool-icon-hr { font-weight: 600; letter-spacing: 0.02em; }
.knowledge-tool-icon-inline-code { font-size: 10px; }

.knowledge-editor-toolbar :deep(.el-divider--vertical) {
  height: 12px;
  margin: 0 2px;
}

.knowledge-editor-paper {
  flex: 1;
  min-height: 0;
  padding: 24px 32px 40px;
  background: #fff;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.knowledge-editor-paper .vditor-wrap {
  margin-top: 0;
}

.knowledge-editor-paper :deep(.vditor-toolbar) {
  display: none !important;
}

.knowledge-editor-paper :deep(.vditor),
.knowledge-editor-paper :deep(.vditor-content) {
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
  background: #fff !important;
}
.knowledge-editor-paper :deep(.vditor) {
  --textarea-background-color: #fff !important;
}

.knowledge-editor-paper :deep(.vditor-content) {
  background: #fff !important;
  caret-color: #111 !important;
  min-height: 200px !important;
}

/* 编辑区内部（wysiwyg/ir 等）纯白，覆盖 Vditor 默认浅灰 */
.knowledge-editor-paper :deep(.vditor-wysiwyg),
.knowledge-editor-paper :deep(.vditor-ir),
.knowledge-editor-paper :deep(.vditor-sv) {
  background: #fff !important;
}

.knowledge-editor-paper ::selection {
  background: #e0e7eb;
  color: #111;
}

.knowledge-editor-close {
  position: absolute;
  top: 50%;
  right: 8px;
  transform: translateY(-50%);
  z-index: 10;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  font-size: 16px;
  color: #666;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.knowledge-editor-close:hover {
  color: #BB1919;
  background: #e9ecef;
}

.hidden-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
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
  font-size: 12px;
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

.knowledge-search-no-result {
  margin: 0 0 8px;
  padding: 8px 0;
  font-size: 13px;
  color: #999;
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
