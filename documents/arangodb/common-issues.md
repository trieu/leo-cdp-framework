
## arangodb 3.11 apt key expired

* sudo apt-key del EA93F5E56E751E9B
* wget -O- https://download.arangodb.com/arangodb311/DEBIAN/Release.key | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/arangodb-archive-keyring.gpg