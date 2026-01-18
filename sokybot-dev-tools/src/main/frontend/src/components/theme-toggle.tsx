import { Moon, Sun } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useEffect, useState } from "react"

export function ThemeToggle() {
    const [theme, setTheme] = useState<"light" | "dark">("light")

    useEffect(() => {
        // Check initial preference
        if (
            localStorage.theme === 'dark' ||
            (!('theme' in localStorage) &&
                window.matchMedia('(prefers-color-scheme: dark)').matches)
        ) {
            setTheme("dark")
            document.documentElement.classList.add('dark')
        } else {
            setTheme("light")
            document.documentElement.classList.remove('dark')
        }
    }, [])

    const toggleTheme = () => {
        if (theme === "dark") {
            setTheme("light")
            document.documentElement.classList.remove("dark")
            localStorage.theme = "light"
        } else {
            setTheme("dark")
            document.documentElement.classList.add("dark")
            localStorage.theme = "dark"
        }
    }

    return (
        <Button variant="ghost" size="icon" onClick={toggleTheme} className="w-full justify-start px-2">
            {theme === "dark" ? (
                <>
                    <Sun className="h-[1.2rem] w-[1.2rem] mr-2" />
                    <span>Light Mode</span>
                </>
            ) : (
                <>
                    <Moon className="h-[1.2rem] w-[1.2rem] mr-2" />
                    <span>Dark Mode</span>
                </>
            )}
        </Button>
    )
}
