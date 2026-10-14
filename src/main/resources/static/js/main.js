/* ==========================================================================
   前台交互脚本
   对应《概要设计》3.1：原生 JavaScript 负责表单校验、删除二次确认、返回顶部、
   评论提交等页面交互逻辑，减少对第三方库的依赖。
   ========================================================================== */
(function () {
    'use strict';

    /* ── 窄屏汉堡菜单 ──────────────────────────────────────────────── */
    function initNavToggle() {
        var toggle = document.querySelector('.nav-toggle');
        var links = document.querySelector('.nav-links');
        if (!toggle || !links) {
            return;
        }
        toggle.addEventListener('click', function () {
            links.classList.toggle('open');
        });
    }

    /* ── 返回顶部 ──────────────────────────────────────────────────── */
    function initBackToTop() {
        var btn = document.querySelector('.back-to-top');
        if (!btn) {
            return;
        }
        window.addEventListener('scroll', function () {
            btn.classList.toggle('show', window.scrollY > 320);
        });
        btn.addEventListener('click', function () {
            window.scrollTo({top: 0, behavior: 'smooth'});
        });
    }

    /* ── 轻提示 ────────────────────────────────────────────────────── */
    function toast(message, ok) {
        var el = document.createElement('div');
        el.className = 'toast ' + (ok ? 'ok' : 'err');
        el.textContent = message;
        document.body.appendChild(el);
        // 下一帧再加 show，触发 CSS 过渡
        requestAnimationFrame(function () {
            el.classList.add('show');
        });
        setTimeout(function () {
            el.classList.remove('show');
            setTimeout(function () {
                el.remove();
            }, 260);
        }, 2600);
    }

    /* ── 评论提交 ──────────────────────────────────────────────────── */
    function initCommentForm() {
        var form = document.querySelector('.comment-form');
        if (!form) {
            return;
        }
        var textarea = form.querySelector('textarea[name="content"]');
        var parentInput = form.querySelector('input[name="parentId"]');
        var submitBtn = form.querySelector('button[type="submit"]');
        var tip = form.querySelector('.comment-tip');
        var originalTip = tip ? tip.textContent : '';

        form.addEventListener('submit', function (event) {
            event.preventDefault();

            var content = textarea.value.trim();
            if (!content) {
                toast('评论内容不能为空', false);
                textarea.focus();
                return;
            }
            if (content.length > 500) {
                toast('评论内容不能超过 500 个字符', false);
                return;
            }

            var articleId = form.dataset.articleId;
            var body = new URLSearchParams();
            body.append('articleId', articleId);
            body.append('content', content);
            if (parentInput && parentInput.value) {
                body.append('parentId', parentInput.value);
            }

            submitBtn.disabled = true;
            var originalText = submitBtn.textContent;
            submitBtn.textContent = '提交中…';

            fetch('/api/comments', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: body.toString()
            })
                .then(function (res) {
                    return res.json();
                })
                .then(function (data) {
                    if (data.code === 200) {
                        textarea.value = '';
                        if (parentInput) {
                            parentInput.value = '';
                        }
                        if (tip) {
                            tip.textContent = originalTip;
                        }
                        toast(data.message || '评论提交成功，等待审核', true);
                    } else {
                        toast(data.message || '提交失败', false);
                    }
                })
                .catch(function () {
                    toast('网络异常，请稍后重试', false);
                })
                .finally(function () {
                    submitBtn.disabled = false;
                    submitBtn.textContent = originalText;
                });
        });
    }

    /* ── 回复某条评论：把 parentId 填进表单并把输入框滚到视野内 ────── */
    function initReplyButtons() {
        var form = document.querySelector('.comment-form');
        if (!form) {
            return;
        }
        var parentInput = form.querySelector('input[name="parentId"]');
        var textarea = form.querySelector('textarea[name="content"]');
        var tip = form.querySelector('.comment-tip');

        document.querySelectorAll('[data-reply-to]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (parentInput) {
                    parentInput.value = btn.dataset.replyId;
                }
                if (tip) {
                    tip.textContent = '正在回复 @' + btn.dataset.replyTo;
                }
                textarea.focus();
                form.scrollIntoView({behavior: 'smooth', block: 'center'});
            });
        });
    }

    /* ── 正文里的代码块一键复制 ────────────────────────────────────── */
    function initCopyButtons() {
        document.querySelectorAll('[data-copy]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var target = document.querySelector(btn.dataset.copy);
                if (!target) {
                    return;
                }
                navigator.clipboard.writeText(target.textContent).then(function () {
                    toast('已复制到剪贴板', true);
                }).catch(function () {
                    toast('复制失败，请手动选择', false);
                });
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initNavToggle();
        initBackToTop();
        initCommentForm();
        initReplyButtons();
        initCopyButtons();
    });
})();
