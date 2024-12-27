HEADER=$(echo -n '{"alg": "HS256", "typ": "JWT"}' | openssl base64 -e -A | tr '+/' '-_' | tr -d '=');
PAYLOAD=$(echo -n '{"server": true}' | openssl base64 -e -A | tr '+/' '-_' | tr -d '=');
SECRET='tm4ra4qcn56w3c8jzqavm355k9e4apeavuj9cat5f563uzb3ww33kbm93hyk6yrd';
SIGNATURE=$(echo -n ${HEADER}.${PAYLOAD} | openssl dgst -sha256 -hmac ${SECRET} -binary | openssl base64 -e -A | tr '+/' '-_' | tr -d '=');

TOKEN="${HEADER}.${PAYLOAD}.${SIGNATURE}"

echo $TOKEN