import { useState, useEffect } from 'react'
import {
  Plus,
  Trash2,
  ShoppingBag,
  Users,
  User,
  ArrowLeft,
  ArrowUp,
  ArrowDown,
  X,
  LogOut,
  Clock,
  TrendingUp,
  PieChart as PieChartIcon,
  Search
} from 'lucide-react'
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  LineChart,
  Line
} from 'recharts'
import './App.css'

const API_BASE = 'http://localhost:8080/api'

// Helper to get auth headers
const getHeaders = (token: string | null) => {
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  }
}

interface Profile {
  id: number
  name: string
  active: boolean
}

interface Item {
  id: number
  name: string
  unit: string
}

interface Purchase {
  id: number
  item: Item
  profile: Profile
  quantity: number
  rate: number
  totalAmount: number
  purchaseDate: string
  shop?: string
  paymentMode?: string
  notes?: string
}

function App() {
  // Navigation & Auth State
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'))
  const [currentProfile, setCurrentProfile] = useState<{ id: number; name: string } | null>(
    localStorage.getItem('profile') ? JSON.parse(localStorage.getItem('profile')!) : null
  )
  const [activeTab, setActiveTab] = useState<'dashboard' | 'history' | 'insights' | 'profiles' | 'search'>('dashboard')
  const [searchQuery, setSearchQuery] = useState('')
  const [searchItems, setSearchItems] = useState<Item[]>([])
  const [showAddModal, setShowAddModal] = useState(false)
  const [editingPurchase, setEditingPurchase] = useState<Purchase | null>(null)
  const [selectedItemId, setSelectedItemId] = useState<number | null>(null)

  // PIN Pad state
  const [pinTargetProfile, setPinTargetProfile] = useState<Profile | null>(null)
  const [pin, setPin] = useState<string>('')
  const [pinError, setPinError] = useState<string | null>(null)

  // API Data State
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [items, setItems] = useState<Item[]>([])
  const [purchases, setPurchases] = useState<Purchase[]>([])
  const [dashboardData, setDashboardData] = useState<any>(null)
  const [loading, setLoading] = useState(false)

  // Month selector for Dashboard
  const [selectedMonth, setSelectedMonth] = useState<string>(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  })

  // State for Insights Tab
  const [insightsPeriodType, setInsightsPeriodType] = useState<'month' | 'year'>('month')
  const [insightsPeriodValue, setInsightsPeriodValue] = useState<string>(selectedMonth)
  const [insightsData, setInsightsData] = useState<any>(null)

  // Filters for History Tab
  const [historyFilters, setHistoryFilters] = useState({
    startDate: '',
    endDate: '',
    itemId: '',
    profileId: ''
  })

  // Item Details State (when viewing details of a specific item)
  const [priceHistory, setPriceHistory] = useState<any>(null)
  const [buyingIntervals, setBuyingIntervals] = useState<any>(null)

  // Colors for items in the pie chart
  const itemColors = ['#10b981', '#3b82f6', '#8b5cf6', '#ef4444', '#f59e0b', '#ec4899', '#06b6d4', '#14b8a6']

  // Load basic startup data (profiles list)
  const fetchProfiles = async () => {
    try {
      const res = await fetch(`${API_BASE}/profiles`)
      if (res.ok) {
        const data = await res.json()
        setProfiles(data)
      }
    } catch (e) {
      console.error('Failed to load profiles', e)
    }
  }

  useEffect(() => {
    fetchProfiles()
  }, [])

  // Auto-fetch data when token changes
  useEffect(() => {
    if (token) {
      fetchDashboard()
      fetchItems()
      fetchPurchases()
    }
  }, [token, selectedMonth])

  // Fetch Insights data
  useEffect(() => {
    if (token && activeTab === 'insights') {
      const fetchInsights = async () => {
        try {
          const itemRes = await fetch(`${API_BASE}/analytics/item-spend?period=${insightsPeriodValue}`, { headers: getHeaders(token) })
          const personRes = await fetch(`${API_BASE}/analytics/person-spend?period=${insightsPeriodValue}`, { headers: getHeaders(token) })
          const priceRes = await fetch(`${API_BASE}/analytics/price-changes?period=${insightsPeriodValue}`, { headers: getHeaders(token) })
          
          setInsightsData({
            itemSpend: itemRes.ok ? await itemRes.json() : [],
            personSpend: personRes.ok ? await personRes.json() : [],
            priceChanges: priceRes.ok ? await priceRes.json() : []
          })
        } catch (e) {
          console.error(e)
        }
      }
      fetchInsights()
    }
  }, [token, activeTab, insightsPeriodValue])

  // Fetch dashboard summary
  const fetchDashboard = async () => {
    if (!token) return
    try {
      setLoading(true)
      const res = await fetch(`${API_BASE}/analytics/dashboard?month=${selectedMonth}`, {
        headers: getHeaders(token)
      })
      if (res.ok) {
        const data = await res.json()
        setDashboardData(data)
      }
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  // Fetch Items (for form suggestions)
  const fetchItems = async (search: string = '') => {
    if (!token) return
    try {
      const res = await fetch(`${API_BASE}/items?search=${search}`, { headers: getHeaders(token) })
      if (res.ok) {
        const data = await res.json()
        setItems(data)
      }
    } catch (e) {
      console.error(e)
    }
  }

  // Fetch Items for Search tab
  const fetchSearchItems = async (query: string) => {
    if (!token) return
    try {
      const res = await fetch(`${API_BASE}/items?search=${encodeURIComponent(query)}`, { headers: getHeaders(token) })
      if (res.ok) {
        const data = await res.json()
        setSearchItems(data)
      }
    } catch (e) {
      console.error(e)
    }
  }

  // Trigger search when query changes
  useEffect(() => {
    if (token && activeTab === 'search') {
      const timer = setTimeout(() => fetchSearchItems(searchQuery), 300)
      return () => clearTimeout(timer)
    }
  }, [searchQuery, activeTab, token])

  // Load all items when switching to search tab
  useEffect(() => {
    if (token && activeTab === 'search' && searchItems.length === 0) {
      fetchSearchItems('')
    }
  }, [activeTab])

  // Fetch Purchases
  const fetchPurchases = async () => {
    if (!token) return
    try {
      let query = '?'
      if (historyFilters.startDate) query += `startDate=${historyFilters.startDate}&`
      if (historyFilters.endDate) query += `endDate=${historyFilters.endDate}&`
      if (historyFilters.itemId) query += `itemId=${historyFilters.itemId}&`
      if (historyFilters.profileId) query += `profileId=${historyFilters.profileId}&`

      const res = await fetch(`${API_BASE}/purchases${query}`, { headers: getHeaders(token) })
      if (res.ok) {
        const data = await res.json()
        setPurchases(data)
      }
    } catch (e) {
      console.error(e)
    }
  }

  // Refetch purchases when history filters change
  useEffect(() => {
    if (token && activeTab === 'history') {
      fetchPurchases()
    }
  }, [historyFilters, activeTab])

  // Fetch Item Analytics Details
  useEffect(() => {
    const fetchItemDetails = async () => {
      if (!token || !selectedItemId) return
      try {
        const histRes = await fetch(`${API_BASE}/analytics/item/${selectedItemId}/price-history`, {
          headers: getHeaders(token)
        })
        const intRes = await fetch(`${API_BASE}/analytics/item/${selectedItemId}/buying-interval`, {
          headers: getHeaders(token)
        })

        if (histRes.ok) setPriceHistory(await histRes.json())
        if (intRes.ok) setBuyingIntervals(await intRes.json())
      } catch (e) {
        console.error(e)
      }
    }
    fetchItemDetails()
  }, [selectedItemId, token])

  // Handle Login Flow
  const handleProfileSelect = (profile: Profile) => {
    setPinTargetProfile(profile)
    setPin('')
    setPinError(null)
  }

  const handlePinKey = (val: string) => {
    if (pin.length < 6) {
      setPin(prev => prev + val)
    }
  }

  const handlePinBackspace = () => {
    setPin(prev => prev.slice(0, -1))
  }

  const handleLoginSubmit = async () => {
    if (!pinTargetProfile) return
    try {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ profileId: pinTargetProfile.id, pin })
      })

      if (res.ok) {
        const data = await res.json()
        localStorage.setItem('token', data.token)
        localStorage.setItem('profile', JSON.stringify({ id: data.profileId, name: data.profileName }))
        setToken(data.token)
        setCurrentProfile({ id: data.profileId, name: data.profileName })
        setPinTargetProfile(null)
        setPin('')
      } else {
        setPinError('Invalid PIN')
        setPin('')
      }
    } catch (e) {
      setPinError('Connection error')
    }
  }

  // Auto trigger login when PIN is fully entered
  useEffect(() => {
    if (pinTargetProfile && pin.length >= 4 && pin.length <= 6) {
      const timeout = setTimeout(() => {
        if (pin.length >= 4) {
          handleLoginSubmit()
        }
      }, 300)
      return () => clearTimeout(timeout)
    }
  }, [pin, pinTargetProfile])

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('profile')
    setToken(null)
    setCurrentProfile(null)
    setDashboardData(null)
    setActiveTab('dashboard')
  }

  // Create Profile Flow
  const [newProfileName, setNewProfileName] = useState('')
  const [newProfilePin, setNewProfilePin] = useState('')
  const handleCreateProfile = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newProfileName || !newProfilePin) return
    try {
      const res = await fetch(`${API_BASE}/profiles`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: newProfileName, pin: newProfilePin })
      })
      if (res.ok) {
        fetchProfiles()
        setNewProfileName('')
        setNewProfilePin('')
      }
    } catch (e) {
      console.error(e)
    }
  }

  // Toggle profile status
  const handleToggleProfile = async (profile: Profile) => {
    try {
      const res = await fetch(`${API_BASE}/profiles/${profile.id}`, {
        method: 'PATCH',
        headers: getHeaders(token),
        body: JSON.stringify({ active: !profile.active })
      })
      if (res.ok) {
        fetchProfiles()
        fetchDashboard()
      }
    } catch (e) {
      console.error(e)
    }
  }

  // Add/Edit Purchase Submission
  const handleSavePurchase = async (purchaseData: any) => {
    try {
      const method = editingPurchase ? 'PATCH' : 'POST'
      const url = editingPurchase ? `${API_BASE}/purchases/${editingPurchase.id}` : `${API_BASE}/purchases`

      const res = await fetch(url, {
        method,
        headers: getHeaders(token),
        body: JSON.stringify(purchaseData)
      })

      if (res.ok) {
        setShowAddModal(false)
        setEditingPurchase(null)
        fetchDashboard()
        fetchPurchases()
        if (activeTab === 'insights') {
          setInsightsPeriodValue(insightsPeriodValue) // trigger re-fetch
        }
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handleDeletePurchase = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this purchase?')) return
    try {
      const res = await fetch(`${API_BASE}/purchases/${id}`, {
        method: 'DELETE',
        headers: getHeaders(token)
      })
      if (res.ok) {
        fetchDashboard()
        fetchPurchases()
      }
    } catch (e) {
      console.error(e)
    }
  }

  if (!token) {
    return (
      <div className="app-container" style={{ justifyContent: 'center', alignItems: 'center' }}>
        {!pinTargetProfile ? (
          // PROFILE SELECTION VIEW
          <div className="login-screen animate-fade-in" style={{ padding: '2rem', textAlign: 'center', maxWidth: '500px', width: '100%' }}>
            <h1 className="gradient-emerald" style={{ fontSize: '3rem', marginBottom: '1rem', fontWeight: 800 }}>MINTLY</h1>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '3rem', fontSize: '1.1rem' }}>Select your family profile to log in</p>
            
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))', gap: '1.5rem', marginBottom: '2rem' }}>
              {profiles.map(p => (
                <button
                  key={p.id}
                  onClick={() => handleProfileSelect(p)}
                  className="glass-card"
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '1.5rem',
                    border: '1px solid var(--border-color)',
                    cursor: 'pointer',
                    background: p.active ? 'var(--bg-surface-glass)' : 'rgba(255,255,255,0.02)',
                    opacity: p.active ? 1 : 0.5
                  }}
                >
                  <div style={{
                    width: '64px',
                    height: '64px',
                    borderRadius: '50%',
                    background: 'linear-gradient(135deg, #10b981 0%, #8b5cf6 100%)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '0.75rem',
                    boxShadow: '0 4px 10px rgba(16, 185, 129, 0.2)'
                  }}>
                    <User size={32} color="#000" />
                  </div>
                  <span style={{ fontWeight: 600, fontSize: '1.1rem' }}>{p.name}</span>
                  {!p.active && <span style={{ fontSize: '0.8rem', color: 'var(--text-danger)', marginTop: '0.25rem' }}>Inactive</span>}
                </button>
              ))}
            </div>

            {profiles.length === 0 && (
              <div style={{ color: 'var(--text-muted)', marginBottom: '2rem' }}>No profiles set up yet. Add one below!</div>
            )}

            {/* In-app setup for profiles if empty */}
            <form onSubmit={handleCreateProfile} className="glass-card" style={{ padding: '1.5rem', marginTop: '2rem', textAlign: 'left' }}>
              <h3 style={{ fontSize: '1.2rem', marginBottom: '1rem' }}>Add Family Profile</h3>
              <div className="form-group">
                <input
                  type="text"
                  placeholder="Name (e.g. Mom, Dad)"
                  className="form-control"
                  value={newProfileName}
                  onChange={e => setNewProfileName(e.target.value)}
                />
              </div>
              <div className="form-group">
                <input
                  type="password"
                  placeholder="4-6 Digit PIN"
                  className="form-control"
                  value={newProfilePin}
                  onChange={e => setNewProfilePin(e.target.value)}
                />
              </div>
              <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>Add Profile</button>
            </form>
          </div>
        ) : (
          // PIN ENTRY VIEW
          <div className="login-screen animate-fade-in" style={{ padding: '2rem', textAlign: 'center', maxWidth: '400px', width: '100%' }}>
            <button onClick={() => setPinTargetProfile(null)} style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', marginBottom: '2rem' }}>
              <ArrowLeft size={16} /> Back to profiles
            </button>
            <div style={{
              width: '80px',
              height: '80px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #10b981 0%, #8b5cf6 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1.5rem auto'
            }}>
              <User size={40} color="#000" />
            </div>
            <h2>Enter PIN for {pinTargetProfile.name}</h2>
            
            {pinError && <p style={{ color: 'var(--color-danger)', marginTop: '0.5rem' }}>{pinError}</p>}

            <div className="pin-dots">
              {[...Array(6)].map((_, i) => (
                <div key={i} className={`pin-dot ${i < pin.length ? 'filled' : ''}`}></div>
              ))}
            </div>

            <div className="pin-pad">
              {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map(val => (
                <button key={val} onClick={() => handlePinKey(val)} className="pin-key">{val}</button>
              ))}
              <button onClick={() => setPin('')} className="pin-key" style={{ fontSize: '1rem', color: 'var(--text-danger)' }}>Clear</button>
              <button onClick={() => handlePinKey('0')} className="pin-key">0</button>
              <button onClick={handlePinBackspace} className="pin-key" style={{ fontSize: '1rem' }}>⌫</button>
            </div>
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="app-container">
      {/* Navigation - Sidebar on Desktop, Bottom on Mobile */}
      <nav className="bottom-nav">
        <div style={{ display: 'none', alignItems: 'center', justifyContent: 'center', marginBottom: '2rem' }} className="desktop-logo">
          <h1 className="gradient-emerald" style={{ fontSize: '2rem', fontWeight: 800, letterSpacing: '0.05em' }}>MINTLY</h1>
        </div>
        <button onClick={() => { setSelectedItemId(null); setActiveTab('dashboard') }} className={`bottom-nav-item ${activeTab === 'dashboard' ? 'active' : ''}`}>
          <TrendingUp />
          <span>Dashboard</span>
        </button>
        <button onClick={() => { setSelectedItemId(null); setActiveTab('insights') }} className={`bottom-nav-item ${activeTab === 'insights' ? 'active' : ''}`}>
          <PieChartIcon />
          <span>Insights</span>
        </button>
        <button onClick={() => { setSelectedItemId(null); setActiveTab('search') }} className={`bottom-nav-item ${activeTab === 'search' ? 'active' : ''}`}>
          <Search />
          <span>Search</span>
        </button>
        <button onClick={() => { setSelectedItemId(null); setActiveTab('history') }} className={`bottom-nav-item ${activeTab === 'history' ? 'active' : ''}`}>
          <Clock />
          <span>History</span>
        </button>
        <button onClick={() => { setSelectedItemId(null); setActiveTab('profiles') }} className={`bottom-nav-item ${activeTab === 'profiles' ? 'active' : ''}`}>
          <Users />
          <span>Profiles</span>
        </button>
      </nav>

      {/* Main Content Area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>
        {/* Header */}
        <header style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '1rem 1.5rem',
          borderBottom: '1px solid var(--border-color)',
          background: 'var(--bg-surface-glass)',
          backdropFilter: 'blur(10px)',
          zIndex: 50
        }}>
          <h1 className="gradient-emerald" style={{ fontSize: '1.5rem', fontWeight: 800, letterSpacing: '0.05em' }}>MINTLY</h1>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            {/* Profile Badge */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.625rem',
              background: 'rgba(255,255,255,0.06)',
              border: '1px solid var(--border-color)',
              borderRadius: 'var(--radius-full)',
              padding: '0.375rem 0.875rem 0.375rem 0.375rem'
            }}>
              <div style={{
                width: '28px',
                height: '28px',
                borderRadius: '50%',
                background: 'linear-gradient(135deg, #10b981 0%, #8b5cf6 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}>
                <User size={14} color="#000" />
              </div>
              <span style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-primary)' }}>{currentProfile?.name}</span>
            </div>
            <button
              onClick={handleLogout}
              title="Logout"
              style={{
                background: 'rgba(255,255,255,0.04)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-full)',
                width: '36px',
                height: '36px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                color: 'var(--text-muted)',
                transition: 'var(--transition-fast)'
              }}
            >
              <LogOut size={15} />
            </button>
          </div>
        </header>

        <main style={{ padding: '2rem', flex: 1, overflowY: 'auto' }}>
          {selectedItemId ? (
            // ITEM DETAIL SCREEN OVERLAY VIEW
            <ItemDetailView
              itemId={selectedItemId}
              priceHistory={priceHistory}
              buyingIntervals={buyingIntervals}
              onBack={() => {
                setSelectedItemId(null)
                setPriceHistory(null)
                setBuyingIntervals(null)
              }}
            />
          ) : activeTab === 'dashboard' ? (
            // DASHBOARD
            <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
              {/* Month Picker */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h2 style={{ fontSize: '1.75rem' }}>Monthly Overview</h2>
                <input
                  type="month"
                  className="form-control"
                  style={{ width: '180px', padding: '0.5rem 1rem', background: 'var(--bg-surface)' }}
                  value={selectedMonth}
                  onChange={e => setSelectedMonth(e.target.value)}
                />
              </div>

              {/* Stats Cards */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem' }}>
                <div className="glass-card" style={{ background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(24, 24, 27, 0.8) 100%)', padding: '2rem' }}>
                  <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600, letterSpacing: '0.05em' }}>TOTAL SPENT</span>
                  <div style={{ fontSize: '2.5rem', fontWeight: 800, marginTop: '0.5rem', color: 'var(--color-primary)' }}>
                    ₹{dashboardData?.thisMonthTotal?.toLocaleString('en-IN') || '0'}
                  </div>
                </div>
                <div className="glass-card" style={{ background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.1) 0%, rgba(24, 24, 27, 0.8) 100%)', padding: '2rem' }}>
                  <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600, letterSpacing: '0.05em' }}>PER HEAD SPLIT</span>
                  <div style={{ fontSize: '2.5rem', fontWeight: 800, marginTop: '0.5rem', color: 'var(--color-accent)' }}>
                    ₹{dashboardData?.costPerHead?.perHeadAmount?.toLocaleString('en-IN') || '0'}
                  </div>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>divided by {dashboardData?.costPerHead?.activeMembers || '0'} members</span>
                </div>
              </div>

              {/* Recent Logged Purchases */}
              <div className="glass-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                  <h3 style={{ fontSize: '1.25rem' }}>Recent Logged</h3>
                  <button onClick={() => setActiveTab('history')} className="btn btn-secondary" style={{ padding: '0.5rem 1rem', fontSize: '0.9rem' }}>View All</button>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1rem' }}>
                  {dashboardData?.recentPurchases?.map((p: Purchase) => (
                    <div
                      key={p.id}
                      onClick={() => setSelectedItemId(p.item.id)}
                      className="glass-card hover-lift"
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        padding: '1rem',
                        cursor: 'pointer',
                        background: 'rgba(255,255,255,0.02)'
                      }}
                    >
                      <div style={{
                        width: '48px',
                        height: '48px',
                        borderRadius: '12px',
                        background: 'rgba(16, 185, 129, 0.1)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        marginRight: '1rem'
                      }}>
                        <ShoppingBag size={24} color="var(--color-primary)" />
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontWeight: 600, fontSize: '1.05rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{p.item.name}</div>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          {p.profile.name} • {new Date(p.purchaseDate).toLocaleDateString()}
                        </span>
                      </div>
                      <div style={{ textAlign: 'right', marginLeft: '1rem' }}>
                        <div style={{ fontWeight: 700, color: 'var(--text-primary)', fontSize: '1.1rem' }}>₹{p.totalAmount}</div>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{p.quantity} {p.item.unit}</span>
                      </div>
                    </div>
                  ))}
                  {(!dashboardData?.recentPurchases || dashboardData.recentPurchases.length === 0) && (
                    <div style={{ color: 'var(--text-muted)', padding: '1rem', gridColumn: '1 / -1' }}>No purchases logged this month yet.</div>
                  )}
                </div>
              </div>
            </div>
          ) : activeTab === 'insights' ? (
            // INSIGHTS TAB
            <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h2 style={{ fontSize: '1.75rem' }}>Family Spend Insights</h2>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                  <select 
                    className="form-control" 
                    value={insightsPeriodType} 
                    onChange={e => {
                      const type = e.target.value as 'month' | 'year'
                      setInsightsPeriodType(type)
                      const d = new Date()
                      if (type === 'year') {
                        setInsightsPeriodValue(d.getFullYear().toString())
                      } else {
                        setInsightsPeriodValue(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
                      }
                    }}
                    style={{ width: 'auto', padding: '0.5rem 1rem' }}
                  >
                    <option value="month">Monthly View</option>
                    <option value="year">Yearly View</option>
                  </select>
                  
                  {insightsPeriodType === 'month' ? (
                    <input
                      type="month"
                      className="form-control"
                      style={{ width: '150px', padding: '0.5rem 1rem' }}
                      value={insightsPeriodValue}
                      onChange={e => setInsightsPeriodValue(e.target.value)}
                    />
                  ) : (
                    <input
                      type="number"
                      className="form-control"
                      style={{ width: '120px', padding: '0.5rem 1rem' }}
                      value={insightsPeriodValue}
                      onChange={e => setInsightsPeriodValue(e.target.value)}
                      min="2000" max="2100"
                    />
                  )}
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '1.5rem' }}>
                {/* Person Spend Breakdown */}
                <div className="glass-card">
                  <h3 style={{ fontSize: '1.25rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Users size={20} color="var(--color-primary)" /> Spend by Person
                  </h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    {insightsData?.personSpend?.map((p: any, idx: number) => (
                      <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', background: 'rgba(255,255,255,0.02)', borderRadius: '8px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                          <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: 'linear-gradient(135deg, #10b981 0%, #8b5cf6 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <User size={18} color="#000" />
                          </div>
                          <span style={{ fontWeight: 600, fontSize: '1.1rem' }}>{p.profileName}</span>
                        </div>
                        <span style={{ fontWeight: 800, fontSize: '1.25rem', color: 'var(--text-primary)' }}>₹{p.totalAmount}</span>
                      </div>
                    ))}
                    {(!insightsData?.personSpend || insightsData.personSpend.length === 0) && (
                      <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '1rem' }}>No data for this period.</div>
                    )}
                  </div>
                </div>

                {/* Price Change Tracker */}
                <div className="glass-card">
                  <h3 style={{ fontSize: '1.25rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <TrendingUp size={20} color="var(--color-accent)" /> Price Change Tracker
                  </h3>
                  <span style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
                    Comparing items bought in this period to their previous purchase price.
                  </span>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    {insightsData?.priceChanges?.map((pc: any, idx: number) => (
                      <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', background: 'rgba(255,255,255,0.02)', borderRadius: '8px', borderLeft: `4px solid ${pc.deltaRate > 0 ? 'var(--color-danger)' : 'var(--color-primary)'}` }}>
                        <div>
                          <div style={{ fontWeight: 600, fontSize: '1.05rem', marginBottom: '0.25rem' }}>{pc.itemName}</div>
                          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                            Was ₹{pc.previousRate} ➔ Now <strong style={{color: 'var(--text-primary)'}}>₹{pc.currentRate}</strong>
                          </div>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', color: pc.deltaRate > 0 ? 'var(--color-danger)' : 'var(--color-primary)', fontWeight: 700, fontSize: '1.1rem' }}>
                          {pc.deltaRate > 0 ? <ArrowUp size={16} /> : <ArrowDown size={16} />}
                          {Math.abs(pc.deltaPercent)}%
                        </div>
                      </div>
                    ))}
                    {(!insightsData?.priceChanges || insightsData.priceChanges.length === 0) && (
                      <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '1rem' }}>No notable price changes detected in this period.</div>
                    )}
                  </div>
                </div>

                {/* Top Item Spend */}
                <div className="glass-card" style={{ gridColumn: '1 / -1' }}>
                  <h3 style={{ fontSize: '1.25rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <ShoppingBag size={20} color="var(--color-primary)" /> Total Spend by Item
                  </h3>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '1rem' }}>
                    {insightsData?.itemSpend?.map((item: any, idx: number) => (
                      <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', background: 'rgba(255,255,255,0.02)', borderRadius: '8px' }}>
                        <span style={{ fontWeight: 600, fontSize: '1.05rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{item.itemName}</span>
                        <span style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--color-primary)' }}>₹{item.totalAmount}</span>
                      </div>
                    ))}
                    {(!insightsData?.itemSpend || insightsData.itemSpend.length === 0) && (
                      <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '1rem', gridColumn: '1/-1' }}>No item spends for this period.</div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ) : activeTab === 'search' ? (
            // SEARCH TAB
            <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <h2 style={{ fontSize: '1.75rem' }}>Search Items</h2>
              <div style={{ position: 'relative' }}>
                <Search
                  size={18}
                  style={{
                    position: 'absolute',
                    left: '1rem',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    color: 'var(--text-muted)',
                    pointerEvents: 'none'
                  }}
                />
                <input
                  type="text"
                  placeholder="Search items by name…"
                  className="form-control"
                  style={{ paddingLeft: '2.75rem', fontSize: '1.05rem', padding: '0.9rem 1rem 0.9rem 2.75rem' }}
                  value={searchQuery}
                  onChange={e => setSearchQuery(e.target.value)}
                  autoFocus
                />
              </div>

              {/* Results */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1rem' }}>
                {searchItems.map(item => (
                  <button
                    key={item.id}
                    onClick={() => setSelectedItemId(item.id)}
                    className="glass-card"
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '1rem',
                      padding: '1.25rem',
                      cursor: 'pointer',
                      textAlign: 'left',
                      border: '1px solid var(--border-color)',
                      background: 'rgba(255,255,255,0.02)'
                    }}
                  >
                    <div style={{
                      width: '44px',
                      height: '44px',
                      borderRadius: '10px',
                      background: 'rgba(16, 185, 129, 0.12)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0
                    }}>
                      <ShoppingBag size={20} color="var(--color-primary)" />
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 600, fontSize: '1.05rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {item.name}
                      </div>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>per {item.unit}</span>
                    </div>
                    <ArrowLeft size={16} color="var(--text-muted)" style={{ transform: 'rotate(180deg)', flexShrink: 0 }} />
                  </button>
                ))}
                {searchItems.length === 0 && (
                  <div style={{ gridColumn: '1 / -1', textAlign: 'center', color: 'var(--text-muted)', padding: '3rem', background: 'var(--bg-surface)', borderRadius: 'var(--radius-md)' }}>
                    {searchQuery ? `No items found for "${searchQuery}"` : 'No items yet. Add a purchase to get started.'}
                  </div>
                )}
              </div>
            </div>
          ) : activeTab === 'history' ? (
            // HISTORY TAB
            <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
              <h2 style={{ fontSize: '1.75rem' }}>Purchase History</h2>

              {/* Filters panel */}
              <div className="glass-card" style={{ display: 'flex', gap: '1.5rem', padding: '1.5rem', flexWrap: 'wrap' }}>
                <div className="form-group" style={{ marginBottom: 0, flex: '1 1 200px' }}>
                  <label className="form-label">Start Date</label>
                  <input
                    type="date"
                    className="form-control"
                    value={historyFilters.startDate}
                    onChange={e => setHistoryFilters(prev => ({ ...prev, startDate: e.target.value }))}
                  />
                </div>
                <div className="form-group" style={{ marginBottom: 0, flex: '1 1 200px' }}>
                  <label className="form-label">End Date</label>
                  <input
                    type="date"
                    className="form-control"
                    value={historyFilters.endDate}
                    onChange={e => setHistoryFilters(prev => ({ ...prev, endDate: e.target.value }))}
                  />
                </div>
                <div className="form-group" style={{ marginBottom: 0, flex: '1 1 200px' }}>
                  <label className="form-label">Buyer</label>
                  <select
                    className="form-control"
                    value={historyFilters.profileId}
                    onChange={e => setHistoryFilters(prev => ({ ...prev, profileId: e.target.value }))}
                  >
                    <option value="">All Buyers</option>
                    {profiles.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                  </select>
                </div>
              </div>

              {/* List */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '1.5rem' }}>
                {purchases.map(p => (
                  <div
                    key={p.id}
                    className="glass-card animate-fade-in hover-lift"
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '1rem',
                      padding: '1.5rem',
                      position: 'relative'
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <h3
                          onClick={() => setSelectedItemId(p.item.id)}
                          style={{ fontSize: '1.2rem', cursor: 'pointer', color: 'var(--color-primary)' }}
                        >
                          {p.item.name}
                        </h3>
                      </div>
                      <span style={{ fontWeight: 800, fontSize: '1.25rem' }}>₹{p.totalAmount}</span>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                      <span>{p.quantity} {p.item.unit} @ ₹{p.rate}/{p.item.unit}</span>
                      <span>{new Date(p.purchaseDate).toLocaleDateString()}</span>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid rgba(255,255,255,0.05)', paddingTop: '1rem', marginTop: '0.5rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      <span>Bought by <strong>{p.profile.name}</strong> {p.shop ? `at ${p.shop}` : ''}</span>
                      <div style={{ display: 'flex', gap: '1rem' }}>
                        <button
                          onClick={() => {
                            setEditingPurchase(p)
                            setShowAddModal(true)
                          }}
                          style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', fontWeight: 600 }}
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDeletePurchase(p.id)}
                          style={{ background: 'none', border: 'none', color: 'var(--color-danger)', cursor: 'pointer', fontWeight: 600 }}
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                  </div>
                ))}

                {purchases.length === 0 && (
                  <div style={{ gridColumn: '1 / -1', textAlign: 'center', color: 'var(--text-muted)', padding: '3rem', background: 'var(--bg-surface)', borderRadius: 'var(--radius-md)' }}>
                    No purchases found matching search criteria.
                  </div>
                )}
              </div>
            </div>
          ) : (
            // PROFILES TAB
            <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '2rem', maxWidth: '800px', margin: '0 auto' }}>
              <h2 style={{ fontSize: '1.75rem' }}>Manage Family Profiles</h2>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                {profiles.map(p => (
                  <div key={p.id} className="glass-card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1.5rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                      <div style={{
                        width: '56px',
                        height: '56px',
                        borderRadius: '50%',
                        background: 'linear-gradient(135deg, #10b981 0%, #8b5cf6 100%)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}>
                        <User size={28} color="#000" />
                      </div>
                      <div>
                        <div style={{ fontWeight: 700, fontSize: '1.25rem' }}>{p.name}</div>
                        <span style={{ fontSize: '0.9rem', color: p.active ? 'var(--color-primary)' : 'var(--text-danger)' }}>
                          {p.active ? 'Active Logger' : 'Inactive'}
                        </span>
                      </div>
                    </div>
                    <button
                      onClick={() => handleToggleProfile(p)}
                      className={`btn ${p.active ? 'btn-secondary' : 'btn-primary'}`}
                      style={{ padding: '0.5rem 1.5rem' }}
                    >
                      {p.active ? 'Deactivate' : 'Activate'}
                    </button>
                  </div>
                ))}
              </div>

              {/* Add profile form */}
              <div className="glass-card" style={{ padding: '2rem', marginTop: '1rem' }}>
                <h3 style={{ fontSize: '1.25rem', marginBottom: '1.5rem' }}>Add New Profile</h3>
                <form onSubmit={handleCreateProfile} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: '1.5rem', alignItems: 'end' }}>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label">Profile Name</label>
                    <input
                      type="text"
                      placeholder="e.g. John"
                      className="form-control"
                      value={newProfileName}
                      onChange={e => setNewProfileName(e.target.value)}
                    />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label">Security PIN (4-6 digits)</label>
                    <input
                      type="password"
                      placeholder="PIN"
                      className="form-control"
                      value={newProfilePin}
                      onChange={e => setNewProfilePin(e.target.value)}
                    />
                  </div>
                  <button type="submit" className="btn btn-primary" style={{ height: '45px' }}>Create Profile</button>
                </form>
              </div>
            </div>
          )}
        </main>
      </div>

      {/* Floating Action Button */}
      <div className="fab-container">
        <button onClick={() => {
          setEditingPurchase(null)
          setShowAddModal(true)
        }} className="fab">
          <Plus />
        </button>
      </div>

      {/* Add/Edit Modal */}
      {showAddModal && (
        <AddEditPurchaseModal
          items={items}
          editingPurchase={editingPurchase}
          onClose={() => {
            setShowAddModal(false)
            setEditingPurchase(null)
          }}
          onSave={handleSavePurchase}
          fetchItems={fetchItems}
        />
      )}
    </div>
  )
}

