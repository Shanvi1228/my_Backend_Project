#!/bin/bash
set -e

BASE_URL="http://localhost:8081"
JWT_TOKEN="${JWT_TOKEN:-}"

if [ -z "$JWT_TOKEN" ]; then
  echo "ERROR: Set JWT_TOKEN env var before running."
  exit 1
fi

echo "=== Uploading test file ==="
echo "Fault tolerance test content" > /tmp/test-file.txt
FILE_ID=$(curl -s -X POST "$BASE_URL/api/files/upload" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@/tmp/test-file.txt" \
  -F "password=testpassword" | jq -r '.data.fileId')
echo "Uploaded fileId: $FILE_ID"

echo ""
echo "=== Checking replicas ==="
curl -s -H "Authorization: Bearer $JWT_TOKEN" \
  "$BASE_URL/api/admin/files/$FILE_ID/chunks" | jq .

echo ""
echo "=== Node status before failure ==="
curl -s -H "Authorization: Bearer $JWT_TOKEN" \
  "$BASE_URL/api/admin/nodes" | jq .

echo ""
echo "=== Stopping node-2 ==="
docker compose stop storage-node-2
echo "Waiting 15s for health check to detect DOWN..."
sleep 15

echo ""
echo "=== Node status after failure ==="
curl -s -H "Authorization: Bearer $JWT_TOKEN" \
  "$BASE_URL/api/admin/nodes" | jq .

echo ""
echo "=== Triggering repair ==="
curl -s -X POST "$BASE_URL/api/admin/repair" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq .
sleep 5

echo ""
echo "=== Replicas after repair ==="
curl -s -H "Authorization: Bearer $JWT_TOKEN" \
  "$BASE_URL/api/admin/files/$FILE_ID/chunks" | jq .

echo ""
echo "=== Downloading file despite node-2 being down ==="
curl -s -o /tmp/downloaded-file.txt \
  -H "Authorization: Bearer $JWT_TOKEN" \
  "$BASE_URL/api/files/$FILE_ID?password=testpassword"
echo "Download successful: $(wc -c < /tmp/downloaded-file.txt) bytes"
echo "Content: $(cat /tmp/downloaded-file.txt)"

echo ""
echo "=== Restarting node-2 ==="
docker compose start storage-node-2
echo "Done."
