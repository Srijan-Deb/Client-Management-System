import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
} from 'recharts';

export interface DonutSlice {
  name: string;
  value: number;
  color: string;
}

interface LiveDonutChartProps {
  data: DonutSlice[];
  height?: number;
  title?: string;
  centerLabel?: string;
}

const CustomTooltip = ({ active, payload }: any) => {
  if (!active || !payload?.length) return null;
  const d = payload[0];
  return (
    <div style={{
      background: 'var(--bg-elevated)',
      border: '1px solid var(--border)',
      borderRadius: '8px',
      padding: '10px 14px',
      fontSize: '13px',
      boxShadow: 'var(--shadow)',
    }}>
      <p style={{ color: d.payload.color, fontWeight: 600 }}>{d.name}</p>
      <p style={{ color: 'var(--text-secondary)', marginTop: '4px' }}>Count: <strong style={{ color: 'var(--text-primary)' }}>{d.value}</strong></p>
    </div>
  );
};

export const LiveDonutChart = ({ data, height = 240, title, centerLabel }: LiveDonutChartProps) => {
  const total = data.reduce((s, d) => s + d.value, 0);
  return (
    <div className="table-card" style={{ padding: '20px' }}>
      {title && (
        <h3 style={{ fontSize: '15px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '16px' }}>
          {title}
        </h3>
      )}
      <div style={{ position: 'relative' }}>
        <ResponsiveContainer width="100%" height={height}>
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={height * 0.28}
              outerRadius={height * 0.42}
              paddingAngle={3}
              dataKey="value"
              strokeWidth={0}
            >
              {data.map((slice, idx) => (
                <Cell key={idx} fill={slice.color} />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip />} />
            <Legend
              iconType="circle"
              iconSize={8}
              wrapperStyle={{ fontSize: '12px', color: 'var(--text-secondary)', paddingTop: '8px' }}
            />
          </PieChart>
        </ResponsiveContainer>
        {/* Centre label */}
        <div style={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -62%)',
          textAlign: 'center',
          pointerEvents: 'none',
        }}>
          <div style={{ fontSize: '22px', fontWeight: 700, color: 'var(--text-primary)' }}>{total}</div>
          <div style={{ fontSize: '10px', color: 'var(--text-muted)', letterSpacing: '0.05em', textTransform: 'uppercase' }}>{centerLabel ?? 'total'}</div>
        </div>
      </div>
    </div>
  );
};