// MODAL FOR ADDING/EDITING PURCHASE
interface AddEditModalProps {
  items: Item[]
  editingPurchase: Purchase | null
  onClose: () => void
  onSave: (data: any) => void
  fetchItems: (search?: string) => void
}

function AddEditPurchaseModal({ items, editingPurchase, onClose, onSave, fetchItems }: AddEditModalProps) {
  const [searchItemName, setSearchItemName] = useState(editingPurchase ? editingPurchase.item.name : '')
  const [selectedItem, setSelectedItem] = useState<Item | null>(editingPurchase ? editingPurchase.item : null)
  const [showItemSuggestions, setShowItemSuggestions] = useState(false)
  const [lastPurchase, setLastPurchase] = useState<Purchase | null>(null)

  // Form Fields
  const [quantity, setQuantity] = useState(editingPurchase ? editingPurchase.quantity.toString() : '')
  const [rate, setRate] = useState(editingPurchase ? editingPurchase.rate.toString() : '')
  const [purchaseDate, setPurchaseDate] = useState(
    editingPurchase ? editingPurchase.purchaseDate : new Date().toISOString().split('T')[0]
  )
  const [shop, setShop] = useState(editingPurchase?.shop || '')
  const [paymentMode, setPaymentMode] = useState(editingPurchase?.paymentMode || 'CASH')
  const [notes, setNotes] = useState(editingPurchase?.notes || '')

  // Inline Item Creation fields (if no item found)
  const [createNewItemMode, setCreateNewItemMode] = useState(false)
  const [newUnit, setNewUnit] = useState('kg')

  // Auto calculate total
  const total = (parseFloat(quantity || '0') * parseFloat(rate || '0')).toFixed(2)

  // Fetch suggestions
  useEffect(() => {
    if (searchItemName && !selectedItem && !createNewItemMode) {
      const delayDebounceFn = setTimeout(() => {
        fetchItems(searchItemName)
        setShowItemSuggestions(true)
      }, 300)
      return () => clearTimeout(delayDebounceFn)
    } else {
      setShowItemSuggestions(false)
    }
  }, [searchItemName, selectedItem, createNewItemMode])

  const fetchLastPurchase = async (itemId: number) => {
    try {
      const res = await fetch(`${API_BASE}/purchases/items/${itemId}/last`, {
        headers: getHeaders(localStorage.getItem('token'))
      })
      if (res.ok) {
        setLastPurchase(await res.json())
      } else {
        setLastPurchase(null)
      }
    } catch (e) {
      console.error(e)
      setLastPurchase(null)
    }
  }

  const selectItemSuggestion = (item: Item) => {
    setSelectedItem(item)
    setSearchItemName(item.name)
    setShowItemSuggestions(false)
    setCreateNewItemMode(false)
    if (!editingPurchase) {
      fetchLastPurchase(item.id)
    }
  }

  const handleStartNewItemCreation = () => {
    setCreateNewItemMode(true)
    setShowItemSuggestions(false)
    setLastPurchase(null)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!quantity || !rate) return

    let finalItemId = selectedItem?.id

    // If new item mode, let's create the item first
    if (createNewItemMode && searchItemName && newUnit) {
      try {
        const itemRes = await fetch(`${API_BASE}/items`, {
          method: 'POST',
          headers: getHeaders(localStorage.getItem('token')),
          body: JSON.stringify({
            name: searchItemName,
            unit: newUnit
          })
        })
        if (itemRes.ok) {
          const item = await itemRes.json()
          finalItemId = item.id
        } else {
          alert('Failed to create new item. Item might already exist.')
          return
        }
      } catch (err) {
        console.error(err)
        return
      }
    }

    if (!finalItemId) {
      alert('Please select or create an item first')
      return
    }

    onSave({
      itemId: finalItemId,
      quantity: parseFloat(quantity),
      rate: parseFloat(rate),
      purchaseDate,
      shop,
      paymentMode,
      notes
    })
  }

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0,0,0,0.85)',
      backdropFilter: 'blur(12px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 200,
      padding: '2rem'
    }}>
      <div className="glass-card animate-fade-in" style={{ width: '100%', maxWidth: '600px', display: 'flex', flexDirection: 'column', gap: '1.5rem', maxHeight: '90vh', overflowY: 'auto', padding: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ fontSize: '1.5rem' }}>{editingPurchase ? 'Edit Purchase' : 'Log Purchase'}</h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>
            <X size={24} />
          </button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          
          {/* Item Search / Autocomplete Field */}
          <div className="form-group" style={{ position: 'relative', marginBottom: 0 }}>
            <label className="form-label">Item Name</label>
            <input
              type="text"
              placeholder="Search or enter item..."
              className="form-control"
              style={{ fontSize: '1.1rem', padding: '1rem' }}
              value={searchItemName}
              onChange={e => {
                setSearchItemName(e.target.value)
                setSelectedItem(null)
                setLastPurchase(null)
              }}
              required
            />
            {selectedItem && (
              <span style={{ fontSize: '0.85rem', color: 'var(--color-primary)', display: 'block', marginTop: '0.5rem' }}>
                ✓ Selected: {selectedItem.name} ({selectedItem.unit})
              </span>
            )}

            {/* Last Purchase Context Box */}
            {lastPurchase && !editingPurchase && (
              <div className="animate-fade-in" style={{ marginTop: '0.75rem', padding: '0.75rem 1rem', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: '8px', fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'flex-start', gap: '0.5rem' }}>
                <Clock size={16} color="var(--color-primary)" style={{ flexShrink: 0, marginTop: '2px' }} />
                <div>
                  <strong style={{ color: 'var(--text-primary)' }}>Last purchased context:</strong>
                  <div style={{ marginTop: '0.25rem' }}>
                    ₹{lastPurchase.rate}/{selectedItem?.unit} on {new Date(lastPurchase.purchaseDate).toLocaleDateString()}
                    {lastPurchase.shop ? ` at ${lastPurchase.shop}` : ''}.
                  </div>
                </div>
              </div>
            )}

            {/* Suggestions Dropdown */}
            {showItemSuggestions && (
              <div className="search-results-list" style={{ boxShadow: 'var(--shadow-lg)' }}>
                {items.map(item => (
                  <div key={item.id} onClick={() => selectItemSuggestion(item)} className="search-result-item" style={{ fontSize: '1rem', padding: '1rem' }}>
                    {item.name} <span className="unit-label">{item.unit}</span>
                  </div>
                ))}
                {searchItemName && !items.find(i => i.name.toLowerCase() === searchItemName.toLowerCase()) && (
                  <div onClick={handleStartNewItemCreation} className="search-result-item" style={{ color: 'var(--color-primary)', fontWeight: 'bold', padding: '1rem' }}>
                    + Create New Item: "{searchItemName}"
                  </div>
                )}
              </div>
            )}
          </div>

          {/* New Item Info Panel (Inline Form) */}
          {createNewItemMode && (
            <div className="glass-card" style={{ padding: '1.25rem', background: 'rgba(16, 185, 129, 0.05)', display: 'flex', flexDirection: 'column', gap: '1rem', border: '1px solid rgba(16, 185, 129, 0.2)' }}>
              <div style={{ fontSize: '0.9rem', color: 'var(--color-primary)', fontWeight: 600 }}>✨ NEW ITEM SETUP</div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1rem' }}>
                <div>
                  <label className="form-label">Unit of Measurement</label>
                  <input type="text" placeholder="kg, packet, piece, litters, etc." className="form-control" value={newUnit} onChange={e => setNewUnit(e.target.value)} required />
                </div>
              </div>
            </div>
          )}

          {/* Qty and Rate */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Quantity</label>
              <input
                type="number"
                step="any"
                className="form-control"
                style={{ fontSize: '1.2rem', padding: '1rem' }}
                value={quantity}
                onChange={e => setQuantity(e.target.value)}
                required
              />
            </div>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Rate (Price per Unit)</label>
              <input
                type="number"
                step="any"
                className="form-control"
                style={{ fontSize: '1.2rem', padding: '1rem' }}
                value={rate}
                onChange={e => setRate(e.target.value)}
                required
              />
            </div>
          </div>

          {/* Computed Live Total */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1.5rem', background: 'rgba(255,255,255,0.02)', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
            <span style={{ fontSize: '1rem', color: 'var(--text-secondary)' }}>Live Total:</span>
            <span style={{ fontWeight: 800, fontSize: '2rem', color: 'var(--color-primary)' }}>₹{total}</span>
          </div>

          {/* Date, Shop */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Date</label>
              <input
                type="date"
                className="form-control"
                value={purchaseDate}
                onChange={e => setPurchaseDate(e.target.value)}
                required
              />
            </div>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Shop (Optional)</label>
              <input
                type="text"
                className="form-control"
                value={shop}
                onChange={e => setShop(e.target.value)}
              />
            </div>
          </div>

          {/* Payment Mode & Notes */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '1.5rem' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Payment Mode</label>
              <select
                className="form-control"
                value={paymentMode}
                onChange={e => setPaymentMode(e.target.value)}
              >
                <option value="CASH">Cash</option>
                <option value="ONLINE">Online (UPI)</option>
                <option value="CARD">Card</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Notes (Optional)</label>
              <input
                type="text"
                className="form-control"
                value={notes}
                onChange={e => setNotes(e.target.value)}
              />
            </div>
          </div>

          <button type="submit" className="btn btn-primary" style={{ marginTop: '1rem', padding: '1.25rem', fontSize: '1.1rem' }}>
            {editingPurchase ? 'Update Purchase' : 'Save Purchase'}
          </button>
        </form>
      </div>
    </div>
  )
}

// ITEM DETAILS (ANALYTICS / PRICE TREND) VIEW
interface ItemDetailProps {
  itemId: number
  priceHistory: any
  buyingIntervals: any
  onBack: () => void
}

function ItemDetailView({ itemId, priceHistory, buyingIntervals, onBack }: ItemDetailProps) {
  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '2rem', maxWidth: '800px', margin: '0 auto' }}>
      <button onClick={onBack} className="btn btn-secondary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', alignSelf: 'flex-start' }}>
        <ArrowLeft size={18} /> Back
      </button>

      {priceHistory && (
        <div className="glass-card" style={{ padding: '2rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
            <div>
              <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600 }}>ITEM PRICE TREND</span>
              <h2 style={{ fontSize: '2.5rem', fontWeight: 800 }}>₹{priceHistory.latestRate}/unit</h2>
            </div>
            {priceHistory.deltaRate !== 0 && (
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                backgroundColor: priceHistory.deltaRate > 0 ? 'rgba(239, 68, 68, 0.15)' : 'rgba(16, 185, 129, 0.15)',
                color: priceHistory.deltaRate > 0 ? 'var(--color-danger)' : 'var(--color-primary)',
                padding: '8px 16px',
                borderRadius: '8px',
                fontSize: '1.1rem',
                fontWeight: 600
              }}>
                {priceHistory.deltaRate > 0 ? <ArrowUp size={20} /> : <ArrowDown size={20} />}
                {Math.abs(priceHistory.deltaPercent)}%
              </div>
            )}
          </div>

          {/* Line Chart */}
          {priceHistory.history && priceHistory.history.length > 0 ? (
            <div style={{ height: '300px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={priceHistory.history}>
                  <XAxis dataKey="date" stroke="var(--text-muted)" fontSize={12} tickFormatter={(val) => new Date(val).toLocaleDateString(undefined, {month: 'short', day: 'numeric'})} tickLine={false} axisLine={false} />
                  <YAxis stroke="var(--text-muted)" fontSize={12} domain={['auto', 'auto']} width={40} tickLine={false} axisLine={false} />
                  <Tooltip labelFormatter={(val) => new Date(val).toLocaleDateString()} formatter={(value) => [`₹${value}`, 'Rate']} contentStyle={{ background: 'var(--bg-surface)', border: '1px solid var(--border-color)', borderRadius: '8px' }} />
                  <Line type="monotone" dataKey="rate" stroke="var(--color-primary)" strokeWidth={3} dot={{ r: 5 }} activeDot={{ r: 8 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '2rem' }}>No historical prices recorded.</div>
          )}
        </div>
      )}

      {/* Buying Intervals Card */}
      {buyingIntervals && (
        <div className="glass-card" style={{ padding: '2rem' }}>
          <div style={{ marginBottom: '1.5rem' }}>
            <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600 }}>AVERAGE BUYING INTERVAL</span>
            <h2 style={{ fontSize: '2rem', fontWeight: 800 }}>
              {buyingIntervals.averageIntervalDays ? `${Math.round(buyingIntervals.averageIntervalDays)} days` : 'N/A'}
            </h2>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', maxHeight: '250px', overflowY: 'auto' }}>
            {buyingIntervals.intervals?.map((interval: any, idx: number) => (
              <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '1rem', padding: '0.75rem 0', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Bought on {new Date(interval.date).toLocaleDateString()}</span>
                <span style={{ fontWeight: 600, color: 'var(--color-primary)' }}>{interval.daysSinceLast} days since last</span>
              </div>
            ))}
            {(!buyingIntervals.intervals || buyingIntervals.intervals.length === 0) && (
              <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '1rem' }}>Requires at least 2 purchases to compute interval.</div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default App
