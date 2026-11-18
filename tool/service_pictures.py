#!/usr/bin/env python3
"""Draws the pictures used by this repository's README into docs/assets.

    python3 tool/service_pictures.py

Only the Python standard library is needed. The pictures are plain SVG with CSS and SMIL animation, so GitHub
shows them as they are, with no scripts and no outside files. The type is embedded from tool/fonts (small
subsets of IBM Plex Sans, IBM Plex Sans Condensed and JetBrains Mono, all under the SIL Open Font License).

The same file lives in every Premisave repository. It looks at the folder it sits in, works out which service
that is, and draws that service's pictures. The ports and the app setting names in it match AppConfig in
premisave_flutter_frontend, so change them together.
"""
import base64
import os
import textwrap

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'docs', 'assets')
FONTS = os.path.join(ROOT, 'tool', 'fonts')


# The look: a drafting sheet. Cool paper, green black ink, one amber for the thing that matters.
PAPER = '#F4F6F3'
PAPER2 = '#E8EDE9'
INK = '#0F2A22'
INK2 = '#4C655B'
RULE = '#BAC7C0'
DATA = '#1F6B55'      # the one colour the charts use
AMBER = '#E3A72F'
AMBER_INK = '#8A5F0A'  # amber that stays readable as text on paper
SHEET = '#0B2F25'      # the dark sheet, banner only
SHEET_LINE = '#DCE8E1'
SHEET_SOFT = '#7FA797'

SANS = "'PS Sans','IBM Plex Sans','Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif"
COND = "'PS Cond','IBM Plex Sans Condensed','Arial Narrow','Helvetica Neue',Arial,sans-serif"
MONO = "'PS Mono','JetBrains Mono',ui-monospace,SFMono-Regular,Menlo,Consolas,monospace"

FACES = {
    'cond7': ('PS Cond', 700, 'plex-cond-700.woff2'),
    'cond6': ('PS Cond', 600, 'plex-cond-600.woff2'),
    'sans4': ('PS Sans', 400, 'plex-400.woff2'),
    'sans6': ('PS Sans', 600, 'plex-600.woff2'),
    'mono4': ('PS Mono', 400, 'mono-400.woff2'),
    'mono6': ('PS Mono', 600, 'mono-600.woff2'),
}
_cache = {}


def font_css(*keys):
    out = []
    for k in keys:
        fam, weight, fname = FACES[k]
        if fname not in _cache:
            with open(os.path.join(FONTS, fname), 'rb') as f:
                _cache[fname] = base64.b64encode(f.read()).decode('ascii')
        out.append(f"@font-face{{font-family:'{fam}';font-weight:{weight};src:url(data:font/woff2;base64,{_cache[fname]}) format('woff2');}}")
    return ''.join(out)


BASE_CSS = f"""
text {{ font-family: {SANS}; fill: {INK}; }}
.cond {{ font-family: {COND}; font-weight: 700; }}
.mono {{ font-family: {MONO}; }}
.soft {{ fill: {INK2}; }}
@media (prefers-reduced-motion: reduce) {{ * {{ animation: none !important; }} }}
"""


def esc(s):
    return str(s).replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


def svg(width, height, body, css='', defs='', title='', desc='', fonts=('cond7', 'sans4', 'sans6')):
    head = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" width="{width}" height="{height}" role="img" aria-labelledby="t d">'
    meta = f'<title id="t">{esc(title)}</title><desc id="d">{esc(desc or title)}</desc>'
    return f'{head}{meta}<defs>{defs}</defs><style>{font_css(*fonts)}{BASE_CSS}{css}</style>{body}</svg>\n'


