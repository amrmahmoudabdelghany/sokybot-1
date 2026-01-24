import { Package, Activity, FileText, Server, Settings, Database } from "lucide-react"
import { cn } from "@sokybot/frontend-shared"
import { Button } from "@sokybot/frontend-shared"
import { ThemeToggle } from "../theme-toggle"

interface SidebarProps extends React.HTMLAttributes<HTMLDivElement> {
    activeView: string
    onViewChange: (view: string) => void
}

export function Sidebar({ className, activeView, onViewChange }: SidebarProps) {
    return (
        <div className={cn("pb-12 w-64 border-r bg-card h-full flex flex-col", className)}>
            <div className="space-y-4 py-4 flex-1">
                <div className="px-3 py-2">
                    <div className="flex items-center gap-2 mb-6 px-4">
                        <Settings className="h-6 w-6 text-primary" />
                        <h2 className="text-lg font-semibold tracking-tight">
                            DevTools
                        </h2>
                    </div>
                    <div className="space-y-1">
                        <Button
                            variant={activeView === "bundles" ? "secondary" : "ghost"}
                            className="w-full justify-start"
                            onClick={() => onViewChange("bundles")}
                        >
                            <Package className="mr-2 h-4 w-4" />
                            Bundles
                        </Button>
                        <Button
                            variant={activeView === "services" ? "secondary" : "ghost"}
                            className="w-full justify-start"
                            onClick={() => onViewChange("services")}
                        >
                            <Server className="mr-2 h-4 w-4" />
                            Services
                        </Button>
                        <Button
                            variant={activeView === "metrics" ? "secondary" : "ghost"}
                            className="w-full justify-start"
                            onClick={() => onViewChange("metrics")}
                        >
                            <Activity className="mr-2 h-4 w-4" />
                            Metrics
                        </Button>
                        <Button
                            variant={activeView === "logs" ? "secondary" : "ghost"}
                            className="w-full justify-start"
                            onClick={() => onViewChange("logs")}
                        >
                            <FileText className="mr-2 h-4 w-4" />
                            Logs
                        </Button>
                        <Button
                            variant={activeView === "database" ? "secondary" : "ghost"}
                            className="w-full justify-start"
                            onClick={() => onViewChange("database")}
                        >
                            <Database className="mr-2 h-4 w-4" />
                            Database
                        </Button>
                    </div>
                </div>
            </div>
            <div className="px-3 py-2">
                <div className="space-y-1">
                    <ThemeToggle />
                </div>
            </div>
        </div>
    )
}
