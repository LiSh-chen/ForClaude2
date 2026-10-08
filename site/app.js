(() => {
'use strict';
const NS = 'http://www.w3.org/2000/svg';
const MK = {TW: '台股', US: '美股'};
const FACTORS = [['sector', '產業'], ['leader', '龍頭'], ['quality', '品質'], ['growth', '成長'], ['valuation', '估值'], ['momentum', '動能'], ['news', '新聞']];
const S = {idx: [], date: null, snap: null, perf: null, status: null, tab: 'overview', hm: 'TW', pm: 'main'};

/* ---------- 小工具 ---------- */
function add(e, kids) { for (const k of kids.flat(Infinity)) { if (k == null || k === false) continue; e.append(k.nodeType ? k : document.createTextNode(String(k))); } }
function h(tag, p, ...kids) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(p || {})) {
    if (v == null || v === false) continue;
    if (k === 'class') e.className = v; else if (k === 'on') for (const [ev, fn] of Object.entries(v)) e.addEventListener(ev, fn);
    else if (k === 'tip') e.dataset.tip = v; else e.setAttribute(k, v);
  }
  add(e, kids); return e;
}
function s(tag, a, ...kids) { const e = document.createElementNS(NS, tag); for (const [k, v] of Object.entries(a || {})) e.setAttribute(k, v); add(e, kids); return e; }
const isNum = x => typeof x === 'number' && isFinite(x);
const pct = (x, d = 1, sign = true) => !isNum(x) ? '—' : (sign && x > 0 ? '+' : '') + (x * 100).toFixed(d) + '%';
const px = x => !isNum(x) ? '—' : x.toLocaleString('en-US', {maximumFractionDigits: x >= 1000 ? 0 : 2, minimumFractionDigits: x >= 1000 ? 0 : 2});
const big = x => !isNum(x) ? '—' : x >= 1e12 ? (x / 1e12).toFixed(2) + '兆' : x >= 1e8 ? Math.round(x / 1e8) + '億' : x.toLocaleString('en-US');
const nz = (x, d = 1) => isNum(x) ? x.toFixed(d) : '—';
const dstr = d => d && d.length === 6 ? `20${d.slice(0, 2)}-${d.slice(2, 4)}-${d.slice(4)}` : d;
const safeUrl = u => /^https?:\/\//i.test(u || '') ? u : null;
function delta(x, d = 1) { if (!isNum(x)) return h('span', {class: 'muted'}, '—'); return h('span', {class: 'num'}, h('span', {class: 'arrow ' + (x >= 0 ? 'up' : 'down')}, x >= 0 ? '▲' : '▼'), (Math.abs(x) * 100).toFixed(d) + '%'); }
const isDark = () => { const t = document.documentElement.dataset.theme; return t ? t === 'dark' : matchMedia('(prefers-color-scheme: dark)').matches; };

/* 單色序列色階（藍，分數越高越醒目；暗色模式反向取步階） */
const RAMP_L = ['#cde2fb', '#b7d3f6', '#9ec5f4', '#86b6ef', '#6da7ec', '#5598e7', '#3987e5', '#2a78d6', '#256abf', '#1c5cab', '#184f95', '#104281', '#0d366b'];
const RAMP_D = ['#0d366b', '#104281', '#184f95', '#1c5cab', '#256abf', '#2a78d6', '#3987e5', '#5598e7', '#6da7ec', '#86b6ef'];
const hex2 = c => [1, 3, 5].map(i => parseInt(c.slice(i, i + 2), 16));
function ramp(t) {
  const R = isDark() ? RAMP_D : RAMP_L; t = Math.max(0, Math.min(1, t)); const x = t * (R.length - 1), i = Math.floor(x), f = x - i;
  const a = hex2(R[i]), b = hex2(R[Math.min(i + 1, R.length - 1)]); const c = a.map((v, k) => Math.round(v + (b[k] - v) * f));
  return {bg: `rgb(${c})`, fg: (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) > 150 ? '#0b0b0b' : '#ffffff'};
}

/* ---------- 浮動提示 ---------- */
const tip = document.getElementById('tip');
function showTip(text, x, y) { tip.textContent = text; tip.classList.add('on'); const w = tip.offsetWidth, hh = tip.offsetHeight; tip.style.left = Math.max(6, Math.min(innerWidth - w - 6, x + 12)) + 'px'; tip.style.top = Math.max(6, Math.min(innerHeight - hh - 6, y + 14)) + 'px'; }
const hideTip = () => tip.classList.remove('on');
document.addEventListener('pointerover', e => { const t = e.target.closest && e.target.closest('[data-tip]'); if (t) showTip(t.dataset.tip, e.clientX, e.clientY); });
document.addEventListener('pointermove', e => { const t = e.target.closest && e.target.closest('[data-tip]'); if (t) showTip(t.dataset.tip, e.clientX, e.clientY); else if (!e.target.closest('[data-hover]')) hideTip(); });
document.addEventListener('pointerout', e => { if (e.target.closest && e.target.closest('[data-tip]')) hideTip(); });

