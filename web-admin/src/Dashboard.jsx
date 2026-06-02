import React, { useEffect, useState } from 'react';
import { collection, query, getDocs, doc, updateDoc, onSnapshot } from 'firebase/firestore';
import { auth, db } from './firebase';
import { signOut } from 'firebase/auth';
import { useNavigate } from 'react-router-dom';
import './Dashboard.css';

export default function Dashboard() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [filter, setFilter] = useState('all'); // 'all', 'pending', 'approved', 'rejected'
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    // Listen for ALL requests in real-time
    const q = query(collection(db, "subscriptions"));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const data = [];
      snapshot.forEach((doc) => {
        data.push({ id: doc.id, ...doc.data() });
      });
      // Sort by timestamp (newest first)
      data.sort((a, b) => b.timestamp - a.timestamp);
      setRequests(data);
      setLoading(false);
      setError(null);
    }, (err) => {
      console.error("Error fetching requests:", err);
      setError(err.message);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleLogout = () => {
    signOut(auth).then(() => navigate('/login'));
  };

  const handleApprove = async () => {
    if (!selectedRequest) return;
    
    try {
      await updateDoc(doc(db, "subscriptions", selectedRequest.id), {
        status: "approved"
      });

      const oneYearFromNow = Date.now() + (365 * 24 * 60 * 60 * 1000);
      await updateDoc(doc(db, "teachers", selectedRequest.teacherId), {
        subscriptionStatus: "active",
        subscriptionExpiry: oneYearFromNow
      });

      setSelectedRequest(null);
    } catch (err) {
      alert("Failed to approve: " + err.message);
    }
  };

  const handleReject = async () => {
    if (!selectedRequest) return;

    try {
      await updateDoc(doc(db, "subscriptions", selectedRequest.id), {
        status: "rejected"
      });

      await updateDoc(doc(db, "teachers", selectedRequest.teacherId), {
        subscriptionStatus: "inactive"
      });

      setSelectedRequest(null);
    } catch (err) {
      alert("Failed to reject: " + err.message);
    }
  };

  const filteredRequests = requests.filter(req => filter === 'all' || req.status === filter);

  return (
    <div className="dashboard-layout">
      <main className="dashboard-content">
        <div className="header-actions">
          <h1>Subscription History</h1>
          <p>Review payment screenshots and manage user subscriptions.</p>
        </div>

        <div className="tabs glass-panel">
          <button className={`tab-btn ${filter === 'all' ? 'active' : ''}`} onClick={() => setFilter('all')}>All History</button>
          <button className={`tab-btn ${filter === 'pending' ? 'active' : ''}`} onClick={() => setFilter('pending')}>Pending</button>
          <button className={`tab-btn ${filter === 'approved' ? 'active' : ''}`} onClick={() => setFilter('approved')}>Approved</button>
          <button className={`tab-btn ${filter === 'rejected' ? 'active' : ''}`} onClick={() => setFilter('rejected')}>Rejected</button>
        </div>

        {loading ? (
          <div className="loading">Loading records...</div>
        ) : error ? (
          <div className="glass-panel empty-state animate-fade-in" style={{borderColor: '#ef4444'}}>
            <h3 style={{color: '#ef4444'}}>Firebase Error</h3>
            <p>{error}</p>
            <p style={{fontSize: '12px', marginTop: '10px'}}>Check your Firestore Database Security Rules in the Firebase Console.</p>
          </div>
        ) : filteredRequests.length === 0 ? (
          <div className="glass-panel empty-state animate-fade-in">
            <h3>No records found</h3>
            <p>There are no subscriptions matching this status.</p>
          </div>
        ) : (
          <div className="requests-grid">
            {filteredRequests.map((req) => (
              <div key={req.id} className="glass-panel request-card animate-fade-in">
                <div className="request-header">
                  <span className="user-id">User: {req.teacherId ? req.teacherId.substring(0, 8) : 'Unknown'}...</span>
                  <span className={`status-badge status-${req.status}`}>
                    {req.status.toUpperCase()}
                  </span>
                </div>
                
                <div className="screenshot-container">
                  <img src={req.screenshotUrl} alt="Payment Screenshot" />
                </div>
                
                <div className="request-actions">
                  <span className="date">
                    {new Date(req.timestamp).toLocaleDateString()}
                  </span>
                  <button 
                    className="btn btn-primary"
                    onClick={() => setSelectedRequest(req)}
                  >
                    View Details
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Review Modal */}
      {selectedRequest && (
        <div className="modal-overlay animate-fade-in">
          <div className="glass-panel modal-content">
            <div className="modal-header">
              <h2>Subscription Details</h2>
              <button className="close-btn" onClick={() => setSelectedRequest(null)}>×</button>
            </div>
            <div className="modal-body">
              <p><strong>Teacher ID:</strong> {selectedRequest.teacherId}</p>
              <p><strong>Status:</strong> {selectedRequest.status.toUpperCase()}</p>
              <p><strong>Date:</strong> {new Date(selectedRequest.timestamp).toLocaleString()}</p>
              <div className="full-screenshot">
                <img src={selectedRequest.screenshotUrl} alt="Screenshot Full" />
              </div>
            </div>
            <div className="modal-footer">
              {selectedRequest.status === 'pending' ? (
                <>
                  <button className="btn btn-danger" onClick={handleReject}>Reject</button>
                  <button className="btn btn-success" onClick={handleApprove}>Approve & Activate</button>
                </>
              ) : (
                <button className="btn" onClick={() => setSelectedRequest(null)}>Close</button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
