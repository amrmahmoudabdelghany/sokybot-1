import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.runtime.IMachineContext
import org.sokybot.webview.api.IBundleAdminService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DevBundlesPage implements IScriptedPage {

    private static final Logger log = LoggerFactory.getLogger(DevBundlesPage.class)

    private IMachineContext machineContext
    private IBundleAdminService bundleAdmin

    private List<Map<String, Object>> bundles = []
    private String searchTerm = ""
    private boolean loading = false
    private String error = null

    String getTitle() { "Dev Bundles" }

    String getIcon() { "Package" }

    @Override
    void init(IMachineContext context) {
        this.machineContext = context
        try {
            def sokyCtx = context.getSokybotContext()
            this.bundleAdmin = sokyCtx?.getService(IBundleAdminService.class)
            if (this.bundleAdmin == null) {
                log.warn("IBundleAdminService not available; Dev Bundles page will be empty")
            }
        } catch (Throwable t) {
            log.error("Failed to resolve IBundleAdminService", t)
        }
        refresh()
    }

    Map<String, Object> getSchema() {
        // Loaded from DevBundles.json via ScriptPageLoader
        return [:]
    }

    Map<String, Object> getInitialState() {
        state()
    }

    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        log.debug("DevBundlesPage action: {} data: {}", action, data)
        try {
            switch (action) {
                case "refreshBundles":
                    refresh()
                    break
                case "setBundlesSearch":
                    this.searchTerm = (data?.get("value") ?: "").toString()
                    break
                case "startBundle":
                    def id = data?.get("id") as Number
                    if (id != null && bundleAdmin != null) {
                        bundleAdmin.startBundle(id.longValue())
                        refresh()
                    }
                    break
                case "stopBundle":
                    def id2 = data?.get("id") as Number
                    if (id2 != null && bundleAdmin != null) {
                        bundleAdmin.stopBundle(id2.longValue())
                        refresh()
                    }
                    break
                case "restartBundle":
                    def id3 = data?.get("id") as Number
                    if (id3 != null && bundleAdmin != null) {
                        bundleAdmin.restartBundle(id3.longValue())
                        refresh()
                    }
                    break
                case "reloadBundle":
                    def sym = data?.get("symbolicName") as String
                    if (sym && bundleAdmin != null) {
                        bundleAdmin.reloadBundleFromFileSystem(sym)
                        refresh()
                    }
                    break
            }
        } catch (Throwable t) {
            log.error("Error handling DevBundles action {}", action, t)
            this.error = t.message
        }
        return state()
    }

    reactor.core.publisher.Flux<Map<String, Object>> streamData(String streamId, Map<String, Object> params) {
        // No reactive stream; state is refreshed via actions only.
        return reactor.core.publisher.Flux.empty()
    }

    private void refresh() {
        if (bundleAdmin == null) {
            this.error = "Bundle admin service not available"
            this.bundles = []
            return
        }
        this.loading = true
        try {
            this.error = null
            this.bundles = bundleAdmin.listBundles()
        } catch (Throwable t) {
            log.error("Failed to load bundles", t)
            this.error = t.message
            this.bundles = []
        } finally {
            this.loading = false
        }
    }

    private Map<String, Object> state() {
        List<Map<String, Object>> filtered = bundles ?: []
        if (searchTerm && !searchTerm.isEmpty()) {
            String term = searchTerm.toLowerCase()
            filtered = filtered.findAll { b ->
                (b.symbolicName?.toString()?.toLowerCase()?.contains(term)) ||
                (b.location?.toString()?.toLowerCase()?.contains(term))
            }
        }
        return [
            bundles        : bundles,
            filteredBundles: filtered,
            loading        : loading,
            searchTerm     : searchTerm,
            error          : error
        ]
    }
}

new DevBundlesPage()

