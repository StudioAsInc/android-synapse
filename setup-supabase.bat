@echo off
echo ========================================
echo Synapse Social Media App - Supabase Setup
echo ========================================
echo.

echo Step 1: Checking MCP Configuration...
if exist ".kiro\settings\mcp.json" (
    echo ✓ MCP configuration found
) else (
    echo ✗ MCP configuration not found
    echo Please ensure Supabase MCP server is configured
    pause
    exit /b 1
)

echo.
echo Step 2: Please complete the following steps manually:
echo.
echo 1. Go to https://supabase.com/dashboard
echo 2. Create a new project named 'synapse-social-app'
echo 3. Copy your Project URL and Anon Key
echo 4. Update gradle.properties with your credentials
echo.
echo Press any key when you have your credentials ready...
pause > nul

echo.
echo Step 3: Building project...
call gradlew clean
if %ERRORLEVEL% NEQ 0 (
    echo ✗ Clean failed
    pause
    exit /b 1
)

call gradlew build
if %ERRORLEVEL% NEQ 0 (
    echo ✗ Build failed - check your Supabase credentials
    pause
    exit /b 1
)

echo.
echo ✓ Project built successfully!
echo.
echo Next steps:
echo 1. Run the database schema: supabase-database-schema.sql
echo 2. Set up RLS policies: supabase-rls-policies.sql  
echo 3. Configure storage: supabase-storage-setup.sql
echo 4. Enable real-time: supabase-realtime-setup.sql
echo.
echo Your Supabase backend is ready!
pause