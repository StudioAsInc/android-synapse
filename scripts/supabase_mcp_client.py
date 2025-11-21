import asyncio
import os
import sys
import json
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def main():
    # Configuration (Hardcoded as requested)
    supabase_url = "https://apqvyyphlrtmuyjnzmuq.supabase.co"
    supabase_key = "sbp_3151110037cef4d5d58d56e48bd7e565705ef243"
    project_id = "apqvyyphlrtmuyjnzmuq"

    # Define server parameters
    server_params = StdioServerParameters(
        command="npx",
        args=["@supabase/mcp-server-supabase"],
        env={
            "SUPABASE_URL": supabase_url,
            "SUPABASE_ACCESS_TOKEN": supabase_key,
            **os.environ # Pass through other env vars
        }
    )

    print("Starting Supabase MCP client via SDK...")
    print(f"Target Project ID: {project_id}")

    try:
        async with stdio_client(server_params) as (read, write):
            async with ClientSession(read, write) as session:
                await session.initialize()

                # Call list_tables tool
                print("\nCalling list_tables tool...")
                result = await session.call_tool(
                    name="list_tables",
                    arguments={"project_id": project_id}
                )

                # Process result
                if result and result.content:
                    for content_item in result.content:
                        if content_item.type == "text":
                            try:
                                tables_data = json.loads(content_item.text)
                                print(f"\nFound {len(tables_data)} tables:")
                                for table in tables_data:
                                    schema = table.get('schema', 'public')
                                    name = table.get('name', 'unknown')
                                    rows = table.get('rows', 0)
                                    print(f"- {schema}.{name} (Rows: {rows})")
                            except json.JSONDecodeError:
                                print("Could not parse table data JSON.")
                                print(content_item.text)
                else:
                    print("No content returned from tool call.")

    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    asyncio.run(main())
