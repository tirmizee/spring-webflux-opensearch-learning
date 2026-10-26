#!/usr/bin/env bash
# สร้างเฉพาะ local learning material ไม่แก้ existing output หรือ cluster
set -euo pipefail
umask 077
chapter_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)"
output="${1:-$chapter_dir/certs/generated}"
[[ $# -le 1 ]] || { echo 'Usage: prepare-security.sh [output-directory]' >&2; exit 2; }
[[ ! -e "$output" ]] || { echo 'Output exists; preserve it or choose a new directory. Never regenerate keys for an existing data volume.' >&2; exit 2; }
for tool in openssl keytool; do command -v "$tool" > /dev/null; done
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
CN=Chapter03 Local CA
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
issue_certificate node '/O=Learning/OU=Chapter03/CN=opensearch' 'serverAuth,clientAuth' 'DNS:opensearch,DNS:localhost,IP:127.0.0.1'
issue_certificate http '/O=Learning/OU=Chapter03/CN=opensearch-http' serverAuth 'DNS:opensearch,DNS:localhost,IP:127.0.0.1'
issue_certificate admin '/O=Learning/OU=Chapter03/CN=security-admin' clientAuth 'DNS:security-admin'
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
# Local JVM truststores contain only public certificates. changeit is a demo integrity password, not an API credential.
keytool -importcert -noprompt -alias chapter03 -file "$output/public/ca.pem" \
    -keystore "$output/public/truststore.p12" -storetype PKCS12 -storepass changeit > /dev/null 2>&1
keytool -importcert -noprompt -alias unrelated -file "$output/public/unrelated-ca.pem" \
    -keystore "$output/public/unrelated-truststore.p12" -storetype PKCS12 -storepass changeit > /dev/null 2>&1
chmod 644 "$output/public/"*.pem "$output/public/"*.p12 "$output/http/mismatch.pem"
for file in "$chapter_dir/config/security/"*.yml; do cp "$file" "$output/security/"; done
mkdir -p "$output/runtime"
chmod 755 "$output/runtime"
: > "$output/runtime/monitor.curl"
chmod 644 "$output/runtime/monitor.curl" "$output/security/"*.yml
printf 'cacert = "%s/public/ca.pem"\ncert = "%s/admin/admin.pem"\nkey = "%s/admin/admin.key"\n' \
    "$output" "$output" "$output" > "$output/secrets/admin.curl"
printf 'Prepared Chapter 03 TLS material; no API keys issued yet.\n'
