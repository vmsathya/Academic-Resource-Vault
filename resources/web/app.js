/**
 * ACADEMIC RESOURCE VAULT - FRONTEND APPLICATION
 * Centralized College Study Portal
 */

// Application State
const state = {
  user: null,
  token: localStorage.getItem('vault_token') || null,
  resources: [],
  subjects: [],
  stats: {},
  filters: {
    q: '',
    semType: 'ALL',
    semNumber: 'ALL',
    subject: 'ALL',
    category: 'ALL',
    bookmarked: false
  },
  viewMode: localStorage.getItem('vault_view') || 'grid',
  theme: localStorage.getItem('vault_theme') || 'dark',
  selectedFile: null
};

// ==========================================================================
// Initialization
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  setupEventListeners();
  checkAuthStatus();
  loadStats();
  loadSubjects();
  loadResources();
});

function initTheme() {
  document.documentElement.setAttribute('data-theme', state.theme);
  const themeBtn = document.getElementById('themeToggleBtn');
  if (themeBtn) {
    themeBtn.addEventListener('click', toggleTheme);
  }
}

function toggleTheme() {
  state.theme = state.theme === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', state.theme);
  localStorage.setItem('vault_theme', state.theme);
  showToast(`Switched to ${state.theme} mode`, 'info');
}

function setupEventListeners() {
  // Search input with debounce
  const searchInput = document.getElementById('searchInput');
  const clearSearchBtn = document.getElementById('clearSearchBtn');
  let debounceTimeout;

  searchInput.addEventListener('input', (e) => {
    clearTimeout(debounceTimeout);
    const val = e.target.value.trim();
    clearSearchBtn.style.display = val ? 'block' : 'none';
    debounceTimeout = setTimeout(() => {
      state.filters.q = val;
      loadResources();
    }, 250);
  });

  // Drag and drop for upload zone
  const dropZone = document.getElementById('fileDropZone');
  if (dropZone) {
    ['dragenter', 'dragover'].forEach(eventName => {
      dropZone.addEventListener(eventName, (e) => {
        e.preventDefault();
        dropZone.style.borderColor = 'var(--primary)';
      });
    });
    ['dragleave', 'drop'].forEach(eventName => {
      dropZone.addEventListener(eventName, (e) => {
        e.preventDefault();
        dropZone.style.borderColor = 'var(--border-color)';
      });
    });
    dropZone.addEventListener('drop', (e) => {
      const files = e.dataTransfer.files;
      if (files && files.length > 0) {
        processSelectedFile(files[0]);
      }
    });
  }
}

// ==========================================================================
// API Helpers
// ==========================================================================

async function apiFetch(endpoint, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };
  if (state.token) {
    headers['Authorization'] = `Bearer ${state.token}`;
  }

  try {
    const res = await fetch(endpoint, { ...options, headers });
    if (res.status === 401 && endpoint !== '/api/auth/login') {
      // Session expired
      state.token = null;
      state.user = null;
      localStorage.removeItem('vault_token');
      updateAuthUI();
    }
    return res;
  } catch (err) {
    console.error(`API Error [${endpoint}]:`, err);
    throw err;
  }
}

// ==========================================================================
// Authentication
// ==========================================================================

async function checkAuthStatus() {
  if (!state.token) {
    updateAuthUI();
    return;
  }
  try {
    const res = await apiFetch('/api/auth/me');
    if (res.ok) {
      const data = await res.json();
      if (data.authenticated && data.user) {
        state.user = data.user;
      } else {
        state.token = null;
        state.user = null;
        localStorage.removeItem('vault_token');
      }
    }
  } catch (e) {
    console.warn('Auth check failed:', e);
  }
  updateAuthUI();
}

