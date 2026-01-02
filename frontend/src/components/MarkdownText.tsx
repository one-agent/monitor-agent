/**
 * TypeScript type definitions for Monitor Agent Chat Interface
 */

import React from 'react';
import ReactMarkdown from 'react-markdown';

interface MarkdownTextProps {
  children: string;
}

/**
 * 预处理 Markdown 文本，修复常见的格式问题
 */
const preprocessMarkdown = (text: string): string => {
  let processed = text;

  // 1. 修复标题标记被拆分的情况 (例如 "# # " 或 "### #")
  // 匹配连续的 #，中间可能包含空格，统一替换为连续的 #
  processed = processed.replace(/(#\s*){2,6}/g, (match) => match.replace(/\s+/g, ''));

  // 2. 确保标题 (#) 后面有空格
  processed = processed.replace(/(#{1,6})([^\s#\n])/g, '$1 $2');

  // 3. 确保标题前面有换行（如果前面有文字且不是换行）
  processed = processed.replace(/([^\n])\s*(#{1,6}\s)/g, '$1\n\n$2');

  // 4. 确保标题后面有换行（如果后面紧跟文字且不是换行）
  // 匹配：## 标题文字 接下来是正文 -> ## 标题文字\n\n接下来是正文
  // 这是一个启发式修复：如果一行以标题开头，但后面紧跟了列表或其他文字而没换行
  processed = processed.replace(/^(#{1,6}\s.+?)([^\n])([-*]|\d+\.)/gm, '$1\n$2$3');

  // 5. 修复列表项格式：确保 - 或 * 后面有空格
  processed = processed.replace(/^([-*])([^\s])/gm, '$1 $2');
  processed = processed.replace(/([^\n])\s*([-*]\s)/g, '$1\n$2');

  // 6. 修复列表项结束但后面紧跟正文的情况
  // 例如：- 列表项刚才我已核实 -> - 列表项\n刚才我已核实
  // 匹配：列表项文字 后面紧跟非标点的中文字符（简单判断）
  processed = processed.replace(/([-*]\s.+?)([\u4e00-\u9fa5]{2,})/g, (match, p1, p2) => {
    // 如果 p1 看起来已经包含了很多文字，且 p2 是新句子的开始
    if (p1.length > 10 && (p2.startsWith('刚才') || p2.startsWith('请问') || p2.startsWith('我已'))) {
      return p1 + '\n\n' + p2;
    }
    return match;
  });

  // 7. 修复编号列表格式
  processed = processed.replace(/(\d+\.)([^\s])/g, '$1 $2');
  processed = processed.replace(/([^\n])\s*(\d+\.\s)/g, '$1\n$2');

  return processed;
};

const MarkdownText: React.FC<MarkdownTextProps> = ({ children }) => {
  const processedContent = preprocessMarkdown(children);

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
        h1: ({ children }) => <h1 style={{ fontSize: '1.6em', fontWeight: 600, margin: '12px 0 6px 0', color: '#262626', lineHeight: 1.3, display: 'block' }}>{children}</h1>,
        h2: ({ children }) => <h2 style={{ fontSize: '1.4em', fontWeight: 600, margin: '10px 0 4px 0', color: '#262626', lineHeight: 1.3, display: 'block' }}>{children}</h2>,
        h3: ({ children }) => <h3 style={{ fontSize: '1.2em', fontWeight: 600, margin: '8px 0 4px 0', color: '#262626', lineHeight: 1.3, display: 'block' }}>{children}</h3>,
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
      {processedContent}
    </ReactMarkdown>
  );
};

export default MarkdownText;