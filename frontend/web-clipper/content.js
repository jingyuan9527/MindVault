// content.js — extracts page content and sends to popup
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === 'extract') {
    const title = document.title
    const content = extractMainContent()
    const url = window.location.href
    sendResponse({ title, content, url })
  }
})

function extractMainContent() {
  const article = document.querySelector('article')
  if (article) return article.innerText.slice(0, 50000)

  const main = document.querySelector('main')
  if (main) return main.innerText.slice(0, 50000)

  const body = document.body
  if (body) return body.innerText.slice(0, 50000)

  return ''
}
