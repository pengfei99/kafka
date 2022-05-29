# kafka data encryption

To encrypt the data transmission between clients(i.e. producer and consumer) and brokers, we need to use protocol that 
can encrypt data.

## Generating certificate and key store

keytool -keystore server.keystore.jks -alias <alias> -validity 365 -genkey -keyalg RSA -ext SAN=DNS:<hostname>,DNS:<fqdn>,DNS:localhost,IP:<IP-ADDRESS>,IP:127.0.0.1

openssl req -new -x509 -keyout ca-key -out ca-cert -days 365 -subj '/CN=<fqdn>'   -extensions san   -config <(echo '[req]'; echo 'distinguished_name=req'; echo '[san]'; echo 'subjectAltName = DNS:localhost, IP:127.0.0.1, DNS:<hostname>, IP:<ip-address>')

keytool -keystore server.truststore.jks -alias CARoot -import -file ca-cert

keytool -keystore client.truststore.jks -alias CARoot -import -file ca-cert

keytool -keystore server.keystore.jks -alias <fqdn> -certreq -file cert-file -ext SAN=DNS:<hostname>,DNS:localhost,IP:<ip-address >,IP:127.0.0.1

openssl x509 -req  -extfile <(printf "subjectAltName = DNS:localhost, IP:127.0.0.1, DNS:<fqdn>, IP:<ip-address>") -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 365 -CAcreateserial -passin pass:<password>

keytool -keystore server.keystore.jks -alias CARoot -import -file ca-cert

keytool -keystore server.keystore.jks -alias <alias> -import -file cert-signed.