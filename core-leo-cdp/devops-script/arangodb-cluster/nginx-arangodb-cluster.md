# ArangoDB Request Load balancing

	

	upstream arangodb {
	  server coord-1.my.domain:8529;
	  server coord-2.my.domain:8529;
	  server coord-3.my.domain:8529;
	}
	
	server {
	  listen                *:80 default_server;
	  server_name           _; # Listens for ALL hostnames
	  proxy_next_upstream   error timeout invalid_header;
	  
	  location / {
	    proxy_pass          http://arangodb;
	  }
	}
