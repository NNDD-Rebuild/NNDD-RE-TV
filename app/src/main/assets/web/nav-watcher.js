// TV側の操作でブラウザの画面遷移を促すためのポーリング。全ページ共通で読み込む。
(function () {
  async function pollNavigate() {
    try {
      const res = await fetch('/api/ui/navigate-target', { credentials: 'same-origin' });
      const data = await res.json();
      if (data.target && data.target !== location.pathname) {
        location.href = data.target;
      }
    } catch (e) {
      // 通信失敗は無視して次回ポーリングに任せる
    }
  }
  setInterval(pollNavigate, 3000);
})();
