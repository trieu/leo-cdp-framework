# arangodb doc

https://www.arangodb.com/docs/stable/programs-arangodump-examples.html

# arangodump

arangodump \
  --server.endpoint tcp://192.168.173.13:8531 \
  --server.username backup \
  --server.database mydb \
  --output-directory "dump"

arangodump --server.endpoint tcp://cdpdatabase:8601 --server.username root --server.database [database] --overwrite true --output-directory backup
  zip -r database.zip backup/
  scp -P 234  -i .ssh/trieu_leocdp  leocdp@leocdp-database:/home/leocdp/database.zip  ./0-uspa/data-server-backup/
  
# arangorestore
  
arangorestore \
  --server.endpoint tcp://localhost:8529/ \
  --server.username root \
  --server.database [database] \
  --input-directory "./0-uspa/data-server-backup/database/backup/" \