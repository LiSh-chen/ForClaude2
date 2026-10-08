// Renders svg/<id>.svg to generated/<id>.png with a transparent background, using the Chromium that
// Playwright ships. Usage:  node tools/render_svg.mjs [id ...]      (no ids = every svg/*.svg)
// A line "<!--DEFS-->" in an SVG is replaced with svg/_defs.inc (shared gradients and ink/wash filters).
import { readFileSync, readdirSync, writeFileSync, existsSync, mkdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const require = createRequire('/opt/node-tools/node_modules/');
let playwright;
for (const base of ['/opt/node-tools/node_modules/', '/opt/node22/lib/node_modules/', process.cwd() + '/']) {
  try { playwright = createRequire(base)('playwright'); break; } catch { /* try next */ }
}
if (!playwright) throw new Error('playwright not found');

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const svgDir = path.join(root, 'svg');
const outDir = path.join(root, 'generated');
mkdirSync(outDir, { recursive: true });
const defs = existsSync(path.join(svgDir, '_defs.inc')) ? readFileSync(path.join(svgDir, '_defs.inc'), 'utf8') : '';

const wanted = process.argv.slice(2);
const files = readdirSync(svgDir).filter(f => f.endsWith('.svg') && !f.startsWith('_'))
  .filter(f => wanted.length === 0 || wanted.includes(f.replace(/\.svg$/, '')));

const executablePath = ['/opt/pw-browsers/chromium-1194/chrome-linux/chrome'].find(existsSync);
const browser = await playwright.chromium.launch({ executablePath, args: ['--no-sandbox'] });
for (const file of files) {
  let svg = readFileSync(path.join(svgDir, file), 'utf8').replace('<!--DEFS-->', defs);
  const m = svg.match(/viewBox="0 0 (\d+) (\d+)"/);
  if (!m) throw new Error(`${file}: needs viewBox="0 0 W H"`);
  const [w, h] = [Number(m[1]), Number(m[2])];
  const page = await browser.newPage({ viewport: { width: w, height: h }, deviceScaleFactor: 1 });
  await page.setContent(`<!doctype html><html><body style="margin:0;background:transparent">${svg.replace('<svg', `<svg width="${w}" height="${h}"`)}</body></html>`);
  await page.screenshot({ path: path.join(outDir, file.replace(/\.svg$/, '.png')), omitBackground: true, clip: { x: 0, y: 0, width: w, height: h } });
  await page.close();
  console.log(`rendered ${file} -> ${w}x${h}`);
}
await browser.close();
