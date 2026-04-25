import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import App from './App.tsx'
import { ThemeProvider } from './components/theme-provider'
import { createAppQueryClient } from './query/queryClient'
import { TooltipProvider } from '@sokybot/frontend-shared'
import { ToastContainer } from './components/toast/ToastContainer'
import { RSocketProvider } from './RSocketProvider'

const queryClient = createAppQueryClient()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme">
      <TooltipProvider delayDuration={300}>
        <QueryClientProvider client={queryClient}>
          <RSocketProvider>
            <App />
          </RSocketProvider>
        </QueryClientProvider>
        <ToastContainer />
      </TooltipProvider>
    </ThemeProvider>
  </StrictMode>,
)