/* ---------- 圖表元件 ---------- */
function spark(vals, w = 96, hh = 28) {
  const v = (vals || []).filter(isNum); if (v.length < 2) return h('span');
  const lo = Math.min(...v), hi = Math.max(...v), sp = hi - lo || 1, up = v[v.length - 1] >= v[0];
  const pts = v.map((y, i) => [2 + i * (w - 6) / (v.length - 1), 3 + (hh - 6) * (1 - (y - lo) / sp)]);
  const st = `stroke:var(${up ? '--up' : '--down'})`;
  return s('svg', {width: w, height: hh, viewBox: `0 0 ${w} ${hh}`, role: 'img', 'aria-label': up ? '走勢向上' : '走勢向下'},
    s('path', {d: 'M' + pts.map(p => p.map(n => n.toFixed(1)).join(',')).join('L'), fill: 'none', 'stroke-width': 2, 'stroke-linejoin': 'round', 'stroke-linecap': 'round', style: st}),
    s('circle', {cx: pts.at(-1)[0], cy: pts.at(-1)[1], r: 3.5, style: `fill:var(${up ? '--up' : '--down'});stroke:var(--surface);stroke-width:2`}));
}
function rangeBar(price, bear, base, bull, full) {
  const lo = Math.min(bear, price) * 0.985, hi = Math.max(bull, price) * 1.015, pos = v => (v - lo) / (hi - lo) * 100, P = v => pos(v).toFixed(2) + '%';
  const marks = full ? [['現價', price], ['目標', base], ['bear', bear], ['bull', bull]] : [];
  const kept = []; for (const m of marks) { const ps = pos(m[1]); if (kept.every(k => Math.abs(k[2] - ps) > 12)) kept.push([m[0], m[1], ps]); }
  return h('div', {class: 'range' + (full ? ' full' : ''), tip: `bear ${px(bear)}｜現價 ${px(price)}｜base 目標 ${px(base)}｜bull ${px(bull)}`},
    h('div', {class: 'bar'}), h('div', {class: 'fill', style: `left:${P(bear)};width:calc(${P(bull)} - ${P(bear)})`}),
    h('div', {class: 'mk', style: `left:${P(bear)}`}), h('div', {class: 'mk', style: `left:${P(bull)}`}),
    h('div', {class: 'tg', style: `left:${P(base)}`}), h('div', {class: 'px', style: `left:${P(price)}`}),
    kept.map(k => h('span', {class: 'lb', style: `left:${k[2].toFixed(1)}%;transform:translateX(${k[2] < 14 ? '0' : k[2] > 86 ? '-100%' : '-50%'})`}, k[0] + ' ' + px(k[1]))));
}
function barRows(rows, wide, emph) { // [{label, v(0-100), text, tip}]；emph=true：≥80 標「強」（實色粗體）、<30 標「弱」（轉灰）
  return h('div', {class: 'bl'}, rows.map(r => { const hi = emph && (r.v || 0) >= 80, lo = emph && isNum(r.v) && r.v < 30;
    return h('div', {class: 'row' + (wide ? ' wide' : '') + (hi ? ' hi' : '') + (lo ? ' lo' : ''), tip: r.tip},
      h('span', null, r.label), h('div', {class: 'tr'}, h('div', {class: 'fi' + (emph && !hi ? ' mid' : ''), style: `width:${Math.max(0, Math.min(100, r.v || 0))}%`})), h('span', {class: 'v num'}, r.text ?? nz(r.v, 0), hi ? ' 強' : lo ? ' 弱' : '')); }));
}
function lineChart(series) {
  const c = series.c, n = c.length; if (n < 3) return h('p', {class: 'muted'}, '價格資料不足');
  const W = 560, H = 200, L = 46, R = 10, T = 10, B = 22; let lo = Math.min(...c), hi = Math.max(...c); const pad = (hi - lo) * .08 || 1; lo -= pad; hi += pad;
  const X = i => L + i * (W - L - R) / (n - 1), Y = v => T + (H - T - B) * (1 - (v - lo) / (hi - lo));
  const svg = s('svg', {viewBox: `0 0 ${W} ${H}`, width: '100%', role: 'img', 'aria-label': `近 ${n} 個交易日收盤價走勢`, 'data-hover': 1});
  for (let k = 0; k <= 3; k++) { const v = lo + (hi - lo) * k / 3, y = Y(v); svg.append(s('line', {x1: L, x2: W - R, y1: y, y2: y, style: 'stroke:var(--grid);stroke-width:1'}), s('text', {x: L - 6, y: y + 4, 'text-anchor': 'end'}, px(v))); }
  [0, Math.floor(n / 2), n - 1].forEach(i => svg.append(s('text', {x: X(i), y: H - 6, 'text-anchor': i === 0 ? 'start' : i === n - 1 ? 'end' : 'middle'}, dstr(series.d[i]).slice(2))));
  const d = c.map((v, i) => `${i ? 'L' : 'M'}${X(i).toFixed(1)},${Y(v).toFixed(1)}`).join('');
  svg.append(s('path', {d: d + `L${X(n - 1)},${H - B}L${X(0)},${H - B}Z`, style: 'fill:var(--accent);opacity:.1'}), s('path', {d, fill: 'none', 'stroke-width': 2, 'stroke-linejoin': 'round', style: 'stroke:var(--accent)'}));
  const cur = s('line', {y1: T, y2: H - B, style: 'stroke:var(--axis);stroke-width:1', visibility: 'hidden'}), dot = s('circle', {r: 4.5, style: 'fill:var(--accent);stroke:var(--surface);stroke-width:2', visibility: 'hidden'});
  svg.append(cur, dot, s('circle', {cx: X(n - 1), cy: Y(c[n - 1]), r: 4.5, style: 'fill:var(--accent);stroke:var(--surface);stroke-width:2'}));
  const hit = s('rect', {x: L, y: T, width: W - L - R, height: H - T - B, fill: 'transparent'});
  hit.addEventListener('pointermove', e => { const r = svg.getBoundingClientRect(), x = (e.clientX - r.left) / r.width * W, i = Math.max(0, Math.min(n - 1, Math.round((x - L) / (W - L - R) * (n - 1))));
    cur.setAttribute('x1', X(i)); cur.setAttribute('x2', X(i)); cur.setAttribute('visibility', 'visible'); dot.setAttribute('cx', X(i)); dot.setAttribute('cy', Y(c[i])); dot.setAttribute('visibility', 'visible'); showTip(`${dstr(series.d[i])}\n收盤 ${px(c[i])}`, e.clientX, e.clientY); });
  hit.addEventListener('pointerleave', () => { cur.setAttribute('visibility', 'hidden'); dot.setAttribute('visibility', 'hidden'); hideTip(); });
  svg.append(hit); return svg;
}

/* ---------- 側欄（點擊後的詳細資料） ---------- */
const dr = document.getElementById('dr'), ov = document.getElementById('ov'); let lastFocus = null;
function openDrawer(title, sub, body) {
  lastFocus = document.activeElement; dr.replaceChildren(h('div', {class: 'hd'}, h('div', null, h('h3', {id: 'drt'}, title), sub && h('div', {class: 'muted sm'}, sub)), h('button', {class: 'x', type: 'button', 'aria-label': '關閉', on: {click: closeDrawer}}, '×')), h('div', {class: 'bd'}, body));
  dr.classList.add('on'); ov.classList.add('on'); dr.scrollTop = 0; dr.querySelector('.x').focus();
}
function closeDrawer() { dr.classList.remove('on'); ov.classList.remove('on'); hideTip(); if (lastFocus) lastFocus.focus(); }
ov.addEventListener('click', closeDrawer); document.addEventListener('keydown', e => { if (e.key === 'Escape' && dr.classList.contains('on')) closeDrawer(); });
const sec = (t, ...k) => h('section', null, h('h4', null, t), ...k);
const links = arr => arr && arr.length ? h('ul', null, arr.map(n => h('li', null, safeUrl(n.link) ? h('a', {href: n.link, target: '_blank', rel: 'noopener noreferrer'}, n.title) : n.title, h('span', {class: 'muted sm'}, ` ｜${n.source}${isNum(n.sent) && n.sent >= .3 ? '｜關鍵字判斷偏正面' : isNum(n.sent) && n.sent <= -.3 ? '｜關鍵字判斷偏負面' : ''}`)))) : h('p', {class: 'muted sm'}, '近期無相關報導命中。');
const kpi = (l, x, tp) => h('div', {class: 'kpi', tip: tp}, h('div', {class: 'l'}, l), h('div', {class: 'x num'}, x));

function newsLinks(a) { return a && a.length ? h('ul', null, a.map(n => h('li', null, safeUrl(n.link) ? h('a', {href: n.link, target: '_blank', rel: 'noopener noreferrer'}, n.title) : n.title, h('span', {class: 'muted sm'}, ` ｜${n.source}`)))) : null; }
function whyBlock(p) {
  if (!p.why) return null;
  return h('section', {class: 'whybox'}, h('h4', null, '為什麼推薦（利多）'), h('p', {class: 'hl'}, p.headline),
    p.highlights && p.highlights.length ? h('div', {class: 'hlrow'}, p.highlights.map(x => h('span', {class: 'hl-chip'}, x.t))) : null,
    h('ul', {class: 'why'}, p.why.map(w => h('li', null, h('span', {class: 'badge'}, w.tag), ' ', w.t, w.news ? newsLinks([w.news]) : null))));
}
function riskBlock(p) {
  if (!p.risks_now && !p.risks_watch) return sec('主要風險', h('ul', null, (p.risks || []).map(r => h('li', null, r))));
  const item = x => h('li', null, x.t, newsLinks(x.news));
  return h('div', {style: 'display:grid;gap:12px'},
    h('section', {class: 'riskbox now'}, h('h4', null, h('span', {class: 'tagx now'}, '已出現'), ' 目前資料已經顯示的狀況'), h('ul', null, p.risks_now.length ? p.risks_now.map(item) : [h('li', {class: 'muted'}, '目前資料未顯示明顯警訊。')])),
    h('section', {class: 'riskbox watch'}, h('h4', null, h('span', {class: 'tagx watch'}, '尚未發生'), ' 未來需留意：只是可能發生的事件，列出供追蹤', p.theme ? '（若發生，代表前瞻論點不成立）' : ''), h('ul', null, p.risks_watch.map(item))));
}

