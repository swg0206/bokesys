/* ==========================================================================
   后台交互脚本
   对应《概要设计》6.2.4：表格多选批量删除、筛选条件变更后回到第 1 页、
   删除前的二次确认弹窗、提交期间的加载态。
   ========================================================================== */
(function () {
    'use strict';

    /* ── 轻提示 ────────────────────────────────────────────────────── */
    function toast(message, ok) {
        var el = document.createElement('div');
        el.className = 'toast ' + (ok ? 'ok' : 'err');
        el.textContent = message;
        document.body.appendChild(el);
        requestAnimationFrame(function () {
            el.classList.add('show');
        });
        setTimeout(function () {
            el.classList.remove('show');
            setTimeout(function () {
                el.remove();
            }, 260);
        }, 2800);
    }

    window.adminToast = toast;

    /* ── 窄屏侧边栏开关 ────────────────────────────────────────────── */
    function initSidebarToggle() {
        var btn = document.querySelector('.sidebar-toggle');
        var sidebar = document.querySelector('.admin-sidebar');
        if (!btn || !sidebar) {
            return;
        }
        btn.addEventListener('click', function () {
            sidebar.classList.toggle('open');
        });
    }

    /* ── 二次确认弹窗 ──────────────────────────────────────────────── */
    var confirmMask = null;
    var confirmCallback = null;

    function ensureConfirmModal() {
        if (confirmMask) {
            return confirmMask;
        }
        confirmMask = document.createElement('div');
        confirmMask.className = 'modal-mask';
        confirmMask.innerHTML =
            '<div class="modal-box">' +
            '  <h3 id="confirm-title">确认操作</h3>' +
            '  <p id="confirm-text"></p>' +
            '  <div class="actions">' +
            '    <button type="button" class="btn btn-outline" data-confirm-cancel>取消</button>' +
            '    <button type="button" class="btn btn-danger" data-confirm-ok>确定</button>' +
            '  </div>' +
            '</div>';
        document.body.appendChild(confirmMask);

        confirmMask.querySelector('[data-confirm-cancel]').addEventListener('click', closeConfirm);
        confirmMask.addEventListener('click', function (e) {
            if (e.target === confirmMask) {
                closeConfirm();
            }
        });
        confirmMask.querySelector('[data-confirm-ok]').addEventListener('click', function () {
            var cb = confirmCallback;
            closeConfirm();
            if (cb) {
                cb();
            }
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                closeConfirm();
            }
        });
        return confirmMask;
    }

    function openConfirm(title, text, callback) {
        ensureConfirmModal();
        confirmMask.querySelector('#confirm-title').textContent = title || '确认操作';
        confirmMask.querySelector('#confirm-text').textContent = text || '';
        confirmCallback = callback;
        confirmMask.classList.add('show');
    }

    function closeConfirm() {
        if (confirmMask) {
            confirmMask.classList.remove('show');
        }
        confirmCallback = null;
    }

    /* ── 统一的 AJAX 提交 ──────────────────────────────────────────── */
    function request(url, options) {
        return fetch(url, options).then(function (res) {
            return res.json().then(function (data) {
                return data;
            });
        });
    }

    /**
     * 把表单序列化成 URLSearchParams。
     * 复选框未选中时不提交，正是后台标签多选想要的行为。
     */
    function formToParams(form) {
        return new URLSearchParams(new FormData(form));
    }

    function submitForm(form, methodOverride) {
        // action 与 data-ajax 都在提交时才读取：编辑器页会在点击不同按钮时先改这两项
        var url = form.getAttribute('action');
        var method = methodOverride || form.dataset.ajax || form.method || 'post';
        var buttons = form.querySelectorAll('button[type="submit"]');
        var texts = [];

        buttons.forEach(function (b, i) {
            texts[i] = b.textContent;
            b.disabled = true;
        });

        var params = formToParams(form);

        return request(url, {
            method: method.toUpperCase(),
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: params.toString()
        }).then(function (data) {
            if (data.code === 200) {
                toast(data.message || '操作成功', true);
                afterSuccess(form);
            } else {
                toast(data.message || '操作失败', false);
                buttons.forEach(function (b, i) {
                    b.disabled = false;
                    b.textContent = texts[i];
                });
            }
            return data;
        }).catch(function () {
            toast('网络异常，请稍后重试', false);
            buttons.forEach(function (b, i) {
                b.disabled = false;
                b.textContent = texts[i];
            });
        });
    }

    /** 提交成功后的去向：优先跳 data-redirect，否则按 data-reload 决定是否刷新 */
    function afterSuccess(form) {
        var redirect = form.dataset.redirect;
        setTimeout(function () {
            if (redirect) {
                window.location.href = redirect;
            } else if (form.dataset.reload !== 'false') {
                window.location.reload();
            }
        }, 700);
    }

    /* ── 表单声明式绑定：data-ajax="put|post|delete" ────────────────── */
    function initAjaxForms() {
        document.querySelectorAll('form[data-ajax]').forEach(function (form) {
            form.addEventListener('submit', function (event) {
                event.preventDefault();
                var method = form.dataset.ajax;

                // 需要二次确认的（主要是删除）先弹窗
                if (form.dataset.confirm) {
                    openConfirm(form.dataset.confirmTitle || '请确认',
                        form.dataset.confirm, function () {
                            submitForm(form, method);
                        });
                    return;
                }
                submitForm(form, method);
            });
        });
    }

    function reloadPage() {
        setTimeout(function () {
            window.location.reload();
        }, 700);
    }

    // 供文章编辑器之类需要动态改 action/method 的页面调用
    window.adminSubmitForm = submitForm;

    /* ── 表格行内按钮：data-action + data-url ──────────────────────── */
    function initRowActions() {
        document.querySelectorAll('[data-row-action]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var url = btn.dataset.url;
                var method = (btn.dataset.method || 'post').toUpperCase();
                var confirmText = btn.dataset.confirm;
                var reload = btn.dataset.reload !== 'false';

                var doIt = function () {
                    request(url, {
                        method: method,
                        headers: {'X-Requested-With': 'XMLHttpRequest'}
                    }).then(function (data) {
                        if (data.code === 200) {
                            toast(data.message || '操作成功', true);
                            if (reload) {
                                reloadPage();
                            }
                        } else {
                            toast(data.message || '操作失败', false);
                        }
                    }).catch(function () {
                        toast('网络异常，请稍后重试', false);
                    });
                };

                if (confirmText) {
                    openConfirm(btn.dataset.confirmTitle || '请确认', confirmText, doIt);
                } else {
                    doIt();
                }
            });
        });
    }

    /* ── 表格全选与批量删除 ────────────────────────────────────────── */
    function initBatchTable() {
        var selectAll = document.querySelector('[data-select-all]');
        var batchBtn = document.querySelector('[data-batch-delete]');

        function rowChecks() {
            return Array.prototype.slice.call(
                document.querySelectorAll('[data-row-check]'));
        }

        function selectedIds() {
            return rowChecks().filter(function (c) {
                return c.checked;
            }).map(function (c) {
                return Number(c.value);
            });
        }

        function syncBatchButton() {
            if (!batchBtn) {
                return;
            }
            var count = selectedIds().length;
            batchBtn.disabled = count === 0;
            var label = batchBtn.dataset.label || '批量删除';
            batchBtn.textContent = count > 0 ? label + '（' + count + '）' : label;
        }

        if (selectAll) {
            selectAll.addEventListener('change', function () {
                rowChecks().forEach(function (c) {
                    c.checked = selectAll.checked;
                });
                syncBatchButton();
            });
        }

        rowChecks().forEach(function (c) {
            c.addEventListener('change', syncBatchButton);
        });

        if (batchBtn) {
            batchBtn.addEventListener('click', function () {
                var ids = selectedIds();
                if (ids.length === 0) {
                    return;
                }
                openConfirm('批量删除',
                    '删除后这些文章及其评论将不可恢复，确定删除吗？',
                    function () {
                        request(batchBtn.dataset.batchDelete, {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json;charset=UTF-8',
                                'X-Requested-With': 'XMLHttpRequest'
                            },
                            body: JSON.stringify(ids)
                        }).then(function (data) {
                            if (data.code === 200) {
                                toast(data.message || '删除成功', true);
                                reloadPage();
                            } else {
                                toast(data.message || '删除失败', false);
                            }
                        }).catch(function () {
                            toast('网络异常，请稍后重试', false);
                        });
                    });
            });
        }

        syncBatchButton();
    }

    /* ── 筛选条件变更后回到第 1 页 ────────────────────────────────── */
    function initFilterForm() {
        var form = document.querySelector('[data-filter-form]');
        if (!form) {
            return;
        }
        // 切换下拉即提交；文本框回车时也提交（提交前把页码归零）
        form.querySelectorAll('select[data-auto-submit]').forEach(function (select) {
            select.addEventListener('change', function () {
                resetPage(form);
                form.submit();
            });
        });
    }

    function resetPage(form) {
        var hidden = form.querySelector('input[name="pageNo"]');
        if (hidden) {
            hidden.value = '1';
        }
    }

    /* ── 文章编辑器：字数统计 + 返回列表 ──────────────────────────── */
    function initEditor() {
        var textarea = document.querySelector('#contentInput');
        var counter = document.querySelector('#contentCounter');
        if (!textarea || !counter) {
            return;
        }
        function update() {
            counter.textContent = textarea.value.length + ' 字符';
        }
        textarea.addEventListener('input', update);
        update();
    }

    /* ── 封面图上传 ────────────────────────────────────────────────── */
    function initCoverUpload() {
        var input = document.querySelector('#coverFileInput');
        if (!input) {
            return;
        }
        input.addEventListener('change', function () {
            var file = input.files && input.files[0];
            if (!file) {
                return;
            }
            var data = new FormData();
            data.append('file', file);

            fetch('/admin/profile/upload', {
                method: 'POST',
                headers: {'X-Requested-With': 'XMLHttpRequest'},
                body: data
            }).then(function (res) {
                return res.json();
            }).then(function (data) {
                if (data.code === 200) {
                    var hidden = document.querySelector('#coverImageInput');
                    if (hidden) {
                        hidden.value = data.data;
                    }
                    var preview = document.querySelector('#coverPreview');
                    if (preview) {
                        preview.src = data.data;
                        preview.style.display = 'block';
                    }
                    toast('封面已上传', true);
                } else {
                    toast(data.message || '上传失败', false);
                }
            }).catch(function () {
                toast('上传失败，请稍后重试', false);
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initSidebarToggle();
        initAjaxForms();
        initRowActions();
        initBatchTable();
        initFilterForm();
        initEditor();
        initCoverUpload();
    });
})();
