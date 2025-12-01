const { spawn } = require('child_process');

// Project ref can be passed as an env var or hardcoded for this specific test context
const PROJECT_REF = process.env.SUPABASE_PROJECT_REF || 'apqvyyphlrtmuyjznmuq';

const mcpServer = spawn('npx', [
  '-y',
  '@supabase/mcp-server-supabase@latest',
  '--project-ref',
  PROJECT_REF
], {
  env: process.env // Inherit env vars including SUPABASE_ACCESS_TOKEN
});

console.log(`Starting Supabase MCP Server for project: ${PROJECT_REF}`);

mcpServer.stderr.on('data', (data) => {
  // Ignore npx install logs but print actual errors if any
  const msg = data.toString();
  if (!msg.includes('npm install') && !msg.includes('package')) {
     console.error(`stderr: ${msg}`);
  }
});

let buffer = '';

mcpServer.stdout.on('data', (data) => {
  const chunk = data.toString();
  buffer += chunk;

  // Try to parse JSON messages from buffer
  const lines = buffer.split('\n');
  buffer = lines.pop(); // Keep the last incomplete line

  for (const line of lines) {
    if (!line.trim()) continue;
    try {
      const message = JSON.parse(line);

      if (message.id === 1) {
          console.log('Initialize response received.');
          // Initialize response received, send tools/list
          const listToolsRequest = {
            jsonrpc: '2.0',
            id: 2,
            method: 'tools/list'
          };
          console.log('Sending tools/list request...');
          mcpServer.stdin.write(JSON.stringify(listToolsRequest) + '\n');
      } else if (message.id === 2) {
          // Tools list received, we are done
          console.log('Test successful: Tools list received.');
          console.log('Tools found:', message.result.tools.map(t => t.name).join(', '));
          mcpServer.kill();
          process.exit(0);
      }

    } catch (e) {
       // Ignore parse errors for non-JSON output
    }
  }
});

// Send initialize request
const initRequest = {
  jsonrpc: '2.0',
  id: 1,
  method: 'initialize',
  params: {
    protocolVersion: '2024-11-05',
    capabilities: {},
    clientInfo: {
      name: 'test-client',
      version: '1.0.0'
    }
  }
};

console.log('Sending initialize request...');
mcpServer.stdin.write(JSON.stringify(initRequest) + '\n');

// Timeout to prevent hanging
setTimeout(() => {
    console.error('Timeout waiting for response.');
    mcpServer.kill();
    process.exit(1);
}, 30000);
