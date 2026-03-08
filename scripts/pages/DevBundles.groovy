import org.sokybot.machinepages.api.BasePage
import org.sokybot.webview.api.IBundleAdminService

class DevBundlesPage extends BasePage {

    private IBundleAdminService bundleAdmin
    private List<Map<String, Object>> bundles = []
    private String searchTerm = ""
    private boolean loading = false
    private String error = null

    DevBundlesPage() { super("Dev Bundles", "Package") }

    @Override
    void setup() {
        try {
            this.bundleAdmin = service(IBundleAdminService)
            if (this.bundleAdmin == null) {
                log.warn("IBundleAdminService not available; Dev Bundles page will be empty")
            }
        } catch (Throwable t) {
            log.error("Failed to resolve IBundleAdminService", t)
        }
        refresh()
    }

    @Override
    Map<String, Object> getInitialState() { state() }

    @Override
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        try {
            switch (action) {
                case "refreshBundles": refresh(); break
                case "setBundlesSearch": this.searchTerm = (data?.get("value") ?: "").toString(); break
                case "startBundle":
                    def id = data?.get("id") as Number
                    if (id != null && bundleAdmin != null) { bundleAdmin.startBundle(id.longValue()); refresh() }
                    break
                case "stopBundle":
                    def id = data?.get("id") as Number
                    if (id != null && bundleAdmin != null) { bundleAdmin.stopBundle(id.longValue()); refresh() }
                    break
                case "restartBundle":
                    def id = data?.get("id") as Number
                    if (id != null && bundleAdmin != null) { bundleAdmin.restartBundle(id.longValue()); refresh() }
                    break
                case "reloadBundle":
                    def sym = data?.get("symbolicName") as String
                    if (sym && bundleAdmin != null) { bundleAdmin.reloadBundleFromFileSystem(sym); refresh() }
                    break
            }
            return withSuccess(state())
        } catch (Throwable t) {
            log.error("Error handling DevBundles action {}", action, t)
            return withError(t.message)
        }
    }

    private void refresh() {
        if (bundleAdmin == null) { this.error = "Bundle admin service not available"; this.bundles = []; return }
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
        return [bundles: bundles, filteredBundles: filtered, loading: loading, searchTerm: searchTerm, error: error]
    }
}

new DevBundlesPage()