function stockDrawer(p) {
  const t = p.target, f = p.fin, k = p.tech;
  const body = [
    h('div', null, h('div', {style: 'display:flex;align-items:baseline;gap:10px;flex-wrap:wrap'}, h('span', {class: 'big', style: 'font-size:26px;font-weight:700'}, px(p.price)), h('span', {class: 'muted'}, p.ccy || ''), h('span', null, '目標 ', h('b', {class: 'num'}, px(t.base)), ' ', delta(t.upside))),
      h('div', {style: 'margin-top:6px'}, rangeBar(p.price, t.bear, t.base, t.bull, true))),
    h('div', {class: 'sm', style: 'display:flex;gap:6px;flex-wrap:wrap;margin-top:-6px'},
      h('span', {class: 'badge' + (t.confidence === '低' ? ' lo' : '')}, '估值信心 ' + t.confidence), t.capped && h('span', {class: 'badge lo', tip: `模型原始上檔 ${pct(t.raw_upside, 0)}，已套用上限`}, '已套用上檔上限'),
      p.relaxed && h('span', {class: 'badge lo', tip: '各方法對幅度看法分歧，但方向一致（最保守方法仍高於現價）'}, '幅度分歧・方向一致'), p.also_main && h('span', {class: 'badge'}, '亦入選主榜'),
      h('span', {class: 'badge'}, `風險報酬比 ${nz(t.rr, 2)}`), h('span', {class: 'badge'}, `參考停損 ${px(t.stop)}`), h('span', {class: 'badge', tip: '機率加權期望值（bull/base/bear）'}, `期望值 ${px(t.ev)}（${pct(t.ev_upside)}）`)),
    whyBlock(p),
    sec('近 120 個交易日收盤價', lineChart(p.series)),
    sec('估值方法（隱含上檔，點擊看計算）', h('div', {style: 'display:grid;gap:6px'}, p.methods.map(m => {
      const w = Math.min(50, Math.abs(m.upside || 0) * 50), pos = (m.upside || 0) >= 0;
      return h('details', null, h('summary', null, h('span', {style: 'display:inline-grid;grid-template-columns:132px 1fr 92px;gap:8px;align-items:center;width:calc(100% - 22px);vertical-align:middle'},
        h('span', {style: 'white-space:nowrap'}, `${m.label} ${Math.round(m.weight * 100)}%`), h('span', {class: 'dv'}, h('i', {class: 'zero'}), h('i', {class: 'b', style: `${pos ? 'left:50%' : `right:50%`};width:${w}%;background:var(${pos ? '--up' : '--down'});border-radius:${pos ? '0 4px 4px 0' : '4px 0 0 4px'}`})), h('span', {class: 'num', style: 'text-align:right;white-space:nowrap'}, `${px(m.value)} ${pct(m.upside, 0)}`))),
        h('p', {class: 'p'}, m.detail + `（隱含 ${pct(m.upside)}）`)); })),
      p.dropped.length ? h('p', {class: 'muted sm'}, '已剔除離群估值：' + p.dropped.join('、')) : null),
    sec('綜合評分（0–100；≥80 標「強」、<30 標「弱」）', barRows(FACTORS.map(([key, lb]) => ({label: lb, v: p.scores[key], text: nz(p.scores[key], 0)})), false, true)),
    sec('關鍵指標', h('div', {class: 'kpis'}, kpi('ROE', pct(f.roe, 1, false)), kpi('營業利益率', pct(f.op_margin, 1, false)), kpi('毛利率', pct(f.gross_margin, 1, false)), kpi('營收年增', pct(f.rev_growth)), kpi('獲利年增', pct(f.eps_growth)),
      kpi('預估本益比', nz(f.pe_fwd), `同業中位數 ${nz(t.peer_pe)}`), kpi('同業本益比', nz(t.peer_pe)), kpi('PEG', nz(f.peg, 2)), kpi('負債權益比', nz(f.de, 2)), kpi('Beta', nz(f.beta, 2)), kpi('市值', big(f.market_cap)), kpi('券商目標價', px(f.target_mean), `${f.n_analysts || '—'} 位分析師`),
      kpi('近3月相對大盤', pct(k.rel_3m)), kpi('距200日線', pct(k.dist200)), kpi('距52週高', pct(k.from_high)), kpi('年化波動', pct(k.vol_ann, 0, false)))),
    riskBlock(p),
    sec('相關報導', links(p.news)),
    h('details', null, h('summary', null, '研究摘要（完整文字）'), ...p.thesis.split('\n\n').map(x => h('p', {class: 'p'}, x.replace(/\*\*/g, '')))),
  ];
  openDrawer(`${p.ticker} ${p.name}`, `${p.sector_name}${p.theme_name ? '・' + p.theme_name : ''}｜資料日 ${p.asof}`, body);
}
function memberTable(mem) {
  return h('table', {class: 't'}, h('thead', null, h('tr', null, h('th', null, '代號'), h('th', null, '名稱'), h('th', {class: 'r'}, '綜合'), h('th', null, '結果'))),
    h('tbody', null, mem.map(m => h('tr', null, h('td', null, m.t), h('td', null, m.n), h('td', {class: 'r num'}, nz(m.c)), h('td', {class: m.s === '入選' ? '' : 'muted'}, m.s === '入選' ? '✅ 入選' : m.s)))));
}
function sectorDrawer(x, m) {
  openDrawer(x.name, `${MK[m]}產業排名第 ${x.rank}${x.focus ? '・今日重點產業' : ''}`, [
    h('div', null, h('span', {class: 'big', style: 'font-size:26px;font-weight:700'}, nz(x.score, 0)), h('span', {class: 'muted'}, ' / 100　'), (x.tags || []).map(t => h('span', {class: 'badge', style: 'margin-right:4px'}, t))),
    sec('分數組成（跨產業百分位）', barRows([{label: '新聞熱度', v: x.parts.news}, {label: '價格動能', v: x.parts.mom}, {label: '基本面成長', v: x.parts.fund}])),
    h('div', {class: 'kpis'}, kpi('近1月漲跌（成分股均）', pct(x.ret_1m)), kpi('相關新聞數', x.n_news), kpi('新聞情緒', x.sent > .1 ? '偏正面' : x.sent < -.1 ? '偏負面' : '中性')),
    sec('新聞證據', links(x.heads)), sec('成分標的與篩選結果', memberTable(x.members)), sec('產業主要風險', h('p', {class: 'p'}, x.risk))]);
}
function shortageDrawer(m, st, book) {
  const reasons = Object.entries(st.reasons || {}), mx = Math.max(1, ...reasons.map(r => r[1]));
  openDrawer(`${MK[m]}${book}：${st.n}／${st.N} 檔`, '合格標的不足時不放寬門檻、也不跨產業補位', [
    h('p', {class: 'p'}, `候選 ${st.candidates} 檔，通過 ${st.n} 檔。未入選原因：`),
    reasons.length ? barRows(reasons.map(([k, v]) => ({label: k, v: v / mx * 100, text: String(v)})), true) : h('p', {class: 'muted'}, '—'),
    h('div', {class: 'note'}, '名單偏少本身就是資訊：代表目前沒有標的同時具備品質與估值空間。')]);
}
function themeDrawer(x, e) {
  openDrawer(x.name, `證據級 ${x.tier}｜綜合 ${nz(x.score, 0)}｜${x.status}`, [
    h('p', {class: 'p'}, x.thesis),
    sec('分數組成', barRows([{label: '證據', v: x.parts.evidence}, {label: '低新聞覆蓋', v: x.parts.low_coverage}, {label: '基本面', v: x.parts.fundamental}, {label: '未擁擠', v: x.parts.not_crowded}])),
    h('div', {class: 'kpis'}, kpi('新聞覆蓋度', nz(x.coverage, 2), `近 48 小時命中 ${x.n_headlines} 則；1.0＝主流產業中位數；0 不等於沒人報導`), kpi('近6月相對大盤', pct(x.rel_6m)), kpi('200日線之上', pct(x.above200, 0, false))),
    sec('證據（研究假設，請自行查證原始來源）', h('ul', null, x.evidence.map(t => h('li', null, t)))),
    sec('論點不成立的條件（證偽）', h('ul', null, x.falsifiers.map(t => h('li', null, t)))),
    sec('相關報導', links(x.heads)), sec('成分標的與篩選結果', memberTable(x.members))]);
}

