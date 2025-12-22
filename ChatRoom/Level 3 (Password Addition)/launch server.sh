java \
  -Djavax.net.ssl.keyStore="$PWD/serverkeystore.p12" \
  -Djavax.net.ssl.keyStoreType=PKCS12 \
  -Djavax.net.ssl.keyStorePassword=changeit \
  ChatServer 6000