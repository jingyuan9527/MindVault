// popup.js
let tags = []

document.addEventListener('DOMContentLoaded', async () => {
  chrome.storage.sync.get(['serverUrl', 'apiToken'], (prefs) => {
    if (prefs.serverUrl) document.getElementById('serverUrl').value = prefs.serverUrl
    if (prefs.apiToken) document.getElementById('apiToken').value = prefs.apiToken
  })

  chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
    chrome.tabs.sendMessage(tabs[0].id, { action: 'extract' }, (resp) => {
      if (resp) {
        document.getElementById('title').value = resp.title
        document.getElementById('content').value = resp.content
        document.getElementById('url').value = resp.url
      }
    })
  })

  const tagInput = document.getElementById('tagInput')
  tagInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      const val = tagInput.value.trim()
      if (val && !tags.includes(val)) {
        tags.push(val)
        renderTags()
      }
      tagInput.value = ''
    }
    if (e.key === 'Backspace' && !tagInput.value && tags.length) {
      tags.pop()
      renderTags()
    }
  })

  document.getElementById('saveBtn').addEventListener('click', saveToMindVault)

  document.getElementById('optionsLink').addEventListener('click', (e) => {
    e.preventDefault()
    chrome.runtime.openOptionsPage()
  })
})

function renderTags() {
  const list = document.getElementById('tagList')
  list.innerHTML = tags.map((t, i) =>
    `<span class="tag">${t}<button data-idx="${i}">&times;</button></span>`
  ).join('')
  list.querySelectorAll('button').forEach(btn => {
    btn.addEventListener('click', () => {
      tags.splice(parseInt(btn.dataset.idx), 1)
      renderTags()
    })
  })
}

async function saveToMindVault() {
  const title = document.getElementById('title').value.trim()
  const content = document.getElementById('content').value.trim()
  const url = document.getElementById('url').value
  const serverUrl = document.getElementById('serverUrl').value.trim().replace(/\/+$/, '')
  const apiToken = document.getElementById('apiToken').value.trim()
  const statusEl = document.getElementById('status')

  if (!title || !content) {
    showStatus('请填写标题和内容', 'error')
    return
  }
  if (!serverUrl) {
    showStatus('请填写服务器地址', 'error')
    return
  }
  if (!apiToken) {
    showStatus('请填写 API Token', 'error')
    return
  }

  document.getElementById('saveBtn').disabled = true
  document.getElementById('saveBtn').textContent = '保存中...'

  chrome.storage.sync.set({ serverUrl, apiToken })

  try {
    const res = await fetch(`${serverUrl}/api/v1/knowledge`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${apiToken}`
      },
      body: JSON.stringify({
        title,
        content,
        tags: JSON.stringify(tags),
        sourceUrl: url
      })
    })
    const data = await res.json()
    if (res.ok && data.code === 0) {
      showStatus('保存成功！', 'success')
      tags = []
      renderTags()
      document.getElementById('title').value = ''
      document.getElementById('content').value = ''
    } else {
      showStatus('保存失败: ' + (data.message || res.statusText), 'error')
    }
  } catch (err) {
    showStatus('网络错误: ' + err.message, 'error')
  } finally {
    document.getElementById('saveBtn').disabled = false
    document.getElementById('saveBtn').textContent = '保存到 MindVault'
  }
}

function showStatus(msg, type) {
  const el = document.getElementById('status')
  el.textContent = msg
  el.className = 'status ' + type
  el.style.display = 'block'
}
