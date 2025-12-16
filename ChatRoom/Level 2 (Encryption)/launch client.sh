PWD=$(pwd)

# opens client with the key store and trust store configured
java \
  -Djavax.net.ssl.trustStore="$PWD/clienttruststore.p12" \
  -Djavax.net.ssl.trustStoreType=PKCS12 \
  -Djavax.net.ssl.trustStorePassword=changeit \
  ChatClient localhost 6000