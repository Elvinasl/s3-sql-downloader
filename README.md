# S3 SQL Downloader CLI

A developer tool to bulk download files from AWS S3 based on SQL queries against a PostgreSQL metadata table.

It connects to your database, executes a custom SQL query to find file IDs and names, and then streams the corresponding objects from S3 to your local machine.

## 🚀 Features

* **Flexible Querying:** Select exactly which files you want using standard SQL (e.g., by user ID, date, or content type).
* **Streamed Downloads:** Uses AWS SDK v2 to stream files efficiently without loading them entirely into memory.
* **Safety First:**
    * Sanitizes filenames to prevent directory traversal issues.
    * Auto-renames duplicates (e.g., `invoice (1).pdf`) instead of overwriting.
    * Read-only database access recommended.

## 📋 Prerequisites

* **Java 21** or higher.
* **PostgreSQL** database containing file metadata.
* **AWS S3** bucket containing the actual files.

## ⚙️ Configuration

You can configure the tool using `src/main/resources/application-local.yaml` (gitignored) or via Environment Variables.

### Environment Variables

| Variable | Description | Default |
| :--- | :--- | :--- |
| `DB_HOST` | Database Hostname | `localhost` |
| `DB_NAME` | Database Name | `postgres` |
| `DB_USER` | Database Username | `postgres` |
| `DB_PASS` | Database Password | `password` |
| `AWS_ACCESS_KEY_ID` | AWS Access Key | *Empty* |
| `AWS_SECRET_ACCESS_KEY` | AWS Secret Key | *Empty* |
| `S3_BUCKET_NAME` | S3 Target Bucket | *Empty* |
| `S3_REGION` | AWS Region | `us-east-1` |



##  Usage
Once the application starts, you will see the interactive shell prompt.
You can run any SELECT query. The result set MUST contain columns named id (the S3 Key) and filename.


### Example: Download all files for a specific company
```shell
shell:> download-sql --query "SELECT * FROM files WHERE company_id = 3"
```
### Output
Files are saved to the downloads/ directory in the project root.

## License
MIT
