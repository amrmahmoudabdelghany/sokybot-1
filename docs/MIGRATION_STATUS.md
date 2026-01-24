# Swing to Webview Migration Status

**Last Updated:** Based on comprehensive codebase scan  
**Goal:** Complete migration from Swing UI to React-based webview system

---

## Migration Overview

The application is transitioning from a Swing-based desktop UI to a modern React-based webview system. This document tracks the migration status of all bundles.

---

## ✅ Fully Migrated Bundles

### sokybot-packet-sniffer
**Status:** ✅ **FULLY MIGRATED**

- **Webview Integration:** Uses `IWebviewConfigurator` API
- **UI Definition:** Declarative JSON schemas in `/ui/` resources
- **Removed Swing Components:**
  - ❌ `PacketSniffer.java` (JTabbedPane) - Deleted
  - ❌ `TrafficMonitor.java` (JPanel) - Deleted
  - ❌ `PacketTracer.java` (JPanel) - Deleted
  - ❌ `PacketMonitorHandler.java` - Deleted
  - ❌ `IPacketMonitorHandler.java` - Deleted
  - ❌ `PacketTracerHandler.java` - Deleted
  - ❌ `IPacketTracerHandler.java` - Deleted
  - ❌ `TrafficMonitorCellRender.java` - Deleted
  - ❌ Test/Driver files using Swing - Deleted

- **Dependencies:** 
  - ✅ Excludes Swing in `Import-Package`: `!javax.swing.*`, `!java.awt.*`
  - ✅ No Swing dependencies in `<dependencies>`
  - ✅ Uses `sokybot-webview` for UI

- **Remaining Swing Code:**
  - ⚠️ `PacketAnalyzer.java` - Standalone JFrame tool (not used in production, kept for potential future use)
  - ⚠️ `PacketMonitorTableModel.java` - Extends AbstractTableModel (legacy, not used in new service)
  - ⚠️ `PacketTracerTableModel.java` - Extends AbstractTableModel (legacy, not used in new service)
  - ⚠️ Test files in `packetanalyzer/` package (TEST2.java, PopupListener.java, VarTable.java)

### sokybot-webview
**Status:** ✅ **NO SWING DEPENDENCIES**

- Pure React/TypeScript frontend
- RSocket-based communication
- No Swing imports or dependencies

---

## ❌ Still Using Swing

### sokybot-ui
**Status:** ❌ **FULLY SWING-BASED**

**Swing Components:**
- `SkillTab.java` - JPanel with JTable, JButton, JCheckBox
- `TrainingPage.java` - JPanel, JTabbedPane
- `ItemPage.java` - JPanel, JTabbedPane
- `EnvTab.java` - JPanel, JTabbedPane
- `MachineDashboard.java` - JPanel
- `MachineControll.java` - JPanel with JButtons
- `TrainerInfoPanel.java` - JPanel with custom painting
- Many more Swing components...

**Dependencies:**
- Uses `sokybot-ui-api` (defines `IMachinePageViewer` with Swing)
- Uses `sokybot-swing` bundle
- SwingX libraries
- MyDoggy docking framework

**Migration Priority:** 🔴 **HIGH** - This is the main UI bundle

### sokybot-ui-api
**Status:** ❌ **DEFINES SWING INTERFACES**

**Swing Dependencies:**
- `IMachinePageViewer.java` - Uses `JComponent`, `Icon` (Swing types)
- `IPageViewer.java` - Uses `JComponent`, `Icon`

**Migration Priority:** 🟡 **MEDIUM** - API needs to be updated or deprecated

### sokybot-runtime
**Status:** ⚠️ **REFERENCES SWING API**

**Swing References:**
- `IMachineContext.java` - Has `machinePageViewer()` method returning `IMachinePageViewer`
- `MachineContextImpl.java` - Implements `machinePageViewer()` using Swing API

**Migration Priority:** 🟡 **MEDIUM** - Needs to support both old and new APIs during transition

### sokybot-builders
**Status:** ❌ **SWING DIALOGS**

**Swing Components:**
- `MachineGroupBuilderDialog.java` - JDialog
- `MachineBuilderDialog.java` - JDialog
- `MachineDataPanel.java` - JPanel
- `GameDataPanel.java` - JPanel
- `AccountInfoPanel.java` - JPanel
- `SilkroadFileBrowser.java` - JFileChooser

**Migration Priority:** 🟢 **LOW** - Builder dialogs may be acceptable to keep as Swing (utility tools)

### sokybot-map-debug
**Status:** ❌ **SWING VIEWER**

**Swing Components:**
- `ViewerPanel.java` - JPanel with custom graphics rendering

**Migration Priority:** 🟢 **LOW** - Debug tool, may be acceptable to keep as Swing

---

## 🔍 Mixed/Unclear Status

### sokybot-engine
**Status:** ⚠️ **MAY HAVE SWING DEPENDENCIES**

**Findings:**
- Has Swing dependencies in `pom.xml` (swingx-all, flatlaf)
- May have Swing components in `machine/page/` directories (duplicates of sokybot-ui)
- Needs investigation to determine if actively used

**Migration Priority:** 🟡 **MEDIUM** - Needs verification

