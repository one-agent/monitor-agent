/**
 * TypeScript type definitions for Monitor Agent Chat Interface
 */

import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface MarkdownTextProps {
  children: string;
}

/**
 * 预处理 Markdown 文本，修复常见的格式问题
 */
const preprocessMarkdown = (text: string): string => {
  let processed = text;

  // 1. 修复标题标记被拆分的情况 (例如 "# # " 或 "### #")
  processed = processed.replace(/(#\s*){2,6}/g, (match) => match.replace(/\s+/g, ''));

  // 2. 确保标题 (#) 后面有空格
  processed = processed.replace(/(#{1,6})([^\s#\n])/g, '$1 $2');

  // 3. 确保标题前面有换行
  processed = processed.replace(/([^\n])\s*(#{1,6}\s)/g, '$1\n\n$2');

  // 4. 确保标题后面有换行
  processed = processed.replace(/^(#{1,6}\s.+?)([^\n])([-*]|\d+\.)/gm, '$1\n$2$3');

  // 5. 修复列表项格式
  // 避免匹配负数 (如 -1)，只匹配后面非数字的情况
  processed = processed.replace(/^([-*])([^\s\d])/gm, '$1 $2');
  processed = processed.replace(/([^\n])\s*([-*]\s)/g, '$1\n$2');

  // 6. 修复列表项结束但后面紧跟正文的情况
  processed = processed.replace(/([-*]\s.+?)([\u4e00-\u9fa5]{2,})/g, (match, p1, p2) => {
    if (p1.length > 10 && (p2.startsWith('刚才') || p2.startsWith('请问') || p2.startsWith('我已'))) {
      return p1 + '\n\n' + p2;
    }
    return match;
  });

  // 7. 修复编号列表格式
  // 避免匹配小数 (如 0.1)，只匹配后面非数字的情况
  processed = processed.replace(/(\d+\.)([^\s\d])/g, '$1 $2');
  processed = processed.replace(/([^\n])\s*(\d+\.\s)/g, '$1\n$2');

  // 8. 修复表格格式：确保分隔行前后有且仅有一个换行
  // 匹配：|...| (Header) 紧接着 |---| (Separator)
  // 使用 $1\n$2 替换，\s* 会消耗掉原来的所有空白（包括多个换行），只保留一个 \n
  processed = processed.replace(/(\|)\s*(\|[-:]{3,})/g, '$1\n$2');
  
  // 匹配：|---| (Separator) 紧接着 |...| (Body)
  processed = processed.replace(/(\|[-:]{3,}\|)\s*(\|)/g, '$1\n$2');

  // 9. 修复表格行合并问题：|...| |...| -> |...|\n|...|
  // 匹配：结束的 |，任意空白，开始的 |，且后面紧跟非空白字符（避免破坏空单元格 | |）
  // 或者是后面跟中文/英文
  processed = processed.replace(/(\|)[ \t]+(\|[\u4e00-\u9fa5a-zA-Z])/g, '$1\n$2');

  return processed;
};

const MarkdownText: React.FC<MarkdownTextProps> = ({ children }) => {
  const processedContent = preprocessMarkdown(children);

  return (
    <ReactMarkdown
      remarkPlugins={[remarkGfm]}
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