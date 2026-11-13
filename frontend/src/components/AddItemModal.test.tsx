import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import AddItemModal from './AddItemModal';
import { getNlpAvailability, parseItemDescription } from '../api/nlpApi';

jest.mock('../api/nlpApi');
jest.mock('../context/AuthContext', () => ({
  useAuth: () => ({ user: { currentOrg: 'CAB', isOSIAdmin: false } }),
}));

const availability = getNlpAvailability as jest.MockedFunction<typeof getNlpAvailability>;
const parse = parseItemDescription as jest.MockedFunction<typeof parseItemDescription>;

beforeEach(() => {
  jest.resetAllMocks();
  availability.mockResolvedValue(true);
  parse.mockResolvedValue({
    name: 'Folding chairs', category: 'Furniture', location: 'CAB closet', quantity: 12,
    lastUsed: null, shared: true, missingRequiredFields: [],
  });
});

test('AI suggestions require review and never save automatically', async () => {
  const save = jest.fn();
  render(<AddItemModal nextId={1} onSave={save} onClose={jest.fn()} />);
  const fill = screen.getByRole('button', { name: 'Fill with AI' });
  fireEvent.change(screen.getByLabelText('Describe your item'), { target: { value: '12 chairs in CAB closet, share them' } });
  await waitFor(() => expect(fill).toBeEnabled());
  fireEvent.click(fill);
  await screen.findByText('Review suggestions');
  expect(parse).toHaveBeenCalledWith('12 chairs in CAB closet, share them', 'CAB', expect.any(AbortSignal));
  expect(screen.getByLabelText('Item Name *')).toHaveValue('');
  expect(save).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: 'Use suggestions' }));
  expect(screen.getByLabelText('Item Name *')).toHaveValue('Folding chairs');
  expect(screen.getByLabelText('Quantity *')).toHaveValue(12);
  expect(save).not.toHaveBeenCalled();
  fireEvent.submit(screen.getByRole('button', { name: 'Add Item' }).closest('form')!);
  expect(save).toHaveBeenCalledWith(expect.objectContaining({ name: 'Folding chairs', quantity: 12, org: 'CAB', shared: true }));
});

test('disabled AI leaves manual entry available', async () => {
  availability.mockResolvedValue(false);
  render(<AddItemModal nextId={1} onSave={jest.fn()} onClose={jest.fn()} />);
  await screen.findByText(/AI is not configured/);
  expect(screen.getByRole('button', { name: 'Fill with AI' })).toBeDisabled();
  expect(screen.getByLabelText('Item Name *')).toBeEnabled();
  expect(screen.getByRole('button', { name: 'Add Item' })).toBeEnabled();
  expect(parse).not.toHaveBeenCalled();
});

test('provider errors preserve manually entered fields', async () => {
  parse.mockRejectedValue(new Error('AI is busy. Try later.'));
  render(<AddItemModal nextId={1} onSave={jest.fn()} onClose={jest.fn()} />);
  fireEvent.change(screen.getByLabelText('Item Name *'), { target: { value: 'My item' } });
  fireEvent.change(screen.getByLabelText('Describe your item'), { target: { value: 'chairs' } });
  const fill = screen.getByRole('button', { name: 'Fill with AI' });
  await waitFor(() => expect(fill).toBeEnabled());
  fireEvent.click(fill);
  expect(await screen.findByRole('alert')).toHaveTextContent('AI is busy');
  expect(screen.getByLabelText('Item Name *')).toHaveValue('My item');
});

test('missing AI fields do not erase existing manual values', async () => {
  parse.mockResolvedValue({ name: 'Chairs', category: null, location: null, quantity: null,
    lastUsed: null, shared: null, missingRequiredFields: ['category', 'location', 'quantity'] });
  render(<AddItemModal nextId={1} onSave={jest.fn()} onClose={jest.fn()} />);
  fireEvent.change(screen.getByLabelText('Quantity *'), { target: { value: '7' } });
  fireEvent.change(screen.getByLabelText('Describe your item'), { target: { value: 'chairs' } });
  const fill = screen.getByRole('button', { name: 'Fill with AI' });
  await waitFor(() => expect(fill).toBeEnabled());
  fireEvent.click(fill);
  fireEvent.click(await screen.findByRole('button', { name: 'Use suggestions' }));
  expect(screen.getByLabelText('Quantity *')).toHaveValue(7);
  expect(screen.getByRole('status')).toHaveTextContent('Check or complete');
});

test('unspecified quantity does not silently retain the default of one', async () => {
  parse.mockResolvedValue({ name: 'Chairs', category: null, location: null, quantity: null,
    lastUsed: null, shared: null, missingRequiredFields: ['category', 'location', 'quantity'] });
  const save = jest.fn();
  render(<AddItemModal nextId={1} onSave={save} onClose={jest.fn()} />);
  fireEvent.change(screen.getByLabelText('Describe your item'), { target: { value: 'chairs' } });
  const fill = screen.getByRole('button', { name: 'Fill with AI' });
  await waitFor(() => expect(fill).toBeEnabled());
  fireEvent.click(fill);
  fireEvent.click(await screen.findByRole('button', { name: 'Use suggestions' }));
  expect(screen.getByLabelText('Quantity *')).toHaveValue(null);
  fireEvent.submit(screen.getByRole('button', { name: 'Add Item' }).closest('form')!);
  expect(save).not.toHaveBeenCalled();
});
