import { render, screen } from '@testing-library/react';
import App from './App';

test('renders the local Commons dashboard', async () => {
  render(<App />);
  expect(await screen.findByRole('heading', { name: /the commons/i })).toBeInTheDocument();
  expect(screen.getByText(/demo user/i)).toBeInTheDocument();
});
