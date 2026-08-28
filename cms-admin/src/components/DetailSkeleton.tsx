export const DetailSkeleton = () => (
  <div className="client-detail-page">
    <div className="page-header" style={{ alignItems: 'center' }}>
      <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
        <div className="skeleton skeleton-avatar" style={{ width: '64px', height: '64px' }} />
        <div>
          <div className="skeleton skeleton-title" style={{ width: '200px' }} />
          <div className="skeleton skeleton-text" style={{ width: '140px', marginBottom: 0 }} />
        </div>
      </div>
    </div>
    
    <div style={{ display: 'flex', gap: '20px', marginTop: '32px' }}>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div className="table-card" style={{ padding: '24px' }}>
          <div className="skeleton skeleton-title" style={{ width: '120px' }} />
          <div className="skeleton skeleton-text" />
          <div className="skeleton skeleton-text" style={{ width: '80%' }} />
        </div>
        <div className="table-card" style={{ padding: '24px' }}>
          <div className="skeleton skeleton-title" style={{ width: '120px' }} />
          <div className="skeleton skeleton-text" />
          <div className="skeleton skeleton-text" style={{ width: '80%' }} />
        </div>
      </div>
      <div style={{ width: '320px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div className="table-card" style={{ padding: '24px' }}>
          <div className="skeleton skeleton-title" style={{ width: '140px' }} />
          <div className="skeleton skeleton-text" />
          <div className="skeleton skeleton-text" style={{ width: '60%' }} />
          <div className="skeleton skeleton-text" style={{ width: '70%' }} />
        </div>
      </div>
    </div>
  </div>
);
