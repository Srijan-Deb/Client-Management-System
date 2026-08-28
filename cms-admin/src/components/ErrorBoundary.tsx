import { Component, type ErrorInfo, type ReactNode } from 'react';
import * as Sentry from '@sentry/react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null
  };

  public static getDerivedStateFromError(error: Error): State {
    // Update state so the next render will show the fallback UI.
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error:', error, errorInfo);
    Sentry.captureException(error, { extra: { errorInfo } });
  }

  private handleRetry = () => {
    this.setState({ hasError: false, error: null });
  };

  public render() {
    if (this.state.hasError) {
      // Determine if it's an API error like 429 or 5xx
      // For axios, error.response.status might be available if the error is an AxiosError
      const isRateLimited = (this.state.error as any)?.response?.status === 429;
      const isServerError = (this.state.error as any)?.response?.status >= 500;
      
      const title = isRateLimited 
        ? 'Too Many Requests' 
        : isServerError 
          ? 'Server Error' 
          : 'Something went wrong';
          
      const message = isRateLimited
        ? 'You are making requests too quickly. Please wait a moment and try again.'
        : isServerError
          ? 'Our servers are experiencing issues. Please try again later.'
          : 'An unexpected error occurred in the application.';

      return (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100vh',
          padding: '24px',
          textAlign: 'center',
          background: 'var(--bg-base)',
          color: 'var(--text-primary)'
        }}>
          <div style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border)',
            padding: '40px',
            borderRadius: 'var(--radius)',
            maxWidth: '500px',
            width: '100%',
            boxShadow: 'var(--shadow)'
          }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
            <h1 style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '12px' }}>{title}</h1>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '24px', lineHeight: 1.5 }}>
              {message}
            </p>
            <button 
              onClick={this.handleRetry}
              className="btn btn-primary"
            >
              Try Again
            </button>
            
            {/* Show technical details if available and not a standard API error */}
            {!isRateLimited && !isServerError && this.state.error && (
              <details style={{ marginTop: '24px', textAlign: 'left' }}>
                <summary style={{ cursor: 'pointer', color: 'var(--text-muted)', fontSize: '13px' }}>Technical Details</summary>
                <pre style={{ 
                  marginTop: '12px', 
                  padding: '12px', 
                  background: 'var(--bg-elevated)', 
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '11px',
                  overflowX: 'auto',
                  color: 'var(--danger)'
                }}>
                  {this.state.error.toString()}
                </pre>
              </details>
            )}
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
