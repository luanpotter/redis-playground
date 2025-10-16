const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

interface CreateWorkflowResponse {
  workflowToken: string;
}

export async function createWorkflow(): Promise<string> {
  const response = await fetch(`${API_BASE}/workflows`, { method: 'POST' });
  if (!response.ok) {
    throw new Error('createOp failed');
  }

  const json = (await response.json()) as CreateWorkflowResponse;
  return json.workflowToken;
}

export async function startWorkflow(workflowToken: string): Promise<void> {
  await fetch(`${API_BASE}/workflows/${workflowToken}/start`, { method: 'POST' });
}
