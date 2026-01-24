---
description: How to maintain and review the .agent directory
---

# Agent Documentation Review Workflow

This workflow is for iteratively reviewing and updating the `.agent` directory (Rules and Workflows) to ensure it stays synchronized with the codebase.

## 1. Inventory Check
List all current rules and workflows.

```bash
ls -R .agent/
```

## 2. Validity Check
For each rule/workflow, ask:
- **Does this still match the code?** (e.g., directory paths, library versions, coding patterns).
- **Is it too generic?** Can we make it more specific/actionable?
- **Is it redundant?** Can we merge it?

## 3. Gap Analysis
Identify missing documentation:
- Are there new modules without architectural rules?
- Are there recurring manual tasks that need a workflow?
- Are there new technologies (e.g., a new library) that need a rule?

## 4. Update Execution
1.  **Modify**: Update existing `.md` files in `.agent/rules` or `.agent/workflows`.
2.  **Create**: Add new files for gaps.
3.  **Delete**: Remove obsolete files.

## 5. Verification
Read the modified rules to ensure they are consistent with each other (e.g., `workspace-structure.md` matches `create-bundle.md`).