function updateAuthUI() {
  const authBox = document.getElementById('authBox');
  const adminActionsGroup = document.getElementById('adminActionsGroup');

  if (state.user) {
    const initials = state.user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
    authBox.innerHTML = `
      <div class="user-profile-badge">
        <div class="user-avatar">${initials}</div>
        <div class="user-meta">
          <span class="user-name">${escapeHtml(state.user.name)}</span>
          <span class="user-role-tag">${state.user.isAdmin ? 'Staff / Admin' : 'Student'}</span>
        </div>
        <button class="btn-logout" onclick="handleLogout()" title="Sign Out">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
            <polyline points="16 17 21 12 16 7"></polyline>
            <line x1="21" y1="12" x2="9" y2="12"></line>
          </svg>
        </button>
      </div>
    `;

    if (state.user.isAdmin) {
      adminActionsGroup.style.display = 'flex';
    } else {
      adminActionsGroup.style.display = 'none';
    }
  } else {
    authBox.innerHTML = `
      <button class="btn btn-primary btn-sm" onclick="openAuthModal('login')">
        Sign In
      </button>
    `;
    adminActionsGroup.style.display = 'none';
  }
}

async function quickLogin(email, password) {
  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      state.token = data.token;
      state.user = data.user;
      localStorage.setItem('vault_token', data.token);
      updateAuthUI();
      loadResources();
      showToast(`Welcome back, ${data.user.name}! (${data.user.role})`, 'success');
    } else {
      showToast(data.message || 'Login failed', 'error');
    }
  } catch (err) {
    showToast('Network error during login', 'error');
  }
}

async function handleLoginSubmit(e) {
  e.preventDefault();
  const email = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value.trim();
  await quickLogin(email, password);
  closeModal('authModal');
}

async function handleRegisterSubmit(e) {
  e.preventDefault();
  const name = document.getElementById('regName').value.trim();
  const email = document.getElementById('regEmail').value.trim();
  const password = document.getElementById('regPassword').value.trim();
  const role = document.getElementById('regRole').value;

  try {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, password, role })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      state.token = data.token;
      state.user = data.user;
      localStorage.setItem('vault_token', data.token);
      updateAuthUI();
      closeModal('authModal');
      loadResources();
      showToast(`Account registered successfully as ${role}!`, 'success');
    } else {
      showToast(data.message || 'Registration failed', 'error');
    }
  } catch (err) {
    showToast('Network error during registration', 'error');
  }
}

async function handleLogout() {
  try {
    await apiFetch('/api/auth/logout', { method: 'POST' });
  } catch (ignored) {}
  state.token = null;
  state.user = null;
  localStorage.removeItem('vault_token');
  updateAuthUI();
  loadResources();
  showToast('Logged out of Academic Resource Vault', 'info');
}

function openAuthModal(tab = 'login') {
  switchAuthTab(tab);
  openModal('authModal');
}

function switchAuthTab(tab) {
  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');
  const tabLogin = document.getElementById('tabLogin');
  const tabRegister = document.getElementById('tabRegister');
  const title = document.getElementById('authModalTitle');

  if (tab === 'login') {
    loginForm.style.display = 'flex';
    registerForm.style.display = 'none';
    tabLogin.classList.add('active');
    tabRegister.classList.remove('active');
    title.innerText = 'Sign In to Vault';
  } else {
    loginForm.style.display = 'none';
    registerForm.style.display = 'flex';
    tabLogin.classList.remove('active');
    tabRegister.classList.add('active');
    title.innerText = 'Create College Account';
  }
}

// ==========================================================================
// Statistics & Data Loading
// ==========================================================================

async function loadStats() {
  try {
    const res = await apiFetch('/api/stats');
    if (res.ok) {
      const stats = await res.json();
      state.stats = stats;
      document.getElementById('statTotalResources').innerText = stats.totalResources || '0';
      document.getElementById('statTotalPYQs').innerText = stats.totalPYQs || '0';
      document.getElementById('statTotalNotes').innerText = stats.totalNotes || '0';
      document.getElementById('statTotalKeys').innerText = stats.totalAnswerKeys || '0';
      document.getElementById('statTotalQBs').innerText = stats.totalQuestionBanks || '0';
      document.getElementById('statTotalDownloads').innerText = stats.totalDownloads || '0';

      // Update category count tabs
      document.getElementById('countAll').innerText = stats.totalResources || '0';
      document.getElementById('countPYQ').innerText = stats.totalPYQs || '0';
      document.getElementById('countNotes').innerText = stats.totalNotes || '0';
      document.getElementById('countQB').innerText = stats.totalQuestionBanks || '0';
      document.getElementById('countKey').innerText = stats.totalAnswerKeys || '0';
    }
  } catch (err) {
    console.error('Failed to load stats:', err);
  }
}