/* ---------- 卡片 ---------- */
function pickCard(p) {
  const t = p.target, top = p.rank === 1, hls = p.highlights || [];
  return h('button', {class: 'pick' + (top ? ' top' : ''), type: 'button', on: {click: () => stockDrawer(p)}, 'aria-label': `${p.ticker} ${p.name}，${top ? '首選，' : ''}點擊看詳細`, tip: p.headline},
    h('div', {class: 'r1'}, top ? h('span', {class: 'crown'}, '★ 首選') : h('span', {class: 'rk'}, p.rank), h('span', {class: 'tk'}, p.ticker.replace(/\.(TW|TWO)$/, '')), h('span', {class: 'nm'}, p.name), h('span', {class: 'up2 num', tip: `目標 ${px(t.base)} 相對現價 ${px(p.price)}`}, delta(t.upside))),
    h('div', {class: 'r2'}, hls.length ? hls.map(x => h('span', {class: 'hl-chip', tip: '亮點'}, x.t)) : h('span', {class: 'muted sm sc2'}, p.theme_name || p.sector_name),
      t.confidence === '低' && h('span', {class: 'badge lo', tip: '各估值方法差距大，信心偏低'}, '信心低'), t.capped && h('span', {class: 'badge lo', tip: '模型原始上檔更高，已套用上限'}, '上限'), p.relaxed && h('span', {class: 'badge lo', tip: '幅度分歧、方向一致'}, '分歧'), p.also_main && h('span', {class: 'badge'}, '亦主榜'),
      h('span', {class: 'muted sm num px2'}, `${px(p.price)} → ${px(t.base)}`)),
    h('div', {class: 'r3'}, h('div', {style: 'flex:1;min-width:0'}, rangeBar(p.price, t.bear, t.base, t.bull, null)), spark(p.series.c.slice(-60), 72, 24)));
}
function marketCol(m, picks, st, book) {
  const short = st.n < st.N;
  return h('div', null,
    h('div', {class: 'mh'}, h('h3', null, MK[m]), h('span', {class: 'cnt num' + (short ? ' short' : '')}, `${st.n}／${st.N} 檔`),
      short && h('button', {class: 'chip warn btn', type: 'button', on: {click: () => shortageDrawer(m, st, book)}, tip: '合格標的不足，不硬補滿；點擊看原因'}, '⚠ 合格不足・看原因')),
    picks.length ? picks.map(pickCard) : h('div', {class: 'empty'}, `今日${MK[m]}沒有標的通過全部篩選。`, h('br'), h('button', {class: 'chip btn', type: 'button', style: 'margin-top:8px', on: {click: () => shortageDrawer(m, st, book)}}, '查看未入選原因')));
}

