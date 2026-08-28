

const PlaceholderPage = ({ title }: { title: string }) => (
  <div className="placeholder-page">
    <div className="placeholder-icon">🚧</div>
    <h2>{title}</h2>
    <p>This module is coming in the next phase.</p>
  </div>
);

export const AccountsPage = () => <PlaceholderPage title="Accounts" />;
export const SupportPage = () => <PlaceholderPage title="Support Tickets" />;
export const UsersPage = () => <PlaceholderPage title="User Management" />;
