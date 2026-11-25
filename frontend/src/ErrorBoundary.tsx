import { House, RotateCcw, TriangleAlert } from 'lucide-react'
import { Component, type ErrorInfo, type ReactNode } from 'react'

type Props = { children: ReactNode; scope: 'app' | 'page'; onHome?: () => void }
type State = { error: Error | null }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unexpected UI error', error, info.componentStack)
  }

  private reset = () => this.setState({ error: null })

  render() {
    if (!this.state.error) return this.props.children
    const page = this.props.scope === 'page'
    return (
      <div className={page ? 'crash crash-page' : 'crash crash-app'} role="alert">
        <span className="empty-icon">
          <TriangleAlert size={22} />
        </span>
        <h2>{page ? 'This page ran into a problem' : 'Something went wrong'}</h2>
        <p>
          {page
            ? 'The rest of the app still works. Try loading the page again or go back home.'
            : 'TaskTracker hit an unexpected error. Reload the page to continue.'}
        </p>
        <div className="row">
          <button
            type="button"
            className="button primary"
            onClick={() => {
              if (page) this.reset()
              else window.location.reload()
            }}>
            <RotateCcw size={16} />
            {page ? 'Try again' : 'Reload'}
          </button>
          {page && this.props.onHome && (
            <button
              type="button"
              className="button"
              onClick={() => {
                this.reset()
                this.props.onHome?.()
              }}>
              <House size={16} />
              Go home
            </button>
          )}
        </div>
      </div>
    )
  }
}
