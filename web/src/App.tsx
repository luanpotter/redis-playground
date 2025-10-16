import type { JSX } from 'react';
import { useEffect, useState } from 'react';
import type { OutputPart } from './api';
import { createWorkflow, startWorkflow, fetchOutput } from './api';

interface Workflow {
  token: string;
  parts: OutputPart[];
  status: 'running' | 'ended';
}

export default function App(): JSX.Element {
  const [workflows, setWorkflows] = useState<Workflow[]>([]);

  const handleCreateWorkflow = async (): Promise<void> => {
    const token = await createWorkflow();
    await startWorkflow(token);
    setWorkflows((prev) => [...prev, { token, parts: [], status: 'running' }]);
  };

  useEffect(() => {
    const intervals = workflows.map((workflow) => {
      if (workflow.status === 'ended') return null;

      return setInterval(() => {
        const lastToken =
          workflow.parts.length > 0
            ? (workflow.parts[workflow.parts.length - 1]?.token ?? '-')
            : '-';
        void fetchOutput(workflow.token, lastToken).then((response) => {
          if (response.parts.length > 0) {
            setWorkflows((prev) =>
              prev.map((w) => {
                if (w.token !== workflow.token) return w;
                const newParts = [...w.parts, ...response.parts];
                const hasEnd = response.parts.some((p) => p.data === 'end');
                return {
                  ...w,
                  parts: newParts,
                  status: hasEnd ? 'ended' : 'running',
                };
              }),
            );
          }
        });
      }, 100);
    });

    return () => {
      intervals.forEach((interval) => {
        if (interval) clearInterval(interval);
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
              {workflow.parts.map((part, i) => (
                <div key={i}>{part.data}</div>
              ))}
            </div>
          </article>
        ))}
      </main>
    </>
  );
}
