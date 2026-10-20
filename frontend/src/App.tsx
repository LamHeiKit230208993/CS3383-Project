import { useMemo, useState } from 'react'

type IconName =
  | 'search'
  | 'home'
  | 'compare'
  | 'list'
  | 'bell'
  | 'heart'
  | 'sparkles'
  | 'arrow'
  | 'plus'
  | 'check'
  | 'trend'
  | 'budget'
  | 'globe'
  | 'contrast'
  | 'settings'
  | 'chevron'

function Icon({ name, size = 20 }: { name: IconName; size?: number }) {
  const paths: Record<IconName, React.ReactNode> = {
    search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
    home: <><path d="m3 10 9-7 9 7" /><path d="M5 9v11h14V9M9 20v-6h6v6" /></>,
    compare: <><path d="M7 3 3 7l4 4M3 7h14" /><path d="m17 21 4-4-4-4m4 4H7" /></>,
    list: <><path d="M8 6h13M8 12h13M8 18h13" /><path d="M3 6h.01M3 12h.01M3 18h.01" /></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></>,
    heart: <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.8-7.5 1.1-1.1a5.5 5.5 0 0 0-.1-7.8Z" />,
    sparkles: <><path d="m12 3-1.2 3.4L7.5 8l3.3 1.5L12 13l1.4-3.5L16.5 8l-3.1-1.6L12 3Z" /><path d="m5 13-.8 2.2L2 16l2.2.8L5 19l.8-2.2L8 16l-2.2-.8L5 13Zm14-2-.6 1.5-1.4.6 1.4.6L19 15l.6-1.3 1.4-.6-1.4-.6L19 11Z" /></>,
    arrow: <><path d="M5 12h14" /><path d="m15 8 4 4-4 4" /></>,
    plus: <><path d="M12 5v14M5 12h14" /></>,
    check: <path d="m5 12 4 4L19 6" />,
    trend: <><path d="m3 17 6-6 4 4 8-9" /><path d="M15 6h6v6" /></>,
    budget: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M16 13h2M3 9h18" /></>,
    globe: <><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18" /></>,
    contrast: <><circle cx="12" cy="12" r="9" /><path d="M12 3v18a9 9 0 0 0 0-18Z" /></>,
    settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4V21a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9A1.7 1.7 0 0 0 3 14H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1a1.7 1.7 0 0 0 1.9.3A1.7 1.7 0 0 0 10 3V2.8h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1Z" /></>,
    chevron: <path d="m9 18 6-6-6-6" />,
  }
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name]}
    </svg>
  )
}

const products = [
  { id: 1, name: 'Calbee Original Potato Chips', detail: '55g · Original flavour', category: 'Snacks', price: 10.9, old: 14.5, store: 'Wellcome', color: '#ffd968', emoji: '🥔', stores: 5, drop: 25 },
  { id: 2, name: 'Vitasoy Soy Drink', detail: '6 × 250ml · Original', category: 'Beverages', price: 19.9, old: 24.9, store: 'PARKnSHOP', color: '#cce8b6', emoji: '🥛', stores: 7, drop: 20 },
  { id: 3, name: 'Golden Elephant Jasmine Rice', detail: '5kg · Premium Thai rice', category: 'Rice & Noodles', price: 72.9, old: 89.9, store: 'Market Place', color: '#f2dbc3', emoji: '🌾', stores: 6, drop: 19 },
  { id: 4, name: 'Meiji Fresh Milk', detail: '946ml · Full cream', category: 'Dairy', price: 23.5, old: 27.9, store: 'AEON', color: '#c8e6f4', emoji: '🥛', stores: 4, drop: 16 },
]

const storeColors: Record<string, string> = {
  Wellcome: '#e84142',
  PARKnSHOP: '#007fb0',
  'Market Place': '#709b3f',
  AEON: '#b14b91',
}

