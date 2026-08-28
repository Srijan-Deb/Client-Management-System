import { useState, useMemo } from 'react';
import { useProducts, useInvoices } from '../hooks/useBilling';
import { TableSkeleton } from '../components/TableSkeleton';
import PaymentModal from '../components/PaymentModal';
import { EmptyState } from '../components/EmptyState';
import type { Invoice } from '../types/billing';
import { billingApi } from '../api/billing';
import { LiveDonutChart } from '../components/charts/LiveDonutChart';
import { LiveBarChart } from '../components/charts/LiveBarChart';

type Tab = 'products' | 'invoices';

const STATUS_COLOR: Record<string, string> = {
  PAID: 'status-active',
  PENDING: 'status-pending',
  OVERDUE: 'status-suspended',
  CANCELLED: 'status-inactive',
};

const BillingPage = () => {
  const [activeTab, setActiveTab] = useState<Tab>('products');
  const [payingInvoice, setPayingInvoice] = useState<Invoice | null>(null);

  const { data: products = [], isLoading: productsLoading } = useProducts();
  const {
    data: invoices = [],
    isLoading: invoicesLoading,
    error: invoicesError,
  } = useInvoices();

  const handleDownloadPdf = async (pdfObjectKey: string) => {
    try {
      const url = await billingApi.getPdfDownloadUrl(pdfObjectKey);
      window.open(url, '_blank', 'noopener,noreferrer');
    } catch {
      alert('Failed to generate download link. Please try again.');
    }
  };

  const invoiceStatusChartData = useMemo(() => {
    const counts: Record<string, number> = {};
    invoices.forEach((inv) => { counts[inv.status] = (counts[inv.status] ?? 0) + 1; });
    const COLORS: Record<string, string> = {
      PAID: '#10b981', PENDING: '#f59e0b', OVERDUE: '#ef4444', CANCELLED: '#94a3b8',
    };
    return Object.entries(counts).map(([s, v]) => ({ name: s, value: v, color: COLORS[s] ?? '#8b91b0' }));
  }, [invoices]);

  const invoiceAmountByStatus = useMemo(() => {
    const totals: Record<string, number> = {};
    invoices.forEach((inv) => {
      totals[inv.status] = (totals[inv.status] ?? 0) + Number(inv.totalAmount);
    });
    const COLORS: Record<string, string> = {
      PAID: '#10b981', PENDING: '#f59e0b', OVERDUE: '#ef4444', CANCELLED: '#94a3b8',
    };
    return Object.entries(totals).map(([s, v]) => ({ label: s, value: Math.round(v), color: COLORS[s] ?? '#8b91b0' }));
  }, [invoices]);

  const tabs: { key: Tab; label: string; icon: string }[] = [
    { key: 'products', label: 'Product Catalog', icon: '📦' },
    { key: 'invoices', label: 'Invoices', icon: '🧾' },
  ];

  return (
    <div className="clients-page">
      {/* Header */}
      <div className="page-header">
        <div>
          <h2 className="page-heading">Billing</h2>
          <p className="page-subheading">Manage products, contracts, invoices and payments</p>
        </div>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '4px', borderBottom: '1px solid var(--border)', marginBottom: '24px' }}>
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            style={{
              padding: '12px 20px',
              background: 'none',
              border: 'none',
              borderBottom: activeTab === tab.key ? '2px solid var(--primary)' : '2px solid transparent',
              color: activeTab === tab.key ? 'var(--text-primary)' : 'var(--text-secondary)',
              fontWeight: activeTab === tab.key ? 600 : 400,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              fontSize: '14px',
            }}
          >
            <span>{tab.icon}</span>
            {tab.label}
          </button>
        ))}
      </div>

      {/* ── Product Catalog ─────────────────────────────────────────────── */}
      {activeTab === 'products' && (
        <>
          {productsLoading && <TableSkeleton rows={4} columns={5} />}
          {!productsLoading && products.length === 0 && (
            <EmptyState
              icon="📦"
              title="No products found"
              description="Your product catalog is currently empty. Add products directly in the billing-service database to manage subscriptions."
            />
          )}
          {!productsLoading && products.length > 0 && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '16px' }}>
              {products.map((p) => (
                <div
                  key={p.id}
                  className="table-card"
                  style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <h3 style={{ fontWeight: 600, fontSize: '15px', color: 'var(--text-primary)', margin: 0 }}>
                      {p.name}
                    </h3>
                    <span
                      className="badge"
                      style={{
                        background: 'var(--primary-muted)',
                        color: 'var(--primary)',
                        fontSize: '11px',
                      }}
                    >
                      {p.billingCycle}
                    </span>
                  </div>

                  {p.description && (
                    <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: 0, lineHeight: 1.5 }}>
                      {p.description}
                    </p>
                  )}

                  {p.category && (
                    <span style={{
                      fontSize: '11px', color: 'var(--text-muted)',
                      textTransform: 'uppercase', letterSpacing: '0.5px'
                    }}>
                      {p.category.name}
                    </span>
                  )}

                  <div style={{
                    marginTop: 'auto',
                    paddingTop: '12px',
                    borderTop: '1px solid var(--border)',
                    display: 'flex',
                    alignItems: 'baseline',
                    gap: '4px'
                  }}>
                    <span style={{ fontSize: '22px', fontWeight: 700, color: 'var(--text-primary)' }}>
                      {p.currency} {Number(p.price).toFixed(2)}
                    </span>
                    <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                      / {p.billingCycle.toLowerCase()}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* ── Invoices ────────────────────────────────────────────────────── */}
      {activeTab === 'invoices' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Chart row — only shown when there are invoices */}
          {!invoicesLoading && !invoicesError && invoices.length > 0 && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px' }}>
              <LiveDonutChart
                title="🧾 Invoice Distribution"
                data={invoiceStatusChartData}
                centerLabel="invoices"
                height={220}
              />
              <LiveBarChart
                title="💰 Revenue by Status"
                data={invoiceAmountByStatus}
                height={220}
                valuePrefix="₹"
              />
            </div>
          )}

          <div className="table-card">
          {invoicesLoading && <TableSkeleton rows={5} columns={6} />}


          {!invoicesLoading && invoicesError && (
            <EmptyState
              icon="⚠️"
              title="Failed to load invoices"
              description={(invoicesError as any)?.response?.data?.message ?? 'An error occurred while fetching invoices. Please try again later.'}
            />
          )}

          {!invoicesLoading && !invoicesError && (
            <table className="table">
              <thead>
                <tr>
                  <th className="th">Invoice #</th>
                  <th className="th">Due Date</th>
                  <th className="th">Subtotal</th>
                  <th className="th">Tax</th>
                  <th className="th">Total</th>
                  <th className="th">Status</th>
                  <th className="th">Actions</th>
                </tr>
              </thead>
              <tbody>
                {invoices.length === 0 && (
                  <tr>
                    <td colSpan={7} style={{ padding: 0 }}>
                      <EmptyState
                        icon="🧾"
                        title="No invoices found"
                        description="There are currently no invoices available for the selected clients."
                      />
                    </td>
                  </tr>
                )}
                {invoices.map((inv) => (
                  <tr key={inv.id} className="tr">
                    <td className="td" style={{ fontWeight: 600 }}>{inv.invoiceNumber}</td>
                    <td className="td td-secondary">
                      {new Date(inv.dueDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                    </td>
                    <td className="td td-secondary">{inv.currency} {Number(inv.subtotal).toFixed(2)}</td>
                    <td className="td td-secondary">{inv.currency} {Number(inv.taxAmount).toFixed(2)}</td>
                    <td className="td" style={{ fontWeight: 600 }}>{inv.currency} {Number(inv.totalAmount).toFixed(2)}</td>
                    <td className="td">
                      <span className={`badge ${STATUS_COLOR[inv.status] ?? 'status-pending'}`}>{inv.status}</span>
                    </td>
                    <td className="td" style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                      {inv.pdfObjectKey && (
                        <button
                          className="btn btn-ghost btn-sm"
                          title="Download PDF"
                          onClick={() => handleDownloadPdf(inv.pdfObjectKey!)}
                        >
                          📄 PDF
                        </button>
                      )}
                      {inv.status === 'PENDING' && (
                        <button
                          className="btn btn-primary btn-sm"
                          onClick={() => setPayingInvoice(inv)}
                        >
                          💳 Pay
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        </div>
      )}


      {/* Payment Modal */}
      {payingInvoice && (
        <PaymentModal
          invoice={payingInvoice}
          clientEmail=""   // populated from client context when embedded on client detail
          onClose={() => setPayingInvoice(null)}
        />
      )}
    </div>
  );
};

export default BillingPage;
