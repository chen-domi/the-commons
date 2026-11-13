import { render, screen } from '@testing-library/react';
import App from './App';
import { getCurrentUser } from './api/authApi';

jest.mock('./api/authApi');

test('renders Google sign-in when there is no authenticated session', async () => {
  (getCurrentUser as jest.MockedFunction<typeof getCurrentUser>).mockResolvedValue(null);
  render(<App />);
  expect(await screen.findByRole('heading', { name: /the commons/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /sign in with BC Google/i })).toBeInTheDocument();
});
