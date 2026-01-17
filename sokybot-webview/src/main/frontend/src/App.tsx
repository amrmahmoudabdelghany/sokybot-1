import { useState } from 'react'
import { Layout } from './Layout'
import { MachineView } from './MachineView'
import './App.css'

function App() {
  const [selectedMachine, setSelectedMachine] = useState<string>('');

  return (
    <Layout
      selectedMachine={selectedMachine}
      onMachineSelect={setSelectedMachine}
    >
      {selectedMachine ? (
        <MachineView machineId={selectedMachine} />
      ) : (
        <div className="flex items-center justify-center h-full text-slate-500">
          Select a machine from the sidebar to view details
        </div>
      )}
    </Layout>
  )
}

export default App