async function loadSubjects() {
  try {
    const res = await apiFetch('/api/subjects');
    if (res.ok) {
      const subjects = await res.json();
      state.subjects = subjects;
      populateSubjectDropdowns(subjects);
      renderSubjectsTable(subjects);
    }
  } catch (err) {
    console.error('Failed to load subjects:', err);
  }
}

function populateSubjectDropdowns(subjects) {
  const filterSelect = document.getElementById('subjectSelect');
  const uploadSelect = document.getElementById('uploadSubject');
  const editSelect = document.getElementById('editSubject');

  let filterOptions = '<option value="ALL">All Subjects</option>';
  let uploadOptions = '';

  subjects.forEach(s => {
    filterOptions += `<option value="${s.subjectCode}">${s.subjectCode} - ${s.subjectName} (Sem ${s.semester})</option>`;
    uploadOptions += `<option value="${s.subjectCode}">${s.subjectCode} - ${s.subjectName} (Sem ${s.semester} ${s.semesterType})</option>`;
  });

  if (filterSelect) filterSelect.innerHTML = filterOptions;
  if (uploadSelect) uploadSelect.innerHTML = uploadOptions;
  if (editSelect) editSelect.innerHTML = uploadOptions;
}

// ==========================================================================
// Resource Loading & Filtering
// ==========================================================================

async function loadResources() {
  const container = document.getElementById('resourcesContainer');
  const tableContainer = document.getElementById('tableContainer');
  const emptyState = document.getElementById('emptyState');
  const resultsCount = document.getElementById('resultsCount');

  // Build query string
  const params = new URLSearchParams();
  if (state.filters.q) params.append('q', state.filters.q);
  if (state.filters.semType !== 'ALL') params.append('semester_type', state.filters.semType);
  if (state.filters.semNumber !== 'ALL') params.append('semester', state.filters.semNumber);
  if (state.filters.subject !== 'ALL') params.append('subject_code', state.filters.subject);
  if (state.filters.category !== 'ALL' && state.filters.category !== 'BOOKMARKED') {
    params.append('type', state.filters.category);
  }
  if (state.filters.bookmarked) {
    params.append('bookmarked', 'true');
  }

  try {
    const res = await apiFetch(`/api/resources?${params.toString()}`);
    if (res.ok) {
      const resources = await res.json();
      state.resources = resources;

      // Update count
      resultsCount.innerText = `${resources.length} resource${resources.length === 1 ? '' : 's'}`;
      updateActiveFilterTag();

      if (resources.length === 0) {
        container.style.display = 'none';
        tableContainer.style.display = 'none';
        emptyState.style.display = 'block';
      } else {
        emptyState.style.display = 'none';
        if (state.viewMode === 'grid') {
          container.style.display = 'grid';
          tableContainer.style.display = 'none';
          renderResourcesGrid(resources);
        } else {
          container.style.display = 'none';
          tableContainer.style.display = 'block';
          renderResourcesTable(resources);
        }
      }
    }
  } catch (err) {
    console.error('Failed to load resources:', err);
    showToast('Failed to load study resources', 'error');
  }
}

