# KaPA Proxy backend service
**KaPA Proxy server**

This component is the main backend component for handling the identity management requests and identity data enrichment.
This Proxy server offers REST APIs for both Shibboleth IdP and SP front ends that are used in the identification process.
This component also requires the necessary meta-information about the metadata which is fetched from Metadata server.

**Compiling the component**

This component can be compiled and packaged with Maven tool with the following commands:
```
mvn clean install
mvn assembly:single
```
This produces the required tar ball that contains the necessary binaries and configuration templates.

For local testing with Vagrant environment, there exists a helper script in /script directory which does the necessary work and unpacks the built package ready for Ansible provisioning.
This assumes that there exists environment specific directory structure for Ansible. Here's the required directory structure as an example for local Vagrant environment:
```
/data00/deploy/proxy-server/vagrant
```
**Note!** Copy Ansible user's public key file into this repository's directory /ssl before running vagrant up.

**Works as follows on Ubuntu:**

Edit the hosts file
```
sudo vim /etc/hosts
```
Add the line
```
192.168.10.12	auth-proxy.vagrant.dev
```
Start Vagrant virtual box (could be a while)
```
vagrant up
```
Complete by pushing configurations from your Ansible repository. Example fileset can be found from 'kapa-config' repository.

**Errors**

In case of errors, bugs or security issues please contact kapa@vrk.fi.

