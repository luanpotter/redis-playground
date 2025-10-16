import type { JSX } from 'react';
import { useEffect, useRef, useState } from 'react';
import { createWorkflow, startWorkflow } from './api';

interface Workflow {
  token: string;
  lines: string[];
  lastEventId: string;
  status: 'running' | 'ended' | 'disconnected';
}

export default function App(): JSX.Element {
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const seenEventIds = useRef<Map<string, Set<string>>>(new Map());
  const eventSourcesRef = useRef<Map<string, EventSource>>(new Map());

  const handleCreateWorkflow = async (): Promise<void> => {
    const token = await createWorkflow();
    await startWorkflow(token);
    setWorkflows((prev) => [...prev, { token, lines: [], lastEventId: '-', status: 'running' }]);
    seenEventIds.current.set(token, new Set());
  };

  const connectWorkflow = (workflowToken: string, fromEventId: string) => {
    // Close existing connection if any
    const existing = eventSourcesRef.current.get(workflowToken);
    if (existing) {
      existing.close();
    }

    const baseUrl = `http://localhost:8080/workflows/${workflowToken}/stream`;
    const queryParams =
      fromEventId === '-' ? '' : `?lastEventId=${encodeURIComponent(fromEventId)}`;
    const url = `${baseUrl}${queryParams}`;

    const eventSource = new EventSource(url);

    // Random disconnection: 20% chance every 2 seconds
    const disconnectTimer = setInterval(() => {
      if (Math.random() < 0.2) {
        eventSource.close();
        clearInterval(disconnectTimer);
        setWorkflows((prev) =>
          prev.map((w) =>
            w.token === workflowToken ? { ...w, status: 'disconnected' as const } : w,
          ),
        );
      }
    }, 2000);

    eventSource.addEventListener('output', (event) => {
      const eventId = event.lastEventId;
      const workflowSeenIds = seenEventIds.current.get(workflowToken);

      if (workflowSeenIds?.has(eventId)) {
        return;
      }
      workflowSeenIds?.add(eventId);

      setWorkflows((prev) =>
        prev.map((workflow) => {
          if (workflow.token !== workflowToken) {
            return workflow;
          }
          return {
            ...workflow,
            lines: [...workflow.lines, event.data],
            lastEventId: eventId,
          };
        }),
      );
    });

    eventSource.addEventListener('end', () => {
      clearInterval(disconnectTimer);
      setWorkflows((prev) =>
        prev.map((workflow) => {
          if (workflow.token !== workflowToken) return workflow;
          return {
            ...workflow,
            status: 'ended',
          };
        }),
      );
      eventSource.close();
    });

    eventSource.onerror = () => {
      console.error('EventSource error for workflow', workflowToken);
      clearInterval(disconnectTimer);
      eventSource.close();
      setWorkflows((prev) =>
        prev.map((workflow) =>
          workflow.token === workflowToken
            ? { ...workflow, status: 'disconnected' as const }
            : workflow,
        ),
      );
    };

    eventSourcesRef.current.set(workflowToken, eventSource);

    return () => {
      clearInterval(disconnectTimer);
      eventSource.close();
    };
  };

  const handleReconnect = (workflowToken: string, lastEventId: string) => {
    connectWorkflow(workflowToken, lastEventId);
    setWorkflows((prev) =>
      prev.map((workflow) =>
        workflow.token === workflowToken ? { ...workflow, status: 'running' as const } : workflow,
      ),
    );
  };

  useEffect(() => {
    workflows.forEach((workflow) => {
      if (workflow.status === 'running') {
        // Check if we already have a connection
        if (!eventSourcesRef.current.has(workflow.token)) {
          connectWorkflow(workflow.token, workflow.lastEventId);
        }
      }
    });
  }, [workflows]);

  return (
    <>
      <link rel="stylesheet" href="https://cdn.simplecss.org/simple.min.css" />
      <style>{`
        .code-output {
          font-family: 'Courier New', Courier, monospace;
          background-color: #1e1e1e;
          color: #d4d4d4;
          padding: 12px;
          border-radius: 4px;
          max-height: 400px;
          overflow-y: auto;
          white-space: pre;
          line-height: 1.2;
          margin: 12px 0;
        }
        .code-output div {
          margin: 0;
          padding: 0;
        }
        .workflow-status {
          display: flex;
          align-items: center;
          gap: 12px;
        }
        .status-badge {
          padding: 4px 8px;
          border-radius: 4px;
          font-size: 0.9em;
          font-weight: bold;
        }
        .status-running {
          background-color: #4caf50;
          color: white;
        }
        .status-ended {
          background-color: #2196f3;
          color: white;
        }
        .status-disconnected {
          background-color: #ff9800;
          color: white;
        }
      `}</style>
      <header>
        <h1>Redis Playground</h1>
        <button onClick={handleCreateWorkflow}>Create Workflow</button>
      </header>
      <main>
        {workflows.map((workflow) => (
          <article key={workflow.token}>
            <header className="workflow-status">
              <strong>{workflow.token}</strong>
              <span className={`status-badge status-${workflow.status}`}>{workflow.status}</span>
              {workflow.status === 'disconnected' && (
                <button onClick={() => handleReconnect(workflow.token, workflow.lastEventId)}>
                  Reconnect
                </button>
              )}
            </header>
            <div className="code-output">
              {workflow.lines.map((line, i) => (
                <div key={i}>{line}</div>
              ))}
            </div>
          </article>
        ))}
      </main>
    </>
  );
}
