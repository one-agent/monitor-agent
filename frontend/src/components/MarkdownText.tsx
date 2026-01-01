import React from 'react';
import ReactMarkdown from 'react-markdown';

interface MarkdownTextProps {
  children: string;
}

const MarkdownText: React.FC<MarkdownTextProps> = ({ children }) => {
  return (
    <ReactMarkdown
      components={{
        code: ({ className, children, ...props }) => {
          const match = /language-(\w+)/.exec(className || '');
          return match ? (
            <pre style={{
              background: '#1e1e1e',
              color: '#ffffff',
              borderRadius: '6px',
              padding: '12px',
              margin: '8px 0',
              overflowX: 'auto',
              fontFamily: "'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace",
              fontSize: '13px',
              lineHeight: '1.45'
            }}>
              <code className={className} style={{ color: 'inherit' }} {...props}>
                {children}
              </code>
            </pre>
          ) : (
            <code style={{
              background: 'rgba(0, 0, 0, 0.05)',
              borderRadius: '3px',
              padding: '2px 4px',
              fontFamily: "'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace",
              fontSize: '0.9em',
              color: '#c41d7f'
            }} {...props}>
              {children}
            </code>
          );
        },
        blockquote: ({ children }) => (
          <blockquote style={{
            margin: '8px 0',
            padding: '4px 12px',
            borderLeft: '4px solid #1890ff',
            background: 'rgba(24, 144, 255, 0.04)',
            borderRadius: '0 4px 4px 0',
            color: 'rgba(0, 0, 0, 0.65)'
          }}>
            {children}
          </blockquote>
        ),
        h1: ({ children }) => <h1 style={{ fontSize: '1.6em', fontWeight: 600, margin: '12px 0 6px 0', color: '#262626', lineHeight: 1.3 }}>{children}</h1>,
        h2: ({ children }) => <h2 style={{ fontSize: '1.4em', fontWeight: 600, margin: '10px 0 4px 0', color: '#262626', lineHeight: 1.3 }}>{children}</h2>,
        h3: ({ children }) => <h3 style={{ fontSize: '1.2em', fontWeight: 600, margin: '8px 0 4px 0', color: '#262626', lineHeight: 1.3 }}>{children}</h3>,
        p: ({ children }) => <p style={{ margin: '4px 0', color: 'rgba(38, 38, 38, 0.85)', lineHeight: '1.5' }}>{children}</p>,
        ul: ({ children }) => <ul style={{ margin: '4px 0', paddingLeft: '20px' }}>{children}</ul>,
        ol: ({ children }) => <ol style={{ margin: '4px 0', paddingLeft: '20px' }}>{children}</ol>,
        li: ({ children }) => <li style={{ margin: '2px 0', color: 'rgba(38, 38, 38, 0.85)', lineHeight: '1.5' }}>{children}</li>,
        a: ({ href, children }) => (
          <a href={href} target="_blank" rel="noopener noreferrer" style={{ color: '#1890ff', textDecoration: 'none' }}>
            {children}
          </a>
        ),
        table: ({ children }) => (
          <div style={{ overflowX: 'auto', margin: '8px 0' }}>
            <table style={{ borderCollapse: 'collapse', width: '100%', background: 'rgba(0, 0, 0, 0.02)' }}>{children}</table>
          </div>
        ),
        th: ({ children }) => <th style={{ border: '1px solid rgba(0, 0, 0, 0.1)', padding: '6px 10px', textAlign: 'left', background: 'rgba(0, 0, 0, 0.05)', fontWeight: 600, color: '#262626' }}>{children}</th>,
        td: ({ children }) => <td style={{ border: '1px solid rgba(0, 0, 0, 0.1)', padding: '6px 10px', color: 'rgba(38, 38, 38, 0.85)' }}>{children}</td>,
      }}
    >
      {children}
    </ReactMarkdown>
  );
};

export default MarkdownText;