import { getCsrfToken } from './authApi';

export interface OrganizationSummary {
  id: number;
  name: string;
}

export interface OrganizationMembership {
  organizationId: number;
  organizationName: string;
  role: 'EBOARD';
}

async function organizationResponseError(response: Response): Promise<Error> {
  try {
    const data: { message?: string } = await response.json();
    if (data.message) return new Error(data.message);
  } catch {
    // The response did not contain a JSON error body.
  }

  return new Error(`Organization request failed (${response.status})`);
}

export async function getOrganizations(): Promise<OrganizationSummary[]> {
  const response = await fetch('/api/organizations', {
    credentials: 'include',
  });

  if (!response.ok) throw await organizationResponseError(response);

  return response.json();
}

export async function getMyMemberships(): Promise<OrganizationMembership[]> {
  const response = await fetch('/api/organizations/mine', {
    credentials: 'include',
  });

  if (!response.ok) throw await organizationResponseError(response);

  return response.json();
}

export async function joinOrganization(
  organizationName: string,
  joinCode: string
): Promise<OrganizationMembership> {
  const csrf = await getCsrfToken();
  const response = await fetch('/api/organizations/join', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify({ organizationName, joinCode }),
  });

  if (!response.ok) throw await organizationResponseError(response);

  return response.json();
}

export async function createOrganization(
  name: string,
  joinCode: string
): Promise<OrganizationSummary> {
  const csrf = await getCsrfToken();
  const response = await fetch('/api/admin/organizations', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify({ name, joinCode }),
  });

  if (!response.ok) throw await organizationResponseError(response);

  return response.json();
}
