# Contributing to Sokybot

Thank you for your interest in contributing to Sokybot! This document provides guidelines and information about contributing to this project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Code Style](#code-style)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Issue Guidelines](#issue-guidelines)

## Code of Conduct

Please be respectful and constructive in all interactions. We welcome contributors of all experience levels.

## Getting Started

### Prerequisites

- **Java 11** or higher
- **Maven 3.6+** (or use the included Maven Wrapper)
- **Git**
- IDE of your choice (IntelliJ IDEA, Eclipse, VS Code)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/sokybot.git
   cd sokybot
   ```
3. Add the upstream remote:
   ```bash
   git remote add upstream https://github.com/sokybot/sokybot.git
   ```

## Development Setup

### Building the Project

```bash
# Build with development profile (fast, skips tests)
./mvnw clean install -P dev

# Build with all tests
./mvnw clean install -P ci
```

### Running Locally

```bash
# Start the Karaf distribution
cd sokybot-dist/target/assembly
./bin/karaf
```

### IDE Setup

#### IntelliJ IDEA
1. File → Open → Select the `sokybot` directory
2. Import as Maven project
3. Enable annotation processing for Lombok

#### Eclipse
1. File → Import → Maven → Existing Maven Projects
2. Install Lombok plugin

## Making Changes

### Branching Strategy

1. Create a feature branch from `main` or `develop`:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```

2. Keep your branch updated:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

### Commit Guidelines

Follow conventional commit messages:

```
type(scope): description

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style (formatting, missing semicolons)
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `chore`: Maintenance tasks
- `deps`: Dependency updates

**Examples:**
```
feat(engine): add new packet handler for inventory updates
fix(proxy): resolve connection timeout issue (#123)
docs(readme): update installation instructions
refactor(commons): simplify byte conversion utilities
```

## Code Style

### Java Code Style

We use Checkstyle to enforce code style. Configuration is in `checkstyle.xml`.

```bash
# Check code style
./mvnw checkstyle:check

# Run all quality checks
./mvnw verify -P ci
```

### Key Guidelines

1. **Naming Conventions**
   - Classes: `PascalCase`
   - Methods/Variables: `camelCase`
   - Constants: `UPPER_SNAKE_CASE`
   - Packages: `lowercase`

2. **Documentation**
   - Add Javadoc to public APIs
   - Include `@param`, `@return`, `@throws` tags

3. **Formatting**
   - 4 spaces for indentation (no tabs)
   - 120 character line limit
   - Braces on same line

4. **OSGi**
   - Follow OSGi bundle conventions
   - Use Declarative Services annotations
   - Export only public API packages

### Static Analysis

```bash
# SpotBugs (bug detection)
./mvnw spotbugs:check

# PMD (code analysis)
./mvnw pmd:check

# All quality checks
./mvnw verify -P ci
```

## Testing

### Running Tests

```bash
# Run all tests
./mvnw test -P ci

# Run specific module tests
./mvnw test -pl sokybot-engine

# Run integration tests
./mvnw verify -P integration-test
```

### Writing Tests

1. **Unit Tests**
   - Name: `*Test.java`
   - Location: `src/test/java`
   - Use JUnit 5 and Mockito

2. **Integration Tests**
   - Name: `*IT.java` or `*IntegrationTest.java`
   - Test OSGi integration

### Test Coverage

```bash
# Generate coverage report
./mvnw verify -P ci
# Report at: target/site/jacoco/index.html
```

## Submitting Changes

### Pull Request Process

1. **Before Submitting**
   ```bash
   # Ensure code compiles
   ./mvnw clean compile
   
   # Run all checks
   ./mvnw verify -P ci
   
   # Update your branch
   git fetch upstream
   git rebase upstream/main
   ```

2. **Create Pull Request**
   - Use the PR template
   - Link related issues
   - Provide clear description
   - Add screenshots if UI changes

3. **Review Process**
   - Address reviewer feedback
   - Keep PRs focused and small
   - Squash commits if requested

### PR Checklist

- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] Code style checks pass
- [ ] Documentation updated (if needed)
- [ ] Commit messages follow guidelines
- [ ] PR description is complete

## Issue Guidelines

### Bug Reports

Use the bug report template and include:
- Clear description
- Steps to reproduce
- Expected vs actual behavior
- System information
- Relevant logs

### Feature Requests

Use the feature request template and include:
- Problem statement
- Proposed solution
- Alternatives considered

### Before Creating an Issue

1. Search existing issues
2. Check closed issues
3. Review documentation/wiki

## Project Structure

```
sokybot/
├── sokybot-commons/       # Common utilities
├── sokybot-engine/        # Core bot engine
├── sokybot-proxy/         # Network proxy
├── sokybot-http-server/   # HTTP/WebSocket server
├── sokybot-webview/       # Web UI
├── sokybot-dist/          # Distribution assembly
└── sokybot-features/      # Karaf features
```

## Getting Help

- **Documentation**: Check `DEVELOPER_GUIDE.md`
- **Discussions**: Use GitHub Discussions for questions
- **Issues**: For bugs and feature requests

## Recognition

Contributors are recognized in:
- GitHub contributors page
- Release notes (for significant contributions)

Thank you for contributing to Sokybot!
