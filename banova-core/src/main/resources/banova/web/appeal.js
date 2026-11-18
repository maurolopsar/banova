/* Banova public appeal page. */
(() => {
  'use strict';

  const I18N = {
    en: {
      title: 'Appeal a punishment', intro: 'Enter the appeal code shown when you were banned or muted.',
      code: 'Appeal code', lookup: 'Continue', notFound: 'We could not find that code. Check it and try again.',
      disabled: 'Appeals are not available on this server.', reason: 'Reason', staff: 'Staff', date: 'Date',
      expires: 'Expires', never: 'Never', status: 'Status', yourAppeals: 'Your appeals', write: 'Explain why the punishment should be removed',
      writeHint: 'Be honest and polite. At least {min} characters.', contact: 'Contact (optional: Discord, email...)',
      send: 'Send appeal', sent: 'Your appeal was sent. Come back later with the same code to see the answer.',
      response: 'Staff reply', tooMany: 'Too many attempts. Please wait a bit and try again.', error: 'Something went wrong.',
      back: 'Use another code', ban: 'Ban', mute: 'Mute', warn: 'Warning', kick: 'Kick'
    },
    es: {
      title: 'Apelar una sanción', intro: 'Escribe el código de apelación que aparecía al ser baneado o silenciado.',
      code: 'Código de apelación', lookup: 'Continuar', notFound: 'No encontramos ese código. Revísalo e inténtalo de nuevo.',
      disabled: 'Las apelaciones no están disponibles en este servidor.', reason: 'Motivo', staff: 'Staff', date: 'Fecha',
      expires: 'Expira', never: 'Nunca', status: 'Estado', yourAppeals: 'Tus apelaciones', write: 'Explica por qué debería retirarse la sanción',
      writeHint: 'Sé sincero y educado. Mínimo {min} caracteres.', contact: 'Contacto (opcional: Discord, email...)',
      send: 'Enviar apelación', sent: 'Tu apelación se ha enviado. Vuelve más tarde con el mismo código para ver la respuesta.',
      response: 'Respuesta del staff', tooMany: 'Demasiados intentos. Espera un poco y vuelve a intentarlo.', error: 'Algo ha fallado.',
      back: 'Usar otro código', ban: 'Baneo', mute: 'Silencio', warn: 'Aviso', kick: 'Expulsión'
    }
  };
  // English unless the server is set to Spanish (config.yml "language") or the visitor picked a language.
  const savedLang = () => {
    try { const v = localStorage.getItem('banova.lang'); return v === 'en' || v === 'es' ? v : null; } catch (e) { return null; }
  };
  let lang = savedLang() || 'en';
  let redraw = () => {};
  const t = (k, v) => {
    let s = I18N[lang][k] || I18N.en[k] || k;
    if (v) for (const key of Object.keys(v)) s = s.replace('{' + key + '}', v[key]);
    return s;
  };

  function el(tag, attrs, ...children) {
    const node = document.createElement(tag);
    if (attrs) {
      for (const [k, v] of Object.entries(attrs)) {
        if (v === null || v === undefined || v === false) continue;
        if (k === 'class') node.className = v;
        else if (k === 'style') node.style.cssText = v;
        else if (k.startsWith('on') && typeof v === 'function') node.addEventListener(k.substring(2), v);
        else if (k === 'value') node.value = v;
        else node.setAttribute(k, v === true ? '' : v);
      }
    }
    for (const c of children.flat(Infinity)) {
      if (c === null || c === undefined || c === false) continue;
      node.appendChild(c instanceof Node ? c : document.createTextNode(String(c)));
    }
    return node;
  }
  const root = document.getElementById('appeal');
  const clear = (n) => { while (n.firstChild) n.removeChild(n.firstChild); return n; };
  const fmt = (ms) => (ms > 0 ? new Date(ms).toLocaleString(lang === 'es' ? 'es-ES' : 'en-GB', { dateStyle: 'medium', timeStyle: 'short' }) : '—');
  const strip = (s) => (s == null ? '' : String(s).replace(/[&§](#[0-9a-fA-F]{6}|[0-9a-fk-orA-FK-OR])/g, ''));
  let info = { server: '', enabled: true, minLength: 20 };

  async function post(path, body) {
    const res = await fetch('/api/public/' + path, {
      method: 'POST', credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json', 'X-Banova': '1' }, body: JSON.stringify(body)
    });
    let data = null;
    try { data = await res.json(); } catch (e) { data = null; }
    return { status: res.status, data };
  }

  function langSwitch() {
    return el('div', { class: 'seg', style: 'margin-top:18px' }, ['en', 'es'].map((l) => el('button', { class: l === lang ? 'active' : '', type: 'button',
      onclick: () => {
        lang = l;
        try { localStorage.setItem('banova.lang', l); } catch (e) { /* ignore */ }
        document.documentElement.lang = l;
        document.title = t('title');
        redraw();
      } }, l.toUpperCase())));
  }

  function card(...children) {
    clear(root).appendChild(el('div', { class: 'card', style: 'max-width:560px;text-align:left' },
      el('div', { style: 'text-align:center' }, el('img', { src: '/assets/logo.png', alt: '', style: 'width:48px;height:48px' }),
        el('h1', null, t('title')), info.server ? el('p', { class: 'muted' }, strip(info.server)) : null),
      ...children,
      el('div', { style: 'text-align:center' }, langSwitch())));
  }

  function codeForm(error, initial) {
    redraw = () => codeForm(error, initial);
    if (!info.enabled) {
      card(el('p', { class: 'muted', style: 'text-align:center' }, t('disabled')));
      return;
    }
    const input = el('input', { type: 'text', placeholder: 'XXXX-XXXX', value: initial || '', maxlength: '12',
      style: 'text-align:center;font-family:var(--mono);font-size:22px;letter-spacing:.12em;text-transform:uppercase' });
    const submit = () => lookup(input.value.trim());
    input.addEventListener('keydown', (e) => { if (e.key === 'Enter') submit(); });
    card(el('p', { class: 'muted', style: 'text-align:center' }, t('intro')),
      el('label', { class: 'field' }, el('span', null, t('code')), input),
      error ? el('p', { style: 'color:var(--ban)' }, error) : null,
      el('button', { class: 'btn primary', style: 'width:100%', onclick: submit }, t('lookup')));
    input.focus();
  }

  async function lookup(code) {
    if (!code) return;
    const res = await post('lookup', { code });
    if (res.status === 429) { codeForm(t('tooMany'), code); return; }
    if (res.status !== 200 || !res.data) { codeForm(t('notFound'), code); return; }
    showPunishment(code, res.data, null);
  }

  function showPunishment(code, p, notice) {
    redraw = () => showPunishment(code, p, notice);
    const typeName = t((p.type || '').toLowerCase());
    const message = el('textarea', { placeholder: t('write'), maxlength: '2000', style: 'min-height:150px' });
    const contact = el('input', { type: 'text', placeholder: t('contact'), maxlength: '128' });
    const counter = el('small', { class: 'muted' }, t('writeHint', { min: info.minLength }));
    const errorBox = el('p', { style: 'color:var(--ban)' });
    const send = async (ev) => {
      if (message.value.trim().length < info.minLength) { errorBox.textContent = t('writeHint', { min: info.minLength }); return; }
      ev.target.disabled = true;
      const res = await post('submit', { code, message: message.value, contact: contact.value });
      ev.target.disabled = false;
      if (res.status === 429) { errorBox.textContent = t('tooMany'); return; }
      if (!res.data) { errorBox.textContent = t('error'); return; }
      if (res.data.ok) { showPunishment(code, res.data, t('sent')); return; }
      errorBox.textContent = res.data.message || t('error');
    };
    card(
      notice ? el('div', { class: 'item', style: 'border-color:var(--ok);margin-bottom:14px' }, notice) : null,
      el('div', { class: 'row', style: 'margin-bottom:10px' }, el('span', { class: 'badge ' + p.type }, typeName), el('span', { class: 'badge ' + p.status }, strip(p.statusName))),
      el('dl', { class: 'kv' },
        el('dt', null, t('reason')), el('dd', null, p.reason),
        el('dt', null, t('staff')), el('dd', null, p.actorName),
        el('dt', null, t('date')), el('dd', null, fmt(p.createdAt)),
        p.type !== 'KICK' ? el('dt', null, t('expires')) : null, p.type !== 'KICK' ? el('dd', null, p.permanent ? t('never') : fmt(p.expiresAt)) : null),
      p.appeals && p.appeals.length ? el('div', { style: 'margin-top:16px' }, el('h2', null, t('yourAppeals')),
        el('div', { class: 'list' }, p.appeals.map((a) => el('div', { class: 'item' },
          el('div', { class: 'meta' }, el('span', { class: 'badge ' + a.status }, strip(a.statusName)), fmt(a.createdAt)),
          a.response ? el('p', null, t('response') + ': ' + a.response) : null)))) : null,
      p.canAppeal ? el('div', { style: 'margin-top:16px' },
        el('label', { class: 'field' }, el('span', null, t('write')), message, counter),
        el('label', { class: 'field' }, el('span', null, t('contact')), contact),
        errorBox,
        el('button', { class: 'btn primary', style: 'width:100%', onclick: send }, t('send')))
        : (p.blockMessage && !notice ? el('p', { class: 'muted', style: 'margin-top:16px' }, p.blockMessage) : null),
      el('div', { style: 'text-align:center;margin-top:16px' }, el('button', { class: 'btn ghost small', onclick: () => codeForm() }, t('back'))));
  }

  async function boot() {
    try {
      const res = await fetch('/api/public/info', { credentials: 'same-origin' });
      info = await res.json();
      if (!savedLang() && info.language === 'es') lang = 'es';
    } catch (e) { /* defaults */ }
    document.documentElement.lang = lang;
    document.title = t('title');
    const fromUrl = new URLSearchParams(location.search).get('code');
    if (fromUrl) lookup(fromUrl); else codeForm();
  }
  boot();
})();
