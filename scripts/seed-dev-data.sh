#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASSWORD="${SEED_PASSWORD:-DemoPass2026}"

ORGANIZATION_TITLE="Demo Organization"
PROJECT_TITLE="Demo Project"
SECOND_PROJECT_TITLE="Second Project"

USERS=(
  "owner@example.com:demo_owner"
  "admin@example.com:demo_admin"
  "lead@example.com:demo_lead"
  "dev@example.com:demo_dev"
  "dev2@example.com:demo_dev2"
  "guest@example.com:demo_guest"
  "outsider@example.com:demo_outsider"
)

JSON="Content-Type: application/json"
BODY=""
STATUS=""

call() {
  local method=$1 path=$2 token=$3 data=${4:-}
  local args=(-s -o /tmp/seed_body.$$ -w "%{http_code}" -X "$method" "$BASE_URL$path" -H "$JSON")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-d "$data")
  STATUS=$(curl "${args[@]}")
  BODY=$(cat /tmp/seed_body.$$)
  rm -f /tmp/seed_body.$$
}

expect() {
  local label=$1; shift
  for code in "$@"; do
    [ "$STATUS" = "$code" ] && return 0
  done
  echo "FAILED: $label -> HTTP $STATUS: $BODY" >&2
  exit 1
}

field() {
  echo "$BODY" | sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p" | head -1
}

id_by_email() {
  echo "$BODY" | sed -n "s/.*\"id\":\"\([^\"]*\)\"[^{}]*\"email\":\"$1\".*/\1/p" | head -1
}

id_by_title() {
  echo "$BODY" | sed -n "s/.*\"id\":\"\([^\"]*\)\"[^{}]*\"title\":\"$1\".*/\1/p" | head -1
}

declare -A TOKEN