export default function App() {
  const [active, setActive] = useState('Home')
  const [query, setQuery] = useState('')
  const [saved, setSaved] = useState<number[]>([2])
  const [list, setList] = useState<number[]>([])
  const [language, setLanguage] = useState('EN')
  const [contrast, setContrast] = useState(false)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [notice, setNotice] = useState('')
  const [chatOpen, setChatOpen] = useState(false)

  const filtered = useMemo(
    () => products.filter((product) => `${product.name} ${product.category} ${product.store}`.toLowerCase().includes(query.toLowerCase())),
    [query],
  )

  function flash(message: string) {
    setNotice(message)
    window.setTimeout(() => setNotice(''), 2200)
  }

  function toggleSaved(id: number) {
    setSaved((items) => (items.includes(id) ? items.filter((item) => item !== id) : [...items, id]))
  }

  function addToList(id: number) {
    if (list.includes(id)) return flash('This item is already on your list')
    setList((items) => [...items, id])
    flash('Added to Weekend groceries')
  }

  const nav = [
    ['Home', 'home'],
    ['Compare', 'compare'],
    ['My lists', 'list'],
    ['Alerts', 'bell'],
    ['Saved', 'heart'],
  ] as [string, IconName][]

  return (
    <div className={contrast ? 'app high-contrast' : 'app'}>
      <aside className="sidebar">
        <button className="brand" onClick={() => setActive('Home')} aria-label="Pricewise home">
          <span className="brand-mark"><span /></span>
          <span>pricewise</span>
        </button>

        <nav className="side-nav" aria-label="Main navigation">
          {nav.map(([label, icon]) => (
            <button key={label} className={active === label ? 'active' : ''} onClick={() => {
              setActive(label)
              setSettingsOpen(false)
            }}>
              <Icon name={icon} /><span>{label}</span>
              {label === 'My lists' && list.length > 0 && <b>{list.length}</b>}
              {label === 'Alerts' && <i />}
            </button>
          ))}
          <button
            className={active === 'Settings' ? 'active' : ''}
            onClick={() => {
              setActive('Settings')
              setSettingsOpen((open) => !open)
            }}
            aria-expanded={settingsOpen}
          >
            <Icon name="settings" /><span>Settings</span>
            <span className={settingsOpen ? 'settings-chevron open' : 'settings-chevron'}><Icon name="chevron" size={15} /></span>
          </button>
          {settingsOpen && (
            <div className="settings-panel">
              <button onClick={() => setLanguage(language === 'EN' ? '繁' : language === '繁' ? '简' : 'EN')}>
                <Icon name="globe" size={18} /><span>Language</span><strong>{language}</strong>
              </button>
              <button onClick={() => setContrast(!contrast)}>
                <Icon name="contrast" size={18} /><span>High contrast</span>
                <span className={contrast ? 'switch on' : 'switch'}><span /></span>
              </button>
            </div>
          )}
        </nav>

        <div className="sidebar-bottom">
          <div className="local-note">
            <span><Icon name="check" size={13} /></span>
            <div><b>Saved on this device</b><small>Your data stays local</small></div>
          </div>
        </div>
      </aside>

      <main>
        <header className="topbar">
          <div className="mobile-brand">
            <span className="brand-mark"><span /></span><b>pricewise</b>
          </div>
          <div className="search-wrap">
            <Icon name="search" size={21} />
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search products, brands, categories..." />
            <kbd>⌘ K</kbd>
          </div>
          <button className="icon-button" aria-label="Notifications" onClick={() => setActive('Alerts')}><Icon name="bell" /><span /></button>
          <button className="profile" aria-label="Local profile">JL</button>
        </header>

        <div className="content">
          <section className="welcome">
            <div>
              <p className="eyebrow">MONDAY, 19 OCTOBER</p>
              <h1>Good morning, Jamie.</h1>
              <p>Here’s what’s happening with your groceries today.</p>
            </div>
            <button className="primary-button" onClick={() => flash('New shopping list created')}>
              <Icon name="plus" size={18} /> New shopping list
            </button>
          </section>

          <section className="hero-grid">
            <article className="deal-card">
              <div className="deal-copy">
                <span className="pill"><Icon name="trend" size={14} /> PRICE DROP</span>
                <h2>Your saved items<br />just got cheaper.</h2>
                <p>3 products dropped in price since yesterday. You could save up to <strong>HK$28.40</strong>.</p>
                <button onClick={() => setActive('Saved')}>View price drops <Icon name="arrow" size={17} /></button>
              </div>
              <div className="deal-visual" aria-hidden="true">
                <div className="orbit orbit-one" />
                <div className="orbit orbit-two" />
                <div className="bag">
                  <span>SMART<br /><b>SAVINGS</b></span>
                  <div className="bag-handle" />
                </div>
                <span className="float-tag tag-one">−20%</span>
                <span className="float-tag tag-two">−HK$8</span>
                <span className="sparkle s1">✦</span><span className="sparkle s2">✦</span>
              </div>
            </article>

            <article className="budget-card">
              <div className="card-head">
                <div><span className="mini-icon peach"><Icon name="budget" size={19} /></span><h3>October budget</h3></div>
                <button aria-label="Budget details"><Icon name="chevron" size={18} /></button>
              </div>
              <div className="budget-total"><strong>HK$1,284</strong><span>of HK$2,000</span></div>
              <div className="progress"><span /></div>
              <div className="budget-foot"><span>64% used</span><b>HK$716 left</b></div>
              <div className="budget-message"><Icon name="sparkles" size={16} /><span>You’re on track. That’s <b>HK$86 less</b> than this time last month.</span></div>
            </article>
          </section>

          <section className="section-block">
            <div className="section-heading">
              <div><h2>Today’s best drops</h2><p>Biggest price changes across stores you follow</p></div>
              <button onClick={() => setActive('Compare')}>View all deals <Icon name="arrow" size={17} /></button>
            </div>
            <div className="product-grid">
              {filtered.length ? filtered.map((product) => (
                <article className="product-card" key={product.id}>
                  <div className="product-image" style={{ background: product.color }}>
                    <span className="discount">−{product.drop}%</span>
                    <button className={saved.includes(product.id) ? 'save saved' : 'save'} onClick={() => toggleSaved(product.id)} aria-label="Save product">
                      <Icon name="heart" size={18} />
                    </button>
                    <span className="product-emoji" aria-hidden="true">{product.emoji}</span>
                    <span className="package-label">{product.name.split(' ')[0]}<small>{product.category}</small></span>
                  </div>
                  <div className="product-copy">
                    <span className="category">{product.category}</span>
                    <h3>{product.name}</h3>
                    <p>{product.detail}</p>
                    <div className="price-row">
                      <div><strong>HK${product.price.toFixed(2)}</strong><del>HK${product.old.toFixed(2)}</del></div>
                      <span className="store-dot" style={{ background: storeColors[product.store] }}>{product.store.slice(0, 1)}</span>
                    </div>
                    <div className="product-actions">
                      <span>Best at <b>{product.store}</b> · {product.stores} stores</span>
                      <button onClick={() => addToList(product.id)} aria-label={`Add ${product.name} to list`}><Icon name="plus" size={18} /></button>
                    </div>
                  </div>
                </article>
              )) : <div className="no-results"><Icon name="search" size={30} /><h3>No products found</h3><p>Try a different product, brand, or category.</p></div>}
            </div>
          </section>

          <section className="bottom-grid">
            <article className="list-card">
              <div className="card-head">
                <div><span className="mini-icon green"><Icon name="list" size={19} /></span><div><h3>Weekend groceries</h3><p>{8 + list.length} items · Updated today</p></div></div>
                <button aria-label="List details"><Icon name="chevron" size={18} /></button>
              </div>
              <div className="list-stats">
                <div><small>BEST COMBINATION</small><strong>HK$386.70</strong><span>Across 3 stores</span></div>
                <div><small>ONE-STORE OPTION</small><strong>HK$412.30</strong><span>at Wellcome</span></div>
              </div>
              <div className="saving-line"><span><Icon name="sparkles" size={15} /> Mix & match saves you</span><b>HK$25.60</b></div>
            </article>

            <article className="assistant-card">
              <div className="assistant-icon"><Icon name="sparkles" size={25} /></div>
              <div><span>PRICEWISE ASSISTANT</span><h3>Ask about any price.</h3><p>“Where is Golden Elephant rice cheapest today?”</p></div>
              <button onClick={() => setChatOpen(!chatOpen)}>{chatOpen ? 'Close' : 'Ask now'} <Icon name="arrow" size={17} /></button>
              {chatOpen && <div className="chat-popover"><b>Golden Elephant Jasmine Rice, 5kg</b><p>The lowest listed price is <strong>HK$72.90 at Market Place</strong>, HK$7 less than the next listing.</p><small>Checked against 6 retailer listings · Demo data</small></div>}
            </article>
          </section>

          <footer><span>Prices last refreshed today at 8:30 AM</span><span>Listed prices may differ in store. Stock and delivery fees are not included.</span></footer>
        </div>
      </main>

      <nav className="mobile-nav">
        {nav.slice(0, 4).map(([label, icon]) => (
          <button key={label} className={active === label ? 'active' : ''} onClick={() => setActive(label)}><Icon name={icon} size={20} /><span>{label}</span></button>
        ))}
      </nav>

      {notice && <div className="toast"><span><Icon name="check" size={15} /></span>{notice}</div>}
    </div>
  )
}
