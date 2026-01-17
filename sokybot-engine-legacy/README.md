# sokybot-engine-legacy

**Status:** Legacy code - Do not use for new development

This module contains the legacy `org.sokybot.machine` package which uses Spring State Machine for workflow management.

## Contents

This module contains:
- **Controllers**: Spring State Machine controllers using `@WithStateMachine`
- **Services**: Legacy services tied to Spring State Machine
- **Workflow**: Legacy workflow actions and guards for Spring State Machine
- **Config**: Spring Boot configuration (`MachineConfig`, `EngineServiceConfig`, etc.)

## Migration Path

**For new development, use:**
- `org.sokybot.engine.core` - Framework-agnostic engine architecture
- `org.sokybot.engine.api` - Engine APIs
- Actuator system - OSGi-based actuator bundles

## Dependencies

This module requires:
- Spring Boot 2.7.13
- Spring State Machine 2.5.1
- Spring Data JPA

## Notes

- This module is kept for backward compatibility
- New features should use the new engine architecture
- This module may be removed in a future version after full migration