/* ---------- 分頁 ---------- */
function indexTile(x) {
  return h('div', {class: 'tile'}, h('div', {class: 'lb'}, x.name), h('div', {class: 'v num'}, px(x.last)),
    h('div', {class: 'd'}, delta(x.ret_1d, 2), h('span', {class: 'muted'}, '日'), delta(x.ret_1m), h('span', {class: 'muted'}, '月')), spark(x.series, 150, 30));
}
function heatmap(m) {
  const list = S.snap.sectors[m]; const sc = list.map(x => x.score).filter(isNum), lo = Math.min(...sc), hi = Math.max(...sc);
  const R = ramp(0), R2 = ramp(1);
  const tile = x => { const c = ramp(hi > lo ? (x.score - lo) / (hi - lo) : .5);
    return h('button', {class: 'hx ' + (x.focus ? 'focus' : 'dim'), type: 'button', style: `background:${c.bg};color:${c.fg}`, tip: `${x.name}\n分數 ${nz(x.score, 0)}（新聞 ${x.parts.news}｜動能 ${x.parts.mom}｜成長 ${x.parts.fund}）\n${(x.tags || []).join('、')}`, on: {click: () => sectorDrawer(x, m)}, 'aria-label': `${x.focus ? '重點產業 ' : ''}${x.name} 分數 ${nz(x.score, 0)}`},
      h('span', {class: 'n'}, (x.focus ? '★ ' : '') + x.name), h('span', {class: 's num'}, nz(x.score, 0), x.focus && x.tags && x.tags[0] ? h('small', null, x.tags[0].split('（')[0]) : null)); };
  const foc = list.filter(x => x.focus), oth = list.filter(x => !x.focus);
  return h('div', null,
    h('div', {class: 'hm-title'}, h('span', {class: 'star'}, '★'), ' 重點產業', h('span', {class: 'muted sm'}, '　今日只從這裡選股')), h('div', {class: 'heat big'}, foc.map(tile)),
    oth.length ? h('div', {class: 'hm-title dim'}, '其他產業') : null, oth.length ? h('div', {class: 'heat small'}, oth.map(tile)) : null,
    h('div', {class: 'scale'}, '分數低', h('i', {style: `background:linear-gradient(90deg,${R.bg},${R2.bg})`}), '分數高（顏色＝同市場內相對高低）'));
}
function keyCell(label, main, sub, chips, onClick, muted) {
  return h('button', {class: 'key' + (muted ? ' off' : ''), type: 'button', on: {click: onClick}}, h('span', {class: 'kl'}, label), h('span', {class: 'km'}, main), h('span', {class: 'ks'}, sub), chips && chips.length ? h('span', {class: 'kc'}, chips.map(x => h('span', {class: 'hl-chip'}, x.t))) : null);
}
function keyStrip(sn) {
  const cells = ['TW', 'US'].map(m => { const p = sn.picks[m][0];
    return p ? keyCell(`${MK[m]}首選`, `${p.ticker.replace(/\.(TW|TWO)$/, '')} ${p.name}`, [delta(p.target.upside), h('span', {class: 'muted'}, ' 目標上檔')], p.highlights, () => stockDrawer(p))
      : keyCell(`${MK[m]}首選`, '今日無合格標的', [h('span', {class: 'muted'}, '不硬補滿；點看原因')], [], () => shortageDrawer(m, sn.shortage[m], ''), true); });
  const hot = ['TW', 'US'].map(m => [m, sn.sectors[m][0]]).filter(x => x[1]);
  if (hot.length) cells.push(h('div', {class: 'key'}, h('span', {class: 'kl'}, '最熱產業'), hot.map(([m, x]) => h('button', {type: 'button', class: 'kline', on: {click: () => sectorDrawer(x, m)}}, h('span', {class: 'muted'}, MK[m] + ' '), h('b', null, x.name), h('span', {class: 'num'}, ` ${nz(x.score, 0)}`)))));
  const th = sn.emerging && sn.emerging.themes[0];
  if (th) cells.push(keyCell('前瞻首選趨勢', th.name, [h('span', {class: 'num'}, `分數 ${nz(th.score, 0)}`), h('span', {class: 'muted'}, `　證據級${th.tier}・${th.status}`)], [], () => themeDrawer(th)));
  return h('div', {class: 'keys', role: 'list', 'aria-label': '今日重點'}, cells);
}
function overview() {
  const sn = S.snap, root = h('div');
  root.append(h('div', {class: 'sec', style: 'margin-top:4px'}, h('h2', null, '今日重點'), h('span', {class: 'sub'}, `資料日 ${sn.date}；點任一格看詳細`)), keyStrip(sn));
  root.append(h('div', {class: 'sec'}, h('h2', null, '市場')), h('div', {class: 'tiles'}, sn.indices.map(indexTile)));
  if (sn.summary) root.append(h('div', {class: 'note', style: 'margin-top:10px'}, 'AI 摘要：' + sn.summary));
  root.append(h('div', {class: 'sec'}, h('h2', null, '今日推薦'), h('span', {class: 'sub'}, '點卡片看估值、財務、風險與走勢；進度條＝bear｜現價｜目標｜bull')),
    h('div', {class: 'grid g2'}, marketCol('TW', sn.picks.TW, sn.shortage.TW, ''), marketCol('US', sn.picks.US, sn.shortage.US, '')));
  const seg = h('div', {class: 'seg', role: 'group', 'aria-label': '市場'}, ['TW', 'US'].map(m => h('button', {type: 'button', 'aria-pressed': S.hm === m, on: {click: () => { S.hm = m; render(); }}}, MK[m])));
  root.append(h('div', {class: 'sec'}, h('h2', null, '產業熱力圖'), seg, h('span', {class: 'sub'}, '點方塊看分數組成、新聞證據與成分股')), heatmap(S.hm));
  if (sn.emerging) { const top = sn.emerging.themes.slice(0, 3);
    root.append(h('div', {class: 'sec'}, h('h2', null, '前瞻雷達'), h('button', {class: 'chip btn', type: 'button', on: {click: () => go('emerging')}}, '進入前瞻專區 →')),
      h('div', {class: 'grid g2'}, h('div', {class: 'card'}, barRows(top.map(x => ({label: x.name, v: x.score, text: nz(x.score, 0), tip: `${x.name}｜證據級${x.tier}｜${x.status}`})), true)),
        h('div', {class: 'card sm'}, '主榜追逐「現在被關注」的產業；前瞻專區找「證據已具備、但新聞尚未充分報導、股價尚未被擠進去」的結構性趨勢。', h('br'), h('span', {class: 'muted'}, `今日前瞻推薦：台股 ${sn.emerging.shortage.TW.n} 檔、美股 ${sn.emerging.shortage.US.n} 檔`)))); }
  if (sn.bargain) { const all = ['TW', 'US'].flatMap(m => sn.bargain.picks[m]).slice(0, 6);
    root.append(h('div', {class: 'sec'}, h('h2', null, '便宜好貨'), h('button', {class: 'chip btn', type: 'button', on: {click: () => go('bargain')}}, '進入便宜好貨專區 →')),
      all.length ? h('div', {class: 'grid g2'}, ['TW', 'US'].map(m => h('div', null, h('div', {class: 'mh'}, h('h3', null, MK[m]), h('span', {class: 'cnt num'}, `${sn.bargain.shortage[m].n}／${sn.bargain.shortage[m].N} 檔`)), sn.bargain.picks[m].slice(0, 2).map(pickCard)))) : h('div', {class: 'empty'}, '今日沒有標的同時通過低基期、品質、成長與止跌四道關卡。')); }
  return root;
}
function radar(themes, topN) {
  const W = 560, H = 300, L = 44, R = 14, T = 26, B = 38;
  const mx = Math.max(.5, Math.ceil(Math.max(...themes.map(t => t.coverage || 0)) * 1.3 * 20) / 20);
  const ys = themes.map(t => t.parts.fundamental ?? 0), y0 = Math.max(0, Math.floor((Math.min(...ys) - 12) / 10) * 10), y1 = 100;
  const PADX = 22, X = v => L + PADX + (v / mx) * (W - L - R - PADX), Y = v => T + (H - T - B) * (1 - (v - y0) / (y1 - y0));
  const svg = s('svg', {viewBox: `0 0 ${W} ${H}`, width: '100%', role: 'img', 'aria-label': '趨勢雷達：橫軸新聞覆蓋度、縱軸基本面分數'});
  { const cx1 = X(Math.min(.5, mx)), yb = Y(Math.max(60, y0)); // 重點象限：新聞覆蓋度低於主流一半、基本面分數 ≥ 60
    svg.append(s('rect', {x: L, y: T, width: cx1 - L, height: Math.max(0, yb - T), style: 'fill:var(--accent);opacity:.09'}), s('text', {x: L + 4, y: T - 8, style: 'font-weight:650;fill:var(--ink)'}, '淺藍區＝冷門且基本面強（重點）')); }
  for (let v = y0; v <= y1; v += (y1 - y0) / 4) svg.append(s('line', {x1: L, x2: W - R, y1: Y(v), y2: Y(v), style: 'stroke:var(--grid)'}), s('text', {x: L - 6, y: Y(v) + 4, 'text-anchor': 'end'}, Math.round(v)));
  const step = mx <= .6 ? .1 : .25; for (let v = 0; v <= mx + 1e-9; v += step) svg.append(s('text', {x: X(v), y: H - 22, 'text-anchor': 'middle'}, v.toFixed(2)));
  if (mx >= 1) svg.append(s('line', {x1: X(1), x2: X(1), y1: T, y2: H - B, style: 'stroke:var(--axis)'}), s('text', {x: X(1) + 4, y: T + 10}, '主流產業水準 1.0'));
  svg.append(s('text', {x: (L + W - R) / 2, y: H - 6, 'text-anchor': 'middle'}, `新聞覆蓋度（越左越冷門；1.0＝主流產業中位數）→`), s('text', {x: 12, y: (T + H - B) / 2, transform: `rotate(-90 12 ${(T + H - B) / 2})`, 'text-anchor': 'middle'}, '基本面分數 ↑'));
  const sc = themes.map(t => t.score), lo = Math.min(...sc), hi = Math.max(...sc), placed = [];
  [...themes].sort((a, b) => (b.parts.fundamental ?? 0) - (a.parts.fundamental ?? 0)).forEach(t => {
    const top = t.rank <= topN, c = ramp(hi > lo ? (t.score - lo) / (hi - lo) : .5), r = t.tier === 'A' ? 15 : t.tier === 'B' ? 12 : 9, cy = Y(t.parts.fundamental ?? 0); let cx = X(t.coverage || 0);
    // 重疊時只沿橫向錯開（位置為示意；tooltip 與側欄仍顯示真值）
    for (let k = 1; k < 20 && placed.some(q => Math.hypot(q[0] - cx, q[1] - cy) < q[2] + r + 2); k++) cx = X(t.coverage || 0) + k * (r + 6);
    placed.push([cx, cy, r]);
    const g = s('g', {tabindex: 0, role: 'button', 'aria-label': `${t.rank}. ${t.name}`, style: 'cursor:pointer', 'data-tip': `${t.rank}. ${t.name}\n證據級${t.tier}｜綜合${nz(t.score, 0)}\n覆蓋度 ${nz(t.coverage, 2)}｜基本面 ${t.parts.fundamental}`},
      s('circle', {cx, cy, r: top ? r + 2 : r, style: `fill:${c.bg};stroke:${top ? 'var(--ink)' : 'var(--surface)'};stroke-width:${top ? 3 : 2};${top ? '' : 'opacity:.5'}`}), s('text', {x: cx, y: cy + 4, 'text-anchor': 'middle', style: `fill:${c.fg};font-weight:700;${top ? '' : 'opacity:.7'}`}, t.rank));
    g.addEventListener('click', () => themeDrawer(t)); g.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); themeDrawer(t); } }); svg.append(g); });
  return svg;
}
function emergingView() {
  const em = S.snap.emerging, root = h('div');
  if (!em) return h('div', {class: 'empty'}, '此日期沒有前瞻專區資料。');
  root.append(h('div', {class: 'note'}, '找「結構性證據已具備、但新聞尚未充分報導、股價尚未被擠進去」的趨勢。「確定」只指驅動力（人口、法規時程、成本限制、已簽約產能）有可查證證據，', h('b', null, '不代表股價必漲'), '；每個趨勢都附證偽條件。趨勢清單為人工研究假設，請自行查證。',
    h('div', {class: 'muted sm', style: 'margin-top:4px'}, `新聞覆蓋度＝近48小時命中數÷主流產業中位數（本次掃描 ${em.n_scanned} 則標題；0 只代表沒命中，不等於沒人報導）。證據級 A＝不可逆驅動力、B＝採用率/成本曲線有資料、C＝商業化未定。`)));
  root.append(h('div', {class: 'sec'}, h('h2', null, '趨勢雷達'), h('span', {class: 'sub'}, '★／粗框＝前 4 名重點趨勢（其餘淡化）；氣泡數字＝排名、大小＝證據級、深淺＝綜合分；愈靠左上愈冷門且基本面愈強；點氣泡或右側列看詳細')),
    h('div', {class: 'grid g2'}, h('div', {class: 'card'}, radar(em.themes, em.top_n || 4)),
      h('div', {class: 'card'}, h('div', {class: 'bl'}, em.themes.map(t => h('button', {type: 'button', class: 'row wide' + (t.rank <= (em.top_n || 4) ? ' hot' : ''), style: 'text-align:left', on: {click: () => themeDrawer(t)}, tip: `證據${t.parts.evidence}｜低覆蓋${t.parts.low_coverage}｜基本面${t.parts.fundamental}｜未擁擠${t.parts.not_crowded}`},
        h('span', {style: 'line-height:1.3'}, `${t.rank <= (em.top_n || 4) ? '★ ' : ''}${t.rank}. ${t.name}`), h('div', {class: 'tr'}, h('div', {class: 'fi', style: `width:${t.score}%`})), h('span', {class: 'v num'}, `${nz(t.score, 0)}·${t.tier}`)))))));
  root.append(h('div', {class: 'sec'}, h('h2', null, '前瞻推薦'), h('span', {class: 'sub'}, '門檻比主榜寬，但仍需品質、上檔與估值一致性；合格不足就照實少選')),
    h('div', {class: 'grid g2'}, marketCol('TW', em.picks.TW, em.shortage.TW, '（前瞻）'), marketCol('US', em.picks.US, em.shortage.US, '（前瞻）')));
  if (em.clues && em.clues.length) root.append(h('div', {class: 'sec'}, h('h2', null, 'AI 新興線索'), h('span', {class: 'sub'}, '未經驗證，不在推薦中')), h('div', {class: 'card'}, h('ul', null, em.clues.map(c => h('li', null, h('b', null, c.topic), '：' + c.why)))));
  return root;
}
function bargainView() {
  const ba = S.snap.bargain, root = h('div');
  if (!ba) return h('div', {class: 'empty'}, '此日期沒有便宜好貨專區資料。');
  const r = ba.rules;
  root.append(h('div', {class: 'note'}, '找「股價已被打到低基期，但公司沒有壞掉、獲利有轉強條件」的標的。', h('b', null, '便宜不等於好貨'), '：股價低常是因為基本面惡化（價值陷阱），所以要同時通過四道關卡，且不保證反彈。',
    h('div', {class: 'gates'}, [['① 低基期', `距52週高回檔 ≥ ${pct(r.drawdown, 0, false)}、位於區間下 ${pct(r.pos52, 0, false)}`], ['② 便宜', `目標價上檔 ≥ ${pct(r.upside, 0, false)}`], ['③ 好貨', `品質分 ≥ ${r.quality}、獲利為正、營收未衰退`], ['④ 有轉機', `成長分 ≥ ${r.growth}、自20日低點回升 ≥ ${pct(r.bounce, 0, false)}`]].map(([a, b]) => h('span', {class: 'gate'}, h('b', null, a), ' ', b)))));
  root.append(h('div', {class: 'sec'}, h('h2', null, '便宜好貨推薦'), h('span', {class: 'sub'}, '依「便宜、品質、爆發、轉機」加權排序；合格不足就照實少選；點卡片看估值、風險與走勢')),
    h('div', {class: 'grid g2'}, marketCol('TW', ba.picks.TW, ba.shortage.TW, '（便宜好貨）'), marketCol('US', ba.picks.US, ba.shortage.US, '（便宜好貨）')));
  const all = ['TW', 'US'].flatMap(m => ba.picks[m]);
  if (all.length) root.append(h('div', {class: 'sec'}, h('h2', null, '低基期位置'), h('span', {class: 'sub'}, '橫軸＝距52週高點回檔幅度，縱軸＝目標價上檔；愈右上愈「便宜」；點圓點看詳細')), h('div', {class: 'card'}, h('div', {style: 'max-width:680px'}, scatterBargain(all))));
  return root;
}
function scatterBargain(list) {
  const W = 560, H = 260, L = 44, R = 14, T = 14, B = 34, xs = list.map(p => -p.bargain.facts.from_high), ys = list.map(p => p.target.upside);
  const xm = Math.max(.5, ...xs) * 1.05, ym = Math.max(.4, ...ys) * 1.1, X = v => L + (W - L - R) * v / xm, Y = v => H - B - (H - T - B) * v / ym;
  const svg = s('svg', {viewBox: `0 0 ${W} ${H}`, width: '100%', role: 'img', 'aria-label': '低基期位置與目標價上檔'});
  for (let i = 0; i <= 4; i++) { const v = ym * i / 4; svg.append(s('line', {x1: L, x2: W - R, y1: Y(v), y2: Y(v), style: 'stroke:var(--grid)'}), s('text', {x: L - 5, y: Y(v) + 4, 'text-anchor': 'end'}, (v * 100).toFixed(0) + '%')); }
  for (let i = 0; i <= 5; i++) { const v = xm * i / 5; svg.append(s('text', {x: X(v), y: H - 14, 'text-anchor': 'middle'}, '-' + (v * 100).toFixed(0) + '%')); }
  svg.append(s('text', {x: (L + W - R) / 2, y: H - 1, 'text-anchor': 'middle'}, '距52週高點回檔'));
  list.forEach(p => { const top = p.rank === 1, c = s('circle', {cx: X(-p.bargain.facts.from_high), cy: Y(p.target.upside), r: top ? 9 : 6, tabindex: 0, role: 'button', style: `fill:var(--up);opacity:${top ? 1 : .75};cursor:pointer`, 'data-tip': `${p.ticker} ${p.name}\n距高點 ${pct(p.bargain.facts.from_high, 0)}｜上檔 ${pct(p.target.upside, 0)}｜總分 ${nz(p.bargain.score, 0)}`, 'aria-label': `${p.ticker} ${p.name}`});
    c.addEventListener('click', () => stockDrawer(p)); c.addEventListener('keydown', e => { if (e.key === 'Enter') stockDrawer(p); }); svg.append(c);
    svg.append(s('text', {x: X(-p.bargain.facts.from_high) + 9, y: Y(p.target.upside) - 8, style: 'font-size:10px'}, p.ticker.replace(/\.(TW|TWO)$/, ''))); });
  return svg;
}
function perfView() {
  const P = S.perf, root = h('div');
  const pos = (P && P.positions || []).filter(p => (p.book || 'main') === S.pm), summ = S.pm === 'main' ? (P && P.summary) : (P && P[S.pm] && P[S.pm].summary);
  const seg = h('div', {class: 'seg', role: 'group', 'aria-label': '範圍'}, [['main', '主榜'], ['emerging', '前瞻專區'], ['bargain', '便宜好貨']].map(([k, l]) => h('button', {type: 'button', 'aria-pressed': S.pm === k, on: {click: () => { S.pm = k; render(); }}}, l)));
  root.append(h('div', {class: 'sec'}, h('h2', null, '績效追蹤'), seg, h('span', {class: 'sub'}, '進場價＝推薦日收盤；報酬未計成本；基準＝台股加權／S&P 500')));
  if (!pos.length) { root.append(h('div', {class: 'empty'}, '尚無追蹤資料：推薦後需累積交易日才會出現績效。')); return root; }
  const done = h => summ && summ[h] && summ[h].n ? summ[h] : null, s5 = done('5'), s20 = done('20'), s60 = done('60');
  const last = pos.reduce((a, b) => (a.date > b.date ? a : b)).date;
  root.append(h('div', {class: 'kpis', style: 'margin-top:8px'}, kpi('追蹤中推薦', pos.length + ' 筆'), kpi('推薦日數', new Set(pos.map(p => p.date)).size), kpi('最新推薦日', last),
    kpi('5日平均超額', s5 ? pct(s5.avg_alpha) : '待累積', s5 ? `${s5.n} 筆到期；贏大盤 ${pct(s5.win_alpha, 0, false)}` : '需 5 個交易日'), kpi('20日平均超額', s20 ? pct(s20.avg_alpha) : '待累積', s20 ? `${s20.n} 筆到期；贏大盤 ${pct(s20.win_alpha, 0, false)}` : '需 20 個交易日'),
    kpi('60日平均超額', s60 ? pct(s60.avg_alpha) : '待累積', s60 ? `${s60.n} 筆到期` : '需 60 個交易日')));
  const coh = (P.cohorts || []).filter(c => (c.book || 'main') === S.pm && isNum(c.avg_alpha)).sort((a, b) => a.date < b.date ? -1 : 1).slice(-40);
  if (coh.length) { const W = 640, H = 210, L = 40, R = 8, T = 12, B = 30, mx = Math.max(.02, ...coh.map(c => Math.abs(c.avg_alpha))) * 1.1, Y = v => T + (H - T - B) / 2 * (1 - v / mx), bw = Math.min(24, (W - L - R) / coh.length - 4);
    const svg = s('svg', {viewBox: `0 0 ${W} ${H}`, width: '100%', role: 'img', 'aria-label': '各期推薦批次迄今平均超額報酬'});
    for (const v of [-mx, -mx / 2, 0, mx / 2, mx]) svg.append(s('line', {x1: L, x2: W - R, y1: Y(v), y2: Y(v), style: `stroke:var(${v === 0 ? '--axis' : '--grid'})`}), s('text', {x: L - 5, y: Y(v) + 4, 'text-anchor': 'end'}, (v * 100).toFixed(0) + '%'));
    coh.forEach((c, i) => { const x = L + 6 + i * (W - L - R - 6) / coh.length, y0 = Y(0), y1 = Y(c.avg_alpha), up = c.avg_alpha >= 0, top = Math.min(y0, y1), hgt = Math.max(1, Math.abs(y1 - y0)), r = Math.min(4, hgt / 2);
      const rect = s('path', {d: up ? `M${x},${y0}V${top + r}Q${x},${top} ${x + r},${top}H${x + bw - r}Q${x + bw},${top} ${x + bw},${top + r}V${y0}Z` : `M${x},${y0}V${top + hgt - r}Q${x},${top + hgt} ${x + r},${top + hgt}H${x + bw - r}Q${x + bw},${top + hgt} ${x + bw},${top + hgt - r}V${y0}Z`,
        style: `fill:var(${up ? '--up' : '--down'})`, 'data-tip': `${c.date}（${c.n} 檔，已持有 ${c.n_days} 交易日）\n平均報酬 ${pct(c.avg_ret)}\n平均超額 ${pct(c.avg_alpha)}`}); svg.append(rect);
      if (i === coh.length - 1) svg.append(s('text', {x: x + bw / 2, y: up ? top - 5 : top + hgt + 13, 'text-anchor': 'middle', style: 'font-weight:700;fill:var(--ink)'}, pct(c.avg_alpha)));
      if (coh.length <= 14 || i % Math.ceil(coh.length / 12) === 0) svg.append(s('text', {x: x + bw / 2, y: H - 10, 'text-anchor': 'middle'}, c.date.slice(5))); });
    root.append(h('div', {class: 'sec'}, h('h2', null, '各期推薦批次：平均超額報酬'), h('span', {class: 'sub'}, '▲紅＝贏大盤、▼藍＝輸大盤；滑過長條看詳細')), h('div', {class: 'card'}, h('div', {style: 'max-width:760px'}, svg))); }
  root.append(h('details', {class: 'card', style: 'margin-top:12px'}, h('summary', null, `全部推薦明細（${pos.length} 筆，點擊展開）`),
    h('div', {style: 'overflow-x:auto;margin-top:8px'}, h('table', {class: 't'}, h('thead', null, h('tr', null, ['推薦日', '代號', '名稱', '進場', '目標', '現價', '報酬', '超額', '天數', '狀態'].map((x, i) => h('th', {class: i > 2 && i < 9 ? 'r' : ''}, x)))),
      h('tbody', null, pos.slice().sort((a, b) => a.date < b.date ? 1 : -1).map(p => h('tr', null, h('td', null, p.date), h('td', null, p.ticker), h('td', null, p.name), h('td', {class: 'r num'}, px(p.entry)), h('td', {class: 'r num'}, px(p.target)), h('td', {class: 'r num'}, px(p.last_price)),
        h('td', {class: 'r num'}, delta(p.rlast)), h('td', {class: 'r num'}, delta(p.alast)), h('td', {class: 'r num'}, p.n_days), h('td', null, p.target_hit ? '達標' : p.stop_hit ? '觸停損' : '持有'))))))));
  return root;
}

