import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Cell,
} from 'recharts';

export interface BarDataPoint {
  label: string;
  value: number;
  color?: string;
}

interface LiveBarChartProps {
  data: BarDataPoint[];
  height?: number;
  title?: string;
  color?: string;
  valuePrefix?: string;
}

const CustomTooltip = ({ active, payload, label, valuePrefix }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{
      background: 'var(--bg-elevated)',
      border: '1px solid var(--border)',
      borderRadius: '8px',
      padding: '10px 14px',
      fontSize: '13px',
      boxShadow: 'var(--shadow)',
    }}>
      <p style={{ color: 'var(--text-muted)', marginBottom: '4px', fontWeight: 600 }}>{label}</p>
      <p style={{ color: payload[0].fill }}>
        {valuePrefix}{payload[0].value.toLocaleString()}
      </p>
    </div>
  );
};

export const LiveBarChart = ({ data, height = 240, title, color = 'var(--primary)', valuePrefix = '' }: LiveBarChartProps) => (
  <div className="table-card" style={{ padding: '20px' }}>
    {title && (
      <h3 style={{ fontSize: '15px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '16px' }}>
        {title}
      </h3>
    )}
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={data} margin={{ top: 4, right: 8, left: -8, bottom: 0 }} barCategoryGap="30%">
        <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
        <XAxis
          dataKey="label"
          tick={{ fill: 'var(--text-muted)', fontSize: 11 }}
          axisLine={{ stroke: 'var(--border)' }}
          tickLine={false}
        />
        <YAxis
          tick={{ fill: 'var(--text-muted)', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
          allowDecimals={false}
        />
        <Tooltip content={(props) => <CustomTooltip {...props} valuePrefix={valuePrefix} />} cursor={{ fill: 'rgba(99,102,241,0.05)' }} />
        <Bar dataKey="value" radius={[6, 6, 0, 0]}>
          {data.map((entry, index) => (
            <Cell key={index} fill={entry.color ?? color} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  </div>
);
