#!/usr/bin/env bash
# ============================================================================
# run_tests.sh — Ejecuta todos los requests de ms-usuarios.http
# ============================================================================
# Uso:  ./http/run_tests.sh              # ejecuta todo
#       ./http/run_tests.sh --verbose     # muestra cuerpo de cada respuesta
# ============================================================================
set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
HTTP_FILE="$SCRIPT_DIR/ms-usuarios.http"
BASE_URL="http://localhost:8081"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

PASS=0; FAIL=0; TOTAL=0
VERBOSE=false
[[ "${1:-}" == "--verbose" ]] && VERBOSE=true
TIMESTAMP=$(date +%s)  # único por corrida, compartido entre todos los requests

declare -A NAMED   # nombre -> response body completo

# ─── Variables de entorno para el .http ───────────────────────────────────────

# La variable {{base_url}} se reemplaza directamente
# Las variables {{nombre.response.body.campo}} se buscan en NAMED[nombre]

# ─── Sustitución de variables tipo {{...}} ────────────────────────────────────

subst() {
    local text="$1"
    local result=""
    local rest="$text"

    while [[ "$rest" =~ \{\{([^}]+)\}\} ]]; do
        local before="${rest%%\{\{*\}\}*}"  # todo antes del primer {{
        # Encontrar el match completo
        local full_match="{{${BASH_REMATCH[1]}}}"
        local var_expr="${BASH_REMATCH[1]}"

        # Separar antes del match
        local prefix="${rest%%"$full_match"*}"
        result+="$prefix"
        rest="${rest#"$prefix$full_match"}"

        # Resolver variable
        local val=""
        if [[ "$var_expr" == "base_url" ]]; then
            val="$BASE_URL"
        elif [[ "$var_expr" == "\$timestamp" ]]; then
            val="$TIMESTAMP"
        elif [[ "$var_expr" =~ ^([a-zA-Z_]+)\.response\.body\.([a-zA-Z_]+)$ ]]; then
            local rname="${BASH_REMATCH[1]}"
            local field="${BASH_REMATCH[2]}"
            local stored="${NAMED[$rname]:-}"
            if [[ -n "$stored" ]]; then
                val=$(echo "$stored" | jq -r ".$field" 2>/dev/null || echo "ERR:JSON_PATH")
            else
                val="ERR:UNDEF:$rname"
            fi
        elif [[ "$var_expr" =~ ^([a-zA-Z_]+)\.response\.body$ ]]; then
            local rname="${BASH_REMATCH[1]}"
            local stored="${NAMED[$rname]:-}"
            if [[ -n "$stored" ]]; then
                val="$stored"
            else
                val="ERR:UNDEF:$rname"
            fi
        else
            val="ERR:UNKNOWN:$var_expr"
        fi
        result+="$val"
    done
    result+="$rest"
    echo "$result"
}

# ─── Ejecutar un request ─────────────────────────────────────────────────────

