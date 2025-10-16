const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

interface CreateWorkflowResponse {
  workflowToken: string;
}

interface OutputPartsResponse {
  parts: OutputPart[];
  fromToken: string;
  lastToken: string;
}

export interface OutputPart {
  token: string;
  ts: number;
  data: string;
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

export async function fetchOutput(
  workflowToken: string,
  fromToken = '-',
  count = 500,
): Promise<OutputPartsResponse> {
  const response = await fetch(
    `${API_BASE}/workflows/${workflowToken}/output?fromToken=${encodeURIComponent(fromToken)}&count=${count}`,
  );
  if (!response.ok) {
    throw new Error('fetchLogs failed');
  }
  return (await response.json()) as OutputPartsResponse;
}
