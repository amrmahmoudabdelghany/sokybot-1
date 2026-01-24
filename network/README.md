# Sokybot Network

This directory handles all network communication, including proxying and security.

## Modules
- **`sokybot-proxy`**: The Netty-based proxy that sits between the client and serve.
- **`sokybot-security`**: Implementation of Silkroad Online's security (Blowfish, Checksum).
- **`sokybot-packet-sniffer`**: Tools for capturing and analyzing network packets.