def save(name, content):
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, name), 'w', encoding='utf-8') as f:
        f.write(content)
    print('wrote', name, len(content) // 1024, 'KB')


def sheet(w, h, title, note=None):
    """A light drawing sheet: paper, an inset border, a heading and the rule under it."""
    out = (f'<rect width="{w}" height="{h}" fill="{PAPER}"/>'
           f'<rect x="8.5" y="8.5" width="{w - 17}" height="{h - 17}" fill="none" stroke="{INK}" stroke-width="1"/>'
           f'<text class="cond" x="32" y="52" font-size="26">{esc(title)}</text>')
    if note:
        out += f'<text class="soft" x="{w - 32}" y="50" text-anchor="end" font-size="13.5">{esc(note)}</text>'
    out += f'<line x1="32" y1="68" x2="{w - 32}" y2="68" stroke="{INK}" stroke-width="1.5"/>'
    return out


def num(x, y, n, r=11, fill=INK, ink=PAPER):
    """A numbered callout, the circle architects use to point at a detail."""
    return (f'<circle cx="{x}" cy="{y}" r="{r}" fill="{fill}"/>'
            f'<text x="{x}" y="{y + 4.4}" text-anchor="middle" font-size="12.5" font-weight="600" style="fill:{ink}" class="mono">{n}</text>')


def pc(t, cycle):
    return f'{min(max(t / cycle * 100, 0), 100):.3f}%'


def kf(name, cycle, frames):
    """Keyframes from [(seconds, 'declarations')], written as percentages of the cycle."""
    frames = sorted(frames, key=lambda f: f[0])
    return f'@keyframes {name} {{ ' + ' '.join(f'{pc(t, cycle)} {{ {s} }}' for t, s in frames) + ' }\n'


def lit_kf(name, cycle, base, on, wins, fade=0.25):
    """Keyframes that sit on `base` and switch to `on` during each (start, end) window, in seconds."""
    frames = []
    for s, e in wins:
        frames += [(s, base), (s + fade, on), (e, on), (e + fade, base)]
    if not frames or frames[0][0] > 0:
        frames.insert(0, (0, base))
    if frames[-1][0] < cycle:
        frames.append((cycle, base))
    return kf(name, cycle, [(min(t, cycle), s) for t, s in frames])


def band_kf(name, cycle, n, hold, pitch, axis='Y', glide=0.4):
    """A highlight band that rests on item after item (n items, `pitch` px apart) and then fades."""
    def at(k, op):
        return f'opacity:{op};transform:translate{axis}({k * pitch}px)'
    frames = [(0, at(0, 0)), (0.3, at(0, 1))]
    for k in range(n):
        frames += [(k * hold + (0.3 if k == 0 else 0), at(k, 1)), ((k + 1) * hold - glide, at(k, 1))]
    frames += [(n * hold - glide + 0.3, at(n - 1, 0)), (cycle, at(n - 1, 0))]
    return kf(name, cycle, frames)


def logo_mark(x, y, s):
    """The Premisave mark: a cream house beside an amber house, on a 1024 unit grid."""
    wins = ''.join(f'<rect class="lgw" style="--d:{1.1 + k * 0.12:.2f}s" x="{wx}" y="{wy}" width="48" height="48" fill="{SHEET}"/>'
                   for k, (wx, wy) in enumerate(((582, 541), (649, 541), (582, 608), (649, 608))))
    return (f'<g transform="translate({x} {y}) scale({s})">'
            f'<path class="lg1" d="M282 742V474L512 282V742H445V617A48 48 0 0 0 349 617V742Z" fill="{SHEET_LINE}"/>'
            f'<path class="lg2" d="M536 742V302L742 474V742Z" fill="{AMBER}"/>{wins}</g>')


def divider():
    w, h = 1200, 22
    ticks = ''.join(f'M{x} 11v{-7 if x % 50 == 0 else -4}' for x in range(8, 1193, 10))
    body = (f'<line x1="8" y1="11" x2="1192" y2="11" stroke="#8FA39A" stroke-width="1.2"/>'
            f'<path d="{ticks}" stroke="#8FA39A" stroke-width="1" fill="none"/>'
            f'<rect class="mk" x="595" y="5" width="10" height="12" fill="{AMBER}"/>')
    css = ('.mk { animation: slide 18s ease-in-out infinite alternate; } '
           '@keyframes slide { from { transform: translateX(-560px); } to { transform: translateX(560px); } }')
    save('divider.svg', svg(w, h, body, css, '', 'Divider', fonts=()))


def lanes_picture(fname, title, note, lanes, steps, foot, h, alt, top=168, gap=66, cycle=16.0):
    """A sequence diagram: lanes across, one step per row, a packet that crosses each arrow in turn.

    lanes: [(name, x)], steps: [(from, to, label, note, is_answer)]. A step whose from equals to is shown as work done
    inside one lane.
    """
    w = 1200
    css, drawn, marks = '.cur, .pk { opacity: 0; }\n', [], []
    lane_svg = ''
    for n, x in lanes:
        lane_svg += (f'<text class="cond" x="{x}" y="104" text-anchor="middle" font-size="20">{esc(n)}</text>'
                     f'<line x1="{x - 74}" y1="116" x2="{x + 74}" y2="116" stroke="{INK}" stroke-width="1.5"/>'
                     f'<line x1="{x}" y1="116" x2="{x}" y2="{h - 96}" stroke="{INK2}" stroke-width="1" stroke-dasharray="3 5"/>')
    total = len(steps) + 0.6
    for i, (a, b, label, sub, back) in enumerate(steps):
        y = top + i * gap
        xa, xb = lanes[a][1], lanes[b][1]
        if a == b:
            body = (f'<rect x="{xa - 6}" y="{y - 24}" width="12" height="54" fill="{PAPER}" stroke="{INK}" stroke-width="1.4"/>'
                    f'<path d="M{xa + 6} {y - 4}h24v22h-24" fill="none" stroke="{INK}" stroke-width="1.4"/><path d="M{xa + 11} {y + 14}l-5 4" stroke="{INK}" stroke-width="1.4"/>'
                    f'<text class="mono" x="{xa + 42}" y="{y + 2}" font-size="13" font-weight="600">{esc(label)}</text>'
                    f'<text x="{xa + 42}" y="{y + 21}" font-size="12.5" class="soft">{esc(sub)}</text>')
        else:
            d = 1 if xb > xa else -1
            mid = (xa + xb) / 2
            dash = ' stroke-dasharray="6 4"' if back else ''
            body = (f'<path d="M{xa + 8 * d} {y}H{xb - 12 * d}" stroke="{INK}" stroke-width="1.6"{dash}/>'
                    f'<path d="M{xb - 14 * d} {y - 6}L{xb - 2 * d} {y}L{xb - 14 * d} {y + 6}" fill="none" stroke="{INK}" stroke-width="1.6"/>'
                    f'<text class="mono" x="{mid}" y="{y - 10}" text-anchor="middle" font-size="13" font-weight="600">{esc(label)}</text>'
                    f'<text x="{mid}" y="{y + 22}" text-anchor="middle" font-size="12.5" class="soft">{esc(sub)}</text>')
        drawn.append(f'<g class="s{i}">{num(40, y, i + 1)}{body}</g>')
        on = i / total * 100
        css += f'.s{i} {{ animation: st{i} {cycle}s infinite; }}\n@keyframes st{i} {{ 0%,{on:.1f}% {{ opacity: 0; }} {on + 3.0:.1f}%,93% {{ opacity: 1; }} 98%,100% {{ opacity: 0; }} }}\n'
        nxt = (i + 1) / total * 100 if i + 1 < len(steps) else 96.0
        marks.append(f'<g class="cur" style="animation: cur{i} {cycle}s infinite">{num(40, y, i + 1, 11, AMBER, INK)}</g>')
        css += f'@keyframes cur{i} {{ 0%,{on:.2f}% {{ opacity: 0; }} {on + 1.5:.2f}%,{nxt:.2f}% {{ opacity: 1; }} {nxt + 1.5:.2f}%,100% {{ opacity: 0; }} }}\n'
        if a != b:
            d = 1 if xb > xa else -1
            dist = (xb - 12 * d) - (xa + 8 * d)
            marks.append(f'<rect class="pk" x="{xa + 8 * d - 4}" y="{y - 4}" width="8" height="8" fill="{AMBER}" stroke="{INK}" stroke-width="1" style="animation: apk{i} {cycle}s infinite"/>')
            span = 100 / total * 0.85
            css += (f'@keyframes apk{i} {{ 0%,{on + 1.5:.2f}% {{ opacity: 0; transform: translateX(0px); }} '
                    f'{on + 1.9:.2f}% {{ opacity: 1; transform: translateX(0px); animation-timing-function: cubic-bezier(.55,0,.35,1); }} '
                    f'{on + span:.2f}% {{ opacity: 1; transform: translateX({dist}px); }} {on + span + 0.4:.2f}%,100% {{ opacity: 0; transform: translateX({dist}px); }} }}\n')
    rule = f'<line x1="32" y1="{h - 78}" x2="{w - 32}" y2="{h - 78}" stroke="{INK}" stroke-width="1.5"/>'
    foots = ''
    for k, t in enumerate(foot):
        cls = '' if k == 0 else ' class="soft"'
        foots += f'<text x="32" y="{h - 50 + k * 22}" font-size="14.5"{cls}>{esc(t)}</text>'
    body = sheet(w, h, title, note) + lane_svg + ''.join(drawn) + ''.join(marks) + rule + foots
    save(fname, svg(w, h, body, css, '', title, alt, ('cond7', 'sans4', 'sans6', 'mono4', 'mono6')))


# ---------------------------------------------------------------------------------------- the services

# One entry per Premisave service. `key` is the setting the Flutter app reads to find it (see AppConfig).
SERVICES = [
    dict(slug='premisave_auth_service', name='Auth Service', short='auth-service', port=8080, key='BASE_URL', token='issues the token'),
    dict(slug='premisave-messenger-service', name='Messenger', short='messenger', port=8081, key='MESSENGER_URL', token='auth token'),
    dict(slug='premisave-listing-service', name='Listing Service', short='listing-service', port=8082, key='LISTING_URL', token='auth token'),
    dict(slug='premisave-wallet-service', name='Wallet Service', short='wallet-service', port=8084, key='WALLET_URL', token='auth token'),
    dict(slug='premisave-property-management-service', name='Property Management', short='property-service', port=8085, key='PROPERTY_URL', token='auth token'),
    dict(slug='premisave-c2b-hakikisha-service-m-pesa', name='C2B Hakikisha', short='c2b-hakikisha', port=8090, key='C2B_URL', token='client credentials'),
    dict(slug='premisave-developer-service', name='Developer Service', short='developer-service', port=8091, key='DEVELOPER_URL', token='its own login'),
]

# What each repository's pictures say. `flow` is (from, to, label, note, is_answer) with lane numbers.
PAGES = {
    'premisave-wallet-service': dict(
        tagline='One wallet per user, however the money moves.',
        sub='Deposits, transfers, disbursements and a commercial API, kept in one ledger.',
        chips=['Java 21', 'Spring Boot 4.1', 'MongoDB', 'Redis', 'JWT', 'OpenAPI'],
        connect=dict(screens=['wallet_screen.dart', 'transactions_screen.dart', 'wallet_forms.dart'], repo='wallet_repository.dart',
                     client='ApiClient', client_note='shares the sign in, refresh goes to auth', service_note='wallet, deposits, transfers, payouts'),
        lanes=[('The app', 130), ('Wallet service', 560), ('Payment provider', 1010)],
        flow=[(0, 1, 'GET /wallet', 'balance and currency of the signed in user', False),
              (1, 0, 'the wallet', 'created on first use with POST /wallet/create', True),
              (0, 1, 'POST /wallet/deposit', 'an amount and the M-Pesa phone number', False),
              (1, 2, 'start the charge', 'converted to the provider currency, tracked by a reference', False),
              (2, 1, 'callback', 'the provider reports the result later, matched by reference', True),
              (0, 1, 'GET /deposits/history', 'the app reads the final status', False),
              (0, 1, 'POST /wallet/transfer', 'wallet to wallet, by email or wallet id', False)],
        foot=['A retried callback can never credit twice: every transaction carries one idempotency reference.',
              'The app sends the auth-service token as a bearer token. The wallet checks the same signature.'],
        alt='The app asks for the wallet, starts a deposit, the wallet service starts the charge with the provider, the provider calls back and the app reads the final status.'),
    'premisave-c2b-hakikisha-service-m-pesa': dict(
        tagline='Tell the payer who they are about to pay.',
        sub='The provider side of the Safaricom Hakikisha name lookup.',
        chips=['Java 21', 'Spring Boot', 'MongoDB', 'M-Pesa', 'Basic auth'],
        connect=dict(screens=['name_lookup_screen.dart'], repo='c2b_repository.dart',
                     client='ServiceClient', client_note='its own credentials, kept in memory', service_note='token, name lookup, saved accounts'),
        lanes=[('Safaricom', 130), ('C2B Hakikisha', 560), ('Account source', 1010)],
        flow=[(0, 1, 'POST /oauth2/v1/generate', 'Basic client credentials ask for a token', False),
              (1, 0, 'access token and expires in', 'short lived, kept by the caller', True),
              (0, 1, 'POST /api/v1/c2b/hakikisha/name-lookup', 'an account number, the customer phone', False),
              (1, 0, 'the account name', 'answered from the local MongoDB copy', True),
              (1, 2, 'sync the accounts', 'at startup and every 5 minutes, from the wallet service', False),
              (2, 1, 'a page of accounts', 'the local copy is refreshed', True)],
        foot=['The Flutter app makes the same two calls from the staff tool, with the credentials typed in by the person.',
              'The credentials and the token stay in memory and are gone when the app closes.'],
        alt='Safaricom asks for a token and then for an account name, the service answers from its MongoDB copy, which a scheduled sync keeps fresh.'),
    'premisave-listing-service': dict(
        tagline='List it, find it, book it.',
        sub='Stays, leases, land and sales, with bookings paid through the wallet.',
        chips=['Java 21', 'Spring Boot 4.1', 'MongoDB', 'Redis', 'Resilience4j', 'OpenAPI'],
        connect=dict(screens=['explore_screen.dart', 'bookings_screen.dart', 'wishlists_screen.dart', 'owner_listings_screen.dart'], repo='listing_repository.dart',
                     client='ApiClient', client_note='shares the sign in, refresh goes to auth', service_note='listings, bookings, interests'),
        lanes=[('The app', 130), ('Listing service', 560), ('Wallet service', 1010)],
        flow=[(0, 1, 'GET /listings/search', 'text, category, price, dates and paging', False),
              (1, 0, 'a page of listings', 'the host profile is fetched live, never stored', True),
              (0, 1, 'POST /bookings', 'a listing, dates and guests', False),
              (1, 2, 'move the money', 'the guest pays the owner, or the wallet holds it', False),
              (2, 1, 'transfer done', 'by reference, so a retry cannot pay twice', True),
              (1, 0, 'the booking', 'an email goes to both people', True),
              (0, 1, 'POST /bookings/{id}/cancel', 'the refund follows the cancellation policy', False)],
        foot=['A double booking is blocked by an atomic guard, so two people cannot take the same nights.',
              'The app only asks. Every payment decision is made by the service.'],
        alt='The app searches listings, books one, the listing service moves the money through the wallet service and confirms the booking.'),
    'premisave-property-management-service': dict(
        tagline='From the first unit to the last notice.',
        sub='Units, leases, rent and maintenance, with rent paid through the wallet.',
        chips=['Java 21', 'Spring Boot 4.1', 'MongoDB', 'Redis', 'RabbitMQ', 'MapStruct'],
        connect=dict(screens=['owner_property_screen.dart', 'tenancy_screen.dart'], repo='property_repository.dart',
                     client='ApiClient', client_note='shares the sign in, refresh goes to auth', service_note='owners, tenants, rent, maintenance'),
        lanes=[('The app', 130), ('Property service', 560), ('Wallet service', 1010)],
        flow=[(0, 1, 'GET /api/v1/rent/balance/me', 'what the tenant owes, by lease and unit', False),
              (1, 0, 'the rent summary', 'arrears, credit and the next due date', True),
              (0, 1, 'POST /api/v1/rent/units/pay', 'a unit and an amount', False),
              (1, 2, 'wallet to wallet', 'debit the tenant, credit the owner', False),
              (2, 1, 'transfer done', 'one reference, so a retry cannot charge twice', True),
              (1, 0, 'the payment', 'an email goes out for the receipt', True),
              (0, 1, 'POST /api/v1/maintenance', 'a request from the tenant, the owner moves its status', False)],
        foot=['If the wallet service or the auth-service is down, the answer says so plainly instead of timing out.',
              'The same screens serve owners and tenants. The service decides what each person may see.'],
        alt='The app reads the tenant rent summary, pays rent, the property service moves the money through the wallet service and the receipt is emailed.'),
    'premisave-developer-service': dict(
        tagline='The front door for people who build on Premisave.',
        sub='Developer accounts, apps and API keys, and the public /v1 payments entry.',
        chips=['Java 21', 'Spring Boot 4.1', 'MongoDB', 'Mail', 'API keys', 'Rate limits'],
        connect=dict(screens=['developer_portal_screen.dart'], repo='developer_repository.dart',
                     client='ServiceClient', client_note='its own token in secure storage', service_note='accounts, apps, API keys'),
        lanes=[('Developer portal', 130), ('Developer service', 560), ('Wallet service', 1010)],
        flow=[(0, 1, 'POST /api/v1/auth/login', 'a developer account, not a Premisave account', False),
              (1, 0, 'a developer token', 'its own token, with no refresh', True),
              (0, 1, 'POST /api/v1/apps', 'a sandbox or production app', False),
              (0, 1, 'POST /api/v1/apps/{id}/keys', 'a key and a secret', False),
              (1, 0, 'the key and the secret', 'shown once, only a hash is kept', True),
              (0, 1, 'POST /v1/live/... with the key', 'from the merchant server, not the app', False),
              (1, 2, 'forward the payment call', 'after the key, the rate limit and the app rules', False)],
        foot=['The wallet is never exposed to merchants. Every call enters here first.',
              'The Flutter portal keeps the developer token apart from the person sign in.'],
        alt='A developer signs in, creates an app and an API key, and a merchant server calls the public entry, which forwards to the wallet service.'),
}

FLUTTER = 'premisave_flutter_frontend'


def this_service():
    here = os.path.basename(ROOT)
    for s in SERVICES:
        if s['slug'] == here:
            return s
    return None


def banner_for(s, p):
    w, h = 1280, 420
    g = 350
    lines, lit = [], []

    def draw(d, delay, width=1.6, colour=SHEET_LINE):
        lines.append(f'<path class="dr" pathLength="1" d="{d}" fill="none" stroke="{colour}" stroke-width="{width}" stroke-linejoin="miter" style="animation-delay:{delay:.2f}s"/>')

    draw(f'M650 {g}H1262', 0.2, 2)
    hatch = ''.join(f'M{x} {g}l-8 9' for x in range(660, 1262, 11))
    lines.append(f'<path d="{hatch}" fill="none" stroke="{SHEET_SOFT}" stroke-width="1" opacity=".55" class="fade" style="animation-delay:.9s"/>')
    draw(f'M702 {g}V282H858V{g}', 0.5)
    draw('M690 284L780 222L870 284', 0.7)
    draw(f'M766 {g}V325a14 14 0 0 1 28 0V{g}', 0.9)
    for i, x in enumerate((712, 820)):
        draw(f'M{x} 298h30v26h-30z', 1.0 + i * .1, 1.2)
        draw(f'M{x + 15} 298v26M{x} 311h30', 1.1 + i * .1, 0.8, SHEET_SOFT)
    draw(f'M870 {g}V112H1110V{g}', 0.8)
    draw('M862 112H1118', 1.0)
    draw('M940 112V94H1040V112', 1.1)
    draw(f'M954 {g}V312H1026V{g}M990 {g}V312', 1.3, 1.2)
    for f in range(6):
        for i in range(4):
            x = 882 + i * 58
            y = g - (f + 1) * 40 + 10
            if f == 0 and i in (1, 2):
                continue
            draw(f'M{x} {y}h34v22h-34z', 1.2 + (f * 4 + i) * 0.035, 1.1)
    draw(f'M1110 {g}V232H1230V{g}', 1.0)
    draw('M1104 232H1236', 1.2)
    for f in range(2):
        for i in range(2):
            x, y = 1124 + i * 52, g - (f + 1) * 44 + 12
            draw(f'M{x} {y}h38v24h-38z', 1.5 + (f * 2 + i) * 0.05, 1.1)
    glow = [(3, 1), (1, 0), (1, 3), (2, 1), (3, 2), (3, 0), (4, 3), (5, 1), (2, 3), (4, 0)]
    for k, (f, i) in enumerate(glow):
        x = 882 + i * 58
        y = g - (f + 1) * 40 + 10
        lit.append(f'<rect class="lit" x="{x}" y="{y}" width="34" height="22" fill="{AMBER}" style="--d:{2.6 + k * 0.28:.2f}s;--p:{4.4 + (k * 7) % 5 * 0.45:.2f}s"/>')
    lit.append(f'<rect class="lit" x="712" y="298" width="30" height="26" fill="{AMBER}" style="--d:3s;--p:5.6s"/>')
    lit.append(f'<rect class="lit" x="1124" y="274" width="38" height="24" fill="{AMBER}" style="--d:4.4s;--p:6.1s"/>')
    ax = ''
    for x, letter in ((780, 'A'), (990, 'B'), (1170, 'C')):
        ax += (f'<line x1="{x}" y1="86" x2="{x}" y2="388" stroke="{SHEET_SOFT}" stroke-width="1" stroke-dasharray="10 4 2 4" opacity=".6" class="march"/>'
               f'<circle cx="{x}" cy="68" r="13" fill="none" stroke="{SHEET_LINE}" stroke-width="1.2"/>'
               f'<text x="{x}" y="72.4" text-anchor="middle" font-size="12.5" font-weight="600" style="fill:{SHEET_LINE}" class="mono">{letter}</text>')
    css = f"""
.dr {{ stroke-dasharray: 1; animation: draw 1.5s cubic-bezier(.5,0,.2,1) both; }}
@keyframes draw {{ from {{ stroke-dashoffset: 1; }} to {{ stroke-dashoffset: 0; }} }}
.fade {{ animation: fadein 1s both; }} @keyframes fadein {{ from {{ opacity: 0; }} }}
.lit {{ opacity: .92; animation: on 1.4s var(--d) ease-out both, breathe var(--p) calc(var(--d) + 2.4s) ease-in-out infinite; }}
@keyframes on {{ from {{ opacity: 0; }} 40% {{ opacity: .35; }} to {{ opacity: .92; }} }}
@keyframes breathe {{ 0%, 100% {{ opacity: .92; }} 50% {{ opacity: .45; }} }}
.in1 {{ animation: slide .9s .1s cubic-bezier(.2,.7,.2,1) both; }} .in2 {{ animation: slide .9s .35s cubic-bezier(.2,.7,.2,1) both; }}
@keyframes slide {{ from {{ opacity: 0; transform: translateY(10px); }} }}
.lg1, .lg2 {{ animation: lgrise 1s cubic-bezier(.2,.8,.2,1) both; }} .lg2 {{ animation-delay: .25s; }}
@keyframes lgrise {{ from {{ opacity: 0; transform: translateY(90px); }} }}
.lgw {{ transform-box: fill-box; transform-origin: center; animation: pop .5s var(--d) cubic-bezier(.3,1.5,.5,1) both; }}
@keyframes pop {{ from {{ transform: scale(0); }} }}
.march {{ animation: march 3s linear infinite; }} @keyframes march {{ to {{ stroke-dashoffset: -20; }} }}
.scan {{ opacity: 0; animation: scan 12s 5.2s linear infinite; }}
@keyframes scan {{ 0% {{ opacity: 0; transform: translateX(640px); }} 4% {{ opacity: 1; }} 56% {{ opacity: 1; transform: translateX(1268px); }}
  60%, 100% {{ opacity: 0; transform: translateX(1268px); }} }}
.chip {{ animation: tick 8s calc(3.4s + var(--i) * 1.1s) infinite; }}
@keyframes tick {{ 0%, 100% {{ fill: {SHEET_SOFT}; }} 2%, 13% {{ fill: {AMBER}; }} 16% {{ fill: {SHEET_SOFT}; }} }}
text {{ fill: {SHEET_LINE}; }}
"""
    scan = (f'<g class="scan"><rect x="-96" y="92" width="96" height="280" fill="url(#trail)"/><rect x="-1" y="92" width="2" height="280" fill="{AMBER}"/>'
            f'<circle cx="0" cy="92" r="3.5" fill="{AMBER}"/><circle cx="0" cy="372" r="3.5" fill="{AMBER}"/></g>')
    trail = f'<linearGradient id="trail" x1="0" x2="1" y1="0" y2="0"><stop offset="0" stop-color="{AMBER}" stop-opacity="0"/><stop offset="1" stop-color="{AMBER}" stop-opacity=".3"/></linearGradient>'
    gridlines = ''.join(f'M{x} 0V{h}' for x in range(40, w, 40)) + ''.join(f'M0 {y}H{w}' for y in range(40, h, 40))
    name = s['name']
    size = min(84, int(560 / (0.47 * len(name))))
    px, chips = 66, ''
    for k, c in enumerate(p['chips']):
        chips += f'<text x="{px}" y="332" font-size="14.5" class="mono chip" style="fill:{SHEET_SOFT};--i:{k}">{esc(c)}</text>'
        px += len(c) * 8.7 + 11
        chips += f'<path d="M{px} 320v14" stroke="{SHEET_SOFT}" stroke-width="1" opacity=".6"/>'
        px += 13
    chips = chips.rsplit('<path', 1)[0]
    port = f'<text x="66" y="388" font-size="13.5" class="mono" style="fill:{SHEET_SOFT}">port {s["port"]}  |  app setting {s["key"]}</text>'
    body = (f'<rect width="{w}" height="{h}" fill="{SHEET}"/>'
            f'<path d="{gridlines}" stroke="#fff" stroke-width="1" opacity=".045" fill="none"/>'
            f'{ax}{"".join(lines)}{"".join(lit)}{scan}'
            f'<g class="in1">{logo_mark(60, 62 - 742 * 0.07, 0.07)}<text class="cond" x="118" y="92" font-size="30" letter-spacing="-.3" style="fill:{SHEET_SOFT}">Premisave</text>'
            f'<text class="cond" x="64" y="{150 + size * 0.62:.0f}" font-size="{size}" letter-spacing="-1.2">{esc(name)}</text></g>'
            f'<g class="in2"><text x="66" y="270" font-size="25" style="fill:{SHEET_LINE}">{esc(p["tagline"])}</text>'
            f'<text x="66" y="300" font-size="15.5" style="fill:{SHEET_SOFT}">{esc(p["sub"])}</text>{chips}{port}</g>')
    save('banner.svg', svg(w, h, body, css, trail, f'Premisave {name}',
                           f'The Premisave logo and the words {name} beside an architectural elevation drawing whose windows light up one by one.'))


def services_map(current):
    """All seven services in a column, the Flutter app on the left, and the one this repository holds picked out."""
    w, h = 1200, 590
    ax, ay, aw, ah = 36, 128, 250, 380
    rx, rw, rh, gap, top = 470, 694, 50, 8, 100
    rows = []
    css = ('.rise { animation: rise .7s cubic-bezier(.2,.8,.2,1) both; } @keyframes rise { from { opacity:0; transform: translateY(12px); } }\n'
           '.pk, .pl { opacity: 0; } .march { animation: march 1.4s linear infinite; } @keyframes march { to { stroke-dashoffset: -6; } }\n')
    cycle = 14.0
    n = len(SERVICES)
    for i, s in enumerate(SERVICES):
        y = top + i * (rh + gap)
        here = s['slug'] == current
        fill = INK if here else PAPER
        txt = PAPER if here else INK
        soft = SHEET_LINE if here else INK2
        acc = AMBER if here else DATA
        rows.append(f'<g class="rise" style="animation-delay:{i * 0.07:.2f}s">'
                    f'<rect x="{rx}" y="{y}" width="{rw}" height="{rh}" fill="{fill}" stroke="{INK}" stroke-width="{2 if here else 1.2}"/>'
                    f'<text class="cond" x="{rx + 18}" y="{y + 31}" font-size="21" style="fill:{txt}">{esc(s["short"])}</text>'
                    f'<text class="mono" x="{rx + 196}" y="{y + 31}" font-size="13" style="fill:{acc}">:{s["port"]}</text>'
                    f'<text class="mono" x="{rx + 262}" y="{y + 31}" font-size="12.5" style="fill:{acc}">{s["key"]}</text>'
                    f'<text x="{rx + 444}" y="{y + 31}" font-size="13" style="fill:{soft}">{esc(s["token"])}</text></g>')
    # the app
    app_fill = INK if current == FLUTTER else PAPER
    app_txt = PAPER if current == FLUTTER else INK
    app_soft = SHEET_LINE if current == FLUTTER else INK2
    app = (f'<g class="rise"><rect x="{ax}" y="{ay}" width="{aw}" height="{ah}" fill="{app_fill}" stroke="{INK}" stroke-width="2"/>'
           f'<rect x="{ax + 6}" y="{ay + 6}" width="{aw - 12}" height="{ah - 12}" fill="none" stroke="{INK if current != FLUTTER else SHEET_SOFT}" stroke-width=".8"/>'
           f'<text class="cond" x="{ax + 22}" y="{ay + 54}" font-size="27" style="fill:{app_txt}">Flutter app</text>'
           f'<text class="mono" x="{ax + 22}" y="{ay + 80}" font-size="12" style="fill:{AMBER if current == FLUTTER else DATA}">premisave_flutter_frontend</text>')
    for k, t in enumerate(['one app, six roles', 'screens call a repository', 'a repository calls a client', 'the client adds the token,', 'and a rejected token is', 'refreshed by the auth-service']):
        app += f'<text x="{ax + 22}" y="{ay + 124 + k * 26}" font-size="13.5" style="fill:{app_soft}">{t}</text>'
    app += f'<text class="mono" x="{ax + 22}" y="{ay + ah - 26}" font-size="12" style="fill:{app_soft}">lib/src/state/providers.dart</text></g>'
    # links and packets
    links, packets, pulses = [], [], []
    sx = ax + aw
    for i, s in enumerate(SERVICES):
        y = top + i * (rh + gap) + rh / 2
        here = s['slug'] == current
        links.append(f'<path d="M{sx} {ay + ah / 2}H{sx + 70}V{y}H{rx - 2}" fill="none" stroke="{AMBER_INK if here else INK}" stroke-width="{2 if here else 1.2}"/>'
                     f'<path d="M{rx - 10} {y - 5}L{rx - 1} {y}L{rx - 10} {y + 5}Z" fill="{AMBER_INK if here else INK}"/>')
        t0 = i * (cycle - 1.6) / n
        a, b = t0 / cycle * 100, (t0 + 1.4) / cycle * 100
        dx, dy = (rx - 14) - (sx + 70), y - (ay + ah / 2)
        # the packet runs out along the elbow: right, then vertical, then right
        css += (f'@keyframes go{i} {{ 0%,{a:.2f}% {{ opacity:0; transform: translate(0px,0px); }} {a + .3:.2f}% {{ opacity:1; transform: translate(0px,0px); }} '
                f'{a + (b - a) * .3:.2f}% {{ opacity:1; transform: translate(70px,0px); }} {a + (b - a) * .65:.2f}% {{ opacity:1; transform: translate(70px,{dy:.1f}px); }} '
                f'{b:.2f}% {{ opacity:1; transform: translate({dx + 70:.1f}px,{dy:.1f}px); }} {b + .5:.2f}%,100% {{ opacity:0; transform: translate({dx + 70:.1f}px,{dy:.1f}px); }} }}\n')
        packets.append(f'<rect class="pk" x="{sx + 2}" y="{ay + ah / 2 - 4}" width="8" height="8" fill="{AMBER}" stroke="{INK}" stroke-width="1" style="animation: go{i} {cycle}s infinite"/>')
        css += lit_kf(f'rw{i}', cycle, 'opacity:0', 'opacity:1', [(t0 + 1.0, t0 + 2.4)])
        pulses.append(f'<rect class="pl" x="{rx - 3}" y="{top + i * (rh + gap) - 3}" width="{rw + 6}" height="{rh + 6}" fill="none" stroke="{AMBER}" stroke-width="4" style="animation: rw{i} {cycle}s infinite"/>')
    heads = (f'<text class="soft mono" x="{rx + 18}" y="{top - 12}" font-size="12">service</text>'
             f'<text class="soft mono" x="{rx + 196}" y="{top - 12}" font-size="12">port</text>'
             f'<text class="soft mono" x="{rx + 262}" y="{top - 12}" font-size="12">app setting</text>'
             f'<text class="soft mono" x="{rx + 444}" y="{top - 12}" font-size="12">who it trusts</text>')
    foot_y = top + n * (rh + gap) + 18
    foot = (f'<text x="32" y="{h - 34}" font-size="14">Five services trust the token the auth-service issues. The developer service has its own login and the C2B service its own client credentials.</text>'
            f'<text x="32" y="{h - 14}" font-size="14" class="soft">Every address is a build time setting with a default, so the app finds a local backend with no configuration.</text>')
    me = next((s['name'] for s in SERVICES if s['slug'] == current), 'the Flutter app')
    body = (sheet(w, h, 'How the app reaches each service', 'amber marks this repository') + app + ''.join(links) + ''.join(pulses) + ''.join(rows) +
            ''.join(packets) + heads + foot)
    save('services.svg', svg(w, h, body, css, '', 'Services map',
                             f'The Flutter app on the left and the seven Premisave services on the right with their ports and the setting that holds each address. {me} is picked out.',
                             ('cond7', 'sans4', 'sans6', 'mono4', 'mono6')))


def connect_picture(s, p):
    """The four steps between a screen in the app and this service."""
    c = p['connect']
    w, h = 1200, 470
    nw, nh, ny = 200, 170, 120
    xs = [36 + i * 234 for i in range(5)]
    shown = c['screens'][:4]
    boxes = [
        ('Screens', 'lib/src/features', shown),
        ('Providers', 'lib/src/state', ['service_providers.dart', 'providers.dart']),
        ('Repository', 'lib/src/data', [c['repo']]),
        (c['client'], 'lib/src/core/network', [c['client_note']]),
        (s['short'], f'port {s["port"]}', [c['service_note']]),
    ]
    out = []
    for i, (title, path, notes) in enumerate(boxes):
        x = xs[i]
        solid = i == 4
        body = (f'<rect x="{x}" y="{ny}" width="{nw}" height="{nh}" fill="{INK if solid else PAPER}" stroke="{INK}" stroke-width="2"/>'
                + ('' if solid else f'<rect x="{x + 5}" y="{ny + 5}" width="{nw - 10}" height="{nh - 10}" fill="none" stroke="{INK}" stroke-width=".8"/>') +
                f'<text class="cond" x="{x + 18}" y="{ny + 50}" font-size="22" style="fill:{PAPER if solid else INK}">{esc(title)}</text>'
                f'<text class="mono" x="{x + 18}" y="{ny + 72}" font-size="11.5" style="fill:{AMBER if solid else DATA}">{esc(path)}</text>')
        row = 0
        for t in notes:
            mono = t.endswith('.dart')
            for label in ([t] if mono else textwrap.wrap(t, 25)):
                body += f'<text x="{x + 18}" y="{ny + 100 + row * 19}" font-size="{11.5 if mono else 12.5}" class="{"mono" if mono else ""}" style="fill:{SHEET_LINE if solid else INK2}">{esc(label)}</text>'
                row += 1
        out.append(f'<g class="rise" style="animation-delay:{i * 0.12:.2f}s">{body}{num(x + 18, ny, i + 1, 12, AMBER, INK)}</g>')
    cycle = 10.0
    css = ('.rise { animation: rise .7s cubic-bezier(.2,.8,.2,1) both; } @keyframes rise { from { opacity:0; transform: translateY(14px); } }\n'
           '.pk, .pl { opacity: 0; }\n')
    links, packets, pulses = [], [], []
    ymid = ny + nh / 2
    for i in range(4):
        x1, x2 = xs[i] + nw, xs[i + 1]
        dist = x2 - x1 - 16
        links.append(f'<path d="M{x1} {ymid}H{x2 - 2}" stroke="{INK}" stroke-width="1.5"/><path d="M{x2 - 9} {ymid - 5}L{x2 - 1} {ymid}L{x2 - 9} {ymid + 5}Z" fill="{INK}"/>')
        a, b = i / cycle * 100, (i + 1.0) / cycle * 100
        css += (f'@keyframes hp{i} {{ 0%,{a:.2f}% {{ opacity:0; transform: translateX(0px); }} {a + .4:.2f}% {{ opacity:1; transform: translateX(0px); animation-timing-function: cubic-bezier(.55,0,.35,1); }} '
                f'{b - .4:.2f}% {{ opacity:1; transform: translateX({dist:.1f}px); }} {b:.2f}%,100% {{ opacity:0; transform: translateX({dist:.1f}px); }} }}\n')
        packets.append(f'<rect class="pk" x="{x1 + 4}" y="{ymid - 4}" width="8" height="8" fill="{AMBER}" stroke="{INK}" stroke-width="1" style="animation: hp{i} {cycle}s infinite"/>')
    for n_ in range(5):
        css += lit_kf(f'bx{n_}', cycle, 'opacity:0', 'opacity:1', [(n_, n_ + 1.2)])
        pulses.append(f'<rect class="pl" x="{xs[n_] - 3}" y="{ny - 3}" width="{nw + 6}" height="{nh + 6}" fill="none" stroke="{AMBER}" stroke-width="6" style="animation: bx{n_} {cycle}s infinite"/>')
    token = {
        'auth token': ('The sign in the person already has', 'The client reads the saved access token, adds it as a bearer token and, if it expired,', 'asks the auth-service for a new one. The service checks the same signature.'),
        'issues the token': ('The sign in itself', 'This is where the token is made. The app asks it to sign in, refresh and sign out.', 'Every other service trusts what it signs.'),
        'client credentials': ('Not the person sign in', 'This service has its own client id and secret. The tool asks for them, trades them for a token,', 'keeps both in memory and never writes them to the device.'),
        'its own login': ('Not the person sign in', 'This service has its own accounts and tokens. The app keeps that token in secure storage under', 'a separate key and never sends it to any other service.'),
    }[s['token']]
    lines = [token[1], token[2]]
    foot = (f'<line x1="32" y1="{ny + nh + 44}" x2="{w - 32}" y2="{ny + nh + 44}" stroke="{INK}" stroke-width="1.2"/>'
            f'<text x="32" y="{ny + nh + 72}" font-size="15" font-weight="600">{esc(token[0])}</text>'
            f'<text x="32" y="{ny + nh + 96}" font-size="14" class="soft">{esc(lines[0])}</text>'
            f'<text x="32" y="{ny + nh + 116}" font-size="14" class="soft">{esc(lines[1])}</text>')
    body = sheet(w, h, 'Inside the Flutter app', f'one call, from a screen to {s["short"]}') + ''.join(links) + ''.join(pulses) + ''.join(out) + ''.join(packets) + foot
    save('connect.svg', svg(w, h, body, css, '', 'Inside the Flutter app',
                            f'A screen asks a provider, which asks the repository, which uses {c["client"]} to call the {s["short"]}.',
                            ('cond7', 'sans4', 'sans6', 'mono4', 'mono6')))


def flow_picture(s, p):
    h = 150 + len(p['flow']) * 62 + 120
    lanes_picture('flow.svg', f'{s["name"]}: a request, step by step', 'what the app asks and who answers', p['lanes'], p['flow'], p['foot'], h,
                  p['alt'], gap=62, cycle=2.4 * len(p['flow']) + 2)


def run():
    s = this_service()
    if s is None:
        # the Flutter app has its own pictures (tool/readme_assets.py), this file adds the services map to them
        services_map(FLUTTER)
        return
    p = PAGES.get(s['slug'])
    if p:
        divider()
        banner_for(s, p)
        flow_picture(s, p)
    services_map(s['slug'])
    if p:
        connect_picture(s, p)
    else:
        # the auth-service and the messenger already have their own banner and flow pictures
        fake = dict(connect=dict(screens=[], repo='', client='ApiClient', client_note='', service_note=''))
        connect_picture(s, {**fake, **CONNECT_OTHER[s['slug']]})


CONNECT_OTHER = {
    'premisave_auth_service': dict(connect=dict(screens=['sign_in_screen.dart', 'profile and account', 'security centre', 'admin screens'], repo='auth_repository.dart',
                                                client='ApiClient', client_note='tokens, refresh, errors', service_note='accounts, sessions, profiles')),
    'premisave-messenger-service': dict(connect=dict(screens=['chat_list_pane.dart', 'conversation_pane.dart', 'group_info_screen.dart', 'calls'], repo='messenger_repository.dart',
                                                     client='ApiClient', client_note='shares the sign in, refresh goes to auth', service_note='chats, presence, calls')),
}

if __name__ == '__main__':
    run()