function updateActiveFilterTag() {
  const tag = document.getElementById('activeFilterTag');
  const parts = [];
  if (state.filters.semType !== 'ALL') parts.push(state.filters.semType + ' Semesters');
  if (state.filters.semNumber !== 'ALL') parts.push('Sem ' + state.filters.semNumber);
  if (state.filters.subject !== 'ALL') parts.push('Subject: ' + state.filters.subject);
  if (state.filters.category !== 'ALL') parts.push(getCategoryLabel(state.filters.category));
  if (state.filters.bookmarked) parts.push('Bookmarked Only');
  if (state.filters.q) parts.push(`"${state.filters.q}"`);

  if (parts.length > 0) {
    tag.innerText = parts.join(' • ');
    tag.style.display = 'inline-block';
  } else {
    tag.style.display = 'none';
  }
}

function renderResourcesGrid(resources) {
  const container = document.getElementById('resourcesContainer');
  const isAdmin = state.user && state.user.isAdmin;

  container.innerHTML = resources.map(r => {
    const catClass = getCategoryClass(r.resourceType);
    const catLabel = getCategoryLabel(r.resourceType);
    const isBookmarked = r.isBookmarked;

    return `
      <article class="resource-card" data-id="${r.resourceId}">
        <div class="card-top">
          <div class="card-badges">
            <span class="category-tag ${catClass}">${catLabel}</span>
            <span class="subject-code-tag">${escapeHtml(r.subjectCode)}</span>
            <span class="sem-tag">Sem ${r.semester} • ${r.semesterType}</span>
          </div>
          <div class="card-actions-top">
            <button class="btn-bookmark ${isBookmarked ? 'bookmarked' : ''}" 
                    onclick="toggleBookmark(${r.resourceId})" 
                    title="${isBookmarked ? 'Remove Bookmark' : 'Bookmark this resource'}">
              ${isBookmarked ? '★' : '☆'}
            </button>
            ${isAdmin ? `
              <button class="btn-icon-sm" onclick="openEditModal(${r.resourceId})" title="Edit Resource">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
              </button>
              <button class="btn-icon-sm btn-danger" onclick="confirmDeleteResource(${r.resourceId}, '${escapeHtml(r.title)}')" title="Delete Resource">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"></polyline>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                </svg>
              </button>
            ` : ''}
          </div>
        </div>

        <div class="card-subject-name">${escapeHtml(r.subjectName || '')}</div>
        <h3 class="card-title">${escapeHtml(r.title)}</h3>
        <p class="card-desc">${escapeHtml(r.description || 'No description available for this syllabus material.')}</p>

        <div class="card-meta">
          <div class="card-file-info">
            <span class="ext-badge">${escapeHtml(r.fileExtension || 'PDF')}</span>
            <span>${escapeHtml(r.fileSize || '1.2 MB')}</span>
          </div>
          <div>
            <span>By ${escapeHtml(r.uploaderName || 'Faculty')}</span>
            <span> • ⬇ <strong id="dlCount_${r.resourceId}">${r.downloadsCount}</strong></span>
          </div>
        </div>

        <div class="card-buttons">
          <button class="btn btn-secondary btn-sm" onclick="previewResource(${r.resourceId})">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
              <circle cx="12" cy="12" r="3"></circle>
            </svg>
            Preview
          </button>
          <button class="btn btn-primary btn-sm" onclick="downloadResource(${r.resourceId}, '${escapeHtml(r.fileName)}')">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            Download
          </button>
        </div>
      </article>
    `;
  }).join('');
}