/* ---------- 資料品質 ---------- */
const QL = {ok: ['✓', '正常'], warn: ['⚠', '警示'], bad: ['✕', '未發佈']};
const fmtTime = iso => iso ? new Date(iso).toLocaleString('zh-TW', {timeZone: 'Asia/Taipei', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false}) : '—';
function qualityDrawer(q, st) {
  const body = [];
  if (st && st.level === 'bad' && !st.published) body.push(h('div', {class: 'qbanner bad inline'}, `最近一次更新（${fmtTime(st.attempt_at)}）因資料品質不合格而未發佈；畫面上顯示的是上一份通過檢查的日誌，沒有被覆蓋。`));
  body.push(h('p', {class: 'p'}, '每次產生日誌前，系統都會自動檢查資料是否可信。輕微異常：照常發佈並顯示警示；嚴重異常：不發佈、不覆蓋上一份好的日誌。門檻設定在 config/params.json 的 quality 區塊。'));
  body.push(h('table', {class: 't'}, h('thead', null, h('tr', null, h('th', null, '檢查'), h('th', null, '結果'), h('th', null, '說明'))),
    h('tbody', null, (q.checks || []).map(c => h('tr', null, h('td', null, c.label), h('td', {style: 'white-space:nowrap;font-weight:650'}, `${QL[c.level][0]} ${QL[c.level][1] === '未發佈' ? '嚴重' : QL[c.level][1]}`), h('td', null, c.msg))))));
  openDrawer('資料品質檢查', `整體：${QL[q.level][0]} ${q.level === 'bad' ? '嚴重異常' : QL[q.level][1]}`, body);
}
function renderQuality() {
  const sn = S.snap, st = S.status && S.status.attempt_at ? S.status : null, q = sn && sn.quality;
  const qc = document.getElementById('qchip'), qb = document.getElementById('qbanner');
  const failed = st && st.level === 'bad' && !st.published && (!sn || !sn.generated_at || st.attempt_at > sn.generated_at);
  const level = failed ? 'bad' : q ? q.level : null;
  if (!level) { qc.hidden = true; qb.hidden = true; return; }
  qc.hidden = false; qc.className = 'chip btn q-' + level; qc.textContent = `資料品質 ${QL[level][0]} ${QL[level][1]}`; qc.onclick = () => qualityDrawer(failed ? st : q, st);
  const issues = (failed ? st.issues : q.issues) || [];
  if (level === 'ok') { qb.hidden = true; return; }
  qb.hidden = false; qb.className = 'qbanner ' + level;
  qb.replaceChildren(h('b', null, failed ? '⚠ 本次更新未發佈（資料品質不合格）' : '⚠ 資料品質警示'),
    failed ? ` 最近一次更新（${fmtTime(st.attempt_at)}）檢查未通過，畫面顯示的是 ${sn ? sn.date : '—'} 通過檢查的日誌。` : ' 本日誌已發佈，但下列項目請留意：', h('ul', null, issues.slice(0, 3).map(i => h('li', null, `${i.label}：${i.msg}`))),
    h('button', {type: 'button', class: 'linkbtn', on: {click: () => qualityDrawer(failed ? st : q, st)}}, '查看全部檢查'));
}

