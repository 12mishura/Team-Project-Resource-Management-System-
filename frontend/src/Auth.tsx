import { ArrowRight } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { register, signIn } from './api'
import { useAction } from './hooks'
import { Brand, ErrorNotice } from './ui'

export function Auth({ logoutWarning, passwordChanged }: { logoutWarning: string; passwordChanged: boolean }) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  return (
    <main className="auth-shell">
      <section className="auth-aside">
        <Brand />
        <div>
          <h1>
            Your team’s work
            <br />
            in one place
          </h1>
          <p>Switch between organizations, manage projects, and track issues.</p>
        </div>
      </section>
      <section className="auth-main">
        {passwordChanged && (
          <p className="success-message" role="status">
            Password changed. Sign in with your new password.
          </p>
        )}
        <AuthForm key={mode} mode={mode} onSwitch={() => setMode(mode === 'login' ? 'register' : 'login')} warning={logoutWarning} />
      </section>
    </main>
  )
}

function AuthForm({ mode, onSwitch, warning }: { mode: 'login' | 'register'; onSwitch: () => void; warning: string }) {
  const creating = mode === 'register'
  const { run, busy, error } = useAction()
  const [registeredEmail, setRegisteredEmail] = useState('')

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const email = String(data.get('email')).trim()
    const password = String(data.get('password'))
    void run(async () => {
      if (creating && registeredEmail !== email) {
        await register({ username: String(data.get('username')).trim(), email, password })
        setRegisteredEmail(email)
      }
      await signIn(email, password)
    })
  }

  return (
    <>
      <h2>{creating ? 'Create an account' : 'Sign in to TaskTracker'}</h2>
      <form className="form" onSubmit={submit}>
        {creating && (
          <label>
            Username
            <input name="username" autoComplete="username" minLength={3} maxLength={50} required />
          </label>
        )}
        <label>
          Email
          <input name="email" type="email" autoComplete="email" maxLength={255} required />
        </label>
        <label>
          Password
          <input
            name="password"
            type="password"
            autoComplete={creating ? 'new-password' : 'current-password'}
            minLength={creating ? 8 : undefined}
            required
          />
        </label>
        <ErrorNotice text={error || warning} />
        <button className="button primary" disabled={busy}>
          {busy ? 'Please wait…' : creating ? 'Sign up' : 'Sign in'} <ArrowRight size={16} />
        </button>
      </form>
      <button className="text-button" type="button" disabled={busy} onClick={onSwitch}>
        {creating ? 'Already have an account?' : 'Create an account'}
      </button>
    </>
  )
}