function renderResourcesTable(resources) {
  const tbody = document.getElementById('resourcesTableBody');
  const isAdmin = state.user && state.user.isAdmin;

  tbody.innerHTML = resources.map(r => {
    const catClass = getCategoryClass(r.resourceType);
    const catLabel = getCategoryLabel(r.resourceType);

    return `
      <tr>
        <td><span class="category-tag ${catClass}">${catLabel}</span></td>
        <td>
          <span class="subject-code-tag">${escapeHtml(r.subjectCode)}</span>
          <div style="font-size:0.75rem; color:var(--text-muted);">${escapeHtml(r.subjectName || '')}</div>
        </td>
        <td>
          <div class="table-title">${escapeHtml(r.title)}</div>
          <div class="table-desc">${escapeHtml((r.description || '').substring(0, 90))}...</div>
        </td>
        <td>Sem ${r.semester} (${r.semesterType})</td>
        <td>
          <span class="ext-badge">${escapeHtml(r.fileExtension || 'PDF')}</span>
          <span style="font-size:0.75rem;">${escapeHtml(r.fileSize || '')}</span>
        </td>
        <td>${escapeHtml(r.uploaderName || 'Faculty')}</td>
        <td>⬇ <strong id="dlTableCount_${r.resourceId}">${r.downloadsCount}</strong></td>
        <td>
          <div style="display:flex; gap:6px;">
            <button class="btn btn-secondary btn-xs" onclick="previewResource(${r.resourceId})">View</button>
            <button class="btn btn-primary btn-xs" onclick="downloadResource(${r.resourceId}, '${escapeHtml(r.fileName)}')">Download</button>
            ${isAdmin ? `
              <button class="btn-icon-sm" onclick="openEditModal(${r.resourceId})" title="Edit">✎</button>
              <button class="btn-icon-sm btn-danger" onclick="confirmDeleteResource(${r.resourceId}, '${escapeHtml(r.title)}')" title="Delete">🗑</button>
            ` : ''}
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

// ==========================================================================
// Filter Handling Functions
// ==========================================================================

function filterBySemType(type) {
  state.filters.semType = type;
  document.querySelectorAll('#semTypePills .pill').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-type') === type);
  });
  loadResources();
}

function filterBySemNumber(sem) {
  state.filters.semNumber = sem.toString();
  document.querySelectorAll('#semNumberPills .pill').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-sem') === state.filters.semNumber);
  });
  loadResources();
}

function filterBySubject(code) {
  state.filters.subject = code;
  loadResources();
}

function filterByCategory(cat) {
  state.filters.category = cat;
  state.filters.bookmarked = false;
  document.querySelectorAll('#categoryTabs .cat-tab').forEach(tab => {
    tab.classList.toggle('active', tab.getAttribute('data-cat') === cat);
  });
  loadResources();
}

function filterByBookmarked() {
  if (!state.user) {
    showToast('Please sign in to view your bookmarked materials', 'info');
    openAuthModal('login');
    return;
  }
  state.filters.category = 'BOOKMARKED';
  state.filters.bookmarked = true;
  document.querySelectorAll('#categoryTabs .cat-tab').forEach(tab => {
    tab.classList.toggle('active', tab.getAttribute('data-cat') === 'BOOKMARKED');
  });
  loadResources();
}

function clearSearch() {
  const searchInput = document.getElementById('searchInput');
  searchInput.value = '';
  document.getElementById('clearSearchBtn').style.display = 'none';
  state.filters.q = '';
  loadResources();
}

function resetAllFilters() {
  state.filters.q = '';
  state.filters.semType = 'ALL';
  state.filters.semNumber = 'ALL';
  state.filters.subject = 'ALL';
  state.filters.category = 'ALL';
  state.filters.bookmarked = false;

  document.getElementById('searchInput').value = '';
  document.getElementById('clearSearchBtn').style.display = 'none';
  document.getElementById('subjectSelect').value = 'ALL';

  document.querySelectorAll('#semTypePills .pill').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-type') === 'ALL');
  });
  document.querySelectorAll('#semNumberPills .pill').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-sem') === 'ALL');
  });
  document.querySelectorAll('#categoryTabs .cat-tab').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-cat') === 'ALL');
  });

  loadResources();
  showToast('Filters cleared', 'info');
}

function setViewMode(mode) {
  state.viewMode = mode;
  localStorage.setItem('vault_view', mode);
  document.getElementById('gridViewBtn').classList.toggle('active', mode === 'grid');
  document.getElementById('tableViewBtn').classList.toggle('active', mode === 'table');
  loadResources();
}

// ==========================================================================
// Bookmarks, Download, and Document Preview
// ==========================================================================

async function toggleBookmark(resourceId) {
  if (!state.user) {
    showToast('Please log in to save bookmarked resources', 'info');
    openAuthModal('login');
    return;
  }

  try {
    const res = await apiFetch('/api/bookmarks', {
      method: 'POST',
      body: JSON.stringify({ resourceId })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(data.bookmarked ? 'Added to your bookmarked study vault ★' : 'Removed from bookmarks', 'info');
      loadResources();
    }
  } catch (err) {
    showToast('Failed to toggle bookmark', 'error');
  }
}

async function downloadResource(id, filename) {
  try {
    // Initiate download
    window.location.href = `/api/download?id=${id}`;
    showToast(`Downloading "${filename}"...`, 'success');

    // Dynamically increment counter on card and in stats
    const countEl = document.getElementById(`dlCount_${id}`);
    if (countEl) countEl.innerText = parseInt(countEl.innerText || 0) + 1;
    const tableCountEl = document.getElementById(`dlTableCount_${id}`);
    if (tableCountEl) tableCountEl.innerText = parseInt(tableCountEl.innerText || 0) + 1;

    const totalEl = document.getElementById('statTotalDownloads');
    if (totalEl && totalEl.innerText !== '--') {
      totalEl.innerText = parseInt(totalEl.innerText) + 1;
    }
  } catch (err) {
    showToast('Download failed', 'error');
  }
}

async function previewResource(id) {
  openModal('previewModal');
  const contentBox = document.getElementById('previewContent');
  contentBox.innerText = 'Loading verified study material from repository...';

  try {
    const res = await apiFetch(`/api/preview?id=${id}`);
    if (res.ok) {
      const data = await res.json();
      const r = data.resource;

      document.getElementById('previewTitle').innerText = r.title;
      document.getElementById('previewSubjectCode').innerText = r.subjectCode;
      document.getElementById('previewSubjectName').innerText = r.subjectName || '';
      document.getElementById('previewSemester').innerText = `Semester ${r.semester} (${r.semesterType})`;
      document.getElementById('previewUploader').innerText = `Uploaded by ${r.uploaderName || 'Faculty'}`;
      document.getElementById('previewFileSize').innerText = `File: ${r.fileName} • ${r.fileSize || '1.4 MB'}`;

      const badge = document.getElementById('previewTypeBadge');
      badge.className = `badge category-tag ${getCategoryClass(r.resourceType)}`;
      badge.innerText = getCategoryLabel(r.resourceType);

      contentBox.innerText = data.content;

      // Download button in preview modal
      const dlBtn = document.getElementById('previewDownloadBtn');
      dlBtn.onclick = () => downloadResource(r.resourceId, r.fileName);
    } else {
      contentBox.innerText = 'Unable to preview this document.';
    }
  } catch (err) {
    contentBox.innerText = 'Error loading document content.';
  }
}

function copyPreviewText() {
  const content = document.getElementById('previewContent').innerText;
  navigator.clipboard.writeText(content).then(() => {
    showToast('Document text copied to clipboard!', 'success');
  }).catch(() => {
    showToast('Failed to copy text', 'error');
  });
}

// ==========================================================================
// Staff / Admin Management Functions
// ==========================================================================

function openUploadModal() {
  if (!state.user || !state.user.isAdmin) {
    showToast('Staff / Admin login required to upload materials', 'error');
    openAuthModal('login');
    return;
  }
  document.getElementById('uploadForm').reset();
  clearSelectedFile();
  openModal('uploadModal');
}

function handleFileSelect(e) {
  const file = e.target.files[0];
  if (file) {
    processSelectedFile(file);
  }
}

function processSelectedFile(file) {
  state.selectedFile = file;
  document.getElementById('selectedFileName').innerText = `${file.name} (${(file.size / (1024 * 1024)).toFixed(2)} MB)`;
  document.getElementById('fileSelectedDisplay').style.display = 'inline-flex';

  // If title is empty, suggest file name
  const titleInput = document.getElementById('uploadTitle');
  if (!titleInput.value) {
    titleInput.value = file.name.replace(/\.[^/.]+$/, '').replace(/[_-]/g, ' ');
  }
}

function clearSelectedFile(e) {
  if (e) e.stopPropagation();
  state.selectedFile = null;
  document.getElementById('uploadFileInput').value = '';
  document.getElementById('fileSelectedDisplay').style.display = 'none';
}

function fillUploadTemplate(type) {
  const titleInput = document.getElementById('uploadTitle');
  const descInput = document.getElementById('uploadDescription');
  const typeSelect = document.getElementById('uploadType');

  if (type === 'pyq') {
    typeSelect.value = 'PYQ';
    titleInput.value = 'End-Semester University Examination Paper (Nov/Dec 2025)';
    descInput.value = 'Official semester question paper. Contains Part A (10 short questions, 20 marks) and Part B (5 comprehensive problems, 80 marks). Verified with Bloom\'s taxonomy alignment.';
  } else if (type === 'notes') {
    typeSelect.value = 'NOTES';
    titleInput.value = 'Comprehensive Lecture Notes: Modules 1 to 3 Complete';
    descInput.value = 'Professor classroom lecture notes with step-by-step mathematical proofs, architectural diagrams, pseudocode algorithms, and syllabus review summaries.';
  } else if (type === 'key') {
    typeSelect.value = 'ANSWER_KEY';
    titleInput.value = 'Model Answer Key & Detailed Marking Rubric (Nov/Dec 2025)';
    descInput.value = 'Official evaluation guide with worked step-by-step solutions, mark distribution per step, code implementations, and standard answers for all Part A & Part B questions.';
  }
}

async function handleUploadSubmit(e) {
  e.preventDefault();
  const submitBtn = document.getElementById('uploadSubmitBtn');
  submitBtn.disabled = true;
  submitBtn.innerText = 'Publishing...';

  const subjectCode = document.getElementById('uploadSubject').value;
  const resourceType = document.getElementById('uploadType').value;
  const title = document.getElementById('uploadTitle').value.trim();
  const description = document.getElementById('uploadDescription').value.trim();

  let fileBase64 = null;
  let fileName = `${subjectCode}_${resourceType}.pdf`;

  if (state.selectedFile) {
    fileName = state.selectedFile.name;
    fileBase64 = await toBase64(state.selectedFile);
  }

  const payload = {
    subjectCode,
    resourceType,
    title,
    description,
    fileName,
    fileBase64
  };

  try {
    const res = await apiFetch('/api/resources', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast('Academic material published to vault successfully!', 'success');
      closeModal('uploadModal');
      loadResources();
      loadStats();
    } else {
      showToast(data.message || 'Failed to publish material', 'error');
    }
  } catch (err) {
    showToast('Network error during upload', 'error');
  } finally {
    submitBtn.disabled = false;
    submitBtn.innerHTML = `
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="20 6 9 17 4 12"></polyline>
      </svg>
      Publish to Vault
    `;
  }
}

// Edit Resource
function openEditModal(resourceId) {
  const r = state.resources.find(res => res.resourceId === resourceId);
  if (!r) return;

  document.getElementById('editResourceId').value = r.resourceId;
  document.getElementById('editSubject').value = r.subjectCode;
  document.getElementById('editType').value = r.resourceType;
  document.getElementById('editTitle').value = r.title;
  document.getElementById('editDescription').value = r.description || '';

  openModal('editModal');
}

async function handleEditSubmit(e) {
  e.preventDefault();
  const id = document.getElementById('editResourceId').value;
  const subjectCode = document.getElementById('editSubject').value;
  const resourceType = document.getElementById('editType').value;
  const title = document.getElementById('editTitle').value.trim();
  const description = document.getElementById('editDescription').value.trim();

  try {
    const res = await apiFetch(`/api/resources/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ subjectCode, resourceType, title, description })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast('Study material updated successfully', 'success');
      closeModal('editModal');
      loadResources();
    } else {
      showToast('Failed to update material', 'error');
    }
  } catch (err) {
    showToast('Network error', 'error');
  }
}

