#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

readonly pg_container="duke-bank-db"
readonly pg_database="rinhadb"

function launch_prompt() {
  echo ''

  while true; do
    local press_to=""
    press_to+="\r\033[KPress \033[1;34m[r]\033[0m to reset the schema"
    press_to+=", \033[1;34m[s]\033[0m to restart the container"
    press_to+=" and \033[1;34m[q]\033[0m to quit"

    tput cup "$(tput lines)" 0
    echo -e -n "${press_to}"
    tput cup $(($(tput lines)-2)) 0

    read -n 1 -r -s -t 1 reply || true

    if [[ "$reply" != "" ]]; then
      case "$reply" in
         r)
           recreate_schema
           ;;
         s)
           clear
           tput cup $(($(tput lines)-2)) 0
           stop_database
           start_database
           recreate_schema
           ;;
         q)
           stop_database
           break ;;
         *)
           echo -n ""
           ;;
      esac
      reply=""
    fi
  done
}

function adjust_window() {
  clear

  tput csr 0 $(($(tput lines)-2))
  tput cup 0 0
}

function start_database() {
  echo "Starting ${pg_container}...";
  docker-compose -f postgres-local.yaml up --detach ${pg_container}
}

function stop_database() {
  echo "Stopping ${pg_container}...";
  docker-compose -f postgres-local.yaml stop ${pg_container} &> /dev/null || true
  sleep 0.5
}

function stop_database_silent() {
  docker-compose -f postgres-local.yaml stop ${pg_container} &> /dev/null || true
}

function recreate_schema() {
  until docker exec ${pg_container} pg_isready --host localhost > /dev/null; do
    sleep 0.5
  done; echo;

  docker exec ${pg_container} pg_isready --version

  exec_script_file "bank-schema.sql"
  exec_script_file "scripts/bank-schema-dev.sql"
}

function exec_script_file() {
  local -r script_file_name=$1
  echo "=> Running ${script_file_name}..."; echo;
  docker exec ${pg_container} psql -U duke -d ${pg_database} --echo-all --quiet \
     -f /postgresql/"${script_file_name}"; echo;
  echo "===================================================="; echo
}

function stop_gracefully() {
  stop_database
  exit 0
}

function main() {
  trap stop_gracefully INT

  adjust_window

  stop_database_silent
  start_database
  recreate_schema

  launch_prompt
}

main