### sokybot-commons
**Status:** ⚠️ **UTILITY CLASSES**

**Swing Code:**
- `SwingUtils.java` - Utility methods for Swing

**Migration Priority:** 🟢 **LOW** - Utility class, may be kept for compatibility

---

## 📊 Migration Statistics

| Bundle | Status | Swing Classes | Webview Ready |
|--------|--------|---------------|---------------|
| sokybot-packet-sniffer | ✅ Migrated | 0 (cleaned) | ✅ Yes |
| sokybot-webview | ✅ No Swing | 0 | ✅ Yes |
| sokybot-ui | ❌ Swing | ~50+ | ❌ No |
| sokybot-ui-api | ❌ Swing API | 2 interfaces | ❌ No |
| sokybot-runtime | ⚠️ References | 0 (references only) | ⚠️ Partial |
| sokybot-builders | ❌ Swing | ~6 | ❌ No |
| sokybot-map-debug | ❌ Swing | 1 | ❌ No |
| sokybot-engine | ⚠️ Unclear | ? | ⚠️ Unknown |
| sokybot-commons | ⚠️ Utility | 1 | ✅ N/A |

---

## 🗑️ Cleanup Completed

### Removed from sokybot-packet-sniffer:
1. ✅ `PacketSniffer.java` - Old JTabbedPane component (replaced by declarative UI)
2. ✅ `TrafficMonitor.java` - Swing JPanel table component (replaced by React table)
3. ✅ `PacketTracer.java` - Swing JPanel tracer component (replaced by React UI)
4. ✅ `PacketMonitorHandler.java` - Swing ActionEvent handler (replaced by action handlers)
5. ✅ `IPacketMonitorHandler.java` - Swing handler interface (no longer needed)
6. ✅ `PacketTracerHandler.java` - Swing tracer handler (replaced by action handlers)
7. ✅ `IPacketTracerHandler.java` - Swing tracer handler interface (no longer needed)
8. ✅ `TrafficMonitorCellRender.java` - Swing table cell renderer (replaced by React styling)
9. ✅ `TestPacketSniffer.java` - Test file using old Swing component
10. ✅ `Driver.java` - Driver file using old Swing component

**Total Removed:** 10 Swing-related files

### Verified Clean:
- ✅ No Swing imports in `PacketSnifferService.java`
- ✅ No Swing imports in `PacketSnifferActivator.java`
- ✅ `pom.xml` excludes Swing packages
- ✅ No Swing dependencies in Maven dependencies

---

## 📝 Migration Roadmap

### Phase 1: Core Migration (COMPLETED)
- ✅ Create webview extension API
- ✅ Migrate packet-sniffer bundle
- ✅ Remove unused Swing components
- ✅ Clean up dependencies

### Phase 2: Main UI Migration (TODO)
- [ ] Migrate `sokybot-ui` bundle to webview
  - [ ] Convert SkillTab to declarative UI
  - [ ] Convert TrainingPage to declarative UI
  - [ ] Convert ItemPage to declarative UI
  - [ ] Convert EnvTab to declarative UI
  - [ ] Convert MachineDashboard to React components
  - [ ] Convert MachineControll to React components

### Phase 3: API Updates (TODO)
- [ ] Update or deprecate `IMachinePageViewer` interface
- [ ] Create webview-based page viewer interface
- [ ] Update `sokybot-runtime` to support webview API
- [ ] Maintain backward compatibility during transition

### Phase 4: Optional Migrations (FUTURE)
- [ ] Consider migrating `sokybot-builders` dialogs (low priority)
- [ ] Consider migrating `sokybot-map-debug` viewer (low priority)
- [ ] Clean up `sokybot-engine` Swing dependencies (if any)

---

## 🔧 Technical Notes

### Webview Extension API
- **Interface:** `org.sokybot.webview.api.IWebviewConfigurator`
- **Utility:** `org.sokybot.webview.api.util.SchemaLoader`
- **Communication:** RSocket (WebSocket-based)
- **Frontend:** React + TypeScript + Tailwind CSS + shadcn/ui

### Old Swing API
- **Interface:** `org.sokybot.ui.api.IMachinePageViewer`
- **Communication:** Direct Java method calls
- **Components:** JPanel, JTabbedPane, JTable, etc.

### Migration Pattern
1. Create JSON schema files in `/ui/` resources
2. Use `SchemaLoader.loadSchema()` to load schemas
3. Register page with `IWebviewConfigurator.addDeclarativePage()`
4. Register action handlers for user interactions
5. Register stream handlers for real-time data
6. Remove old Swing components

---

## ✅ Success Criteria

- [x] No unused Swing classes in migrated bundles
- [x] All Swing imports removed from migrated code
- [x] Webview integration verified working
- [x] Build succeeds without errors
- [x] Migration status documented
- [x] Clear separation between old Swing system and new webview system

---

## 📚 References

- **Webview Extension API:** `sokybot-webview/src/main/java/org/sokybot/webview/api/`
- **Example Migration:** `sokybot-packet-sniffer` (fully migrated)
- **Schema Examples:** `sokybot-packet-sniffer/src/main/resources/ui/`
- **Testing Strategy:** `sokybot-webview/TESTING_STRATEGY.md`
