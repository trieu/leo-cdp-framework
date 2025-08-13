
# Create Locally Trusted SSL Certificates with mkcert on Ubuntu

## Steps

sudo apt install libnss3-tools -y

wget https://github.com/FiloSottile/mkcert/releases/download/v1.4.4/mkcert-v1.4.4-linux-amd64

sudo cp mkcert-v1.4.4-linux-amd64 /usr/local/bin/mkcert

sudo chmod +x /usr/local/bin/mkcert

mkcert -install

mkcert -CAROOT

mkcert example.com "*.example.com" example.test localhost 127.0.0.1 ::1

sudo cp example.com+5.pem /usr/local/share/ca-certificates/example.com+5.crt

sudo update-ca-certificates

UPDATE NGINX ssl_certificate
	ssl_certificate /home/thomas/0-uspa/localhost-ssl/example.com+5.pem;
	ssl_certificate_key /home/thomas/0-uspa/localhost-ssl/example.com+5-key.pem;

RESTART CHROME

## Ref

* https://kifarunix.com/create-locally-trusted-ssl-certificates-with-mkcert-on-ubuntu-20-04/

* https://github.com/FiloSottile/mkcert

* https://stackoverflow.com/questions/7580508/getting-chrome-to-accept-self-signed-localhost-certificate