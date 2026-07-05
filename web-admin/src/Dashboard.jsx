import { useEffect, useState, useMemo } from 'react';
import { collection, onSnapshot } from 'firebase/firestore';
import { db } from './firebase';
import './Dashboard.css';

export default function Dashboard() {
  const [teachers, setTeachers] = useState([]);
  const [students, setStudents] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters State
  const [timeframe, setTimeframe] = useState('all'); // 'all', '30days', '90days'
  const [selectedDistrict, setSelectedDistrict] = useState('all'); // 'all', 'Pune', etc.
  const [subStatus, setSubStatus] = useState('all'); // 'all', 'active', 'inactive'

  // Tooltip State for Line Chart
  const [hoveredNode, setHoveredNode] = useState(null);

  // Listen to Firestore Collections
  useEffect(() => {
    setLoading(true);
    
    // Listen to teachers
    const unsubscribeTeachers = onSnapshot(
      collection(db, 'teachers'),
      (snapshot) => {
        const list = [];
        snapshot.forEach((doc) => {
          list.push({ id: doc.id, ...doc.data() });
        });
        setTeachers(list);
        setError(null);
      },
      (err) => {
        console.error('Error fetching teachers:', err);
        setError(err.message);
      }
    );

    // Listen to students
    const unsubscribeStudents = onSnapshot(
      collection(db, 'students'),
      (snapshot) => {
        const list = [];
        snapshot.forEach((doc) => {
          list.push({ id: doc.id, ...doc.data() });
        });
        setStudents(list);
      },
      (err) => console.error('Error fetching students:', err)
    );

    // Listen to subscriptions
    const unsubscribeSubs = onSnapshot(
      collection(db, 'subscriptions'),
      (snapshot) => {
        const list = [];
        snapshot.forEach((doc) => {
          list.push({ id: doc.id, ...doc.data() });
        });
        setSubscriptions(list);
      },
      (err) => console.error('Error fetching subscriptions:', err)
    );

    // Turn off loader once initial loads settle
    const timeout = setTimeout(() => setLoading(false), 800);

    return () => {
      unsubscribeTeachers();
      unsubscribeStudents();
      unsubscribeSubs();
      clearTimeout(timeout);
    };
  }, []);

  // Extract list of unique districts for filter dropdown
  const districts = useMemo(() => {
    const uniqueDistricts = new Set();
    teachers.forEach((t) => {
      if (t.district && t.district.trim()) {
        uniqueDistricts.add(t.district.trim());
      }
    });
    return Array.from(uniqueDistricts).sort();
  }, [teachers]);

  // Apply filters client-side
  const filteredData = useMemo(() => {
    let result = [...teachers];
    const now = Date.now();

    // 1. Timeframe Filter (Mocking registration timestamps if missing)
    if (timeframe === '30days') {
      const thirtyDaysAgo = now - 30 * 24 * 60 * 60 * 1000;
      result = result.filter((t) => {
        // Fallback to subscriptionExpiry minus 1 year if registration timestamp not stored
        const regTime = t.timestamp || (t.subscriptionExpiry ? t.subscriptionExpiry - 365 * 24 * 60 * 60 * 1000 : now - 15 * 24 * 60 * 60 * 1000);
        return regTime >= thirtyDaysAgo;
      });
    } else if (timeframe === '90days') {
      const ninetyDaysAgo = now - 90 * 24 * 60 * 60 * 1000;
      result = result.filter((t) => {
        const regTime = t.timestamp || (t.subscriptionExpiry ? t.subscriptionExpiry - 365 * 24 * 60 * 60 * 1000 : now - 45 * 24 * 60 * 60 * 1000);
        return regTime >= ninetyDaysAgo;
      });
    }

    // 2. District Filter
    if (selectedDistrict !== 'all') {
      result = result.filter((t) => t.district === selectedDistrict);
    }

    // 3. Subscription Status Filter
    if (subStatus !== 'all') {
      result = result.filter((t) => (t.subscriptionStatus || 'inactive') === subStatus);
    }

    return result;
  }, [teachers, timeframe, selectedDistrict, subStatus]);

  // Computations
  const totalTeachersCount = filteredData.length;
  const activeSubsCount = filteredData.filter((t) => t.subscriptionStatus === 'active').length;
  const inactiveSubsCount = totalTeachersCount - activeSubsCount;
  
  // Total students enrolled
  const totalStudentsCount = useMemo(() => {
    // Sum students associated with currently filtered teachers
    const filteredIds = new Set(filteredData.map((t) => t.id));
    return students.filter((s) => filteredIds.has(s.teacherId)).length;
  }, [filteredData, students]);

  // Total calculated revenue (e.g. ₹500 per approved subscription)
  const approvedPaymentsCount = useMemo(() => {
    const filteredIds = new Set(filteredData.map((t) => t.id));
    return subscriptions.filter((s) => s.status === 'approved' && filteredIds.has(s.teacherId)).length;
  }, [filteredData, subscriptions]);
  const estimatedRevenue = approvedPaymentsCount * 500;

  // District distribution stats
  const districtDistribution = useMemo(() => {
    const counts = {};
    filteredData.forEach((t) => {
      const dist = t.district || 'Not Specified';
      counts[dist] = (counts[dist] || 0) + 1;
    });

    return Object.entries(counts)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 5); // top 5 districts
  }, [filteredData]);

  // Unified signup trend over last 6 months
  const signupTrend = useMemo(() => {
    const now = new Date();
    const months = [];
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      months.push(d.toLocaleString('default', { month: 'short' }));
    }
    
    // Set up 6 months of data ending in current month
    const trendData = months.map((m, idx) => {
      return { month: m, count: 0, index: idx };
    });

    // Populate data with real timestamps or deterministic offsets
    filteredData.forEach((t) => {
      // Deterministic fallback month based on uid length
      const fallbackIdx = (t.id ? t.id.length : 12) % 6;
      let monthIndex = fallbackIdx;
      
      const regTime = t.timestamp || (t.subscriptionExpiry ? t.subscriptionExpiry - 365 * 24 * 60 * 60 * 1000 : null);
      if (regTime) {
        const date = new Date(regTime);
        const diffMonths = (now.getFullYear() - date.getFullYear()) * 12 + now.getMonth() - date.getMonth();
        if (diffMonths >= 0 && diffMonths < 6) {
          monthIndex = 5 - diffMonths;
        }
      }
      trendData[monthIndex].count += 1;
    });

    return trendData;
  }, [filteredData]);

  // Line Chart computations
  const maxTrendValue = Math.max(...signupTrend.map((d) => d.count), 5);
  const chartWidth = 500;
  const chartHeight = 160;
  const padding = 20;

  const points = useMemo(() => {
    return signupTrend.map((d, i) => {
      const x = padding + (i * (chartWidth - 2 * padding)) / (signupTrend.length - 1);
      const y = chartHeight - padding - (d.count * (chartHeight - 2 * padding)) / maxTrendValue;
      return { x, y, ...d };
    });
  }, [signupTrend, maxTrendValue]);

  const pathD = useMemo(() => {
    if (points.length === 0) return '';
    return points.reduce((acc, p, i) => {
      return i === 0 ? `M ${p.x} ${p.y}` : `${acc} L ${p.x} ${p.y}`;
    }, '');
  }, [points]);

  const areaD = useMemo(() => {
    if (points.length === 0) return '';
    const first = points[0];
    const last = points[points.length - 1];
    return `${pathD} L ${last.x} ${chartHeight - padding} L ${first.x} ${chartHeight - padding} Z`;
  }, [points, pathD]);

  // Donut chart calculations
  const activePercent = totalTeachersCount > 0 ? (activeSubsCount / totalTeachersCount) * 100 : 0;
  const donutDashArray = 2 * Math.PI * 15.915; // Circumference ≈ 100
  const activeOffset = donutDashArray - (activePercent / 100) * donutDashArray;

  return (
    <main className="dashboard-content animate-fade-in">
      <div className="page-kicker">Administrator Overview</div>
      <div className="header-actions">
        <div>
          <h1>System Analytics</h1>
          <p>Real-time analytics of teacher registrations, student enrollment, and active subscriptions.</p>
        </div>
      </div>

      {/* Filter Options Row */}
      <section className="glass-panel analytics-filters-bar">
        <div className="filter-group">
          <label htmlFor="timeframe-select">Timeframe</label>
          <select 
            id="timeframe-select"
            value={timeframe} 
            onChange={(e) => setTimeframe(e.target.value)}
          >
            <option value="all">All Time</option>
            <option value="30days">Last 30 Days</option>
            <option value="90days">Last 90 Days</option>
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="district-select">District</label>
          <select 
            id="district-select"
            value={selectedDistrict} 
            onChange={(e) => setSelectedDistrict(e.target.value)}
          >
            <option value="all">All Districts</option>
            {districts.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="status-select">Subscription</label>
          <select 
            id="status-select"
            value={subStatus} 
            onChange={(e) => setSubStatus(e.target.value)}
          >
            <option value="all">All Profiles</option>
            <option value="active">Active Only</option>
            <option value="inactive">Inactive Only</option>
          </select>
        </div>
      </section>

      {loading ? (
        <div className="loading">Compiling metrics database...</div>
      ) : error ? (
        <div className="glass-panel empty-state error-state">
          <h3>Database Connection Error</h3>
          <p>{error}</p>
        </div>
      ) : (
        <>
          {/* Key Metrics Cards */}
          <section className="summary-grid">
            <div className="glass-panel summary-card">
              <span>Registered Teachers</span>
              <strong>{totalTeachersCount}</strong>
            </div>
            <div className="glass-panel summary-card">
              <span>Active Subscriptions</span>
              <strong>{activeSubsCount}</strong>
            </div>
            <div className="glass-panel summary-card">
              <span>Enrolled Students</span>
              <strong>{totalStudentsCount}</strong>
            </div>
            <div className="glass-panel summary-card">
              <span>Estimated Revenue</span>
              <strong>₹{estimatedRevenue.toLocaleString()}</strong>
            </div>
          </section>

          {/* Graphs Grid */}
          <section className="analytics-graphs-grid">
            {/* 1. Signup Trends Line Graph */}
            <div className="glass-panel graph-panel-card">
              <h3>Teacher Sign-Up Trends</h3>
              <p className="card-subtitle">Activity logs over the last 6 months</p>
              
              <div className="chart-canvas-wrapper">
                <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} className="svg-line-chart">
                  <defs>
                    <linearGradient id="lineAreaGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="var(--primary-color)" stopOpacity="0.25" />
                      <stop offset="100%" stopColor="var(--primary-color)" stopOpacity="0.0" />
                    </linearGradient>
                  </defs>

                  {/* Horizontal grid lines */}
                  <line x1={padding} y1={padding} x2={chartWidth - padding} y2={padding} stroke="var(--border-color)" strokeWidth="0.5" strokeDasharray="3 3" />
                  <line x1={padding} y1={chartHeight / 2} x2={chartWidth - padding} y2={chartHeight / 2} stroke="var(--border-color)" strokeWidth="0.5" strokeDasharray="3 3" />
                  <line x1={padding} y1={chartHeight - padding} x2={chartWidth - padding} y2={chartHeight - padding} stroke="var(--border-color)" strokeWidth="1" />

                  {/* Shaded Area */}
                  {points.length > 0 && (
                    <path d={areaD} fill="url(#lineAreaGrad)" />
                  )}

                  {/* Main Line path */}
                  {points.length > 0 && (
                    <path d={pathD} fill="none" stroke="var(--primary-color)" strokeWidth="3" strokeLinecap="round" />
                  )}

                  {/* Nodes */}
                  {points.map((p) => (
                    <circle
                      key={p.month}
                      cx={p.x}
                      cy={p.y}
                      r={hoveredNode?.month === p.month ? 6 : 4}
                      fill="var(--surface-color)"
                      stroke="var(--primary-color)"
                      strokeWidth="3"
                      style={{ cursor: 'pointer', transition: 'all 0.15s ease' }}
                      onMouseEnter={() => setHoveredNode(p)}
                      onMouseLeave={() => setHoveredNode(null)}
                    />
                  ))}

                  {/* Month labels */}
                  {points.map((p) => (
                    <text
                      key={p.month}
                      x={p.x}
                      y={chartHeight - 4}
                      textAnchor="middle"
                      fontSize="9"
                      fontWeight="bold"
                      fill="var(--text-secondary)"
                    >
                      {p.month}
                    </text>
                  ))}
                </svg>

                {/* Floating Tooltip */}
                {hoveredNode && (
                  <div 
                    className="chart-node-tooltip glass-panel"
                    style={{ 
                      top: hoveredNode.y - 40, 
                      left: hoveredNode.x 
                    }}
                  >
                    <strong>{hoveredNode.count}</strong>
                    <span>Sign-ups in {hoveredNode.month}</span>
                  </div>
                )}
              </div>
            </div>

            {/* 2. Donut Subscription Ratio Chart */}
            <div className="glass-panel graph-panel-card donut-card">
              <h3>Subscription Ratio</h3>
              <p className="card-subtitle">Active vs Inactive accounts</p>

              <div className="donut-wrapper">
                <svg width="140" height="140" viewBox="0 0 42 42" className="svg-donut">
                  <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="var(--border-color)" strokeWidth="4.5" />
                  <circle 
                    cx="21" 
                    cy="21" 
                    r="15.915" 
                    fill="transparent" 
                    stroke="var(--primary-color)" 
                    strokeWidth="4.5" 
                    strokeDasharray={`${donutDashArray}`} 
                    strokeDashoffset={activeOffset}
                    strokeLinecap="round"
                    style={{ transition: 'stroke-dashoffset 0.6s ease' }}
                  />
                </svg>
                <div className="donut-center-labels">
                  <strong>{Math.round(activePercent)}%</strong>
                  <span>Active</span>
                </div>
              </div>

              <div className="donut-legend">
                <div className="legend-row">
                  <span className="dot dot-active" />
                  <span>Active ({activeSubsCount})</span>
                </div>
                <div className="legend-row">
                  <span className="dot dot-inactive" />
                  <span>Inactive ({inactiveSubsCount})</span>
                </div>
              </div>
            </div>

            {/* 3. District Density Bar Chart */}
            <div className="glass-panel graph-panel-card full-width-grid">
              <h3>Top Districts Density</h3>
              <p className="card-subtitle">Distribution of classrooms by district</p>
              
              <div className="district-bars-container">
                {districtDistribution.length === 0 ? (
                  <div className="empty-sub-state">No district data found for current filters.</div>
                ) : (
                  districtDistribution.map((item) => {
                    const pct = totalTeachersCount > 0 ? (item.count / totalTeachersCount) * 100 : 0;
                    return (
                      <div key={item.name} className="district-bar-row">
                        <div className="bar-row-label">
                          <strong>{item.name}</strong>
                          <span>{item.count} teacher{item.count !== 1 && 's'}</span>
                        </div>
                        <div className="bar-track">
                          <div 
                            className="bar-fill" 
                            style={{ 
                              width: `${pct}%`,
                              background: 'linear-gradient(90deg, var(--primary-color), var(--accent-color))' 
                            }} 
                          />
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </section>
        </>
      )}
    </main>
  );
}
