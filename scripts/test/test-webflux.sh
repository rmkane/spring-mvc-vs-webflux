#!/bin/bash

# Test script for WebFlux API (port 8081)
# Usage: ./test-webflux.sh <operation> [args...]
# Operations: get-all, get <id>, create <title> <author> <isbn>, update <id> <title> <author> <isbn>, delete <id>

BASE_URL="http://localhost:8081/api/v1/books"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Validate required environment variables
if [ -z "$SSL_CLIENT_SUBJECT_DN" ]; then
    echo -e "${RED}Error: SSL_CLIENT_SUBJECT_DN environment variable is not set${NC}"
    echo "Please set SSL_CLIENT_SUBJECT_DN before running this script."
    exit 1
fi

if [ -z "$SSL_CLIENT_ISSUER_DN" ]; then
    echo -e "${RED}Error: SSL_CLIENT_ISSUER_DN environment variable is not set${NC}"
    echo "Please set SSL_CLIENT_ISSUER_DN before running this script."
    exit 1
fi

print_usage() {
    echo "Usage: $0 <operation> [args...]"
    echo ""
    echo "Operations:"
    echo "  get-all                    - Get all books"
    echo "  get <id>                   - Get a book by ID"
    echo "  create <title> <author> <isbn>  - Create a new book"
    echo "  update <id> <title> <author> <isbn>  - Update a book"
    echo "  delete <id>                - Delete a book by ID"
    echo ""
    echo "Required environment variables:"
    echo "  SSL_CLIENT_SUBJECT_DN      - Subject Distinguished Name (DN) for authentication (required)"
    echo "  SSL_CLIENT_ISSUER_DN       - Issuer Distinguished Name (DN) for authentication (required)"
    echo ""
    echo "Available users (from LDAP):"
    echo "  cn=jdoe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org"
    echo "    - ACME_READ_WRITE (full access)"
}

get_all() {
    echo -e "${BLUE}GET ${BASE_URL}${NC}"
    local response=$(curl -s -w "\n%{http_code}" \
        -H "ssl-client-subject-dn: ${SSL_CLIENT_SUBJECT_DN}" \
        -H "ssl-client-issuer-dn: ${SSL_CLIENT_ISSUER_DN}" \
        -H "Content-Type: application/json" \
        "${BASE_URL}")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo -e "HTTP Status: ${http_code}"
    echo ""
}

get_one() {
    local id=$1
    if [ -z "$id" ]; then
        echo -e "${RED}Error: Book ID is required${NC}"
        print_usage
        exit 1
    fi

    echo -e "${BLUE}GET ${BASE_URL}/${id}${NC}"
    local response=$(curl -s -w "\n%{http_code}" \
        -H "ssl-client-subject-dn: ${SSL_CLIENT_SUBJECT_DN}" \
        -H "ssl-client-issuer-dn: ${SSL_CLIENT_ISSUER_DN}" \
        -H "Content-Type: application/json" \
        "${BASE_URL}/${id}")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo -e "HTTP Status: ${http_code}"
    echo ""
}

create() {
    local title=$1
    local author=$2
    local isbn=$3

    if [ -z "$title" ] || [ -z "$author" ] || [ -z "$isbn" ]; then
        echo -e "${RED}Error: title, author, and isbn are required${NC}"
        print_usage
        exit 1
    fi

    local json_body=$(cat <<EOF
{
  "title": "${title}",
  "author": "${author}",
  "isbn": "${isbn}"
}
EOF
)

    echo -e "${BLUE}POST ${BASE_URL}${NC}"
    echo -e "${BLUE}Body: ${json_body}${NC}"
    local response=$(curl -s -w "\n%{http_code}" \
        -X POST \
        -H "ssl-client-subject-dn: ${SSL_CLIENT_SUBJECT_DN}" \
        -H "ssl-client-issuer-dn: ${SSL_CLIENT_ISSUER_DN}" \
        -H "Content-Type: application/json" \
        -d "${json_body}" \
        "${BASE_URL}")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo -e "HTTP Status: ${http_code}"
    echo ""
}

update() {
    local id=$1
    local title=$2
    local author=$3
    local isbn=$4

    if [ -z "$id" ] || [ -z "$title" ] || [ -z "$author" ] || [ -z "$isbn" ]; then
        echo -e "${RED}Error: id, title, author, and isbn are required${NC}"
        print_usage
        exit 1
    fi

    local json_body=$(cat <<EOF
{
  "title": "${title}",
  "author": "${author}",
  "isbn": "${isbn}"
}
EOF
)

    echo -e "${BLUE}PUT ${BASE_URL}/${id}${NC}"
    echo -e "${BLUE}Body: ${json_body}${NC}"
    local response=$(curl -s -w "\n%{http_code}" \
        -X PUT \
        -H "ssl-client-subject-dn: ${SSL_CLIENT_SUBJECT_DN}" \
        -H "ssl-client-issuer-dn: ${SSL_CLIENT_ISSUER_DN}" \
        -H "Content-Type: application/json" \
        -d "${json_body}" \
        "${BASE_URL}/${id}")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo -e "HTTP Status: ${http_code}"
    echo ""
}

delete() {
    local id=$1
    if [ -z "$id" ]; then
        echo -e "${RED}Error: Book ID is required${NC}"
        print_usage
        exit 1
    fi

    echo -e "${BLUE}DELETE ${BASE_URL}/${id}${NC}"
    local http_code=$(curl -s -w "%{http_code}" -o /dev/null \
        -X DELETE \
        -H "ssl-client-subject-dn: ${SSL_CLIENT_SUBJECT_DN}" \
        -H "ssl-client-issuer-dn: ${SSL_CLIENT_ISSUER_DN}" \
        -H "Content-Type: application/json" \
        "${BASE_URL}/${id}")
    echo -e "HTTP Status: ${http_code}"
    echo ""
}

# Main script logic
case "$1" in
    get-all)
        get_all
        ;;
    get)
        get_one "$2"
        ;;
    create)
        create "$2" "$3" "$4"
        ;;
    update)
        update "$2" "$3" "$4" "$5"
        ;;
    delete)
        delete "$2"
        ;;
    *)
        print_usage
        exit 1
        ;;
esac

