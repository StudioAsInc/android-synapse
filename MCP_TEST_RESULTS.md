# Supabase MCP Server Test Results

The Supabase MCP server was successfully tested.
**Date:** 2024-05-22
**Project Ref:** apqvyyphlrtmuyjznmuq
**Status:** Success

## Reproducing the Test

A script `test_supabase_mcp.js` has been included in the root directory to verify the MCP server connection.

**Prerequisites:**
- Node.js installed.
- `SUPABASE_ACCESS_TOKEN` environment variable set.

**Run the test:**
```bash
node test_supabase_mcp.js
```

## Available Tools

The following tools were returned by the MCP server `tools/list` command during the initial test:

- **run_sql_query**: Execute a SQL query on the project's database.
- **get_database_schema**: Gets the schema of the project's database.
- **execute_advisors**: Executes Supabase advisors (security, performance).
- **get_project_url**: Gets the API URL for a project.
- **get_publishable_keys**: Gets all publishable API keys for a project.
- **generate_typescript_types**: Generates TypeScript types for a project.
- **list_edge_functions**: Lists all Edge Functions in a Supabase project.
- **get_edge_function**: Retrieves file contents for an Edge Function.
- **deploy_edge_function**: Deploys an Edge Function to a Supabase project.
- **create_branch**: Creates a development branch on a Supabase project.
- **list_branches**: Lists all development branches of a Supabase project.
- **delete_branch**: Deletes a development branch.
- **merge_branch**: Merges migrations and edge functions from a development branch to production.
- **reset_branch**: Resets migrations of a development branch.
- **rebase_branch**: Rebases a development branch on production.
