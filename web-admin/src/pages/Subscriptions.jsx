import { useEffect, useMemo, useState } from 'react';
import { collection, doc, onSnapshot, query, updateDoc, orderBy, limit, where, getCountFromServer } from 'firebase/firestore';
import { db } from '../firebase';
import './Subscriptions.css';

const FILTERS = [
  { key: 'all', label: 'All' },
  { key: 'pending', label: 'Pending' },
  { key: 'approved', label: 'Approved' },
  { key: 'rejected', label: 'Rejected' },
];

const formatDate = (timestamp) => {
  if (!timestamp) return 'No date';
  return new Date(timestamp).toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
};

const formatDateTime = (timestamp) => {
  if (!timestamp) return 'No date';
  return new Date(timestamp).toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export default function Subscriptions() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [filter, setFilter] = useState('all');
  const [error, setError] = useState(null);
  const [queryLimit, setQueryLimit] = useState(50);
  const [hasMore, setHasMore] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [dateFilter, setDateFilter] = useState('all'); // 'all', 'today', 'custom'
  const [customDate, setCustomDate] = useState('');
  const [counts, setCounts] = useState({ total: 0, pending: 0, approved: 0, rejected: 0 });

  // Fetch count of documents from server
  const fetchCounts = async () => {
    try {
      const coll = collection(db, 'subscriptions');
      const [totalSnap, pendingSnap, approvedSnap, rejectedSnap] = await Promise.all([
        getCountFromServer(coll),
        getCountFromServer(query(coll, where('status', '==', 'pending'))),
        getCountFromServer(query(coll, where('status', '==', 'approved'))),
        getCountFromServer(query(coll, where('status', '==', 'rejected')))
      ]);
      setCounts({
        total: totalSnap.data().count,
        pending: pendingSnap.data().count,
        approved: approvedSnap.data().count,
        rejected: rejectedSnap.data().count
      });
    } catch (err) {
      console.error('Error fetching counts:', err);
    }
  };

  useEffect(() => {
    setLoading(true);
    let q;

    if (searchTerm.trim() !== '') {
      // Direct query for teacherId (case-sensitive exact match)
      q = query(
        collection(db, 'subscriptions'),
        where('teacherId', '==', searchTerm.trim())
      );
    } else {
      if (customDate !== '') {
        const selectedDate = new Date(customDate);
        const startOfDay = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), selectedDate.getDate(), 0, 0, 0, 0).getTime();
        const endOfDay = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), selectedDate.getDate(), 23, 59, 59, 999).getTime();

        q = query(
          collection(db, 'subscriptions'),
          where('timestamp', '>=', startOfDay),
          where('timestamp', '<=', endOfDay),
          orderBy('timestamp', 'desc'),
          limit(queryLimit)
        );
      } else if (dateFilter === 'today') {
        const startOfToday = new Date();
        startOfToday.setHours(0, 0, 0, 0);
        q = query(
          collection(db, 'subscriptions'),
          where('timestamp', '>=', startOfToday.getTime()),
          orderBy('timestamp', 'desc'),
          limit(queryLimit)
        );
      } else {
        q = query(
          collection(db, 'subscriptions'),
          orderBy('timestamp', 'desc'),
          limit(queryLimit)
        );
      }
    }

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const data = [];
      snapshot.forEach((subscriptionDoc) => {
        data.push({ id: subscriptionDoc.id, ...subscriptionDoc.data() });
      });

      // Sort client-side in case where query doesn't specify orderBy
      if (searchTerm.trim() !== '') {
        data.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
      }

      setRequests(data);
      setHasMore(snapshot.size === queryLimit);
      setLoading(false);
      setError(null);
      
      // Refresh counts whenever the snapshot updates
      fetchCounts();
    }, (err) => {
      console.error('Error fetching requests:', err);
      setError(err.message);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [queryLimit, searchTerm, dateFilter, customDate]);

  const stats = useMemo(() => {
    return [
      { label: 'Total requests', value: counts.total },
      { label: 'Pending review', value: counts.pending },
      { label: 'Approved', value: counts.approved },
      { label: 'Rejected', value: counts.rejected },
    ];
  }, [counts]);

  const filteredRequests = requests.filter((req) => filter === 'all' || req.status === filter);

  const handleApprove = async () => {
    if (!selectedRequest) return;

    try {
      await updateDoc(doc(db, 'subscriptions', selectedRequest.id), {
        status: 'approved',
      });

      const oneYearFromNow = Date.now() + (365 * 24 * 60 * 60 * 1000);
      await updateDoc(doc(db, 'teachers', selectedRequest.teacherId), {
        subscriptionStatus: 'active',
        subscriptionExpiry: oneYearFromNow,
      });

      setSelectedRequest(null);
    } catch (err) {
      alert(`Failed to approve: ${err.message}`);
    }
  };

  const handleReject = async () => {
    if (!selectedRequest) return;

    try {
      await updateDoc(doc(db, 'subscriptions', selectedRequest.id), {
        status: 'rejected',
      });

      await updateDoc(doc(db, 'teachers', selectedRequest.teacherId), {
        subscriptionStatus: 'inactive',
      });

      setSelectedRequest(null);
    } catch (err) {
      alert(`Failed to reject: ${err.message}`);
    }
  };

  return (
    <main className="dashboard-content">
      <div className="page-kicker">Subscription Center</div>
      <div className="header-actions">
        <div>
          <h1>Payment Reviews</h1>
          <p>Review payment screenshots and manage user subscriptions.</p>
        </div>
      </div>

      <section className="summary-grid">
        {stats.map((item) => (
          <div key={item.label} className="glass-panel summary-card">
            <span>{item.label}</span>
            <strong>{item.value}</strong>
          </div>
        ))}
      </section>

      <div className="dashboard-controls-row">
        <div className="search-box glass-panel">
          <input 
            type="text" 
            placeholder="Search by Teacher ID..." 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
          />
          {searchTerm && (
            <button className="clear-btn" onClick={() => setSearchTerm('')} type="button">x</button>
          )}
        </div>

        <div className="date-filters glass-panel">
          <button 
            className={`date-filter-btn ${dateFilter === 'all' ? 'active' : ''}`}
            onClick={() => { setDateFilter('all'); setCustomDate(''); setQueryLimit(50); }}
            type="button"
          >
            All Time
          </button>
          <button 
            className={`date-filter-btn ${dateFilter === 'today' ? 'active' : ''}`}
            onClick={() => { setDateFilter('today'); setCustomDate(''); setQueryLimit(50); }}
            type="button"
          >
            Today
          </button>
          <div className="calendar-filter-wrapper">
            <input 
              type="date"
              value={customDate}
              onChange={(e) => {
                const val = e.target.value;
                setCustomDate(val);
                if (val) {
                  setDateFilter('custom');
                } else {
                  setDateFilter('all');
                }
                setQueryLimit(50);
              }}
              className="calendar-input"
            />
            {customDate && (
              <button 
                className="calendar-clear-btn" 
                onClick={() => { setCustomDate(''); setDateFilter('all'); setQueryLimit(50); }}
                type="button"
              >
                x
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="tabs glass-panel" role="tablist" aria-label="Subscription filters">
        {FILTERS.map((item) => (
          <button
            key={item.key}
            className={`tab-btn ${filter === item.key ? 'active' : ''}`}
            onClick={() => setFilter(item.key)}
            type="button"
          >
            {item.label}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loading">Loading records...</div>
      ) : error ? (
        <div className="glass-panel empty-state animate-fade-in error-state">
          <h3>Firebase Error</h3>
          <p>{error}</p>
          <p className="helper-text">Check your Firestore Database Security Rules in the Firebase Console.</p>
        </div>
      ) : filteredRequests.length === 0 ? (
        <div className="glass-panel empty-state animate-fade-in">
          <h3>No records found</h3>
          <p>There are no subscriptions matching this status.</p>
        </div>
      ) : (
        <>
          <div className="requests-grid">
            {filteredRequests.map((req) => (
              <article key={req.id} className="glass-panel request-card animate-fade-in">
                <div className="request-header">
                  <div>
                    <span className="label">Teacher</span>
                    <strong className="user-id">{req.teacherId ? `${req.teacherId.substring(0, 10)}...` : 'Unknown'}</strong>
                  </div>
                  <span className={`status-badge status-${req.status || 'pending'}`}>
                    {req.status || 'pending'}
                  </span>
                </div>

                <button
                  className="screenshot-container"
                  onClick={() => setSelectedRequest(req)}
                  type="button"
                  aria-label="View payment screenshot"
                >
                  {req.screenshotUrl ? (
                    <img 
                      src={req.screenshotUrl} 
                      alt="Payment screenshot" 
                      onError={(e) => console.error("Dashboard list screenshot failed to load. URL: " + req.screenshotUrl, e)}
                    />
                  ) : (
                    <span>No screenshot</span>
                  )}
                </button>

                <div className="request-actions">
                  <span className="date">{formatDate(req.timestamp)}</span>
                  <button
                    className="btn btn-primary"
                    onClick={() => setSelectedRequest(req)}
                    type="button"
                  >
                    Review
                  </button>
                </div>
              </article>
            ))}
          </div>

          {hasMore && !searchTerm && (
            <div className="load-more-container">
              <button 
                className="btn btn-primary load-more-btn" 
                onClick={() => setQueryLimit(prev => prev + 50)}
                type="button"
              >
                Load More (Showing {requests.length} of {counts.total})
              </button>
            </div>
          )}
        </>
      )}

      {selectedRequest && (
        <div className="modal-overlay animate-fade-in">
          <div className="glass-panel modal-content">
            <div className="modal-header">
              <div>
                <span className="label">Payment request</span>
                <h2>Subscription Details</h2>
              </div>
              <button className="close-btn" onClick={() => setSelectedRequest(null)} type="button">x</button>
            </div>
            <div className="modal-body">
              <dl className="detail-list">
                <div>
                  <dt>Teacher ID</dt>
                  <dd>{selectedRequest.teacherId || 'Unknown'}</dd>
                </div>
                <div>
                  <dt>Status</dt>
                  <dd><span className={`status-badge status-${selectedRequest.status || 'pending'}`}>{selectedRequest.status || 'pending'}</span></dd>
                </div>
                <div>
                  <dt>Date</dt>
                  <dd>{formatDateTime(selectedRequest.timestamp)}</dd>
                </div>
              </dl>
              <div className="full-screenshot">
                {selectedRequest.screenshotUrl ? (
                  <img 
                    src={selectedRequest.screenshotUrl} 
                    alt="Payment screenshot full view" 
                    onError={(e) => console.error("Dashboard modal screenshot failed to load. URL: " + selectedRequest.screenshotUrl, e)}
                  />
                ) : (
                  <span>No screenshot uploaded</span>
                )}
              </div>
            </div>
            <div className="modal-footer">
              {selectedRequest.status === 'pending' ? (
                <>
                  <button className="btn btn-danger" onClick={handleReject} type="button">Reject</button>
                  <button className="btn btn-success" onClick={handleApprove} type="button">Approve & Activate</button>
                </>
              ) : (
                <button className="btn" onClick={() => setSelectedRequest(null)} type="button">Close</button>
              )}
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