/* ---------- 框架 ---------- */
const TABS = [['overview', '總覽'], ['emerging', '前瞻專區'], ['bargain', '便宜好貨'], ['perf', '績效']];
function go(t) { S.tab = t; history.replaceState(null, '', '#' + t); render(); window.scrollTo(0, 0); }
function renderTabs() { const el = document.getElementById('tabs'); el.replaceChildren(...TABS.map(([k, l]) => h('button', {type: 'button', role: 'tab', 'aria-selected': S.tab === k, on: {click: () => go(k)}}, l))); }
function render() {
  const v = document.getElementById('view'); renderTabs(); hideTip();
  renderQuality();
  if (!S.snap) { v.replaceChildren(h('div', {class: 'empty'}, '尚無資料。第一份日誌會在排程首次執行後出現。')); return; }
  const sn = S.snap, ch = document.getElementById('srcchip');
  const uc = document.getElementById('updchip');
  if (sn.generated_at) { const d = new Date(sn.generated_at); uc.hidden = false; uc.textContent = '更新 ' + d.toLocaleString('zh-TW', {timeZone: 'Asia/Taipei', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false}); uc.title = `產生時間（台灣時間）。行情截至：台股 ${sn.basis ? sn.basis.TW : '—'} 收盤、美股 ${sn.basis ? sn.basis.US : '—'} 收盤` + ((sn.data_notes || []).length ? '\n' + sn.data_notes.join('\n') : ''); } else uc.hidden = true;
  ch.textContent = `新聞 ${sn.news.ok}/${sn.news.total} 來源`; ch.title = sn.news.failed.length ? '失敗：' + sn.news.failed.join('、') : '全部來源正常';
  document.getElementById('demo').hidden = sn.provider !== 'demo';
  const body = S.tab === 'emerging' ? emergingView() : S.tab === 'bargain' ? bargainView() : S.tab === 'perf' ? perfView() : overview();
  const d = sn.date, foot = h('div', {class: 'foot'}, h('a', {href: `journal/${d}.html`}, '完整研究日誌'), sn.emerging && h('a', {href: `emerging/${d}.html`}, '完整前瞻報告'), sn.bargain && h('a', {href: `bargain/${d}.html`}, '完整便宜好貨報告'), h('a', {href: 'archive.html'}, '歷史日誌'), h('a', {href: 'reviews.html'}, '檢討報告'), h('a', {href: 'methodology.html'}, '方法論'),
    h('span', null, `參數 v${sn.params_version}｜漲跌：紅▲漲、藍▼跌（色盲友善）｜僅供研究，不構成投資建議`));
  v.replaceChildren(body, foot);
}
async function getJSON(u) { const r = await fetch(u, {cache: 'no-cache'}); if (!r.ok) throw new Error(u + ' ' + r.status); return r.json(); }
async function loadDate(d) { S.date = d; S.snap = await getJSON(`data/snap/${d}.json`); render(); }
async function init() {
  const th = document.getElementById('theme'); try { const t = localStorage.getItem('ij-theme'); if (t) document.documentElement.dataset.theme = t; } catch (e) {}
  th.addEventListener('click', () => { const cur = document.documentElement.dataset.theme, nx = !cur ? (isDark() ? 'light' : 'dark') : cur === 'dark' ? 'light' : 'dark'; document.documentElement.dataset.theme = nx; try { localStorage.setItem('ij-theme', nx); } catch (e) {} render(); });
  const hash = location.hash.slice(1); if (TABS.some(t => t[0] === hash)) S.tab = hash;
  try { S.idx = (await getJSON('data/index.json')).dates; } catch (e) { document.getElementById('view').replaceChildren(h('div', {class: 'empty'}, location.protocol === 'file:' ? '請透過網址（http/https）開啟，直接開檔無法載入資料。' : '尚無資料。第一份日誌會在排程首次執行後出現。')); renderTabs(); return; }
  try { S.perf = await getJSON('data/performance.json'); } catch (e) { S.perf = null; }
  try { S.status = await getJSON('data/status.json'); } catch (e) { S.status = null; }
  const sel = document.getElementById('date'); sel.replaceChildren(...S.idx.map(x => h('option', {value: x.date}, x.date))); sel.addEventListener('change', () => loadDate(sel.value));
  if (S.idx.length) await loadDate(S.idx[0].date); else render();
}
addEventListener('hashchange', () => { const t = location.hash.slice(1); if (TABS.some(x => x[0] === t) && t !== S.tab) { S.tab = t; render(); } });
init();
})();
