# Jules API

The Jules API lets you programmatically access Jules's capabilities to automate and enhance your software development lifecycle. You can use the API to create custom workflows, automate tasks like bug fixing and code reviews, and embed Jules's intelligence directly into the tools you use every day, such as Slack, Linear, and GitHub.

**Note:** The Jules API is in an alpha release, which means it is experimental. Be aware that we may change specifications, API keys, and definitions as we work toward stabilization. In the future, we plan to maintain at least one stable and one experimental version.

## Authentication

To get started with the Jules API, you'll need an API key.

### Generate Your API Key

In the Jules web app, go to the Settings page to create a new API key.

### Use Your API Key

To authenticate your requests, pass the API key in the `X-Goog-Api-Key` header of your API calls.

**Important:** Keep your API keys secure. Don't share them or embed them in public code. For your protection, any API keys found to be publicly exposed will be automatically disabled to prevent abuse.

## API concepts

The Jules API is built around a few core resources. Understanding these will help you use the API effectively.

- **Source**: An input source for the agent (e.g., a GitHub repository). Before using a source using the API, you must first install the Jules GitHub app through the Jules web app.
- **Session**: A continuous unit of work within a specific context, similar to a chat session. A session is initiated with a prompt and a source.
- **Activity**: A single unit of work within a Session. A Session contains multiple activities from both the user and the agent, such as generating a plan, sending a message, or updating progress.