// Delete Resource
async function confirmDeleteResource(id, title) {
  if (confirm(`Are you sure you want to delete "${title}"? This will remove the file from the portal.`)) {
    try {
      const res = await apiFetch(`/api/resources/${id}`, { method: 'DELETE' });
      const data = await res.json();
      if (res.ok && data.success) {
        showToast('Resource deleted from vault', 'info');
        loadResources();
        loadStats();
      } else {
        showToast('Failed to delete resource', 'error');
      }
    } catch (err) {
      showToast('Error deleting resource', 'error');
    }
  }
}

// ==========================================================================
// Subject Management Modal
// ==========================================================================

function openSubjectModal() {
  if (!state.user || !state.user.isAdmin) {
    showToast('Admin privilege required', 'error');
    return;
  }
  openModal('subjectModal');
}

function renderSubjectsTable(subjects) {
  const tbody = document.getElementById('subjectsTableBody');
  if (!tbody) return;

  tbody.innerHTML = subjects.map(s => `
    <tr>
      <td><span class="subject-code-tag">${escapeHtml(s.subjectCode)}</span></td>
      <td><strong>${escapeHtml(s.subjectName)}</strong></td>
      <td>Sem ${s.semester}</td>
      <td><span class="sem-tag">${s.semesterType}</span></td>
      <td>${s.resourceCount || 0} files</td>
      <td>
        <button class="btn-icon-sm btn-danger" onclick="deleteSubject('${s.subjectCode}')" title="Delete subject">✕</button>
      </td>
    </tr>
  `).join('');
}

