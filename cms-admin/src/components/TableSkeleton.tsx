export const TableSkeleton = ({ rows = 5, columns = 5 }: { rows?: number; columns?: number }) => (
  <div className="table-card">
    <table className="table">
      <thead>
        <tr>
          {Array.from({ length: columns }).map((_, i) => (
            <th key={i} className="th">
              <div className="skeleton skeleton-text" style={{ width: '60px', marginBottom: 0 }} />
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {Array.from({ length: rows }).map((_, rowIndex) => (
          <tr key={rowIndex} className="tr">
            {Array.from({ length: columns }).map((_, colIndex) => (
              <td key={colIndex} className="td">
                {colIndex === 0 ? (
                  <div className="client-cell">
                    <div className="skeleton skeleton-avatar" />
                    <div style={{ flex: 1 }}>
                      <div className="skeleton skeleton-text" style={{ width: '120px' }} />
                      <div className="skeleton skeleton-text" style={{ width: '80px', marginBottom: 0 }} />
                    </div>
                  </div>
                ) : (
                  <div className="skeleton skeleton-text" style={{ width: '80px', marginBottom: 0 }} />
                )}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);
