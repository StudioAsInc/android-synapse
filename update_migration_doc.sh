#!/bin/bash

# File to store the list of java files
java_files_list="java_files.txt"
# Markdown file
markdown_file="docs/class_Migration.md"

# Get the list of java files
find . -type f -name "*.java" > "$java_files_list"

# Append header for unmigrated classes
echo -e "\n## Unmigrated Classes\n" >> "$markdown_file"

# Read each file from the list
while IFS= read -r file_path; do
  # Get line count
  line_count=$(wc -l < "$file_path" | tr -d ' ')
  # Get file name
  file_name=$(basename "$file_path")

  # Append to markdown file
  echo "### $file_name" >> "$markdown_file"
  echo "- **Path:** \`$file_path\`" >> "$markdown_file"
  echo "- **Status:** Not Migrated" >> "$markdown_file"
  echo "- **Line Count:** $line_count" >> "$markdown_file"
  echo "" >> "$markdown_file"
done < "$java_files_list"

# Clean up the temporary file
rm "$java_files_list"
