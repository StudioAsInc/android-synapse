# MCP Servers

This document provides a comprehensive guide to the MCP (Minecraft Connected Papyrus) servers used for development, testing, and production.

## Server Environments

Below is a list of available server environments. Please use the appropriate server for your tasks.

### Development Server
- **Purpose:** Used for day-to-day development and initial testing of new features.
- **IP Address:** `dev.mcpservers.example.com`
- **Access:**
  - **SSH:** `ssh dev-user@dev.mcpservers.example.com`
  - **Minecraft:** `dev.mcpservers.example.com`
- **Notes:** This server is wiped and rebuilt nightly. Do not store any persistent data here.

### Staging Server
- **Purpose:** Used for pre-production testing and QA. This environment should be as close to production as possible.
- **IP Address:** `staging.mcpservers.example.com`
- **Access:**
  - **SSH:** `ssh staging-user@staging.mcpservers.example.com`
  - **Minecraft:** `staging.mcpservers.example.com`
- **Notes:** Deployments to staging are done manually after a pull request is merged into the `main` branch.

### Production Server
- **Purpose:** The live server that players connect to.
- **IP Address:** `play.mcpservers.example.com`
- **Access:** Access is restricted. Please contact the server administrator for access.
- **Notes:** All changes to the production server must go through the full development and staging process.

## Setup Instructions

To connect to a server, you will need:
1.  A Minecraft client of the correct version.
2.  SSH client for shell access (for developers).

### Connecting via SSH
```bash
ssh <username>@<server_ip>
```
You will be prompted for your password or passphrase for your SSH key.

## Troubleshooting

### Connection Timed Out
- **Issue:** You are unable to connect to the server.
- **Solution:**
  1.  Verify the server IP address is correct.
  2.  Check the server status on the internal dashboard.
  3.  Ensure your firewall is not blocking the connection.
  4.  Contact an administrator if the issue persists.

### Authentication Failed
- **Issue:** Your SSH or Minecraft login is rejected.
- **Solution:**
  1.  Double-check your username and password.
  2.  For SSH, ensure your public key is added to the server's `authorized_keys` file.
  3.  Contact an administrator to reset your credentials if needed.