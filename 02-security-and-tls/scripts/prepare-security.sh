#!/usr/bin/env bash
# สร้างเฉพาะ local learning material ไม่แก้ existing output หรือ cluster
set -euo pipefail
umask 077
chapter_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)"
output="${1:-$chapter_dir/certs/generated}"
[[ $# -le 1 ]] || { echo 'Usage: prepare-security.sh [output-directory]' >&2; exit 2; }
[[ ! -e "$output" ]] || { echo 'Output exists; preserve it or choose a new directory. Never regenerate keys for an existing data volume.' >&2; exit 2; }
for tool in openssl docker; do command -v "$tool" > /dev/null; done
mkdir -p "$output"/{public,ca-private,node,http,admin,secrets,security}
output="$(cd -- "$output" && pwd -P)"
chmod 700 "$output" "$output/ca-private" "$output/secrets"
# Public/mounted subdirectories อ่านได้ใน container แต่ parent บน host จำกัด owner
chmod 755 "$output/public" "$output/node" "$output/http" "$output/admin" "$output/security"
cat > "$output/ca-private/ca.cnf" <<'CONF'
[req]
distinguished_name=dn
x509_extensions=ca
prompt=no
[dn]
CN=Chapter02 Local CA
O=Learning
[ca]
basicConstraints=critical,CA:TRUE
keyUsage=critical,keyCertSign,cRLSign
subjectKeyIdentifier=hash
CONF
openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days 30 \
    -config "$output/ca-private/ca.cnf" -keyout "$output/ca-private/ca.key" \
    -out "$output/public/ca.pem" > /dev/null 2>&1
issue_certificate() {
    local name="$1" subject="$2" usage="$3" san="$4"
    local folder="$output/$name" stem="$name"
    [[ "$name" != admin ]] || stem=admin
    cat > "$output/ca-private/$name.ext" <<EXT
basicConstraints=critical,CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=$usage
subjectAltName=$san
EXT
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$folder/$stem.key" > /dev/null 2>&1
    openssl req -new -key "$folder/$stem.key" -subj "$subject" -out "$output/ca-private/$name.csr"
    openssl x509 -req -in "$output/ca-private/$name.csr" -CA "$output/public/ca.pem" \
        -CAkey "$output/ca-private/ca.key" -CAcreateserial -days 7 -sha256 \
        -extfile "$output/ca-private/$name.ext" -out "$folder/$stem.pem" > /dev/null 2>&1
    chmod 644 "$folder/$stem.key" "$folder/$stem.pem"
}
issue_certificate node '/O=Learning/OU=Chapter02/CN=opensearch' 'serverAuth,clientAuth' 'DNS:opensearch,DNS:localhost,IP:127.0.0.1'
issue_certificate http '/O=Learning/OU=Chapter02/CN=opensearch-http' serverAuth 'DNS:opensearch,DNS:localhost,IP:127.0.0.1'
issue_certificate admin '/O=Learning/OU=Chapter02/CN=security-admin' clientAuth 'DNS:security-admin'
# Wrong CA fixture สร้างจริง ไม่มี private material ที่ commit
openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days 7 -subj '/CN=Unrelated CA' \
    -keyout "$output/ca-private/unrelated.key" -out "$output/public/unrelated-ca.pem" > /dev/null 2>&1
# SAN mismatch ใช้ CA เดียวกัน เพื่อแยก hostname failure ออกจาก trust failure
openssl req -new -key "$output/http/http.key" -subj '/CN=wrong.invalid' -out "$output/ca-private/mismatch.csr"
cat > "$output/ca-private/mismatch.ext" <<'EXT'
basicConstraints=critical,CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:wrong.invalid
EXT
openssl x509 -req -in "$output/ca-private/mismatch.csr" -CA "$output/public/ca.pem" \
    -CAkey "$output/ca-private/ca.key" -CAcreateserial -days 7 -sha256 \
    -extfile "$output/ca-private/mismatch.ext" -out "$output/http/mismatch.pem" > /dev/null 2>&1
chmod 644 "$output/public/"*.pem "$output/http/mismatch.pem"
for identity in monitor info; do
    openssl rand -hex 24 > "$output/secrets/$identity.password"
done
openssl rand -hex 24 > "$output/secrets/invalid.password"
# อ่าน secret จาก env ใน child process ไม่ใส่ค่า password ใน process arguments
export CH02_PASSWORD
CH02_PASSWORD="$(cat "$output/secrets/monitor.password")"
monitor_hash="$(docker run --rm -e CH02_PASSWORD --entrypoint bash opensearchproject/opensearch:3.9.0 \
    -c 'plugins/opensearch-security/tools/hash.sh -env CH02_PASSWORD' | tail -n 1)"
CH02_PASSWORD="$(cat "$output/secrets/info.password")"
info_hash="$(docker run --rm -e CH02_PASSWORD --entrypoint bash opensearchproject/opensearch:3.9.0 \
    -c 'plugins/opensearch-security/tools/hash.sh -env CH02_PASSWORD' | tail -n 1)"
unset CH02_PASSWORD
[[ "$monitor_hash" == '$2'* && "$info_hash" == '$2'* ]] || { echo 'Password hashing failed' >&2; exit 1; }
for file in "$chapter_dir/config/security/"*.yml; do cp "$file" "$output/security/"; done
sed -e "s|@MONITOR_HASH@|$monitor_hash|" -e "s|@INFO_HASH@|$info_hash|" \
    "$chapter_dir/config/security/internal_users.yml.template" > "$output/security/internal_users.yml"
for identity in monitor info; do
    username=ch02_monitor
    [[ "$identity" != info ]] || username=ch02_info_only
    printf 'user = "%s:%s"\n' "$username" "$(cat "$output/secrets/$identity.password")" > "$output/secrets/$identity.curl"
done
printf 'user = "ch02_monitor:%s"\n' "$(cat "$output/secrets/invalid.password")" > "$output/secrets/invalid.curl"
# Healthcheck user ใน Docker อ่านเฉพาะ monitor.curl ไม่ mount secrets directory ทั้งชุด
chmod 644 "$output/secrets/monitor.curl" "$output/security/"*.yml
printf 'Prepared local security material at %s (no existing files overwritten).\n' "$output"
