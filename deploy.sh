#!/bin/bash
# ============================================================
# INK SMS Gateway - Linux Deploy Script
# Usage:
#   ./deploy.sh start   [service]   Start all or one service
#   ./deploy.sh stop    [service]   Stop  all or one service
#   ./deploy.sh restart [service]   Restart all or one service
#   ./deploy.sh status              Show all services status
#   ./deploy.sh log     <service>   Tail log of a service
#
# Services: gateway | user-service | admin-service | api
# ============================================================

# ---- 路径与参数 ----
APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
LIB_DIR="$APP_HOME/lib"
CONF_DIR="$APP_HOME/conf"
LOG_DIR="$APP_HOME/logs"
PID_DIR="$APP_HOME/pids"

mkdir -p "$LOG_DIR" "$PID_DIR"

# JVM 参数（按服务器内存酌情调整）
JVM_OPTS="-server -Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxMetaspaceSize=128m"
JVM_OPTS="$JVM_OPTS -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"

# ---- 服务定义：名称|端口|jar文件名|conf文件名 ----
declare -a SERVICES=(
  "gateway|8001|gateway.jar|gateway-application.yml"
  "user-service|8002|user-service.jar|user-service-application.yml"
  "admin-service|8003|admin-service.jar|admin-service-application.yml"
  "api|8004|api.jar|api-application.yml"
)

# ---- 工具函数 ----
log_info()  { echo -e "\033[32m[INFO]  $*\033[0m"; }
log_warn()  { echo -e "\033[33m[WARN]  $*\033[0m"; }
log_error() { echo -e "\033[31m[ERROR] $*\033[0m"; }

get_pid_file() { echo "$PID_DIR/$1.pid"; }

is_running() {
  local pid_file
  pid_file="$(get_pid_file "$1")"
  [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null
}

wait_for_port() {
  local port=$1 timeout=$2 elapsed=0
  while ! ss -tln 2>/dev/null | grep -q ":${port} "; do
    sleep 1
    (( elapsed++ ))
    if [ "$elapsed" -ge "$timeout" ]; then
      return 1
    fi
  done
  return 0
}

# ---- 核心操作 ----
do_start() {
  local name="$1" port="$2" jar="$3" conf="$4"
  local jar_path="$LIB_DIR/$jar"
  local conf_path="$CONF_DIR/$conf"
  local log_file="$LOG_DIR/${name}.log"
  local pid_file
  pid_file="$(get_pid_file "$name")"

  if [ ! -f "$jar_path" ]; then
    log_error "Jar not found: $jar_path"
    return 1
  fi

  if is_running "$name"; then
    log_warn "$name is already running (PID: $(cat "$pid_file"))"
    return 0
  fi

  log_info "Starting $name (port $port)..."

  # 如果 conf 目录有覆盖配置，通过 --spring.config.location 注入
  local config_arg=""
  if [ -f "$conf_path" ]; then
    config_arg="--spring.config.location=file:$conf_path"
  fi

  nohup java $JVM_OPTS \
    -jar "$jar_path" \
    $config_arg \
    >> "$log_file" 2>&1 &

  local pid=$!
  echo "$pid" > "$pid_file"

  # 等待端口就绪，最多 60 秒
  if wait_for_port "$port" 60; then
    log_info "$name started successfully (PID: $pid, port: $port)"
  else
    log_warn "$name process started (PID: $pid) but port $port not ready within 60s, check log: $log_file"
  fi
}

do_stop() {
  local name="$1" port="$2"
  local pid_file
  pid_file="$(get_pid_file "$name")"

  if ! is_running "$name"; then
    log_warn "$name is not running."
    [ -f "$pid_file" ] && rm -f "$pid_file"
    return 0
  fi

  local pid
  pid="$(cat "$pid_file")"
  log_info "Stopping $name (PID: $pid)..."

  # 先 SIGTERM，等 15 秒，再 SIGKILL
  kill "$pid" 2>/dev/null
  local waited=0
  while kill -0 "$pid" 2>/dev/null; do
    sleep 1
    (( waited++ ))
    if [ "$waited" -ge 15 ]; then
      log_warn "Force killing $name (PID: $pid)..."
      kill -9 "$pid" 2>/dev/null
      sleep 1
      break
    fi
  done

  rm -f "$pid_file"
  log_info "$name stopped."
}

do_status() {
  printf "\n%-20s %-8s %-10s %s\n" "SERVICE" "PORT" "STATUS" "PID"
  printf "%-20s %-8s %-10s %s\n" "-------" "----" "------" "---"
  for entry in "${SERVICES[@]}"; do
    IFS='|' read -r name port jar conf <<< "$entry"
    local pid_file
    pid_file="$(get_pid_file "$name")"
    if is_running "$name"; then
      printf "%-20s %-8s \033[32m%-10s\033[0m %s\n" "$name" "$port" "RUNNING" "$(cat "$pid_file")"
    else
      printf "%-20s %-8s \033[31m%-10s\033[0m %s\n" "$name" "$port" "STOPPED" "-"
    fi
  done
  echo ""
}

do_log() {
  local name="$1"
  local log_file="$LOG_DIR/${name}.log"
  if [ ! -f "$log_file" ]; then
    log_error "Log file not found: $log_file"
    return 1
  fi
  tail -f "$log_file"
}

# ---- 入口 ----
ACTION="$1"
TARGET="$2"   # 可选：指定服务名

if [ -z "$ACTION" ]; then
  sed -n '2,12p' "$0" | sed 's/^# \?//'
  exit 1
fi

run_for_services() {
  local action_fn="$1" target="$2"
  local found=0
  for entry in "${SERVICES[@]}"; do
    IFS='|' read -r name port jar conf <<< "$entry"
    if [ -z "$target" ] || [ "$target" = "$name" ]; then
      $action_fn "$name" "$port" "$jar" "$conf"
      found=1
    fi
  done
  if [ "$found" -eq 0 ] && [ -n "$target" ]; then
    log_error "Unknown service: $target"
    log_info "Available: gateway | user-service | admin-service | api"
    exit 1
  fi
}

case "$ACTION" in
  start)
    run_for_services do_start "$TARGET"
    echo ""
    do_status
    ;;
  stop)
    run_for_services do_stop "$TARGET"
    ;;
  restart)
    run_for_services do_stop   "$TARGET"
    sleep 2
    run_for_services do_start  "$TARGET"
    echo ""
    do_status
    ;;
  status)
    do_status
    ;;
  log)
    if [ -z "$TARGET" ]; then
      log_error "Usage: $0 log <service>"
      log_info "Available: gateway | user-service | admin-service | api"
      exit 1
    fi
    do_log "$TARGET"
    ;;
  *)
    log_error "Unknown action: $ACTION"
    log_info "Actions: start | stop | restart | status | log"
    exit 1
    ;;
esac
