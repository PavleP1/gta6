"""Generates all textures (PNG) and audio (WAV) into ../res. Run: python3 gen_assets.py  (needs pillow + numpy)"""
import os, random, wave
import numpy as np
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'res')
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(7)
R = random.Random(7)

def noise(img, amt):
    a = np.array(img).astype(np.int16)
    a += rng.integers(-amt, amt + 1, a.shape[:2])[:, :, None]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))

def save(img, name):
    img.convert('RGB').save(os.path.join(OUT, name + '.png'), optimize=True)

# ---------------------------------------------------------------- textures
def fac0():  # office, light wall + blue windows
    im = Image.new('RGB', (128, 128), (205, 205, 200)); d = ImageDraw.Draw(im)
    for r in range(4):
        for c in range(4):
            x, y = c * 32, r * 32
            for i in range(20):
                d.line([(x + 5, y + 6 + i), (x + 26, y + 6 + i)], fill=(50 + i * 2, 75 + i * 2, 100 + i * 2))
            d.rectangle([x + 4, y + 5, x + 27, y + 26], outline=(236, 236, 232))
            d.line([(x + 3, y + 28), (x + 28, y + 28)], fill=(150, 150, 146))
    save(noise(im, 6), 'fac0')

def fac1():  # brick apartment
    im = Image.new('RGB', (128, 128), (190, 120, 100)); d = ImageDraw.Draw(im)
    for r in range(0, 128, 4):
        off = (r // 4 % 2) * 4
        for x in range(-8, 128, 8):
            s = R.randint(-18, 18)
            d.rectangle([x + off, r, x + off + 6, r + 2], fill=(190 + s, 120 + s // 2, 100 + s // 2))
        d.line([(0, r + 3), (127, r + 3)], fill=(215, 205, 195))
    for r in range(4):
        for c in range(4):
            x, y = c * 32, r * 32
            d.rectangle([x + 8, y + 7, x + 24, y + 25], fill=(45, 55, 70), outline=(240, 238, 232), width=2)
            d.line([(x + 16, y + 7), (x + 16, y + 25)], fill=(240, 238, 232))
    save(noise(im, 5), 'fac1')

def fac2():  # concrete with ribbon windows
    im = Image.new('RGB', (128, 128), (180, 180, 178)); d = ImageDraw.Draw(im)
    for r in range(4):
        y = r * 32
        d.rectangle([0, y + 8, 127, y + 24], fill=(60, 85, 105))
        for x in range(0, 128, 16):
            d.line([(x, y + 8), (x, y + 24)], fill=(190, 190, 188), width=2)
        d.line([(0, y + 26), (127, y + 26)], fill=(130, 130, 128), width=2)
    save(noise(im, 7), 'fac2')

def fac3():  # dark curtain wall
    im = Image.new('RGB', (128, 128), (70, 100, 115)); d = ImageDraw.Draw(im)
    for r in range(8):
        for c in range(8):
            s = R.randint(-14, 24)
            d.rectangle([c * 16, r * 16, c * 16 + 15, r * 16 + 15], fill=(60 + s, 98 + s, 115 + s))
    for i in range(0, 128, 16):
        d.line([(i, 0), (i, 127)], fill=(200, 205, 210)); d.line([(0, i), (127, i)], fill=(200, 205, 210))
    save(noise(im, 4), 'fac3')

def roof():
    im = Image.new('RGB', (64, 64), (110, 110, 108)); d = ImageDraw.Draw(im)
    for _ in range(4):
        x, y = R.randint(2, 44), R.randint(2, 44)
        d.rectangle([x, y, x + R.randint(6, 14), y + R.randint(6, 14)], fill=(140, 140, 138))
    save(noise(im, 14), 'roof')

def road():  # 128px across 14 m; lane marks
    im = Image.new('RGB', (128, 128), (62, 62, 66)); d = ImageDraw.Draw(im)
    for _ in range(6):
        x, y = R.randint(0, 100), R.randint(0, 100)
        d.rectangle([x, y, x + R.randint(10, 28), y + R.randint(10, 28)], fill=(55, 55, 59))
    d.rectangle([7, 0, 9, 127], fill=(225, 225, 225)); d.rectangle([118, 0, 120, 127], fill=(225, 225, 225))
    d.rectangle([62, 0, 65, 64], fill=(235, 235, 205))
    save(noise(im, 8), 'road')

def dirt():
    im = Image.new('RGB', (128, 128), (146, 112, 74)); d = ImageDraw.Draw(im)
    d.rectangle([38, 0, 50, 127], fill=(120, 92, 60)); d.rectangle([78, 0, 90, 127], fill=(120, 92, 60))
    for y in range(128):
        w1, w2 = R.randint(8, 14), R.randint(8, 14)
        d.line([(0, y), (w1, y)], fill=(80, 110, 55)); d.line([(127 - w2, y), (127, y)], fill=(80, 110, 55))
    save(noise(im, 14), 'dirt')

def pave():
    im = Image.new('RGB', (64, 64), (170, 168, 162)); d = ImageDraw.Draw(im)
    for tx in range(2):
        for ty in range(2):
            s = R.randint(-10, 10)
            d.rectangle([tx * 32, ty * 32, tx * 32 + 31, ty * 32 + 31], fill=(170 + s, 168 + s, 162 + s))
    d.line([(0, 0), (63, 0)], fill=(135, 135, 130)); d.line([(0, 32), (63, 32)], fill=(135, 135, 130))
    d.line([(0, 0), (0, 63)], fill=(135, 135, 130)); d.line([(32, 0), (32, 63)], fill=(135, 135, 130))
    save(noise(im, 8), 'pave')

def grass():
    im = Image.new('RGB', (64, 64), (82, 125, 58)); d = ImageDraw.Draw(im)
    for _ in range(200): d.point((R.randint(0, 63), R.randint(0, 63)), fill=(60, 100, 45))
    for _ in range(150): d.point((R.randint(0, 63), R.randint(0, 63)), fill=(110, 150, 70))
    save(noise(im, 12), 'grass')

def bark():
    im = Image.new('RGB', (32, 32), (95, 65, 40)); d = ImageDraw.Draw(im)
    for x in range(0, 32, 3): d.line([(x, 0), (x, 31)], fill=(70 + R.randint(0, 20), 46, 28))
    save(noise(im, 10), 'bark')

def leaves():
    im = Image.new('RGB', (64, 64), (50, 110, 45)); d = ImageDraw.Draw(im)
    for _ in range(40):
        x, y = R.randint(0, 60), R.randint(0, 60); d.ellipse([x, y, x + 6, y + 5], fill=(85, 150, 60))
    for _ in range(40):
        x, y = R.randint(0, 60), R.randint(0, 60); d.ellipse([x, y, x + 5, y + 5], fill=(30, 80, 30))
    save(noise(im, 14), 'leaves')

def rock():
    im = Image.new('RGB', (64, 64), (125, 125, 122)); d = ImageDraw.Draw(im)
    for _ in range(4):
        x, y = R.randint(0, 60), R.randint(0, 60)
        d.line([(x, y), (x + R.randint(-14, 14), y + R.randint(4, 16))], fill=(80, 80, 78))
    save(noise(im, 20), 'rock')

def plaster():
    save(noise(Image.new('RGB', (64, 64), (225, 215, 190)), 8), 'plaster')

def tiles():
    im = Image.new('RGB', (64, 64), (150, 70, 55)); d = ImageDraw.Draw(im)
    for r in range(0, 64, 8):
        d.line([(0, r), (63, r)], fill=(105, 42, 32))
        for x in range((r // 8 % 2) * 8, 64, 16): d.line([(x, r), (x, r + 7)], fill=(120, 52, 40))
    save(noise(im, 8), 'tiles')

def glass():
    im = Image.new('RGB', (32, 32)); d = ImageDraw.Draw(im)
    for y in range(32): d.line([(0, y), (31, y)], fill=(35 + y, 50 + y, 65 + int(y * 1.4)))
    d.line([(6, 31), (22, 0)], fill=(120, 150, 170), width=2)
    save(im, 'glass')

def wheel():
    im = Image.new('RGB', (32, 32), (25, 25, 25)); d = ImageDraw.Draw(im)
    d.ellipse([4, 4, 28, 28], fill=(60, 60, 62)); d.ellipse([9, 9, 23, 23], fill=(160, 160, 165))
    d.ellipse([14, 14, 18, 18], fill=(70, 70, 72))
    save(noise(im, 5), 'wheel')

def paint():
    im = Image.new('RGB', (64, 64), (240, 240, 240)); d = ImageDraw.Draw(im)
    for y in range(64): d.line([(0, y), (63, y)], fill=(250 - y // 4, 250 - y // 4, 250 - y // 4))
    d.line([(0, 32), (63, 32)], fill=(185, 185, 185)); d.line([(0, 0), (0, 63)], fill=(200, 200, 200))
    save(noise(im, 3), 'paint')

for f in (fac0, fac1, fac2, fac3, roof, road, dirt, pave, grass, bark, leaves, rock, plaster, tiles, glass, wheel, paint):
    f()

# ---------------------------------------------------------------- audio
def wav(name, x, sr):
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.85
    with wave.open(os.path.join(OUT, name + '.wav'), 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr)
        w.writeframes((x * 32767).astype('<i2').tobytes())

SR = 11025
t1 = np.arange(SR) / SR  # exactly 1 s -> integer-Hz partials loop seamlessly
for i, f in enumerate((28, 55, 100)):
    s = sum(np.sin(2 * np.pi * k * f * t1 + k) / k ** 0.9 for k in range(1, 9))
    s *= 1 + 0.3 * np.sin(2 * np.pi * (f // 4 + 3) * t1)
    wav('eng%d' % i, s, SR)
s = sum(R.uniform(0.3, 1) * np.sin(2 * np.pi * R.randint(1500, 4500) * t1 + R.uniform(0, 6)) for _ in range(60))
wav('skid', s * (1 + 0.4 * np.sin(2 * np.pi * 7 * t1)), SR)
tc = np.arange(int(SR * 0.7)) / SR
cr = rng.normal(0, 1, tc.size) * np.exp(-tc * 8)
cr = np.convolve(cr, np.ones(4) / 4, 'same') + 1.2 * np.sin(2 * np.pi * 60 * tc) * np.exp(-tc * 10)
wav('crash', cr, SR)
ts = np.arange(SR * 2) / SR
fr = 700 + 600 * (1 - np.abs(((ts % 2) - 1)))       # triangle sweep 700..1300 Hz, loops
ph = 2 * np.pi * np.cumsum(fr) / SR
wav('siren', np.sin(ph) + 0.3 * np.sin(2 * ph) + 0.15 * np.sin(3 * ph), SR)

MS = 22050
def mtof(n): return 440.0 * 2 ** ((n - 69) / 12.0)

def track(name, bpm, prog, bars, lead):
    beat = 60.0 / bpm
    buf = np.zeros(int(MS * beat * 4 * bars) + MS)
    def add(t0, sig):
        i = int(t0 * MS); n = min(sig.size, buf.size - i)
        if n > 0: buf[i:i + n] += sig[:n]
    def tone(f, dur, decay, saw=0.6, vib=0.0):
        t = np.arange(int(dur * MS)) / MS
        ph = f * t + vib * np.sin(2 * np.pi * 5 * t) * 0.01
        w = saw * (2 * (ph % 1) - 1) + (1 - saw) * np.sin(2 * np.pi * ph)
        return w * np.exp(-t * decay) * np.minimum(1, t * 400)
    tk = np.arange(int(0.25 * MS)) / MS
    kick = np.sin(2 * np.pi * np.cumsum(45 + 90 * np.exp(-tk * 30)) / MS) * np.exp(-tk * 12)
    sn = rng.normal(0, 1, int(0.15 * MS)) * np.exp(-np.arange(int(0.15 * MS)) / MS * 25)
    hat = np.diff(rng.normal(0, 1, int(0.04 * MS) + 1)) * np.exp(-np.arange(int(0.04 * MS)) / MS * 90)
    for b in range(bars):
        root, chord = prog[b % len(prog)]
        t0 = b * 4 * beat
        for k in range(4):
            if k in (0, 2) or (lead and k == 3): add(t0 + k * beat, kick * 1.0)
            if k in (1, 3): add(t0 + k * beat, sn * 0.5)
        for k in range(8):
            add(t0 + k * beat / 2, hat * (0.5 if k % 2 else 0.3))
            add(t0 + k * beat / 2, tone(mtof(root - 12), beat / 2, 6, 0.8) * 0.5)
        for k in range(16):
            n = root + 12 + chord[k % len(chord)]
            add(t0 + k * beat / 4, tone(mtof(n), beat / 3, 9, 0.5) * 0.22)
        if lead:
            for k, st in enumerate((0, 2, 1, 2)):
                add(t0 + k * beat, tone(mtof(root + 24 + chord[st]), beat * 0.95, 2.5, 0.4, 1.0) * 0.28)
    wav(name, buf[:int(MS * beat * 4 * bars)], MS)

track('music0', 128, [(57, (0, 3, 7)), (53, (0, 4, 7)), (60, (0, 4, 7)), (55, (0, 4, 7))], 24, False)
track('music1', 150, [(52, (0, 3, 7)), (48, (0, 4, 7)), (50, (0, 4, 7)), (47, (0, 3, 7))], 28, True)
print('done')
