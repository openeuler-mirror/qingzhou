(function() {
// 已经存在，不需要重复创建
if (typeof window.ChatSidebar !== 'undefined') {
  return;
}

class ChatSidebar {
    // 统一中文常量管理（仅 UI 交互文本，Mock 内容保持原样）
    static LANG = {
        waitGenerate: '请等待当前生成完成',
        defaultTitle: 'AI 助手',
        alreadyNewSession: '当前已是空会话',
        newSessionCreated: '已清空会话',
        requestFailed: '请求失败: ',
        runError: '运行出错',
        welcomeMain: '输入消息开始对话',
        newSessionBtn: '清空会话',
        closeBtn: '关闭',
        inputPlaceholder: '输入消息，Enter 发送',
        stopBtn: '停止',
        sendBtn: '发送',
        thoughtFor: '思考了 ',
        thinking: '思考中',
        thoughtProcess: '思考过程',
        roundThink: '第 {n} 轮思考',
        deleteMsg: '删除',
        copyMsg: '复制',
        copied: '已复制',
    };

    constructor(options = {}) {
        this._opts = {
            baseUrl: options.baseUrl || '',
            endpoint: options.endpoint || '/chat',
            headers: options.headers || {},
            timeout: options.timeout || 12e4,
            prefix: options.storagePrefix || 'tongweb-chat',
            width: typeof options.width === 'number' ? options.width + 'px' : (options.width || '460px'),
            minW: options.minWidth || 320,
            maxW: options.maxWidth || 800,
        };
        this._lang = { ...ChatSidebar.LANG, ...options.lang };
        console.log(this._opts.endpoint);
        this._convs = {};
        this._activeCtx = null;
        this._generating = false;
        this._runId = null;
        this._abortCtrl = null;
        this._listeners = {};
        this._mt = [];
        this._aiMid = null;
        this._segments = [];
        this._curR = null;
        this._curT = null;
        this._rRound = 0;
        this._dragging = false;
        this._injectStyles();
        this._createDOM();
        this._bindEvents();
        this._loadConv();
    }

    /* ---------- 公开 API ---------- */
    open(ctx, title) {
        if (this._generating) return this._toast(this._lang.waitGenerate, 'error');
        this._activeCtx = ctx;
        this._el.title.textContent = title || this._lang.defaultTitle;
        this._el.root.classList.add('chat-sb-open');
        this._el.overlay.classList.add('chat-sb-ov-open');
        this._renderMsgs();
        setTimeout(() => this._el.input.focus(), 300);
        this._emit('open', ctx);
    }
    close() {
        if (this._generating) this._cancelRun();
        this._el.root.classList.remove('chat-sb-open');
        this._el.overlay.classList.remove('chat-sb-ov-open');
        const ctx = this._activeCtx; this._activeCtx = null;
        this._emit('close', ctx);
    }
    get isOpen() { return this._el.root.classList.contains('chat-sb-open'); }
    get activeContextId() { return this._activeCtx; }
    getMessageCount(ctx) { return (this._convs[ctx]?.messages.filter(m => m.role === 'user').length) || 0; }
    hasMessages(ctx) { return (this._convs[ctx]?.messages.length ?? 0) > 0; }
    newSession() {
        if (this._activeCtx == null || this._generating) return this._toast(this._generating ? this._lang.waitGenerate : '', 'error');
        const conv = this._getConv(this._activeCtx);
        if (!conv.messages.length) return this._toast(this._lang.alreadyNewSession);
        conv.messages = []; conv.threadId = this._uid();
        this._saveConv(); this._renderMsgs(); this._el.input.focus();
        this._toast(this._lang.newSessionCreated, 'success');
        this._emit('message-count-change', this._activeCtx, 0);
    }
    on(e, fn) { (this._listeners[e] ||= []).push(fn); }
    off(e, fn) { this._listeners[e] &&= this._listeners[e].filter(f => f !== fn); }
    destroy() {
        this.close(); this._clearMt();
        [this._el.root, this._el.overlay, this._el.style, this._el.toastBox].forEach(el => el.remove());
        this._convs = {}; this._listeners = {};
    }

    /* ---------- 样式 ---------- */
    _injectStyles() {
        const s = document.createElement('style');
        s.id = 'chat-sidebar-styles';
        let css = '';
        css += '.chat-sb-ov{position:fixed;inset:0;background:rgba(0,0,0,.3);z-index:10040;opacity:0;pointer-events:none;transition:opacity .25s}';
        css += '.chat-sb-ov.chat-sb-ov-open{opacity:1;pointer-events:auto}';
        css += '.chat-sb{position:fixed;top:0;right:0;bottom:0;width:' + this._opts.width + ';max-width:100vw;background:#fff;border-left:1px solid #e8e8e8;z-index:10050;display:flex;flex-direction:column;transform:translateX(100%);transition:transform .28s cubic-bezier(.4,0,.2,1);box-shadow:-4px 0 24px rgba(0,0,0,.08);font-family:system-ui,-apple-system,sans-serif;color:#1a1a1a}';
        css += '.chat-sb.chat-sb-open{transform:translateX(0)}';
        css += '.chat-sb-resizer{position:absolute;left:0;top:0;bottom:0;width:6px;cursor:col-resize;z-index:10060;touch-action:none;background:transparent;transition:background .15s}';
        css += '.chat-sb-resizer:hover,.chat-sb-resizer.dragging{background:rgba(0,0,0,.06)}';
        css += '.chat-sb-resizer::after{content:"";position:absolute;left:2px;top:50%;transform:translateY(-50%);width:2px;height:32px;border-radius:1px;background:#ccc;opacity:0;transition:opacity .15s}';
        css += '.chat-sb-resizer:hover::after,.chat-sb-resizer.dragging::after{opacity:1}';
        css += 'body.chat-sb-dragging,body.chat-sb-dragging *{cursor:col-resize!important;user-select:none!important}';
        css += '.chat-sb-hd{height:52px;min-height:52px;display:flex;align-items:center;padding:0 16px;border-bottom:1px solid #ebebeb;background:#fafafa;gap:8px}';
        css += '.chat-sb-hd .chat-sb-logo{width:28px;height:28px;min-width:28px;border-radius:6px;background:rgba(0,180,120,.08);display:flex;align-items:center;justify-content:center}';
        css += '.chat-sb-hd .chat-sb-logo svg{width:14px;height:14px;stroke:#00b878;fill:none;stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round}';
        css += '.chat-sb-hd .chat-sb-t{flex:1;min-width:0;font-size:13px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:#1a1a1a}';
        css += '.chat-sb-btn{height:30px;padding:0 10px;border:1px solid #e0e0e0;border-radius:6px;background:#fff;color:#555;font-size:11px;cursor:pointer;transition:all .15s;font-family:inherit;white-space:nowrap;display:flex;align-items:center;gap:4px;flex-shrink:0}';
        css += '.chat-sb-btn:hover{border-color:#ccc;background:#f5f5f5;color:#333}';
        css += '.chat-sb-btn svg{width:11px;height:11px;stroke:currentColor;fill:none;stroke-width:2;stroke-linecap:round}';
        css += '.chat-sb-ibtn{width:30px;height:30px;border:none;border-radius:6px;cursor:pointer;display:flex;align-items:center;justify-content:center;background:transparent;color:#999;transition:all .15s;flex-shrink:0}';
        css += '.chat-sb-ibtn:hover{background:#f0f0f0;color:#666}';
        css += '.chat-sb-ibtn svg{width:14px;height:14px;stroke:currentColor;fill:none;stroke-width:2;stroke-linecap:round}';
        css += '.chat-sb-msgs{flex:1;overflow-y:auto;background:#fff}';
        css += '.chat-sb-welcome{height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:5px;padding:20px}';
        css += '.chat-sb-welcome .chat-sb-wi{width:40px;height:40px;border-radius:6px;background:rgba(0,180,120,.08);display:flex;align-items:center;justify-content:center;margin-bottom:6px}';
        css += '.chat-sb-welcome .chat-sb-wi svg{width:18px;height:18px;stroke:#00b878;fill:none;stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round}';
        css += '.chat-sb-welcome p{font-size:12px;color:#aaa;text-align:center}';
        css += '.chat-sb-ml{padding:16px 14px;display:flex;flex-direction:column;gap:14px}';
        css += '.chat-sb-msg{animation:chat-sb-fu .3s ease-out}';
        css += '.chat-sb-mu{display:flex;flex-direction:column;align-items:flex-end;gap:3px}';
        css += '.chat-sb-mu .chat-sb-bbl{max-width:82%;display:inline-block;padding:8px 12px;font-size:13px;line-height:1.5;color:#1a1a1a;word-break:break-word;background:#f0faf6;border:1px solid rgba(0,180,120,.15);border-radius:10px;border-bottom-right-radius:4px}';
        css += '.chat-sb-mu .chat-sb-time{font-size:10px;color:#bbb}';
        css += '.chat-sb-ma{display:flex;gap:8px}';
        css += '.chat-sb-ma .chat-sb-av{width:24px;height:24px;min-width:24px;border-radius:6px;background:rgba(0,180,120,.08);display:flex;align-items:center;justify-content:center;margin-top:1px}';
        css += '.chat-sb-ma .chat-sb-av svg{width:11px;height:11px;stroke:#00b878;fill:none;stroke-width:2.5;stroke-linecap:round;stroke-linejoin:round}';
        css += '.chat-sb-ma .chat-sb-body{flex:1;min-width:0}';
        css += '.chat-sb-ma .chat-sb-time{font-size:10px;color:#bbb;margin-top:3px}';
        css += '.chat-sb-err{font-size:12px;color:#e53935;margin-top:4px;padding:5px 8px;background:#fff5f5;border:1px solid rgba(229,57,53,.12);border-radius:6px}';
        css += '.chat-sb-seg{position:relative}';
        css += '.chat-sb-seg+.chat-sb-seg{margin-top:10px}';
        css += '.chat-sb-seg+.chat-sb-seg::before{content:"";position:absolute;top:-5px;left:0;right:0;height:1px;background:linear-gradient(90deg,transparent,#e8e8e8 20%,#e8e8e8 80%,transparent)}';
        css += '.chat-sb-reason{margin-bottom:0;border:1px solid #e8e8e8;border-radius:8px;overflow:hidden;background:#fafafa;transition:border-color .2s,background .2s}';
        css += '.chat-sb-reason:hover{border-color:#ddd}';
        css += '.chat-sb-reason-hd{display:flex;align-items:center;gap:6px;padding:7px 10px;cursor:pointer;user-select:none;font-size:11px;color:#888;transition:background .15s}';
        css += '.chat-sb-reason-hd:hover{background:#f0f0f0}';
        css += '.chat-sb-reason-hd .chat-sb-ri{flex-shrink:0;color:#999}';
        css += '.chat-sb-reason-hd .chat-sb-ri svg{width:13px;height:13px}';
        css += '.chat-sb-reason-hd .chat-sb-rl{font-weight:500;color:#777}';
        css += '.chat-sb-reason-hd .chat-sb-rd{margin-left:auto;color:#bbb;font-size:10px;white-space:nowrap}';
        css += '.chat-sb-reason-hd .chat-sb-rarr{flex-shrink:0;transition:transform .2s;color:#bbb}';
        css += '.chat-sb-reason-hd .chat-sb-rarr svg{width:10px;height:10px}';
        css += '.chat-sb-reason.open .chat-sb-rarr{transform:rotate(180deg)}';
        css += '.chat-sb-reason-bd{padding:8px 10px;font-size:13px;line-height:1.7;color:#999;display:none;word-break:break-word}';
        css += '.chat-sb-reason.open .chat-sb-reason-bd{display:block}';
        css += '.chat-sb-reason.chat-sb-reasoning .chat-sb-reason-hd{color:#666}';
        css += '.chat-sb-reason.chat-sb-reasoning .chat-sb-rl{color:#00b878}';
        css += '.chat-sb-reason.chat-sb-reasoning{border-color:rgba(0,180,120,.2);background:rgba(0,180,120,.02)}';
        css += '.chat-sb-reason.chat-sb-reasoning .chat-sb-reason-bd{display:block;color:#888}';
        css += '.chat-sb-think-dots{display:inline-flex;gap:3px;vertical-align:middle;margin-left:4px}';
        css += '.chat-sb-think-dots span,.chat-sb-dots span{width:3px;height:3px;border-radius:50%;background:#00b878;animation:chat-sb-pulse 1.2s ease-in-out infinite}';
        css += '.chat-sb-think-dots span:nth-child(2),.chat-sb-dots span:nth-child(2){animation-delay:.15s}';
        css += '.chat-sb-think-dots span:nth-child(3),.chat-sb-dots span:nth-child(3){animation-delay:.3s}';
        css += '.chat-sb-dots{display:inline-flex;gap:4px;padding:3px 0}';
        css += '.chat-sb-dots span{width:4px;height:4px}';
        css += '.chat-sb-dots span:nth-child(2){animation-delay:.2s}';
        css += '.chat-sb-dots span:nth-child(3){animation-delay:.4s}';
        css += '.chat-sb-reason-round{display:inline-flex;align-items:center;justify-content:center;width:16px;height:16px;border-radius:50%;background:rgba(0,180,120,.1);color:#00b878;font-size:9px;font-weight:600;margin-left:2px}';
        css += '.chat-sb-mc{font-size:13px;line-height:1.7;color:#444}';
        css += '.chat-sb-mc h1,.chat-sb-mc h2,.chat-sb-mc h3{margin:.6em 0 .3em;font-weight:700;color:#1a1a1a}';
        css += '.chat-sb-mc h1{font-size:1.25em}';
        css += '.chat-sb-mc h2{font-size:1.12em}';
        css += '.chat-sb-mc h3{font-size:1.02em}';
        css += '.chat-sb-mc p{margin:.3em 0}';
        css += '.chat-sb-mc ul,.chat-sb-mc ol{margin:.3em 0;padding-left:1.3em}';
        css += '.chat-sb-mc li{margin:.1em 0;color:#444}';
        css += '.chat-sb-mc a{color:#00b878;text-decoration:underline}';
        css += '.chat-sb-mc blockquote{border-left:2px solid #00b878;padding:.3em .7em;margin:.3em 0;background:#f7fdf9;border-radius:0 6px 6px 0}';
        css += '.chat-sb-mc code:not(pre code){background:rgba(0,180,120,.08);padding:1px 4px;border-radius:3px;font-family:"SF Mono",Consolas,"Courier New",monospace;font-size:.87em;color:#00875a}';
        css += '.chat-sb-mc pre{position:relative;margin:.5em 0;border-radius:6px;overflow:hidden;border:1px solid #e0e8e0}';
        css += '.chat-sb-mc pre code{display:block;padding:.7em .9em;font-family:"SF Mono",Consolas,"Courier New",monospace;font-size:.8em;line-height:1.55;background:#1e1e1e;color:#d4d4d4}';
        css += '.chat-sb-mc table{border-collapse:collapse;margin:.3em 0;width:100%}';
        css += '.chat-sb-mc th,.chat-sb-mc td{border:1px solid #e0e0e0;padding:3px 7px;text-align:left;font-size:11px}';
        css += '.chat-sb-mc th{background:#f5f5f5;font-weight:600;color:#333}';
        css += '.chat-sb-mc hr{border:none;border-top:1px solid #e8e8e8;margin:.6em 0}';
        css += '.chat-sb-mc strong{color:#1a1a1a;font-weight:600}';
        css += '.chat-sb-chd{display:flex;justify-content:space-between;align-items:center;padding:3px 10px;background:#282828;border-bottom:1px solid #333;font-size:10px;color:#999;font-family:"SF Mono",Consolas,"Courier New",monospace}';
        css += '.chat-sb-cpb{background:none;border:none;color:#999;cursor:pointer;font-size:10px;padding:2px 5px;border-radius:3px;font-family:inherit}';
        css += '.chat-sb-cpb:hover{color:#fff;background:rgba(255,255,255,.1)}';
        css += '.chat-sb-del-btn{position:static;display:inline-flex;align-items:center;justify-content:center;width:16px;height:16px;border-radius:3px;border:1px solid #e8e8e8;background:#fff;color:#999;cursor:pointer;opacity:1;transition:all .15s;vertical-align:middle;margin-right:6px}';
        css += '.chat-sb-del-btn:hover{border-color:#e53935;background:#fff5f5;color:#e53935}';
        css += '.chat-sb-del-btn svg{width:10px;height:10px;stroke:currentColor;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}';
        css += '.chat-sb-footer-btns{display:flex;justify-content:flex-start;gap:6px;margin-top:6px}';
        css += '.chat-sb-footer-btn{width:22px;height:22px;display:flex;align-items:center;justify-content:center;border-radius:4px;border:1px solid #e8e8e8;background:#fff;color:#888;cursor:pointer;transition:all .15s}';
        css += '.chat-sb-footer-btn:hover{border-color:#ccc}';
        css += '.chat-sb-footer-btn.delete:hover{border-color:#e53935;background:#fff5f5;color:#e53935}';
        css += '.chat-sb-footer-btn.copy:hover{border-color:#00b878;background:#f0faf6;color:#00b878}';
        css += '.chat-sb-footer-btn svg{width:14px;height:14px;stroke:currentColor;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}';
        css += '.chat-sb-msg{position:relative;animation:chat-sb-fu .3s ease-out}';
        css += '.chat-sb-input{padding:10px 14px;border-top:1px solid #ebebeb;background:#fafafa}';
        css += '.chat-sb-iwrap{position:relative;display:flex;align-items:flex-end;background:#fff;border:1px solid #ddd;border-radius:6px;transition:border-color .2s,box-shadow .2s;box-shadow:0 1px 3px rgba(0,0,0,.04)}';
        css += '.chat-sb-iwrap:focus-within{border-color:#00b878;box-shadow:0 0 0 2px rgba(0,180,120,.1)}';
        css += '.chat-sb-iwrap textarea{flex:1;background:transparent;border:none;outline:none;padding:8px 12px;font-size:13px;color:#1a1a1a;font-family:inherit;resize:none;max-height:140px;line-height:1.5}';
        css += '.chat-sb-iwrap textarea::placeholder{color:#bbb}';
        css += '.chat-sb-ibtns{display:flex;align-items:center;gap:3px;padding:5px}';
        css += '.chat-sb-ibtn.sm{width:26px;height:26px}';
        css += '.chat-sb-ibtn.sm svg{width:12px;height:12px}';
        css += '.chat-sb-ibtn.send{background:rgba(0,180,120,.08);color:#00b878}';
        css += '.chat-sb-ibtn.send:hover{background:rgba(0,180,120,.15)}';
        css += '.chat-sb-ibtn.send:disabled{opacity:.25;cursor:not-allowed}';
        css += '.chat-sb-ibtn.stop{background:#fff5f5;color:#e53935}';
        css += '.chat-sb-ibtn.stop:hover{background:#ffeaea}';
        css += '.chat-sb-spin{animation:chat-sb-sp .6s linear infinite}';
        css += '.chat-sb-toast{position:fixed;top:12px;right:12px;z-index:10100;display:flex;flex-direction:column;gap:6px;pointer-events:none}';
        css += '.chat-sb-toast-item{pointer-events:auto;padding:7px 12px;border-radius:6px;border:1px solid #e0e0e0;font-size:12px;background:#fff;box-shadow:0 4px 16px rgba(0,0,0,.1);animation:chat-sb-fu .25s ease-out;color:#333}';
        css += '.chat-sb-toast-item.err{border-color:rgba(229,57,53,.2);color:#e53935}';
        css += '.chat-sb-toast-item.ok{border-color:rgba(0,180,120,.2);color:#00875a}';
        css += '@keyframes chat-sb-fu{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:translateY(0)}}';
        css += '@keyframes chat-sb-pulse{0%,100%{opacity:.3}50%{opacity:1}}';
        css += '@keyframes chat-sb-sp{to{transform:rotate(360deg)}}';
        css += '@media(max-width:600px){.chat-sb{width:100vw!important}.chat-sb-resizer{display:none}}';
        css += '@media(prefers-reduced-motion:reduce){.chat-sb-msg,.chat-sb-toast-item{animation:none}.chat-sb-dots span,.chat-sb-think-dots span{animation:none;opacity:.5}.chat-sb-spin{animation:none}.chat-sb,.chat-sb-ov{transition:none}}';
        css += '/* 深色模式适配 */';
        css += 'html.dark .chat-sb-ov{background:rgba(0,0,0,.5)}';
        css += 'html.dark .chat-sb{background:#141414;border-left-color:#3a3a3a;color:#e5e5e5}';
        css += 'html.dark .chat-sb-hd{border-bottom-color:#3a3a3a;background:#1e1e1e}';
        css += 'html.dark .chat-sb-hd .chat-sb-t{color:#e5e5e5}';
        css += 'html.dark .chat-sb-btn{border-color:#3a3a3a;background:transparent;color:#aaa}';
        css += 'html.dark .chat-sb-btn:hover{border-color:#555;background:#2a2a2a;color:#ddd}';
        css += 'html.dark .chat-sb-ibtn{color:#777}';
        css += 'html.dark .chat-sb-ibtn:hover{background:#2a2a2a;color:#aaa}';
        css += 'html.dark .chat-sb-msgs{background:#141414}';
        css += 'html.dark .chat-sb-welcome p{color:#777}';
        css += 'html.dark .chat-sb-mu .chat-sb-bbl{color:#e5e5e5;background:rgba(0,180,120,.15);border-color:rgba(0,180,120,.3)}';
        css += 'html.dark .chat-sb-mu .chat-sb-time,html.dark .chat-sb-ma .chat-sb-time{color:#666}';
        css += 'html.dark .chat-sb-err{color:#ff6b6b;background:rgba(229,57,53,.1);border-color:rgba(229,57,53,.2)}';
        css += 'html.dark .chat-sb-seg+.chat-sb-seg::before{background:linear-gradient(90deg,transparent,#3a3a3a 20%,#3a3a3a 80%,transparent)}';
        css += 'html.dark .chat-sb-reason{border-color:#3a3a3a;background:#1e1e1e}';
        css += 'html.dark .chat-sb-reason:hover{border-color:#555}';
        css += 'html.dark .chat-sb-reason-hd{color:#aaa}';
        css += 'html.dark .chat-sb-reason-hd:hover{background:#2a2a2a}';
        css += 'html.dark .chat-sb-reason-hd .chat-sb-ri{color:#777}';
        css += 'html.dark .chat-sb-reason-hd .chat-sb-rl{color:#aaa}';
        css += 'html.dark .chat-sb-reason-hd .chat-sb-rd{color:#555}';
        css += 'html.dark .chat-sb-reason-bd{color:#bbb}';
        css += 'html.dark .chat-sb-reason.chat-sb-reasoning{border-color:rgba(0,180,120,.3);background:rgba(0,180,120,.05)}';
        css += 'html.dark .chat-sb-reason.chat-sb-reasoning .chat-sb-reason-hd{color:#bbb}';
        css += 'html.dark .chat-sb-reason.chat-sb-reasoning .chat-sb-reason-bd{color:#aaa}';
        css += 'html.dark .chat-sb-mc{color:#bbb}';
        css += 'html.dark .chat-sb-mc h1,html.dark .chat-sb-mc h2,html.dark .chat-sb-mc h3{color:#e5e5e5}';
        css += 'html.dark .chat-sb-mc a{color:#4ade80}';
        css += 'html.dark .chat-sb-mc blockquote{background:rgba(0,180,120,.05);border-left-color:#00b878}';
        css += 'html.dark .chat-sb-mc code:not(pre code){background:rgba(0,180,120,.15);color:#4ade80}';
        css += 'html.dark .chat-sb-mc table{border-color:#3a3a3a}';
        css += 'html.dark .chat-sb-mc th{background:#1e1e1e;color:#ddd}';
        css += 'html.dark .chat-sb-mc td{border-color:#3a3a3a}';
        css += 'html.dark .chat-sb-mc hr{border-top-color:#3a3a3a}';
        css += 'html.dark .chat-sb-mc strong{color:#e5e5e5}';
        css += 'html.dark .chat-sb-input{border-top-color:#3a3a3a;background:#1e1e1e}';
        css += 'html.dark .chat-sb-iwrap{background:transparent;border-color:#3a3a3a}';
        css += 'html.dark .chat-sb-iwrap:focus-within{border-color:#00b878;box-shadow:0 0 0 2px rgba(0,180,120,.15)}';
        css += 'html.dark .chat-sb-iwrap textarea{color:#e5e5e5}';
        css += 'html.dark .chat-sb-iwrap textarea::placeholder{color:#666}';
        css += 'html.dark .chat-sb-ibtn.stop{background:rgba(229,57,53,.1);color:#ff6b6b}';
        css += 'html.dark .chat-sb-ibtn.send{background:#00b878;color:#ffffff !important}';
        css += 'html.dark .chat-sb-ibtn.send:hover{background:#00a86b}';
        css += 'html.dark .chat-sb-ibtn.send svg, html.dark .chat-sb-ibtn.send svg path{stroke:#ffffff !important}';
        css += 'html.dark .chat-sb-del-btn{border-color:#3a3a3a;background:transparent;color:#777}';
        css += 'html.dark .chat-sb-del-btn:hover{border-color:#e53935;background:rgba(229,57,53,.1);color:#ff6b6b}';
        css += 'html.dark .chat-sb-footer-btn{border-color:#3a3a3a;background:transparent;color:#777}';
        css += 'html.dark .chat-sb-footer-btn.delete:hover{border-color:#e53935;background:rgba(229,57,53,.1);color:#ff6b6b}';
        css += 'html.dark .chat-sb-footer-btn.copy:hover{border-color:#00b878;background:rgba(0,184,120,.1);color:#4ade80}';
        css += 'html.dark .chat-sb-toast-item{background:#1e1e1e;border-color:#3a3a3a;color:#e5e5e5}';
        s.textContent = css;
        this._el = { style: s };
        document.head.appendChild(s);
    }

    /* ---------- DOM ---------- */
    _createDOM() {
        const L = this._lang;
        const overlay = document.createElement('div');
        overlay.className = 'chat-sb-ov';
        document.body.appendChild(overlay);
        const root = document.createElement('div');
        root.className = 'chat-sb';
        root.innerHTML = '<div class="chat-sb-resizer"></div><div class="chat-sb-hd"><div class="chat-sb-logo"><svg viewBox="0 0 24 24"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg></div><span class="chat-sb-t"></span><button class="chat-sb-btn" data-action="new"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>' + L.newSessionBtn + '</button><button class="chat-sb-ibtn" data-action="close" title="' + L.closeBtn + '"><svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button></div><div class="chat-sb-msgs"><div class="chat-sb-welcome"><div class="chat-sb-wi"><svg viewBox="0 0 24 24"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg></div><p>' + L.welcomeMain + '</p></div><div class="chat-sb-ml" style="display:none"></div></div><div class="chat-sb-input"><div class="chat-sb-iwrap"><textarea placeholder="' + L.inputPlaceholder + '" rows="1"></textarea><div class="chat-sb-ibtns"><button class="chat-sb-ibtn sm stop" data-action="stop" style="display:none" title="' + L.stopBtn + '"><svg viewBox="0 0 24 24" fill="currentColor" stroke="none"><rect x="6" y="6" width="12" height="12" rx="2"/></svg></button><button class="chat-sb-ibtn sm send" data-action="send" disabled title="' + L.sendBtn + '"><svg viewBox="0 0 24 24"><line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/></svg></button></div></div></div>';
        document.body.appendChild(root);
        const toastBox = document.createElement('div');
        toastBox.className = 'chat-sb-toast';
        document.body.appendChild(toastBox);
        Object.assign(this._el, {
            root, overlay, toastBox,
            resizer: root.querySelector('.chat-sb-resizer'),
            title: root.querySelector('.chat-sb-t'),
            input: root.querySelector('textarea'), msgContainer: root.querySelector('.chat-sb-msgs'),
            welcome: root.querySelector('.chat-sb-welcome'), msgList: root.querySelector('.chat-sb-ml'),
            sendBtn: root.querySelector('[data-action="send"]'), stopBtn: root.querySelector('[data-action="stop"]'),
        });
    }

    /* ---------- 事件 ---------- */
    _bindEvents() {
        const resizer = this._el.resizer;
        resizer.addEventListener('pointerdown', e => {
            e.preventDefault();
            this._dragging = true;
            resizer.setPointerCapture(e.pointerId);
            resizer.classList.add('dragging');
            document.body.classList.add('chat-sb-dragging');
        });
        resizer.addEventListener('pointermove', e => {
            if (!this._dragging) return;
            const w = Math.max(this._opts.minW, Math.min(this._opts.maxW, window.innerWidth - e.clientX));
            this._el.root.style.width = w + 'px';
            this._el.root.style.transition = 'none';
        });
        resizer.addEventListener('pointerup', e => {
            if (!this._dragging) return;
            this._dragging = false;
            resizer.releasePointerCapture(e.pointerId);
            resizer.classList.remove('dragging');
            document.body.classList.remove('chat-sb-dragging');
            this._el.root.style.transition = '';
        });

        const inp = this._el.input;
        inp.addEventListener('input', () => {
            inp.style.height = 'auto';
            inp.style.height = Math.min(inp.scrollHeight, 140) + 'px';
            if (!this._generating) this._el.sendBtn.disabled = !inp.value.trim();
        });
        inp.addEventListener('keydown', e => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this._doSend();
            }
        });

        this._el.root.addEventListener('click', e => {
            const a = e.target.closest('[data-action]')?.dataset.action;
            if (a === 'send') this._doSend();
            else if (a === 'stop') this._cancelRun();
            else if (a === 'close') this.close();
            else if (a === 'new') this.newSession();
            else if (a === 'delete') {
                const mid = e.target.closest('[data-mid]')?.dataset.mid;
                if (mid) this._deleteMessage(mid);
            }
            else if (a === 'copy') {
                const mid = e.target.closest('[data-mid]')?.dataset.mid;
                if (mid) this._copyMessage(mid);
            }
        });

        this._el.msgList.addEventListener('click', e => {
            const hd = e.target.closest('.chat-sb-reason-hd');
            if (hd) hd.parentElement.classList.toggle('open');
        });

        this._el.overlay.addEventListener('click', () => this.close());
        document.addEventListener('keydown', e => {
            if (e.key === 'Escape' && this._activeCtx != null) this.close();
        });
    }

    /* ---------- 存储 ---------- */
    _getConv(id) {
        return this._convs[id] ||= { threadId: this._uid(), messages: [] };
    }
    _saveConv() {
        try {
            localStorage.setItem(this._opts.prefix + '-conv', JSON.stringify(this._convs));
        } catch {}
    }
    _loadConv() {
        try {
            const d = localStorage.getItem(this._opts.prefix + '-conv');
            if (d) Object.assign(this._convs, JSON.parse(d));
        } catch {}
    }

    /* ---------- HTTP + SSE ---------- */
    async _startRun(content) {
        const conv = this._getConv(this._activeCtx);
        this._runId = this._uid();
        this._generating = true;
        this._updBtn();
        conv.messages.push({ id: this._uid(), role: 'user', content, timestamp: Date.now() });
        this._saveConv();
        this._renderMsgs();

        const headers = { 'Content-Type': 'application/json', Accept: 'text/event-stream', ...this._opts.headers };
        this._abortCtrl = new AbortController();
        try {
            const res = await fetch(this._opts.baseUrl + this._opts.endpoint, {
                method: 'POST',
                headers,
                body: JSON.stringify({ runId: this._runId, threadId: conv.threadId, message: content }),
                signal: this._abortCtrl.signal,
            });
            if (!res.ok) {
                throw new Error('HTTP ' + res.status + ': ' + (await res.text().catch(() => '') || res.statusText));
            }
            await this._parseSSE(res.body, this._runId);
        } catch (err) {
            if (err.name === 'AbortError') {
                this._endRun();
            } else {
                this._resetRun();
                this._toast(this._lang.requestFailed + err.message, 'error');
                if (this._aiMid) {
                    const m = conv.messages.find(x => x.id === this._aiMid);
                    if (m) {
                        m.streaming = false;
                        m.error = err.message;
                    }
                    this._finalRender(this._aiMid);
                    this._aiMid = null;
                }
            }
        }
    }

    async _parseSSE(body, rid) {
        const reader = body.getReader();
        const dec = new TextDecoder();
        let buf = '';
        const parse = text => {
            let evt = '', dat = '';
            for (const ln of text.split('\n')) {
                if (ln.startsWith('event:')) evt = ln.slice(6).trim();
                else if (ln.startsWith('data:')) dat = dat ? dat + '\n' + ln.slice(5).trim() : ln.slice(5).trim();
            }
            if (evt && dat) {
                try {
                    this._handleEv(evt, JSON.parse(dat));
                } catch {
                    this._handleEv(evt, { content: dat });
                }
            }
        };
        try {
            while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                buf += dec.decode(value, { stream: true });
                const parts = buf.split('\n\n');
                buf = parts.pop() || '';
                for (const p of parts) {
                    if (p.trim()) parse(p);
                }
            }
            if (buf.trim()) parse(buf);
            if (this._generating && this._runId === rid) this._endRun();
        } catch (e) {
            if (e.name !== 'AbortError') throw e;
        } finally {
            this._abortCtrl = null;
        }
    }

    _resetRun() {
        this._generating = false;
        this._runId = null;
        this._abortCtrl = null;
        this._updBtn();
    }
    _cancelRun() {
        this._abortCtrl?.abort();
        this._resetRun();
        this._clearMt();
        this._syncSegs(true);
    }
    _endRun() {
        this._resetRun();
        this._syncSegs(true);
        this._saveConv();
    }

    /* ---------- 同步段落到消息 ---------- */
    _syncSegs(finalize = false) {
        for (const s of this._segments) {
            if (finalize) {
                if (s.type === 'reasoning' && s.status === 'thinking') {
                    s.status = 'done';
                    if (s.startTs) s.duration = Date.now() - s.startTs;
                }
                if (s.type === 'text' && !s.complete) s.complete = true;
            }
        }
        if (finalize) {
            this._curR = null;
            this._curT = null;
        }
        const m = this._getConv(this._activeCtx).messages.find(x => x.id === this._aiMid);
        if (m) {
            m.segments = this._segments.map(s => ({ ...s }));
            m.reasoning = [...this._segments].reverse().find(s => s.type === 'reasoning') || null;
            m.content = this._segments.filter(s => s.type === 'text').map(s => s.content).join('');
        }
    }

    /* ---------- 事件处理 ---------- */
    _handleEv(type, d) {
        const conv = this._getConv(this._activeCtx);
        switch (type) {
            case 'RUN_STARTED':
                this._aiMid = this._uid();
                this._segments = [];
                this._curR = null;
                this._curT = null;
                this._rRound = 0;
                conv.messages.push({
                    id: this._aiMid,
                    role: 'assistant',
                    content: '',
                    segments: [],
                    timestamp: Date.now(),
                    streaming: true,
                    reasoning: null,
                    error: null
                });
                this._renderMsgs();
                break;

            case 'REASONING_START':
                if (this._curT && !this._curT.complete) {
                    this._curT.complete = true;
                    this._curT = null;
                }
                this._rRound++;
                this._curR = {
                    type: 'reasoning',
                    content: '',
                    startTs: Date.now(),
                    status: 'thinking',
                    duration: null,
                    round: this._rRound
                };
                this._segments.push(this._curR);
                this._syncSegs();
                this._streamUpdate();
                break;

            case 'REASONING_CONTENT':
                if (this._curR) this._curR.content += (d.content || '');
                this._streamUpdate();
                break;

            case 'REASONING_END':
                if (this._curR) {
                    this._curR.duration = Date.now() - this._curR.startTs;
                    this._curR.status = 'done';
                }
                this._syncSegs();
                this._streamUpdate();
                break;

            case 'TEXT_MESSAGE_START':
                if (this._curR?.status === 'thinking') {
                    this._curR.status = 'done';
                    this._curR.duration = Date.now() - this._curR.startTs;
                }
                this._curT = { type: 'text', content: '', complete: false };
                this._segments.push(this._curT);
                this._syncSegs();
                this._streamUpdate();
                break;

            case 'TEXT_MESSAGE_CONTENT':
                if (this._curT) {
                    this._curT.content += (d.content || '');
                    const m = conv.messages.find(x => x.id === this._aiMid);
                    if (m) {
                        m.content = this._segments.filter(s => s.type === 'text').map(s => s.content).join('');
                    }
                }
                this._streamUpdate();
                break;

            case 'TEXT_MESSAGE_END':
                if (this._curT) this._curT.complete = true;
                this._syncSegs();
                this._streamUpdate();
                break;

            case 'RUN_FINISHED':
                this._syncSegs(true);
                const m = conv.messages.find(x => x.id === this._aiMid);
                if (m) m.streaming = false;
                this._endRun();
                this._finalRender(this._aiMid);
                this._aiMid = null;
                this._emitMsgCount();
                break;

            case 'RUN_ERROR':
                this._syncSegs(true);
                this._endRun();
                this._toast(d.message || this._lang.runError, 'error');
                if (this._aiMid) {
                    const m = conv.messages.find(x => x.id === this._aiMid);
                    if (m) {
                        m.streaming = false;
                        m.error = d.message || this._lang.runError;
                    }
                    this._finalRender(this._aiMid);
                    this._aiMid = null;
                }
                break;
        }
    }

    /* ---------- 流式 Markdown 安全解析 ---------- */
    _mdParse(content, complete = false) {
        if (!content) return '';
        const matches = content.match(/```/g);
        if (matches && matches.length % 2) content += '\n```';
        return marked.parse(content);
    }

    /* ---------- 流式 DOM 更新 ---------- */
    _streamUpdate() {
        const el = this._el.msgList.querySelector('[data-mid="' + this._aiMid + '"]');
        if (!el) return;
        const m = this._getConv(this._activeCtx).messages.find(x => x.id === this._aiMid);
        const body = el.querySelector('.chat-sb-body');
        body.innerHTML = this._buildSegsHtml(this._segments, false)
            + (m.error ? '<div class="chat-sb-err">' + this._esc(m.error) + '</div>' : '')
            + '<div class="chat-sb-time">' + this._fmtT(m.timestamp) + '</div>';
        this._el.msgContainer.scrollTop = this._el.msgContainer.scrollHeight;
    }

    /* ---------- 段落 HTML ---------- */
    _buildSegsHtml(segs, complete = true) {
        let html = '', ri = 0;
        for (const s of segs) {
            html += '<div class="chat-sb-seg">';
            if (s.type === 'reasoning') {
                ri++;
                html += this._reasoningH(s, ri);
            } else if (s.type === 'text') {
                const c = s.content, done = s.complete && complete;
                html += c ? '<div class="chat-sb-mc">' + this._mdParse(c, done) + '</div>' : (!done ? '<div class="chat-sb-mc"><span class="chat-sb-dots"><span></span><span></span><span></span></span></div>' : '');
            }
            html += '</div>';
        }
        return html;
    }

    _reasoningH(r, round) {
        if (!r?.content) return '';
        const L = this._lang;
        const thinking = r.status === 'thinking';
        const open = thinking;
        const cls = ['chat-sb-reason', thinking && 'chat-sb-reasoning', open && 'open'].filter(Boolean).join(' ');
        const icon = thinking
            ? '<svg class="chat-sb-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 2a10 10 0 0 1 10 10" stroke-linecap="round"/></svg>'
            : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18h6"/><path d="M10 22h4"/><path d="M12 2a7 7 0 0 1 4 12.9V17H8v-2.1A7 7 0 0 1 12 2z"/></svg>';
        const badge = round > 1 ? '<span class="chat-sb-reason-round">' + round + '</span>' : '';
        const dur = thinking
            ? '<span class="chat-sb-think-dots"><span></span><span></span><span></span></span>'
            : (r.duration ? L.thoughtFor + this._fmtDur(r.duration) : '');
        const label = round > 1 ? L.roundThink.replace('{n}', round) : (thinking ? L.thinking : L.thoughtProcess);
        return '<div class="' + cls + '"><div class="chat-sb-reason-hd"><span class="chat-sb-ri">' + icon + '</span><span class="chat-sb-rl">' + label + badge + '</span>' + (dur ? '<span class="chat-sb-rd">' + dur + '</span>' : '') + '<span class="chat-sb-rarr"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></span></div><div class="chat-sb-reason-bd">' + this._mdParse(r.content, r.status === 'done') + '</div></div>';
    }

    /* ---------- 消息渲染 ---------- */
    _esc(s) {
        const d = document.createElement('div');
        d.textContent = s;
        return d.innerHTML;
    }
    _fmtT(ts) {
        return new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    }
    _fmtDur(ms) {
        return ms < 1000 ? ms + 'ms' : (ms / 1000).toFixed(1) + 's';
    }

    _userH(m) {
        const delBtn = '<button class="chat-sb-del-btn" data-action="delete" data-mid="' + this._esc(m.id) + '" title="' + this._lang.deleteMsg + '"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button>';
        return '<div class="chat-sb-msg chat-sb-mu" data-mid="' + this._esc(m.id) + '"><div class="chat-sb-bbl">' + delBtn + this._esc(m.content).replace(/\r?\n/g, '<br>') + '</div><div class="chat-sb-time">' + this._fmtT(m.timestamp) + '</div></div>';
    }

    _aiH(m) {
        const segs = m.segments || [];
        const bodyHtml = this._buildSegsHtml(segs, true);
        const er = m.error ? '<div class="chat-sb-err">' + this._esc(m.error) + '</div>' : '';
        const footer = '<div class="chat-sb-footer-btns">' +
            '<button class="chat-sb-footer-btn delete" data-action="delete" data-mid="' + this._esc(m.id) + '" title="' + this._lang.deleteMsg + '"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button>' +
            '<button class="chat-sb-footer-btn copy" data-action="copy" data-mid="' + this._esc(m.id) + '" title="' + this._lang.copyMsg + '"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg></button>' +
        '</div>';
        return '<div class="chat-sb-msg chat-sb-ma" data-mid="' + m.id + '"><div class="chat-sb-av"><svg viewBox="0 0 24 24"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg></div><div class="chat-sb-body">' + bodyHtml + er + '<div class="chat-sb-time">' + this._fmtT(m.timestamp) + '</div>' + footer + '</div></div>';
    }

    _renderMsgs() {
        const conv = this._getConv(this._activeCtx);
        if (!conv.messages.length) {
            this._el.msgList.style.display = 'none';
            this._el.welcome.style.display = 'flex';
            return;
        }
        this._el.welcome.style.display = 'none';
        this._el.msgList.style.display = 'flex';
        this._el.msgList.innerHTML = conv.messages.map(m => m.role === 'user' ? this._userH(m) : this._aiH(m)).join('');
        this._el.msgContainer.scrollTop = this._el.msgContainer.scrollHeight;
    }

    _finalRender(mid) {
        const el = this._el.msgList.querySelector('[data-mid="' + mid + '"]');
        const m = this._getConv(this._activeCtx).messages.find(x => x.id === mid);
        if (el && m) el.outerHTML = this._aiH(m);
        this._el.msgContainer.scrollTop = this._el.msgContainer.scrollHeight;
    }

    _deleteMessage(mid) {
        if (this._generating) {
            this._toast(this._lang.waitGenerate, 'error');
            return;
        }
        const conv = this._getConv(this._activeCtx);
        const lenBefore = conv.messages.length;
        // 找到要删除的消息索引
        const index = conv.messages.findIndex(m => m.id === mid);
        if (index === -1) {
            return; // 没找到，可能已经删除了
        }
        const deleted = conv.messages[index];
        // 如果删除的是用户消息，且下一条是 AI 消息，一起删除
        if (deleted.role === 'user' && index + 1 < conv.messages.length && conv.messages[index + 1].role === 'assistant') {
            conv.messages = conv.messages.filter((_, i) => i !== index && i !== index + 1);
        } else {
            // 否则只删除当前这一条
            conv.messages = conv.messages.filter(m => m.id !== mid);
        }
        this._saveConv();
        this._renderMsgs();
        this._emitMsgCount();
    }

    async _copyMessage(mid) {
        const conv = this._getConv(this._activeCtx);
        const msg = conv.messages.find(m => m.id === mid);
        if (!msg) {
            return;
        }
        // 获取纯文本内容
        let content = '';
        if (msg.segments) {
            // 从 segments 提取文本
            content = msg.segments.filter(s => s.type === 'text').map(s => s.content).join('');
        } else {
            content = msg.content || '';
        }
        if (!content.trim()) {
            this._toast(this._lang.runError, 'error');
            return;
        }
        try {
            await navigator.clipboard.writeText(content);
            this._toast(this._lang.copied, 'success');
        } catch (err) {
            // 降级处理：execCommand
            const ta = document.createElement('textarea');
            ta.value = content;
            document.body.appendChild(ta);
            ta.select();
            document.execCommand('copy');
            ta.remove();
            this._toast(this._lang.copied, 'success');
        }
    }

    _updBtn() {
        this._el.sendBtn.style.display = this._generating ? 'none' : 'flex';
        this._el.stopBtn.style.display = this._generating ? 'flex' : 'none';
        this._el.input.disabled = this._generating;
        if (!this._generating) this._el.sendBtn.disabled = !this._el.input.value.trim();
    }

    /* ---------- Mock ---------- */
    _clearMt() {
        this._mt.forEach(clearTimeout);
        this._mt = [];
    }

    /* ---------- 工具 ---------- */
    _uid() {
        return crypto.randomUUID?.()
            || 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c =>
                (c === 'x' ? Math.random() * 16 : Math.random() * 4 | 8).toString(16)
            );
    }
    _toast(msg, type) {
        const el = document.createElement('div');
        el.className = 'chat-sb-toast-item' + (type === 'error' ? ' err' : type === 'success' ? ' ok' : '');
        el.textContent = msg;
        this._el.toastBox.appendChild(el);
        setTimeout(() => {
            el.style.opacity = '0';
            el.style.transition = 'opacity .3s';
            setTimeout(() => el.remove(), 300);
        }, 3000);
    }
    _emit(e, ...a) {
        (this._listeners[e] || []).forEach(fn => {
            try {
                fn(...a);
            } catch {}
        });
    }
    _emitMsgCount() {
        if (this._activeCtx != null) {
            this._emit('message-count-change', this._activeCtx, this.getMessageCount(this._activeCtx));
        }
    }
    _doSend() {
        const c = this._el.input.value.trim();
        if (!c || this._generating || this._activeCtx == null) return;
        this._el.input.value = '';
        this._el.input.style.height = 'auto';
        this._updBtn();
        this._startRun(c);
    }
}

// 暴露到全局
window.ChatSidebar = ChatSidebar;
if (typeof globalThis !== 'undefined') {
    globalThis.ChatSidebar = ChatSidebar;
}

})();
