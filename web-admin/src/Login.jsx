import { useState } from 'react';
import { signInWithEmailAndPassword } from 'firebase/auth';
import { useNavigate } from 'react-router-dom';
import { auth } from './firebase';
import './Login.css';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');

    try {
      await signInWithEmailAndPassword(auth, email, password);
      navigate('/');
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="login-container animate-fade-in">
      <section className="login-hero">
        <div className="brand-mark">MS</div>
        <h1>MySchool Admin</h1>
        <p>
          Manage school subscriptions, teachers, classes, and payment approvals from one clean workspace.
        </p>
        <div className="login-highlights">
          <span>Subscription review</span>
          <span>Teacher profiles</span>
          <span>Student insights</span>
        </div>
        <div className="login-preview glass-panel">
          <div className="preview-header">
            <div>
              <span>Today</span>
              <strong>Admin Overview</strong>
            </div>
            <span className="status-badge status-active">Live</span>
          </div>
          <div className="preview-metrics">
            <div>
              <span>Pending</span>
              <strong>12</strong>
            </div>
            <div>
              <span>Teachers</span>
              <strong>248</strong>
            </div>
            <div>
              <span>Students</span>
              <strong>4.8k</strong>
            </div>
          </div>
          <div className="preview-list">
            <span />
            <span />
            <span />
          </div>
        </div>
      </section>

      <div className="login-panel-wrap">
        <div className="login-orbit orbit-one" />
        <div className="login-orbit orbit-two" />
        <div className="glass-panel login-box">
        <div className="login-header">
          <h2>Welcome back</h2>
          <p>Sign in to continue to the admin panel</p>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleLogin}>
          <div className="input-group">
            <label>Email</label>
            <input
              type="email"
              placeholder="admin@myschool.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="input-group">
            <label>Password</label>
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <button type="submit" className="btn btn-primary login-btn">Login</button>
        </form>
      </div>
      </div>
    </div>
  );
}
