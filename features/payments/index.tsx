import EntityPage from '../shared/EntityPage'
import { entityConfigs } from '../shared/config'
import PaymentMethodsPanel from './PaymentMethodsPanel'

export default function PaymentsPage() {
  return <div className="module-page"><PaymentMethodsPanel /><EntityPage config={entityConfigs.payments} /></div>
}
