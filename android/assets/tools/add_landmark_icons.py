"""Puts a small emoji in front of every landmark card feature's tag in res/raw/landmark_facts.txt ("🍤 廟口天婦羅:...").
Emoji are limited to Unicode 11 (2018) or older. Run from android/: python3 assets/tools/add_landmark_icons.py (idempotent)."""
import sys
P = 'app/src/main/res/raw/landmark_facts.txt'
I = {
'taipei101': ['⚖️', '🎆', '⏫'], 'keelung': ['🍤', '🥪', '🏮'], 'jiufen': ['📜', '🚶', '🎬'], 'fulong': ['🏖️', '✨', '🚲'],
'sandiaojiao': ['⛪', '🗣️', '💥'], 'toucheng': ['🏁', '🔢', '⚓'], 'ncta': ['🔨', '🚤', '🎨'], 'suao': ['💧', '🎖️', '🥤'],
'qixingtan': ['⚪', '✈️', '🌅'], 'pinegarden': ['🍶', '🌲', '🔭'], 'lintienshan': ['🔥', '⚫', '🎞️'], 'ruisui': ['⏳', '🚚', '🧭'],
'yuli': ['🍜', '🌬️', '💎'], 'chishang': ['🌳', '🩺', '🌾'], 'guanshan': ['🚲', '🐛', '🚉'], 'luye': ['🎈', '⛰️', '🛫'],
'prehistory': ['🏺', '🌾', '⚰️'], 'taimali': ['🌅', '📜', '🎶'], 'shouka': ['🗾', '🛣️', '🚴'], 'checheng': ['💰', '👑', '🏅'],
'hengchun': ['🚪', '🏰', '🧱'], 'eluanbi': ['🔦', '⛵', '🛡️'], 'maobitou': ['🐱', '🌊', '👗'], 'nmmba': ['🐠', '🦈', '🐧'],
'fangliao': ['📜', '🚂', '✒️'], 'donggang': ['🐟', '🎣', '⛵'], 'pier2': ['🎇', '🌉', '🚂'], 'lotuspond': ['🐉', '🌸', '🙏'],
'qiaotou': ['🍦', '🌳'], 'anping': ['🐚', '🗼', '🏗️'], 'jingzijiao': ['🧂', '☀️', '📆'], 'budai': ['🏙️', '🐟', '📜'],
'peitian': ['🌳', '🎊', '👵'], 'shuishang': ['📍', '🌐', '🌞'], 'hinoki': ['🏘️', '🔨', '🌲'], 'xiluo': ['🚂', '💴', '🌉'],
'lukang': ['🌀', '🚶', '💧'], 'roundhouse': ['🔄', '🏭', '🏨'], 'gaomei': ['🌿', '🦀', '🌇'], 'dajia': ['🍙', '🎲', '🙇'],
'tongxiao': ['⚡', '♨️'], 'hsinchu': ['🏃', '🏮', '⛓️'], 'taoyuan': ['⛩️', '🏮', '🎬'], 'shihsanhang': ['⛵', '🏺', '🧭'],
'dihua': ['💘', '🏘️', '🧧'],
}
out, n, bad = [], 0, []
for ln in open(P, encoding='utf-8').read().split('\n'):
    if ln.startswith('#') or not ln.strip():
        out.append(ln); continue
    f = ln.split('|'); cid = f[0]; feats = f[1:]
    if cid not in I or len(I[cid]) != len(feats): bad.append(cid); out.append(ln); continue
    new = []
    for x, e in zip(feats, I[cid]):
        tag, fact = x.split(':', 1)
        first = tag.split(' ', 1)[0]
        if ' ' in tag and not any(c.isalnum() for c in first): new.append(x); continue
        new.append(f'{e} {tag}:{fact}'); n += 1
    out.append('|'.join([cid] + new))
open(P, 'w', encoding='utf-8').write('\n'.join(out))
print('icons added', n, 'problems', bad)
sys.exit(1 if bad else 0)
