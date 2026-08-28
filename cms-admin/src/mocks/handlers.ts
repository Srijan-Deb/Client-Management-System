import { http, HttpResponse } from 'msw';

// Helper to match full URLs since axios is configured with baseURL: http://localhost:8085
const url = (path: string) => `http://localhost:8085${path}`;

export const handlers = [
  http.get(url('/clients/api/v1/clients'), () => {
    return HttpResponse.json({
      content: [
        {
          id: 1,
          clientId: 1,
          name: 'Acme Corp',
          firstName: 'John',
          lastName: 'Doe',
          contactEmail: 'contact@acme.com',
          tier: 'PLATINUM',
          status: 'ACTIVE',
          createdAt: '2026-01-01T00:00:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20
    });
  }),
  
  http.get(url('/clients/api/v1/clients/:id'), () => {
    return HttpResponse.json({
      id: 1,
      name: 'Acme Corp',
      contactEmail: 'contact@acme.com',
      tier: 'PLATINUM',
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
    });
  }),

  http.post(url('/clients/api/v1/clients'), () => {
    return HttpResponse.json({ id: 2, name: 'New Client' }, { status: 201 });
  }),

  http.get(url('/clients/api/v1/tickets'), () => {
    return HttpResponse.json({
      content: [
        {
          id: 1,
          clientId: 1,
          subject: 'Cannot login',
          status: 'OPEN',
          priority: 'HIGH',
          category: 'Technical',
          createdAt: '2026-01-01T00:00:00Z',
        }
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20
    });
  }),

  http.post(url('/clients/api/v1/tickets'), () => {
    return HttpResponse.json({ id: 2, subject: 'New Ticket' }, { status: 201 });
  }),

  http.get(url('/billing/api/v1/billing/invoices/client/:clientId'), () => {
    return HttpResponse.json([
      {
        id: 1,
        clientId: 1,
        invoiceNumber: 'INV-001',
        subtotal: 100,
        taxAmount: 10,
        taxRate: 0.1,
        totalAmount: 110,
        currency: 'USD',
        status: 'UNPAID',
        dueDate: '2026-02-01T00:00:00Z',
      },
    ]);
  }),
  
  http.post(url('/billing/api/v1/billing/payments'), () => {
    return HttpResponse.json({
      id: 1,
      invoiceId: 1,
      status: 'PAID',
      stripePaymentId: 'pi_test_123',
    }, { status: 200 });
  }),

  http.get(url('/clients/api/v1/dashboard/metrics'), () => {
    return HttpResponse.json({
      totalClients: 10,
      openTickets: 3,
    });
  }),

  http.get(url('/billing/api/v1/dashboard/metrics'), () => {
    return HttpResponse.json({
      activeSubscriptions: 5,
      outstandingInvoices: 2,
    });
  }),
];
