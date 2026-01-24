# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x     | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take the security of Sokybot seriously. If you discover a security vulnerability, please follow these steps:

### Do NOT

- **Do NOT** open a public GitHub issue for security vulnerabilities
- **Do NOT** disclose the vulnerability publicly before it's been addressed

### Do

1. **Email us directly** at: security@sokybot.io (or create a private security advisory on GitHub)

2. **Include the following information:**
   - Type of vulnerability (e.g., XSS, SQL injection, RCE)
   - Affected component/module
   - Steps to reproduce
   - Potential impact
   - Any suggested fixes (optional)

3. **Give us time to respond**
   - We aim to acknowledge reports within 48 hours
   - We'll provide an estimated timeline for a fix
   - We'll keep you updated on progress

### What to Expect

1. **Acknowledgment**: We'll confirm receipt of your report within 48 hours
2. **Assessment**: We'll assess the severity and impact
3. **Fix Development**: We'll develop and test a fix
4. **Disclosure**: We'll coordinate with you on public disclosure timing
5. **Credit**: We'll credit you in the release notes (unless you prefer anonymity)

## Security Best Practices for Users

### Network Security

- Run Sokybot behind a firewall
- Don't expose the HTTP/WebSocket server to the public internet
- Use strong passwords for any authenticated endpoints

### System Security

- Keep Java and all dependencies updated
- Run with minimal required permissions
- Regularly update to the latest Sokybot version

### Data Security

- Don't store sensitive credentials in configuration files
- Use environment variables for secrets
- Regularly backup your data

## Security Updates

Security updates will be released as:
- Patch versions for minor vulnerabilities
- Minor versions for moderate vulnerabilities
- Emergency releases for critical vulnerabilities

We recommend:
- Subscribing to GitHub releases for notifications
- Enabling Dependabot alerts for your fork
- Regularly running `./mvnw verify -P security` to check for vulnerable dependencies

## Automated Security Scanning

This project uses:
- **OWASP Dependency Check**: Scans dependencies for known CVEs
- **SpotBugs**: Static analysis for security bugs
- **GitHub Dependabot**: Automated dependency updates

To run security checks locally:

```bash
# Run OWASP dependency check
./mvnw verify -P security

# Run all quality/security checks
./mvnw verify -P ci
```

## Scope

This security policy covers:
- The Sokybot core application
- Official plugins and extensions
- Build and deployment scripts

It does NOT cover:
- Third-party plugins
- User modifications
- Deployment infrastructure

## Contact

For security concerns: security@sokybot.io

For general questions: Use GitHub Discussions