async function handleAddSubject(e) {
  e.preventDefault();
  const code = document.getElementById('newSubCode').value.trim().toUpperCase();
  const name = document.getElementById('newSubName').value.trim();
  const sem = parseInt(document.getElementById('newSubSem').value);

  try {
    const res = await apiFetch('/api/subjects', {
      method: 'POST',
      body: JSON.stringify({
        subjectCode: code,
        subjectName: name,
        semester: sem,
        credits: 4,
        department: 'Computer Science'
      })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast(`Subject ${code} added successfully!`, 'success');
      document.getElementById('newSubjectForm').reset();
      loadSubjects();
    } else {
      showToast(data.message || 'Failed to add subject', 'error');
    }
  } catch (err) {
    showToast('Network error adding subject', 'error');
  }
}

async function deleteSubject(code) {
  if (confirm(`Delete subject ${code} and its mapped materials?`)) {
    try {
      const res = await apiFetch(`/api/subjects?code=${code}`, { method: 'DELETE' });
      const data = await res.json();
      if (res.ok && data.success) {
        showToast(`Subject ${code} deleted`, 'info');
        loadSubjects();
        loadResources();
        loadStats();
      } else {
        showToast('Failed to delete subject', 'error');
      }
    } catch (err) {
      showToast('Network error', 'error');
    }
  }
}

// ==========================================================================
// Modal & Utility Helpers
// ==========================================================================

function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) {
    modal.style.display = 'flex';
  }
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) {
    modal.style.display = 'none';
  }
}

// Close modals when clicking backdrop
window.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-backdrop')) {
    e.target.style.display = 'none';
  }
});

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ';
  toast.innerHTML = `<span><strong>${icon}</strong> ${escapeHtml(message)}</span>`;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    setTimeout(() => toast.remove(), 200);
  }, 3500);
}

function getCategoryClass(type) {
  switch (type) {
    case 'PYQ': return 'pyq';
    case 'NOTES': return 'notes';
    case 'QUESTION_BANK': return 'qb';
    case 'ANSWER_KEY': return 'key';
    default: return 'notes';
  }
}

function getCategoryLabel(type) {
  switch (type) {
    case 'PYQ': return '📄 PYQ (Paper)';
    case 'NOTES': return '📚 Lecture Notes';
    case 'QUESTION_BANK': return '📋 Question Bank';
    case 'ANSWER_KEY': return '🔑 Answer Key';
    default: return type;
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return str.toString()
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function toBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
  });
}