run_request() {
    local method="$1" url="$2" headers="$3" body="$4"
    local expected="$5" name="$6" desc="$7"

    TOTAL=$((TOTAL + 1))

    # Sustituir variables
    url=$(subst "$url")
    local clean_url="${url//\{\{base_url\}\}/$BASE_URL}"
    clean_url=$(subst "$clean_url")  # doble pasada por si acaso
    headers=$(subst "$headers")
    body=$(subst "$body")

    # Construir curl
    local curl_args=(-s -w "%{http_code}" -o /tmp/ms_usuarios_resp.txt -X "$method")
    curl_args+=("$clean_url")

    # Headers
    while IFS= read -r h; do
        h="${h## }"; h="${h%% }"  # trim
        [[ -z "$h" ]] && continue
        local key="${h%%:*}"
        local val="${h#*:}"
        val="${val# }"
        curl_args+=(-H "$key: $val")
    done <<< "$headers"

    # Body
    if [[ -n "${body## }" ]]; then
        curl_args+=(-d "$body")
    fi

    # Ejecutar
    local http_code
    http_code=$(curl "${curl_args[@]}" 2>/dev/null) || http_code="000"
    local response_body
    response_body=$(cat /tmp/ms_usuarios_resp.txt 2>/dev/null || echo "")

    # Guardar respuesta nombrada
    if [[ -n "$name" ]]; then
        NAMED["$name"]="$response_body"
    fi

    # Mostrar resultado
    local preview="${response_body:0:150}"
    [[ ${#response_body} -gt 150 ]] && preview+="..."

    local ok=true
    if [[ -n "$expected" ]]; then
        if [[ "$http_code" == "$expected" ]]; then
            echo -e "${GREEN}✓${NC} [$TOTAL] $desc → ${GREEN}HTTP $http_code${NC}"
        else
            echo -e "${RED}✗${NC} [$TOTAL] $desc → ${RED}HTTP $http_code${NC} (esperado ${YELLOW}$expected${NC})"
            echo "       $preview"
            ok=false
        fi
    else
        if [[ "$http_code" =~ ^5 ]]; then
            echo -e "${RED}✗${NC} [$TOTAL] $desc → ${RED}HTTP $http_code (5xx)${NC}"
            echo "       $preview"
            ok=false
        elif [[ "$http_code" =~ ^[4] ]]; then
            echo -e "${YELLOW}⚠${NC}  [$TOTAL] $desc → HTTP $http_code (sin esperado definido)"
        else
            echo -e "${GREEN}✓${NC} [$TOTAL] $desc → HTTP $http_code"
        fi
    fi

    if $ok; then PASS=$((PASS + 1)); else FAIL=$((FAIL + 1)); fi
    if $VERBOSE; then
        echo -e "       ${CYAN}Resp completa:${NC} $response_body"
    fi
}

# ─── Parsear y ejecutar ──────────────────────────────────────────────────────

parse_and_run() {
    local method="" url="" headers="" body=""
    local expected="" name="" desc=""
    local state="idle"     # idle | headers | body
    local line

    while IFS= read -r line || [[ -n "$line" ]]; do

        # --- Separador ### (nuevo request) ---
        if [[ "$line" =~ ^### ]]; then
            if [[ "$state" != "idle" ]]; then
                run_request "$method" "$url" "$headers" "$body" "$expected" "$name" "$desc"
            fi
            state="idle"
            method=""; url=""; headers=""; body=""
            expected=""; name=""; desc=""
            continue
        fi

        # --- Línea de sección (####... o # ===...) — ignorar ---
        if [[ "$line" =~ ^#{4,} ]] || [[ "$line" =~ ^#[[:space:]]*=== ]]; then
            continue
        fi

        # --- Comentarios ---
        if [[ "$line" =~ ^#[[:space:]]*@name[[:space:]]+(.+)$ ]]; then
            name="${BASH_REMATCH[1]}"
            name="${name%% *}"  # quitar trailing spaces/comments
            continue
        fi

        if [[ "$line" =~ ^#[[:space:]]*Esperado:[[:space:]]*([0-9]{3}) ]]; then
            expected="${BASH_REMATCH[1]}"
            continue
        fi

        # Líneas de comentario normales: buscar nombre HU en el texto
        if [[ "$line" =~ ^#[[:space:]]*HU-[0-9] ]]; then
            if [[ "$line" =~ HU-([0-9]+[a-z]*)[[:space:]]*[-–|][[:space:]]*(.*) ]]; then
                desc="HU-${BASH_REMATCH[1]} | ${BASH_REMATCH[2]}"
            fi
            continue
        fi

        # Ignorar otros comentarios y líneas decorativas
        if [[ "$line" =~ ^#[[:space:]] ]]; then
            continue
        fi

        # --- Línea vacía: headers → body ---
        if [[ "$state" == "headers" && -z "$line" ]]; then
            state="body"
            continue
        fi

        # --- Método URL (GET/POST/PUT/DELETE ...) ---
        if [[ "$line" =~ ^(GET|POST|PUT|DELETE|PATCH)[[:space:]]+(.+)$ ]]; then
            method="${BASH_REMATCH[1]}"
            url="${BASH_REMATCH[2]}"
            [[ -z "$desc" ]] && desc="${method} ${url//\{\{base_url\}\}\//}"
            state="headers"
            continue
        fi

        # --- Headers ---
        if [[ "$state" == "headers" && "$line" =~ ^[A-Za-z][-A-Za-z]*:[[:space:]] ]]; then
            headers+="$line"$'\n'
            continue
        fi

        # --- Body ---
        if [[ "$state" == "body" ]]; then
            body+="$line"$'\n'
            continue
        fi

    done < "$HTTP_FILE"

    # Último request pendiente
    if [[ "$state" != "idle" ]]; then
        run_request "$method" "$url" "$headers" "$body" "$expected" "$name" "$desc"
    fi
}

# ─── Main ─────────────────────────────────────────────────────────────────────

echo -e "${BOLD}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║  Perfulandia SPA — Test ms-usuarios (HTTP runner)     ║${NC}"
echo -e "${BOLD}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""

# Health check
if ! curl -s --connect-timeout 3 -o /dev/null "${BASE_URL}/actuator/health"; then
    echo -e "${RED}ERROR:${NC} ${BASE_URL} no responde. Iniciá ms-usuarios."
    exit 1
fi
echo -e "${CYAN}→${NC} Servidor OK en ${BASE_URL}"
echo ""

parse_and_run

echo ""
echo -e "${BOLD}────────────────────────────────────────────────────────${NC}"
if [[ $FAIL -eq 0 ]]; then
    echo -e "  ${GREEN}${BOLD}TODOS OK${NC} — ${PASS}/${TOTAL} requests pasaron"
else
    echo -e "  ${RED}${BOLD}${FAIL} FALLARON${NC} — ${GREEN}${PASS} ok${NC}, ${RED}${FAIL} fail${NC} de ${TOTAL}"
fi
echo -e "${BOLD}────────────────────────────────────────────────────────${NC}"

# Resumen de variables capturadas
if $VERBOSE; then
    echo ""
    echo -e "${BOLD}Variables capturadas:${NC}"
    for k in "${!NAMED[@]}"; do
        local preview="${NAMED[$k]:0:80}"
        echo "  $k = $preview"
    done
fi

[[ $FAIL -gt 0 ]] && exit 1 || exit 0