echo "== users"
for entry in "${USERS[@]}"; do
  email=${entry%%:*}
  username=${entry#*:}
  call POST /auth/register "" "{\"email\":\"$email\",\"username\":\"$username\",\"password\":\"$PASSWORD\"}"
  expect "register $email" 201 409
  call POST /auth/login "" "{\"email\":\"$email\",\"password\":\"$PASSWORD\"}"
  expect "login $email (if 401, the account exists with another password)" 200
  TOKEN[$username]=$(field accessToken)
  echo "  $email ($username)"
done

echo "== organization"
call GET "/organizations?size=100" "${TOKEN[demo_owner]}"
expect "list organizations" 200
ORG_ID=$(id_by_title "$ORGANIZATION_TITLE")
if [ -z "$ORG_ID" ]; then
  call POST /organizations "${TOKEN[demo_owner]}" "{\"title\":\"$ORGANIZATION_TITLE\",\"description\":\"Seeded demo data\"}"
  expect "create organization" 201
  ORG_ID=$(field id)
fi
call GET "/organizations/$ORG_ID/invite-code" "${TOKEN[demo_owner]}"
expect "invite code" 200
INVITE_CODE=$(field inviteCode)

for username in demo_admin demo_lead demo_dev demo_dev2 demo_guest; do
  call POST /organizations/join "${TOKEN[$username]}" "{\"inviteCode\":\"$INVITE_CODE\"}"
  expect "join $username" 201 409
done

call GET "/organizations/$ORG_ID/members?size=100" "${TOKEN[demo_owner]}"
expect "list members" 200
declare -A MEMBER
for entry in "${USERS[@]}"; do
  MEMBER[${entry#*:}]=$(id_by_email "${entry%%:*}")
done

call PATCH "/organizations/$ORG_ID/members/${MEMBER[demo_admin]}" "${TOKEN[demo_owner]}" '{"role":"ADMIN"}'
expect "promote demo_admin" 200 409

echo "== projects"
call GET "/organizations/$ORG_ID/projects?size=100" "${TOKEN[demo_owner]}"
expect "list projects" 200
PROJECT_ID=$(id_by_title "$PROJECT_TITLE")
SECOND_PROJECT_ID=$(id_by_title "$SECOND_PROJECT_TITLE")
if [ -z "$PROJECT_ID" ]; then
  call POST "/organizations/$ORG_ID/projects" "${TOKEN[demo_owner]}" "{\"title\":\"$PROJECT_TITLE\",\"description\":\"Main demo project\"}"
  expect "create project" 201
  PROJECT_ID=$(field id)
fi
if [ -z "$SECOND_PROJECT_ID" ]; then
  call POST "/organizations/$ORG_ID/projects" "${TOKEN[demo_owner]}" "{\"title\":\"$SECOND_PROJECT_TITLE\"}"
  expect "create second project" 201
  SECOND_PROJECT_ID=$(field id)
fi

add_to_project() {
  call POST "/organizations/$ORG_ID/projects/$1/members" "${TOKEN[demo_owner]}" \
    "{\"organizationMemberId\":\"${MEMBER[$2]}\",\"role\":\"$3\"}"
  expect "add $2 to project as $3" 201 409
}
add_to_project "$PROJECT_ID" demo_lead EXTENDED
add_to_project "$PROJECT_ID" demo_dev USER
add_to_project "$PROJECT_ID" demo_dev2 USER
add_to_project "$SECOND_PROJECT_ID" demo_dev ADMIN

call GET "/organizations/$ORG_ID/projects/$PROJECT_ID/members?size=100" "${TOKEN[demo_owner]}"
expect "list project members" 200
DEV_PM=$(id_by_email "dev@example.com")
DEV2_PM=$(id_by_email "dev2@example.com")
LEAD_PM=$(id_by_email "lead@example.com")

echo "== issues"
ISSUES="/organizations/$ORG_ID/projects/$PROJECT_ID/issues"
call GET "$ISSUES?size=1" "${TOKEN[demo_owner]}"
expect "list issues" 200
if echo "$BODY" | grep -q '"totalElements":0'; then
  issue() {
    call POST "$ISSUES" "${TOKEN[$1]}" "$2"
    expect "create issue" 201
    field id
  }
  status() {
    for s in "${@:3}"; do
      call PATCH "$ISSUES/$2/status" "${TOKEN[$1]}" "{\"status\":\"$s\"}"
      expect "status $s" 200
    done
  }
  assign() {
    call PATCH "$ISSUES/$2/assignee" "${TOKEN[$1]}" "{\"projectMemberId\":\"$3\"}"
    expect "assign" 200
  }
  comment() {
    call POST "$ISSUES/$2/comments" "${TOKEN[$1]}" "{\"text\":\"$3\"}"
    expect "comment" 201
  }

  EPIC=$(issue demo_lead '{"type":"EPIC","title":"Authentication","description":"Login, registration and sessions","priority":"HIGH"}')
  STORY=$(issue demo_lead "{\"type\":\"STORY\",\"title\":\"User can reset password\",\"storyPoints\":5,\"priority\":\"HIGH\",\"parentId\":\"$EPIC\"}")
  TASK=$(issue demo_lead "{\"type\":\"TASK\",\"title\":\"Add rate limiting to login\",\"priority\":\"MEDIUM\",\"parentId\":\"$EPIC\"}")
  BUG=$(issue demo_dev '{"type":"BUG","title":"Login fails with uppercase email","severity":"MAJOR","stepsToReproduce":"Register with Alice@Mail.com and log in","priority":"CRITICAL"}')
  FREE=$(issue demo_dev2 '{"type":"TASK","title":"Write README","priority":"LOW"}')
  DONE=$(issue demo_dev '{"type":"TASK","title":"Set up Docker Compose","priority":"MEDIUM"}')

  assign demo_lead "$STORY" "$DEV_PM"
  assign demo_lead "$TASK" "$DEV2_PM"
  assign demo_lead "$BUG" "$DEV_PM"
  assign demo_dev "$DONE" "$DEV_PM"
  assign demo_lead "$EPIC" "$LEAD_PM"

  status demo_dev "$STORY" IN_PROGRESS
  status demo_dev "$BUG" IN_PROGRESS IN_REVIEW
  status demo_dev "$DONE" IN_PROGRESS IN_REVIEW DONE

  comment demo_dev "$BUG" "Reproduced locally, fix is in review"
  comment demo_lead "$BUG" "Please add a regression test"
  comment demo_dev2 "$FREE" "I can take this next sprint"
  echo "  6 issues with assignees, statuses and comments"
else
  echo "  issues already exist, skipped"
fi

echo
echo "Done. Organization: $ORG_ID"
echo "All accounts use the password from SEED_PASSWORD (default in this script)."
