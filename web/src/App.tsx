import type { JSX } from 'react';
import { useEffect, useRef, useState } from 'react';
import { createWorkflow, startWorkflow } from './api';

interface Workflow {
  token: string;
  lines: string[];
  status: 'running' | 'ended';
}

export default function App(): JSX.Element {
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const seenEventIds = useRef<Map<string, Set<string>>>(new Map());

  const handleCreateWorkflow = async (): Promise<void> => {
    const token = await createWorkflow();
    await startWorkflow(token);
    setWorkflows((prev) => [...prev, { token, lines: [], status: 'running' }]);
    seenEventIds.current.set(token, new Set());
  };

  useEffect(() => {
    const eventSources = workflows.map((workflow) => {
      if (workflow.status === 'ended') return null;

      const eventSource = new EventSource(
        `http://localhost:8080/workflows/${workflow.token}/stream`,
      );

      eventSource.addEventListener('output', (event) => {
        const eventId = event.lastEventId;
        const workflowSeenIds = seenEventIds.current.get(workflow.token);

        // Deduplicate by event ID
        if (workflowSeenIds?.has(eventId)) {
          return;
        }
        workflowSeenIds?.add(eventId);

        setWorkflows((prev) =>
          prev.map((w) => {
            if (w.token !== workflow.token) return w;
            return {
              ...w,
              lines: [...w.lines, event.data],
            };
          }),
        );
      });

      eventSource.addEventListener('end', () => {
        setWorkflows((prev) =>
          prev.map((w) => {
            if (w.token !== workflow.token) return w;
            return {
              ...w,
              status: 'ended',
            };
          }),
        );
        eventSource.close();
      });

      eventSource.onerror = () => {
        console.error('EventSource error for workflow', workflow.token);
        eventSource.close();
      };

      return eventSource;
    });

    return () => {
      eventSources.forEach((es) => {
        if (es) es.close();
      });
    };
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
      `}</style>
      <header>
        <h1>Redis Playground</h1>
        <button onClick={handleCreateWorkflow}>Create Workflow</button>
      </header>
      <main>
        {workflows.map((workflow) => (
          <article key={workflow.token}>
            <header>
              <strong>{workflow.token}</strong> - {workflow.status}
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
